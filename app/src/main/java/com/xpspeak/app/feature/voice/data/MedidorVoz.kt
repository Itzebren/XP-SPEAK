package com.xpspeak.app.feature.voice.data

import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.xpspeak.app.core.medicion.Meta
import com.xpspeak.app.core.medicion.Tiempos

/**
 * Mide cuánto tarda Azure en devolver la evaluación después de que el usuario
 * deja de hablar (RN-05 y R01, Tabla 93). Se crea antes de empezar a reconocer,
 * para alcanzar el evento de inicio de sesión.
 *
 * [pausaMs] es el silencio que Azure espera para dar la frase por terminada
 * (Speech_SegmentationSilenceTimeoutMs). Forma parte de lo que espera el usuario,
 * así que cuenta para RN-05; para R01 se descuenta, porque no es procesamiento.
 */
class MedidorVoz(reconocedor: SpeechRecognizer, private val medida: String, private val pausaMs: Long?) {
    @Volatile private var inicioSesion = 0L

    init {
        reconocedor.sessionStarted.addEventListener { _, _ ->
            if (inicioSesion == 0L) inicioSesion = Tiempos.ahora()
        }
    }

    fun registrar(resultado: SpeechRecognitionResult) {
        val llegada = Tiempos.ahora()
        if (inicioSesion == 0L) return
        val total = runCatching {
            Tiempos.trasHablar(inicioSesion, llegada, resultado.offset.toLong(), resultado.duration.toLong())
        }.getOrNull() ?: return
        Tiempos.registrar(
            "$medida, tras dejar de hablar", total, Meta.RN05,
            pausaMs?.let { "incluye $it ms de pausa para cerrar la frase" }
        )
        pausaMs?.let {
            Tiempos.registrar("$medida, procesamiento de Azure", maxOf(0, total - it), Meta.R01, "sin la pausa de $it ms")
        }
    }
}
