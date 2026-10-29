package com.xpspeak.app.feature.games.data.voz

import android.util.Log
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
import com.xpspeak.app.feature.voice.data.MedidorVoz
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

    private companion object {
        /** Silencio con el que recognizeOnce da por terminada la frase. */
        const val PAUSA_FIN_FRASE_MS = 1500L
        /** Logcat: qué sonidos salieron mal en cada intento (para revisar la calificación). */
        const val TAG = "XPS-FONEMAS"
    }

    suspend fun evaluar(referencia: String, token: TokenVozDto): EvaluacionVoz = withContext(Dispatchers.IO) {
        val config = SpeechConfig.fromAuthorizationToken(token.token, token.region).apply {
            speechRecognitionLanguage = "en-US"
            // Mismos tiempos que EvaluadorPronunciacion: con los de fábrica Azure se rinde si el
            // usuario tarda en empezar y corta en la primera pausa, y todo sale "no legible".
            setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
            setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "3000")
            // recognizeOnce termina la frase con esta pausa (≈500 ms de fábrica): una respiración a
            // media oración la cortaba y el resto salía como "omitido".
            setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, PAUSA_FIN_FRASE_MS.toString())
        }
        val audio = AudioConfig.fromDefaultMicrophoneInput()
        val reconocedor = SpeechRecognizer(config, audio)
        val medidor = MedidorVoz(reconocedor, "voz.eco_vocal", PAUSA_FIN_FRASE_MS)
        val evaluacion = PronunciationAssessmentConfig(
            referencia,
            PronunciationAssessmentGradingSystem.HundredMark,
            PronunciationAssessmentGranularity.Phoneme,
            true // miscue: marca palabras omitidas o de más
        )
        // Qué sonido se oyó en lugar de cada sonido esperado (AnalisisFonemas).
        evaluacion.setPhonemeAlphabet("IPA")
        evaluacion.setNBestPhonemeCount(5)
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
                    ResultReason.RecognizedSpeech -> convertir(referencia, it).also { _ -> medidor.registrar(it) }
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
        val json = resultado.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult).orEmpty()
        AnalisisFonemas.palabras(json)?.let { palabras ->
            palabras.filter { it.sonidos.isNotEmpty() }.forEach { Log.d(TAG, "${it.palabra} (${it.puntaje}): ${it.sonidos}") }
            return EvaluacionVoz(
                puntaje = AnalisisFonemas.puntaje(palabras),
                confianza = confianza(resultado),
                palabras = palabras,
                transcripcion = resultado.text.orEmpty(),
                fuente = FuenteEvaluacion.AZURE
            )
        }
        // Sin fonemas en el JSON: calificación por palabra del SDK.
        val pa = PronunciationAssessmentResult.fromResult(resultado)
        val palabras = pa?.words.orEmpty()
            // Palabras de más que dijo el usuario: no forman parte de la frase a colorear.
            .filter { it.errorType != "Insertion" }
            .map { PalabraEvaluada(it.word, it.accuracyScore.roundToInt(), it.errorType.takeIf { e -> e != "None" }) }
        return EvaluacionVoz(
            // Precisión de los sonidos, no el PronScore: ese promedia también fluidez y
            // completitud, y una frase dicha de corrido pero mal pronunciada pasaba de 70.
            puntaje = (pa?.accuracyScore ?: 0.0).roundToInt(),
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
