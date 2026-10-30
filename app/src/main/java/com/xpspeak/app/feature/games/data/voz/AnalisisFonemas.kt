package com.xpspeak.app.feature.games.data.voz

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import kotlin.math.roundToInt

/**
 * Califica cada palabra sonido por sonido con el JSON detallado de Azure
 * (granularidad Phoneme + NBestPhonemes en IPA).
 *
 * El AccuracyScore por palabra es muy indulgente con el acento: "party" leído
 * como en español sacaba 95. Aquí un sonido cuenta como mal dicho si su
 * precisión es baja o si el sonido que Azure oyó con más probabilidad no es el
 * esperado; una palabra con algún sonido mal queda en rojo.
 */
object AnalisisFonemas {
    /** Por debajo de esto, el sonido está mal dicho aunque Azure no proponga otro. */
    const val UMBRAL_FONEMA = 60
    /** Ventaja que debe sacar el sonido oído sobre el esperado para contar como sustitución. */
    const val MARGEN_SUSTITUCION = 10
    /** Puntaje máximo de una palabra con un sonido mal dicho: por debajo del regular (60), en rojo. */
    const val TOPE_PALABRA_CON_ERROR = 55

    /** Palabras de la frase (sin las de más que dijo el usuario), o null si el JSON no trae fonemas. */
    fun palabras(json: String): List<PalabraEvaluada>? = runCatching {
        val mejor = JsonParser.parseString(json).asJsonObject.getAsJsonArray("NBest")[0].asJsonObject
        val palabras = mejor.getAsJsonArray("Words").map { it.asJsonObject }
            .filter { evaluacion(it).texto("ErrorType") != "Insertion" }
            .map(::palabra)
        palabras.takeIf { it.isNotEmpty() }
    }.getOrNull()

    /** Precisión general: el promedio de las palabras ya calificadas por sonido. */
    fun puntaje(palabras: List<PalabraEvaluada>): Int = palabras.map { it.puntaje }.average().roundToInt()

    private fun palabra(json: JsonObject): PalabraEvaluada {
        val evaluacion = evaluacion(json)
        val precision = evaluacion.numero("AccuracyScore")
        val error = evaluacion.texto("ErrorType")?.takeIf { it != "None" }
        val sonidosMal = json.getAsJsonArray("Phonemes")?.mapNotNull { sonidoMal(it.asJsonObject) }.orEmpty()
        val texto = json.texto("Word").orEmpty()
        if (error == "Omission" || sonidosMal.isEmpty()) return PalabraEvaluada(texto, precision, error)
        return PalabraEvaluada(texto, minOf(precision, TOPE_PALABRA_CON_ERROR), "Mispronunciation", sonidosMal)
    }

    /** "/ɹ/ sonó como /d/", "/ɑ/ poco claro", o null si el sonido estuvo bien. */
    private fun sonidoMal(fonema: JsonObject): String? {
        val esperado = fonema.texto("Phoneme") ?: return null
        val evaluacion = evaluacion(fonema)
        val candidatos = evaluacion.getAsJsonArray("NBestPhonemes")?.map { it.asJsonObject }.orEmpty()
        val oido = candidatos.maxByOrNull { it.numero("Score") }
        val puntajeEsperado = candidatos.firstOrNull { it.texto("Phoneme") == esperado }?.numero("Score") ?: 0
        val sustituido = oido != null && oido.texto("Phoneme") != esperado &&
            oido.numero("Score") - puntajeEsperado >= MARGEN_SUSTITUCION
        return when {
            sustituido -> "/$esperado/ sonó como /${oido!!.texto("Phoneme")}/"
            evaluacion.numero("AccuracyScore") < UMBRAL_FONEMA -> "/$esperado/ poco claro"
            else -> null
        }
    }

    private fun evaluacion(json: JsonObject): JsonObject = json.getAsJsonObject("PronunciationAssessment") ?: JsonObject()
    private fun JsonObject.texto(clave: String): String? = get(clave)?.takeIf { it.isJsonPrimitive }?.asString
    private fun JsonObject.numero(clave: String): Int = get(clave)?.takeIf { it.isJsonPrimitive }?.asDouble?.roundToInt() ?: 0
}
