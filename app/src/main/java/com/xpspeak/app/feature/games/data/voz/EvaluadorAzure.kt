package com.xpspeak.app.feature.games.data.voz

import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.voice.data.MedidorVoz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Azure AI Speech – Pronunciation Assessment (§5.1, PDF §1.2) de una frase de
 * referencia: Eco Vocal, Práctica de pronunciación y las frases de una lección.
 * El micrófono se transmite en streaming a Azure con el token temporal del
 * backend; no se graba ningún archivo (RN-04). `recognizeOnceAsync` termina solo
 * al detectar silencio, así basta un toque para hablar.
 */
class EvaluadorAzure @Inject constructor() {

    /** Error de Azure que no es "no se entendió" (red, token vencido…): se usa el respaldo. */
    class FalloAzure(mensaje: String) : Exception(mensaje)

    private companion object {
        /** Silencio con el que recognizeOnce da por terminada la frase. */
        const val PAUSA_FIN_FRASE_MS = 1500L
    }

    suspend fun evaluar(referencia: String, token: TokenVozDto, medida: String = "voz.eco_vocal"): EvaluacionVoz = withContext(Dispatchers.IO) {
        val config = SpeechConfig.fromAuthorizationToken(token.token, token.region).apply {
            speechRecognitionLanguage = "en-US"
            // Con los tiempos de fábrica Azure se rinde si el usuario tarda en empezar y corta en
            // la primera pausa, y todo sale "no legible".
            setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
            setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "3000")
            // recognizeOnce termina la frase con esta pausa (≈500 ms de fábrica): una respiración a
            // media oración la cortaba y el resto salía como "omitido".
            setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, PAUSA_FIN_FRASE_MS.toString())
        }
        val audio = AudioConfig.fromDefaultMicrophoneInput()
        val reconocedor = SpeechRecognizer(config, audio)
        val medidor = MedidorVoz(reconocedor, medida, PAUSA_FIN_FRASE_MS)
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
                    ResultReason.RecognizedSpeech -> ConversorAzure.convertir(it, referencia).also { _ -> medidor.registrar(it) }
                    // Silencio o ruido: "no fue legible", se pide repetir (CU-05 A1).
                    ResultReason.NoMatch -> ConversorAzure.noLegible(referencia)
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
}
