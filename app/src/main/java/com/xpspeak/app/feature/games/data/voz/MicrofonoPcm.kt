package com.xpspeak.app.feature.games.data.voz

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lee el micrófono como PCM de 16 kHz / 16 bits / mono (el formato que pide Azure), le aplica
 * [GananciaAutomatica] (la fuente VOICE_RECOGNITION no trae AGC y en tablets la voz llegaba casi
 * muda) y lo empuja al stream del reconocedor, avisando el volumen a la [SesionEscucha] para las
 * barras de «te escucho». Al [cerrar] se suelta el micrófono y se cierra el stream: Azure
 * entiende que la frase terminó y califica lo que ya recibió. Nada se guarda (RN-04).
 */
internal class MicrofonoPcm(
    private val flujo: PushAudioInputStream,
    private val sesion: SesionEscucha,
    /**
     * Chat en vivo: la IA acaba de hablar por la bocina y el micrófono se abre enseguida. Si el
     * teléfono trae cancelador de eco se activa, para no «oír» la cola de su propia respuesta.
     */
    private val cancelarEco: Boolean = false
) {
    private var grabadora: AudioRecord? = null
    private var canceladorEco: AcousticEchoCanceler? = null
    @Volatile private var abierto = false
    private val cerrado = AtomicBoolean(false)
    private val ganancia = GananciaAutomatica()

    /** La voz llegó muy bajita aun antes de amplificarla: se sugiere acercarse al micrófono. */
    val vozBaja: Boolean
        get() = ganancia.rmsMaximoOriginal in 1.0..GananciaAutomatica.RMS_VOZ_BAJA

    /**
     * Abre el micrófono. Lanza [SecurityException] sin permiso y [EvaluadorAzure.FalloAzure] si
     * el teléfono no lo da en este formato (el respaldo del teléfono usa su propio micrófono).
     */
    fun abrir() {
        val minimo = AudioRecord.getMinBufferSize(FRECUENCIA_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val tamano = maxOf(minimo, BYTES_POR_SEGUNDO / 5) // ≥ 200 ms de margen
        val nueva = try {
            AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, FRECUENCIA_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, tamano)
        } catch (error: SecurityException) {
            throw error
        } catch (error: Exception) {
            throw EvaluadorAzure.FalloAzure("No se pudo abrir el micrófono: ${error.message}")
        }
        if (nueva.state != AudioRecord.STATE_INITIALIZED) {
            nueva.release()
            throw EvaluadorAzure.FalloAzure("El micrófono no está disponible en 16 kHz")
        }
        if (cancelarEco && AcousticEchoCanceler.isAvailable()) {
            // No todos los teléfonos lo aplican a esta fuente de audio: si falla, se sigue sin él.
            canceladorEco = runCatching { AcousticEchoCanceler.create(nueva.audioSessionId)?.apply { enabled = true } }.getOrNull()
        }
        nueva.startRecording()
        grabadora = nueva
        abierto = true
    }

    /** Bucle de lectura: corre en un hilo de E/S hasta que se cierre el micrófono. */
    fun transmitir() {
        val lector = grabadora ?: return
        val muestras = ShortArray(FRECUENCIA_HZ / 20) // 50 ms: las barras se mueven con la voz
        val bytes = ByteArray(muestras.size * 2)
        while (abierto) {
            val leidas = lector.read(muestras, 0, muestras.size)
            if (leidas < 0) break // el micrófono se soltó
            if (leidas == 0) continue
            ganancia.procesar(muestras, leidas)
            sesion.nivel(NivelAudio.deMuestras(muestras, leidas))
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(muestras, 0, leidas)
            if (!abierto) break
            // Si el stream ya se cerró (botón «Listo»), escribir falla: solo se deja de leer.
            if (runCatching { flujo.write(if (leidas == muestras.size) bytes else bytes.copyOf(leidas * 2)) }.isFailure) break
        }
    }

    /** «Listo» o fin del intento: suelta el micrófono y cierra el stream (solo la primera vez). */
    fun cerrar() {
        if (!cerrado.compareAndSet(false, true)) return
        abierto = false
        grabadora?.let { lector ->
            runCatching { lector.stop() }
            runCatching { lector.release() }
        }
        grabadora = null
        canceladorEco?.let { runCatching { it.release() } }
        canceladorEco = null
        runCatching { flujo.close() }
    }

    companion object {
        const val FRECUENCIA_HZ = 16_000
        private const val BYTES_POR_SEGUNDO = FRECUENCIA_HZ * 2
    }
}
