package com.xpspeak.app.feature.games.data.voz

import android.util.Log
import com.google.gson.JsonParser
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentResult
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FonemaEvaluado
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Texto
import kotlin.math.roundToInt

/**
 * Convierte un resultado del SDK de Azure al modelo de la app. Lo usan Eco Vocal, la
 * Práctica de pronunciación, las lecciones y el chat en vivo, así una misma
 * pronunciación se califica igual en toda la aplicación (RF-09).
 */
object ConversorAzure {
    /** Logcat: qué sonidos salieron mal en cada intento (para revisar la calificación). */
    private const val TAG = "XPS-FONEMAS"

    /**
     * @param referencia la frase que se pidió leer, o null en el chat (habla libre). Si Azure no
     *   devolvió palabras, la referencia se marca como omitida para que no quede en blanco.
     */
    fun convertir(resultado: SpeechRecognitionResult, referencia: String?): EvaluacionVoz {
        val json = resultado.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult).orEmpty()
        AnalisisFonemas.evaluar(json, resultado.text.orEmpty())?.let { evaluacion ->
            evaluacion.palabras.filter { it.sonidos.isNotEmpty() }.forEach { Log.d(TAG, "${it.palabra} (${it.puntaje}): ${it.sonidos}") }
            return evaluacion
        }
        // Sin fonemas en el JSON: calificación por palabra del SDK.
        val pa = PronunciationAssessmentResult.fromResult(resultado)
        val palabras = pa?.words.orEmpty()
            // Palabras de más que dijo el usuario: no forman parte de la frase a colorear.
            .filter { it.errorType != "Insertion" }
            .map { palabra ->
                PalabraEvaluada(
                    palabra.word,
                    palabra.accuracyScore.roundToInt(),
                    palabra.errorType.takeIf { it != "None" },
                    fonemas = palabra.phonemes.orEmpty().map { FonemaEvaluado(it.phoneme, it.accuracyScore.roundToInt()) }
                )
            }
        return EvaluacionVoz(
            // Precisión de los sonidos, no el PronScore: ese promedia también fluidez y
            // completitud, y una frase dicha de corrido pero mal pronunciada pasaba de 70.
            puntaje = (pa?.accuracyScore ?: 0.0).roundToInt(),
            confianza = confianza(resultado),
            palabras = palabras.ifEmpty { omitidas(referencia) },
            transcripcion = resultado.text.orEmpty(),
            fuente = FuenteEvaluacion.AZURE,
            fluidez = pa?.fluencyScore?.roundToInt(),
            completitud = pa?.completenessScore?.roundToInt(),
            puntajeGeneral = pa?.pronunciationScore?.roundToInt()
        )
    }

    /** Silencio o ruido: "no fue legible", se pide repetir (CU-05 A1). */
    fun noLegible(referencia: String?) = EvaluacionVoz(
        puntaje = 0,
        confianza = 0,
        palabras = omitidas(referencia),
        transcripcion = "",
        fuente = FuenteEvaluacion.AZURE
    )

    private fun omitidas(referencia: String?) = referencia?.let { Texto.palabras(it).map { p -> PalabraEvaluada(p, 0, "Omission") } }.orEmpty()

    /** NBest[0].Confidence (0–1) del JSON detallado de Azure. */
    private fun confianza(resultado: SpeechRecognitionResult): Int = runCatching {
        val json = resultado.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
        val nBest = JsonParser.parseString(json).asJsonObject.getAsJsonArray("NBest")
        (nBest[0].asJsonObject["Confidence"].asDouble * 100).roundToInt()
    }.getOrDefault(100)
}
