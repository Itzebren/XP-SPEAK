package com.xpspeak.app.feature.games.ui.comun

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Lee palabras y oraciones en inglés con el TextToSpeech del teléfono
 * (§4.5), igual que el campo `audio: "tts:..."` de las lecciones. Funciona
 * sin conexión si la voz está instalada.
 *
 * Hay un solo motor para toda la app ([LocutorApp]): arrancarlo tarda de 1 a 3 s y antes
 * cada pantalla creaba el suyo, así que la primera frase de Eco Vocal (y un «Escuchar»
 * tocado apenas al entrar) se perdían en silencio. Lo que se pide antes de que esté listo
 * se guarda y se dice en cuanto arranca. Si el teléfono no tiene voz en inglés o el motor
 * falla, [problema] lo explica para que la pantalla lo muestre en vez de callar.
 */
class Locutor internal constructor() {
    internal var tts: TextToSpeech? = null
    internal var listo = false
    /** Voz en español instalada en el teléfono, o null si no hay ninguna. */
    internal var espanol: Locale? = null

    private val _problema = MutableStateFlow<String?>(null)
    /** Por qué no se oye la voz, o null si todo va bien. */
    val problema: StateFlow<String?> = _problema.asStateFlow()

    /** Lo último que se pidió decir antes de que el motor estuviera listo. */
    private var pendiente: (() -> Unit)? = null

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
            Log.w(TAG, "El motor de voz falló al leer ($utteranceId)")
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
        if (fragmentos.isEmpty()) return avisarFin()
        val motor = tts
        if (motor == null || !listo) {
            // Todavía arranca (o se está reiniciando): se dice en cuanto esté; si nunca está,
            // iniciar() avisa el fin para que el chat en vivo no se quede esperando.
            pendiente = { hablar(fragmentos, lento, null) }
            Log.i(TAG, "Voz aún no lista: se guarda la frase para decirla al arrancar")
            return
        }
        // 0.5: a 0.7 la diferencia casi no se notaba en palabras sueltas.
        motor.setSpeechRate(if (lento) 0.5f else 0.95f)
        // Varios motores (Samsung, algunas voces de Google) ignoran setSpeechRate: en modo
        // lento además se dice palabra por palabra con una pausa, que sí se nota en cualquiera.
        val piezas = if (lento) fragmentos.flatMap { f -> palabrasDe(f.texto).map { Fragmento(it, f.esEspanol) } } else fragmentos
        piezas.forEachIndexed { i, fragmento ->
            // El idioma se toma al encolar cada frase, así que puede cambiar entre una y otra.
            val idioma = espanol?.takeIf { fragmento.esEspanol } ?: Locale.US
            val disponible = motor.setLanguage(idioma)
            if (idioma == Locale.US && disponible < TextToSpeech.LANG_AVAILABLE) {
                Log.w(TAG, "Sin voz en inglés (setLanguage=$disponible)")
                _problema.value = SIN_VOZ_INGLES
            }
            val modo = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val id = "${fragmento.texto.hashCode()}-$i-${System.nanoTime()}"
            if (i == piezas.lastIndex) ultimaFrase = id
            val resultado = motor.speak(fragmento.texto, modo, null, id)
            if (resultado != TextToSpeech.SUCCESS) {
                Log.w(TAG, "speak devolvió $resultado para «${fragmento.texto}»")
                _problema.value = MOTOR_FALLO
                if (i == piezas.lastIndex) avisarFin()
            }
            if (lento && i < piezas.lastIndex) {
                motor.playSilentUtterance(PAUSA_ENTRE_PALABRAS_MS, TextToSpeech.QUEUE_ADD, "pausa-$i-${System.nanoTime()}")
            }
        }
    }

    /**
     * Deja de hablar. Con [avisar] se dispara el "ya terminé" pendiente (el chat en vivo vuelve
     * a abrir el micrófono); al salir de una pantalla se descarta sin avisar, para que una
     * pantalla que ya no existe no reabra nada.
     */
    fun callar(avisar: Boolean = true) {
        pendiente = null
        tts?.stop()
        if (avisar) avisarFin() else synchronized(this) { alTerminar = null }
    }

    private fun avisarFin() {
        val aviso = synchronized(this) { alTerminar.also { alTerminar = null } }
        aviso?.invoke()
    }

    /** Arranca (o vuelve a arrancar) el motor del teléfono. */
    internal fun iniciar(context: Context) {
        listo = false
        tts?.shutdown()
        _problema.value = null
        var motor: TextToSpeech? = null
        motor = TextToSpeech(context.applicationContext) { estado ->
            if (estado != TextToSpeech.SUCCESS) {
                Log.w(TAG, "TextToSpeech no arrancó (estado $estado)")
                _problema.value = MOTOR_FALLO
                pendiente = null
                avisarFin()
                return@TextToSpeech
            }
            val listoMotor = motor ?: tts ?: return@TextToSpeech
            Log.i(TAG, "Motor de voz listo: ${listoMotor.defaultEngine}")
            // Suena por el canal multimedia, como los sonidos de los juegos.
            listoMotor.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            val ingles = listoMotor.setLanguage(Locale.US)
            if (ingles < TextToSpeech.LANG_AVAILABLE) {
                Log.w(TAG, "Sin voz en inglés al arrancar (setLanguage=$ingles)")
                _problema.value = SIN_VOZ_INGLES
            }
            listoMotor.setOnUtteranceProgressListener(progreso)
            espanol = listOf("es-MX", "es-US", "es-ES", "es")
                .map(Locale::forLanguageTag)
                .firstOrNull { listoMotor.isLanguageAvailable(it) >= TextToSpeech.LANG_AVAILABLE }
            listo = true
            pendiente?.let { pendiente = null; it() }
        }
        tts = motor
    }

    companion object {
        /** Logcat: por qué no se oye la voz. */
        const val TAG = "XPS-TTS"
        const val SIN_VOZ_INGLES = "Tu teléfono no tiene instalada la voz en inglés. Ábrela en los ajustes de voz e instala «English (United States)»."
        const val MOTOR_FALLO = "La voz del teléfono no respondió. Revisa el motor en los ajustes de voz o vuelve a intentarlo."
    }
}

/** Un solo [Locutor] para toda la app, creado con el contexto de la aplicación. */
object LocutorApp {
    @Volatile private var instancia: Locutor? = null

    fun obtener(context: Context): Locutor = instancia ?: synchronized(this) {
        instancia ?: Locutor().also { it.iniciar(context); instancia = it }
    }

    /** Botón «Reintentar» del aviso de voz: vuelve a arrancar el motor (p. ej. tras instalar la voz). */
    fun reiniciar(context: Context) {
        obtener(context).iniciar(context)
    }
}

internal data class Fragmento(val texto: String, val esEspanol: Boolean)

/** Silencio entre palabras en modo lento ("Más lento" de Eco Vocal). */
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
    val locutor = remember { LocutorApp.obtener(context) }
    // Al salir de la pantalla se calla, pero el motor sigue vivo para la siguiente.
    DisposableEffect(locutor) { onDispose { locutor.callar(avisar = false) } }
    return locutor
}
