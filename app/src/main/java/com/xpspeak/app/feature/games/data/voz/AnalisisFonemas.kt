package com.xpspeak.app.feature.games.data.voz

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FonemaEvaluado
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.SilabaEvaluada
import kotlin.math.roundToInt

/**
 * Califica cada palabra sonido por sonido con el JSON detallado de Azure
 * (granularidad Phoneme + NBestPhonemes en IPA).
 *
 * El AccuracyScore por palabra es muy indulgente con el acento: "party" leído
 * como en español sacaba 95. Aquí un sonido cuenta como mal dicho si su
 * precisión es baja o si el sonido que Azure oyó con más probabilidad no es el
 * esperado; una palabra con algún sonido mal queda en rojo.
 *
 * Además rescata todo lo demás que manda Azure para la retroalimentación (RF-09): sílabas,
 * prosodia (pausas de más o de menos, entonación plana), palabras dichas de más o en lugar
 * de otra, y la relación señal/ruido del audio.
 */
object AnalisisFonemas {
    /** Por debajo de esto, el sonido está mal dicho aunque Azure no proponga otro. */
    const val UMBRAL_FONEMA = 60
    /** Ventaja que debe sacar el sonido oído sobre el esperado para contar como sustitución. */
    const val MARGEN_SUSTITUCION = 10
    /** Puntaje máximo de una palabra con un sonido mal dicho: por debajo del regular (60), en rojo. */
    const val TOPE_PALABRA_CON_ERROR = 55

    /**
     * La evaluación completa de la frase: palabras calificadas por sonido más las métricas de
     * frase de Azure (fluidez, completitud, PronScore) y la confianza del reconocimiento.
     * Null si el JSON no trae palabras (se usa entonces la calificación del SDK).
     */
    fun evaluar(json: String, transcripcion: String): EvaluacionVoz? = runCatching {
        val raiz = JsonParser.parseString(json).asJsonObject
        val mejor = raiz.getAsJsonArray("NBest")[0].asJsonObject
        val palabras = palabrasDe(mejor) ?: return null
        val frase = evaluacion(mejor)
        if (!calificado(mejor)) return null
        val todas = mejor.getAsJsonArray("Words").map { it.asJsonObject }
        // Entonación plana: Azure la marca palabra por palabra; se avisa una vez si es la mayoría.
        val conProsodia = todas.filter { prosodia(it) != null }
        val planas = conProsodia.count { "Monotone" in erroresProsodia(it, "Intonation") }
        EvaluacionVoz(
            puntaje = puntaje(palabras),
            confianza = mejor.get("Confidence")?.takeIf { it.isJsonPrimitive }?.asDouble?.let { (it * 100).roundToInt() } ?: 100,
            palabras = palabras,
            transcripcion = transcripcion,
            fuente = FuenteEvaluacion.AZURE,
            fluidez = frase.numeroONulo("FluencyScore"),
            completitud = frase.numeroONulo("CompletenessScore"),
            puntajeGeneral = frase.numeroONulo("PronScore"),
            prosodia = frase.numeroONulo("ProsodyScore"),
            palabrasDeMas = todas.filter { evaluacion(it).texto("ErrorType") == "Insertion" }.mapNotNull { it.texto("Word") },
            // Azure marca «Monotone» muy seguido: solo se avisa si además bajó la entonación de la frase.
            monotono = conProsodia.size >= PALABRAS_PARA_ENTONACION && planas * 2 > conProsodia.size &&
                (frase.numeroONulo("ProsodyScore") ?: 0) < PROSODIA_PLANA,
            ruidoDb = raiz.numeroONulo("SNR")
        )
    }.getOrNull()

    /**
     * ¿Trae calificación? En una tablet (10-nov-2026) Azure transcribió «Weekend.» sin ningún
     * AccuracyScore y la app mostró 0 puntos como si se hubiera pronunciado mal.
     */
    fun calificado(json: String): Boolean = runCatching { calificado(mejorAlternativa(json)) }.getOrDefault(false)

    private fun calificado(mejor: JsonObject): Boolean =
        evaluacion(mejor).has("AccuracyScore") ||
            mejor.getAsJsonArray("Words")?.any { evaluacion(it.asJsonObject).has("AccuracyScore") } == true

    /** Frases de menos palabras no tienen «melodía» que calificar. */
    private const val PALABRAS_PARA_ENTONACION = 3
    private const val PROSODIA_PLANA = 80

    /** Palabras de la frase (sin las de más que dijo el usuario), o null si el JSON no trae palabras. */
    fun palabras(json: String): List<PalabraEvaluada>? = runCatching { palabrasDe(mejorAlternativa(json)) }.getOrNull()

    /** Precisión general: el promedio de las palabras ya calificadas por sonido. */
    fun puntaje(palabras: List<PalabraEvaluada>): Int = palabras.map { it.puntaje }.average().roundToInt()

    private fun mejorAlternativa(json: String): JsonObject =
        JsonParser.parseString(json).asJsonObject.getAsJsonArray("NBest")[0].asJsonObject

