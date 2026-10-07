package com.xpspeak.app.feature.games.ui.comun

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Lee palabras y oraciones en inglés con el TextToSpeech del teléfono
 * (§4.5), igual que el campo `audio: "tts:..."` de las lecciones. Funciona
 * sin conexión si la voz está instalada.
 */
class Locutor internal constructor() {
    internal var tts: TextToSpeech? = null
    internal var listo = false

    fun decir(texto: String, lento: Boolean = false) {
        val motor = tts?.takeIf { listo } ?: return
        motor.setSpeechRate(if (lento) 0.7f else 0.95f)
        motor.speak(texto, TextToSpeech.QUEUE_FLUSH, null, texto.hashCode().toString())
    }

    fun callar() {
        tts?.stop()
    }
}

@Composable
fun rememberLocutor(): Locutor {
    val context = LocalContext.current
    val locutor = remember { Locutor() }
    DisposableEffect(context) {
        locutor.tts = TextToSpeech(context.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                locutor.tts?.language = Locale.US
                locutor.listo = true
            }
        }
        onDispose {
            locutor.listo = false
            locutor.tts?.shutdown()
            locutor.tts = null
        }
    }
    return locutor
}
