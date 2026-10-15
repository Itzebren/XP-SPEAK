package com.xpspeak.app.feature.games.data.voz

import com.google.gson.JsonParser
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentResult
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Texto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

/**
 * Azure AI Speech – Pronunciation Assessment (§5.1, PDF §1.2). El micrófono
 * se transmite en streaming a Azure con el token temporal del backend; no se
 * graba ningún archivo (RN-04). `recognizeOnceAsync` termina solo al detectar
 * silencio, así basta un toque para hablar.
 */
class EvaluadorAzure @Inject constructor() {

    /** Error de Azure que no es "no se entendió" (red, token vencido…): se usa el respaldo. */
    class FalloAzure(mensaje: String) : Exception(mensaje)

    suspend fun evaluar(referencia: String, token: TokenVozDto): EvaluacionVoz = withContext(Dispatchers.IO) {
        val config = SpeechConfig.fromAuthorizationToken(token.token, token.region).apply {
            speechRecognitionLanguage = "en-US"
        }
        val audio = AudioConfig.fromDefaultMicrophoneInput()
        val reconocedor = SpeechRecognizer(config, audio)
        val evaluacion = PronunciationAssessmentConfig(
            referencia,
            PronunciationAssessmentGradingSystem.HundredMark,
            PronunciationAssessmentGranularity.Phoneme,
            true // miscue: marca palabras omitidas o de más
        )
        try {
            evaluacion.applyTo(reconocedor)
            val futuro = reconocedor.recognizeOnceAsync()
            val resultado = suspendCancellableCoroutine { continuacion ->
                continuacion.invokeOnCancellation { runCatching { futuro.cancel(true) } }
                try {
                    continuacion.resume(futuro.get())
                } catch (error: ExecutionException) {
                    continuacion.resumeWithException(FalloAzure(error.cause?.message ?: "Azure no respondió"))
                }
            }
            resultado.use {
                when (it.reason) {
                    ResultReason.RecognizedSpeech -> convertir(referencia, it)
                    // Silencio o ruido: "no fue legible", se pide repetir (CU-05 A1).
                    ResultReason.NoMatch -> noLegible(referencia)
                    else -> throw FalloAzure("Azure canceló el reconocimiento (${it.reason})")
                }
            }
        } finally {
            evaluacion.close()
            reconocedor.close()
            audio.close()
            config.close()
        }
    }

    private fun convertir(referencia: String, resultado: com.microsoft.cognitiveservices.speech.SpeechRecognitionResult): EvaluacionVoz {
        val pa = PronunciationAssessmentResult.fromResult(resultado)
        val palabras = pa?.words.orEmpty()
            // Palabras de más que dijo el usuario: no forman parte de la frase a colorear.
            .filter { it.errorType != "Insertion" }
            .map { PalabraEvaluada(it.word, it.accuracyScore.roundToInt(), it.errorType.takeIf { e -> e != "None" }) }
        return EvaluacionVoz(
            puntaje = (pa?.pronunciationScore ?: pa?.accuracyScore ?: 0.0).roundToInt(),
            confianza = confianza(resultado),
            palabras = palabras.ifEmpty { Texto.palabras(referencia).map { PalabraEvaluada(it, 0, "Omission") } },
            transcripcion = resultado.text.orEmpty(),
            fuente = FuenteEvaluacion.AZURE
        )
    }

    /** NBest[0].Confidence (0–1) del JSON detallado de Azure. */
    private fun confianza(resultado: com.microsoft.cognitiveservices.speech.SpeechRecognitionResult): Int =
        runCatching {
            val json = resultado.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
            val nBest = JsonParser.parseString(json).asJsonObject.getAsJsonArray("NBest")
            (nBest[0].asJsonObject["Confidence"].asDouble * 100).roundToInt()
        }.getOrDefault(100)

    private fun noLegible(referencia: String) = EvaluacionVoz(
        puntaje = 0,
        confianza = 0,
        palabras = Texto.palabras(referencia).map { PalabraEvaluada(it, 0, "Omission") },
        transcripcion = "",
        fuente = FuenteEvaluacion.AZURE
    )
}