    private fun palabrasDe(mejor: JsonObject): List<PalabraEvaluada>? {
        val todas = mejor.getAsJsonArray("Words").map { it.asJsonObject }
        fun error(i: Int) = todas.getOrNull(i)?.let { evaluacion(it).texto("ErrorType") }
        val usadas = mutableSetOf<Int>()
        return todas.mapIndexedNotNull { i, json ->
            if (error(i) == "Insertion") return@mapIndexedNotNull null
            val base = palabra(json)
            if (error(i) != "Omission") return@mapIndexedNotNull base
            // Omitida junto a una de más: se dijo otra palabra en su lugar («the» por «I»).
            val vecina = listOf(i - 1, i + 1).firstOrNull { it !in usadas && error(it) == "Insertion" }
            vecina?.let { usadas += it; base.copy(dichaEnSuLugar = todas[it].texto("Word")) } ?: base
        }.takeIf { it.isNotEmpty() }
    }

    private fun palabra(json: JsonObject): PalabraEvaluada {
        val evaluacion = evaluacion(json)
        val precision = evaluacion.numero("AccuracyScore")
        val error = evaluacion.texto("ErrorType")?.takeIf { it != "None" }
        val fonemas = json.getAsJsonArray("Phonemes")?.map { it.asJsonObject }.orEmpty()
        val sonidosMal = fonemas.mapNotNull(::sonidoMal)
        val detalle = fonemas.mapNotNull { f -> f.texto("Phoneme")?.let { FonemaEvaluado(it, evaluacion(f).numero("AccuracyScore"), sustituto(f)) } }
        val silabas = json.getAsJsonArray("Syllables")?.map { it.asJsonObject }.orEmpty().map { s ->
            SilabaEvaluada(s.texto("Syllable").orEmpty(), s.texto("Grapheme").orEmpty(), evaluacion(s).numero("AccuracyScore"))
        }
        val pausas = erroresProsodia(json, "Break")
        val base = PalabraEvaluada(
            json.texto("Word").orEmpty(), precision, error,
            fonemas = detalle,
            silabas = silabas,
            pausaInesperada = "UnexpectedBreak" in pausas,
            faltaPausa = "MissingBreak" in pausas
        )
        if (error == "Omission" || sonidosMal.isEmpty()) return base
        return base.copy(puntaje = minOf(precision, TOPE_PALABRA_CON_ERROR), error = "Mispronunciation", sonidos = sonidosMal)
    }

    private fun prosodia(palabra: JsonObject): JsonObject? =
        evaluacion(palabra).getAsJsonObject("Feedback")?.getAsJsonObject("Prosody")

    /** ErrorTypes de prosodia de la palabra ("Break" o "Intonation"), sin el "None". */
    private fun erroresProsodia(palabra: JsonObject, tipo: String): List<String> = runCatching {
        prosodia(palabra)?.getAsJsonObject(tipo)?.getAsJsonArray("ErrorTypes")
            ?.mapNotNull { it.takeIf { e -> e.isJsonPrimitive }?.asString }?.filter { it != "None" }
    }.getOrNull().orEmpty()

    /** El sonido que se oyó en lugar de [fonema], si fue otro con suficiente ventaja. */
    private fun sustituto(fonema: JsonObject): String? {
        val esperado = fonema.texto("Phoneme") ?: return null
        val candidatos = evaluacion(fonema).getAsJsonArray("NBestPhonemes")?.map { it.asJsonObject }.orEmpty()
        val oido = candidatos.maxByOrNull { it.numero("Score") } ?: return null
        val puntajeEsperado = candidatos.firstOrNull { it.texto("Phoneme") == esperado }?.numero("Score") ?: 0
        return oido.texto("Phoneme")?.takeIf { it != esperado && oido.numero("Score") - puntajeEsperado >= MARGEN_SUSTITUCION }
    }

    /** "/ɹ/ sonó como /d/", "/ɑ/ poco claro", o null si el sonido estuvo bien. */
    private fun sonidoMal(fonema: JsonObject): String? {
        val esperado = fonema.texto("Phoneme") ?: return null
        val oido = sustituto(fonema)
        return when {
            oido != null -> "/$esperado/ sonó como /$oido/"
            evaluacion(fonema).numero("AccuracyScore") < UMBRAL_FONEMA -> "/$esperado/ poco claro"
            else -> null
        }
    }

    /**
     * Las calificaciones vienen anidadas en "PronunciationAssessment" (JSON del SDK) o planas en
     * el mismo objeto (REST de Azure, verificado el 29-oct con el recurso de producción).
     */
    private fun evaluacion(json: JsonObject): JsonObject =
        json.getAsJsonObject("PronunciationAssessment") ?: json.takeIf { it.has("AccuracyScore") || it.has("ErrorType") } ?: JsonObject()
    private fun JsonObject.texto(clave: String): String? = get(clave)?.takeIf { it.isJsonPrimitive }?.asString
    private fun JsonObject.numero(clave: String): Int = numeroONulo(clave) ?: 0
    private fun JsonObject.numeroONulo(clave: String): Int? = get(clave)?.takeIf { it.isJsonPrimitive }?.asDouble?.roundToInt()
}
