package com.xpspeak.app

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

/**
 * Diagnóstico del micrófono del emulador: abre y cierra el micrófono varias veces seguidas
 * (como hace el chat en vivo en cada turno) y comprueba que cada vez llega audio real y no
 * silencio. Sirve para saber si el emulador está pasando el micrófono de la Mac.
 * Hay que hacer ruido mientras corre: `scripts/emulador/emulador.sh --probar` lo lanza y pone
 * a hablar a la Mac. No va con las pruebas normales (en silencio fallaría).
 */
@RunWith(AndroidJUnit4::class)
class MicrofonoEmuladorTest {

    @SuppressLint("MissingPermission")
    @Test
    fun elMicrofonoEntregaAudioEnCadaApertura() {
        val instrumentacion = InstrumentationRegistry.getInstrumentation()
        instrumentacion.uiAutomation.grantRuntimePermission(
            instrumentacion.targetContext.packageName, Manifest.permission.RECORD_AUDIO,
        )
        val minimo = AudioRecord.getMinBufferSize(HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val niveles = (1..RONDAS).map { ronda ->
            val grabadora = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, HZ,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minimo * 4,
            )
            assertEquals("Ronda $ronda: el micrófono no abrió", AudioRecord.STATE_INITIALIZED, grabadora.state)
            val muestras = ShortArray(HZ * SEGUNDOS)
            var leidas = 0
            try {
                grabadora.startRecording()
                while (leidas < muestras.size) {
                    val n = grabadora.read(muestras, leidas, muestras.size - leidas)
                    assertTrue("Ronda $ronda: error $n al leer del micrófono", n > 0)
                    leidas += n
                }
            } finally {
                grabadora.release()
            }
            val rms = sqrt(muestras.sumOf { it.toDouble() * it } / muestras.size)
            val pico = muestras.maxOf { kotlin.math.abs(it.toInt()) }
            Log.i(ETIQUETA, "ronda=$ronda rms=${rms.toInt()} pico=$pico")
            Thread.sleep(PAUSA_MS)
            rms
        }
        niveles.forEachIndexed { i, rms ->
            assertTrue("Ronda ${i + 1}: solo llegó silencio (rms=${rms.toInt()}); el emulador no está pasando el micrófono", rms > RMS_MINIMO)
        }
    }

    private companion object {
        const val ETIQUETA = "MicrofonoEmulador"
        const val HZ = 16_000
        const val SEGUNDOS = 2
        const val RONDAS = 5
        const val PAUSA_MS = 700L
        const val RMS_MINIMO = 30.0
    }
}
