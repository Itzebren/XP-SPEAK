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
    /** Voz en español instalada en el teléfono, o null si no hay ninguna. */
    internal var espanol: Locale? = null

    fun decir(texto: String, lento: Boolean = false) {
        hablar(listOf(Fragmento(texto, esEspanol = false)), lento)
    }

    /**
     * Para el chat: la IA a veces explica en español. Cada oración se lee con la
     * voz de su idioma; con la voz en inglés el español no se entiende.
     */
    fun decirConIdiomas(texto: String) {
        hablar(fragmentosPorIdioma(texto), lento = false)
    }

    private fun hablar(fragmentos: List<Fragmento>, lento: Boolean) {
        val motor = tts?.takeIf { listo } ?: return
        // 0.5: a 0.7 la diferencia casi no se notaba en palabras sueltas.
        motor.setSpeechRate(if (lento) 0.5f else 0.95f)
        fragmentos.forEachIndexed { i, fragmento ->
            // El idioma se toma al encolar cada frase, así que puede cambiar entre una y otra.
            motor.language = espanol?.takeIf { fragmento.esEspanol } ?: Locale.US
            val modo = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            motor.speak(fragmento.texto, modo, null, "${fragmento.texto.hashCode()}-$i")
        }
    }

    fun callar() {
        tts?.stop()
    }
}

internal data class Fragmento(val texto: String, val esEspanol: Boolean)

private val PALABRAS_ES = setOf(
    "el", "la", "los", "las", "de", "del", "que", "en", "y", "es", "son", "un", "una", "por", "para", "con",
    "como", "pero", "muy", "más", "tu", "tú", "su", "se", "lo", "esta", "este", "está", "puedes", "significa",
    "palabra", "oración", "ejemplo", "también", "cuando", "porque", "hola", "gracias", "bien", "usa", "decir",
    "quiere", "pregunta", "respuesta", "inglés", "español"
)

private val PALABRAS_EN = setOf(
    "the", "is", "are", "you", "your", "to", "and", "of", "it", "i", "my", "what", "do", "does", "how", "that",
    "this", "for", "with", "can", "have", "like", "we", "be", "was", "great", "good", "let's", "say", "an", "at"
)

/**
 * Parte el texto en oraciones y decide si cada una está en español: cuentan las
 * palabras frecuentes de cada idioma y, para el español, los acentos, la ñ y ¿ ¡.
 * Una oración sin pistas ("OK!") sigue el idioma de la anterior.
 */
internal fun fragmentosPorIdioma(texto: String): List<Fragmento> {
    var anterior = false
    return texto.split(Regex("(?<=[.!?:;\\n])\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .map { oracion ->
            val palabras = oracion.lowercase().split(Regex("[^\\p{L}']+")).filter { it.isNotEmpty() }
            val es = palabras.count { it in PALABRAS_ES } + 2 * oracion.count { it in "áéíóúñ¿¡" }
            val en = palabras.count { it in PALABRAS_EN }
            val esEspanol = if (es == en) anterior else es > en
            anterior = esEspanol
            Fragmento(oracion, esEspanol)
        }
}

@Composable
fun rememberLocutor(): Locutor {
    val context = LocalContext.current
    val locutor = remember { Locutor() }
    DisposableEffect(context) {
        locutor.tts = TextToSpeech(context.applicationContext) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                val motor = locutor.tts ?: return@TextToSpeech
                motor.language = Locale.US
                locutor.espanol = listOf("es-MX", "es-US", "es-ES", "es")
                    .map(Locale::forLanguageTag)
                    .firstOrNull { motor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
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
