package com.xpspeak.app.feature.games.ui.comun

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
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

    /** Aviso pendiente de "ya terminé" y el id de la última frase encolada que lo dispara. */
    private var alTerminar: (() -> Unit)? = null
    @Volatile private var ultimaFrase: String? = null

    internal val progreso = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}
        override fun onDone(utteranceId: String?) {
            if (utteranceId == ultimaFrase) avisarFin()
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            if (utteranceId == ultimaFrase) avisarFin()
        }
    }

    /**
     * Para el chat: la IA a veces explica en español. Cada oración se lee con la
     * voz de su idioma; con la voz en inglés el español no se entiende.
     * [alTerminar] se llama al acabar de hablar (chat en vivo: vuelve a abrir el micrófono).
     */
    fun decirConIdiomas(texto: String, alTerminar: (() -> Unit)? = null) {
        hablar(fragmentosPorIdioma(texto), lento = false, alTerminar)
    }

    private fun hablar(fragmentos: List<Fragmento>, lento: Boolean, alTerminar: (() -> Unit)? = null) {
        // Si se repite otra frase mientras hay un aviso pendiente, el aviso espera a que acabe la nueva.
        synchronized(this) { if (alTerminar != null) this.alTerminar = alTerminar }
        val motor = tts?.takeIf { listo }
        if (motor == null || fragmentos.isEmpty()) return avisarFin()
        // 0.5: a 0.7 la diferencia casi no se notaba en palabras sueltas.
        motor.setSpeechRate(if (lento) 0.5f else 0.95f)
        // Varios motores (Samsung, algunas voces de Google) ignoran setSpeechRate: en modo
        // lento además se dice palabra por palabra con una pausa, que sí se nota en cualquiera.
        val piezas = if (lento) fragmentos.flatMap { f -> palabrasDe(f.texto).map { Fragmento(it, f.esEspanol) } } else fragmentos
        piezas.forEachIndexed { i, fragmento ->
            // El idioma se toma al encolar cada frase, así que puede cambiar entre una y otra.
            motor.language = espanol?.takeIf { fragmento.esEspanol } ?: Locale.US
            val modo = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = "${fragmento.texto.hashCode()}-$i-${System.nanoTime()}"
            if (i == piezas.lastIndex) ultimaFrase = id
            motor.speak(fragmento.texto, modo, null, id)
            if (lento && i < piezas.lastIndex) {
                motor.playSilentUtterance(PAUSA_ENTRE_PALABRAS_MS, TextToSpeech.QUEUE_ADD, "pausa-$i-${System.nanoTime()}")
            }
        }
    }

    fun callar() {
        tts?.stop()
        avisarFin()
    }

    private fun avisarFin() {
        val aviso = synchronized(this) { alTerminar.also { alTerminar = null } }
        aviso?.invoke()
    }
}

internal data class Fragmento(val texto: String, val esEspanol: Boolean)

/** Silencio entre palabras en modo lento ("🐢 Más lento" de Eco Vocal). */
internal const val PAUSA_ENTRE_PALABRAS_MS = 350L

/** "Good morning!" → ["Good", "morning!"]: la puntuación se queda pegada para conservar la entonación. */
internal fun palabrasDe(texto: String): List<String> = texto.split(Regex("\\s+")).filter { it.isNotBlank() }

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
                motor.setOnUtteranceProgressListener(locutor.progreso)
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
