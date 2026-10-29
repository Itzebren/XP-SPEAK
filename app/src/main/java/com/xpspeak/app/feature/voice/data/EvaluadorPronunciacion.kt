package com.xpspeak.app.feature.voice.data

import android.util.Log
import com.google.gson.JsonParser
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.NoMatchDetails
import com.microsoft.cognitiveservices.speech.PhraseListGrammar
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentResult
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import javax.inject.Inject

/**
 * RF-09: evalúa la pronunciación del usuario comparándola con un texto de
 * referencia usando Azure Pronunciation Assessment.
 *
 * Diferencias clave respecto a ReconocedorVoz:
 *  - Se aplica PronunciationAssessmentConfig al recognizer ANTES de llamar
 *    recognizeOnceAsync (orden exigido por el SDK).
 *  - Se configuran los mismos timeouts de silencio que usa ReconocedorVoz
 *    para evitar NoMatch por InitialSilenceTimeout.
 *  - enableProsodyAssessment() está desactivado por ahora porque es el
 *    componente más propenso a fallar en emulador / SDK preview.
 */
class EvaluadorPronunciacion @Inject constructor(
    private val speechRepository: SpeechRepository
) {
    companion object {
        private const val TAG = "EvaluadorPronunciacion"
    }

    /** Nadie habló en el tiempo de espera: el chat en vivo se pausa para no dejar el micrófono abierto. */
    class SinVoz : Exception("Pausé la conversación porque no te escuché. Toca el micrófono para seguir.")

    data class ResultadoPronunciacion(
        val textoReconocido: String,
        val puntuacionGeneral: Double,
        val puntuacionPrecision: Double,
        val puntuacionFluidez: Double,
        val puntuacionCompletitud: Double,
        val palabras: List<PalabraEvaluada>,
        /** 0–100, NBest[0].Confidence: por debajo de 30 el audio no fue legible (CU-05 A3). */
        val confianza: Int = 100
    )

    data class PalabraEvaluada(
        val palabra: String,
        val puntuacionPrecision: Double,
        val errorType: String,
        val fonemas: List<FonemaEvaluado> = emptyList()
    )

    /** Fonema en IPA (p. ej. "θ") con su precisión 0–100. */
    data class FonemaEvaluado(val fonema: String, val puntuacionPrecision: Double)

    suspend fun evaluar(
        textoReferencia: String,
        idioma: String = "en-US"
    ): Result<ResultadoPronunciacion> = withContext(Dispatchers.IO) {
        runCatching {
            val cred = speechRepository.obtenerCredenciales().getOrThrow()

            val config = SpeechConfig.fromAuthorizationToken(cred.token, cred.region)
            config.speechRecognitionLanguage = idioma
            // ── Mismos timeouts que ReconocedorVoz ──
            config.setProperty(
                PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs,
                "15000"
            )
            config.setProperty(
                PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs,
                "3000"
            )

            val audio = AudioConfig.fromDefaultMicrophoneInput()

            // ── Configuración de evaluación ──
            val pronConfig = PronunciationAssessmentConfig(
                textoReferencia,
                PronunciationAssessmentGradingSystem.HundredMark,
                PronunciationAssessmentGranularity.Phoneme,
                true // enableMiscue
            )
            // IPA en vez de SAPI ("th"): es la notación de los diccionarios que conoce el estudiante.
            pronConfig.setPhonemeAlphabet("IPA")
            // Prosody desactivado: es opcional y el más propenso a fallar.
            // pronConfig.enableProsodyAssessment()

            val recognizer = SpeechRecognizer(config, audio)

            // applyTo DEBE llamarse ANTES de recognizeOnceAsync
            pronConfig.applyTo(recognizer)

            try {
                val resultado = recognizer.recognizeOnceAsync().get()

                when (resultado.reason) {
                    ResultReason.RecognizedSpeech -> convertir(resultado)

                    ResultReason.NoMatch -> {
                        val detalle = NoMatchDetails.fromResult(resultado)
                        val motivo = detalle.reason
                        Log.w(TAG, "NoMatch – motivo: $motivo")
                        throw Exception(
                            "No te escuché bien. Intenta de nuevo. (motivo: $motivo)"
                        )
                    }

                    ResultReason.Canceled -> {
                        val detalle = CancellationDetails.fromResult(resultado)
                        Log.e(TAG, "Canceled: ${detalle.reason} – ${detalle.errorDetails}")
                        throw Exception("Error de voz: ${detalle.errorDetails}")
                    }

                    else -> {
                        Log.e(TAG, "Resultado inesperado: ${resultado.reason}")
                        throw Exception("No se pudo evaluar la pronunciación.")
                    }
                }
            } finally {
                pronConfig.close()
                recognizer.close()
                audio.close()
                config.close()
            }
        }
    }

    /**
     * Chat en vivo: escucha una frase libre (sin referencia) y avisa con [onParcial]
     * lo que va oyendo mientras el usuario habla. Termina en la primera pausa con la
     * frase calificada contra la pronunciación estándar; cancelar la corrutina cierra
     * el micrófono. Si nadie habla en [silencioInicialMs] falla con [SinVoz].
     */
    suspend fun escucharFrase(
        idioma: String = "en-US",
        silencioInicialMs: Long = 15_000,
        onParcial: (String) -> Unit
    ): Result<ResultadoPronunciacion> = withContext(Dispatchers.IO) {
        runCatching {
            val cred = speechRepository.obtenerCredenciales().getOrThrow()

            val config = SpeechConfig.fromAuthorizationToken(cred.token, cred.region)
            config.speechRecognitionLanguage = idioma
            // En una conversación la pausa natural entre ideas es más larga que al leer una frase.
            config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "1500")

            val audio = AudioConfig.fromDefaultMicrophoneInput()
            val pronConfig = PronunciationAssessmentConfig(
                "",
                PronunciationAssessmentGradingSystem.HundredMark,
                PronunciationAssessmentGranularity.Phoneme,
                false // miscue: sin referencia no hay palabras omitidas ni de más
            )
            pronConfig.setPhonemeAlphabet("IPA")
            val recognizer = SpeechRecognizer(config, audio)
            pronConfig.applyTo(recognizer)

            val empezoAHablar = CompletableDeferred<Unit>()
            recognizer.recognizing.addEventListener { _, evento ->
                empezoAHablar.complete(Unit)
                onParcial(evento.result.text)
            }

            try {
                coroutineScope {
                    val frase = async {
                        suspendCancellableCoroutine { continuacion ->
                            recognizer.recognized.addEventListener { _, evento ->
                                // NoMatch (ruido, tos) no corta: se sigue escuchando hasta oír una frase.
                                val resultado = evento.result
                                if (resultado.reason == ResultReason.RecognizedSpeech &&
                                    resultado.text.isNotBlank() && continuacion.isActive
                                ) {
                                    // Corre en un hilo del SDK: un error aquí debe llegar a la corrutina, no tumbar la app.
                                    runCatching { convertir(resultado) }
                                        .onSuccess { continuacion.resume(it) }
                                        .onFailure { continuacion.resumeWithException(it) }
                                }
                            }
                            recognizer.canceled.addEventListener { _, evento ->
                                Log.e(TAG, "Canceled: ${evento.reason} – ${evento.errorDetails}")
                                if (continuacion.isActive) {
                                    continuacion.resumeWithException(Exception("Error de voz: ${evento.errorDetails}"))
                                }
                            }
                            recognizer.startContinuousRecognitionAsync()
                        }
                    }
                    withTimeoutOrNull(silencioInicialMs) { empezoAHablar.await() }
                        ?: run { frase.cancel(); throw SinVoz() }
                    frase.await()
                }
            } finally {
                runCatching { recognizer.stopContinuousRecognitionAsync().get() }
                pronConfig.close()
                recognizer.close()
                audio.close()
                config.close()
            }
        }
    }

    /**
     * Ráfaga de Palabras: escucha sin parar hasta que se cancele la corrutina y avisa con
     * [onTexto] todo lo que oye, parcial o final, junto con el inicio de la frase en el audio
     * (sirve para no contar dos veces la misma). [frases] sesga a Azure hacia las palabras
     * que se esperan, así un "yes" o "no" dicho rápido no sale como "yeah" o "know".
     */
    suspend fun escucharContinuo(
        frases: List<String>,
        idioma: String = "en-US",
        onTexto: (inicio: Long, texto: String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val cred = speechRepository.obtenerCredenciales().getOrThrow()

            val config = SpeechConfig.fromAuthorizationToken(cred.token, cred.region)
            config.speechRecognitionLanguage = idioma
            // Respuestas de una palabra: cerrar la frase pronto para pasar al siguiente par.
            config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "500")

            val audio = AudioConfig.fromDefaultMicrophoneInput()
            val recognizer = SpeechRecognizer(config, audio)
            val gramatica = PhraseListGrammar.fromRecognizer(recognizer)
            frases.forEach { gramatica.addPhrase(it) }

            recognizer.recognizing.addEventListener { _, evento ->
                onTexto(evento.result.offset.toLong(), evento.result.text)
            }
            recognizer.recognized.addEventListener { _, evento ->
                if (evento.result.reason == ResultReason.RecognizedSpeech) {
                    onTexto(evento.result.offset.toLong(), evento.result.text)
                }
            }

            try {
                suspendCancellableCoroutine<Unit> { continuacion ->
                    recognizer.canceled.addEventListener { _, evento ->
                        Log.e(TAG, "Canceled: ${evento.reason} – ${evento.errorDetails}")
                        if (continuacion.isActive) {
                            continuacion.resumeWithException(Exception("Error de voz: ${evento.errorDetails}"))
                        }
                    }
                    recognizer.startContinuousRecognitionAsync()
                }
            } finally {
                runCatching { recognizer.stopContinuousRecognitionAsync().get() }
                gramatica.close()
                recognizer.close()
                audio.close()
                config.close()
            }
        }
    }

    private fun convertir(resultado: SpeechRecognitionResult): ResultadoPronunciacion {
        val pa = PronunciationAssessmentResult.fromResult(resultado)
        val palabras = pa.words.orEmpty()
            // Palabras de más que dijo el usuario: no forman parte de la frase a colorear.
            .filter { it.errorType != "Insertion" }
            .map { w ->
                PalabraEvaluada(
                    palabra = w.word,
                    puntuacionPrecision = w.accuracyScore,
                    errorType = w.errorType,
                    fonemas = w.phonemes.orEmpty().map { FonemaEvaluado(it.phoneme, it.accuracyScore) }
                )
            }

        return ResultadoPronunciacion(
            textoReconocido = resultado.text,
            puntuacionGeneral = pa.pronunciationScore,
            puntuacionPrecision = pa.accuracyScore,
            puntuacionFluidez = pa.fluencyScore,
            puntuacionCompletitud = pa.completenessScore,
            palabras = palabras,
            confianza = confianza(resultado)
        )
    }

    /** NBest[0].Confidence (0–1) del JSON detallado de Azure, igual que EvaluadorAzure de Eco Vocal. */
    private fun confianza(resultado: SpeechRecognitionResult): Int = runCatching {
        val json = resultado.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
        val nBest = JsonParser.parseString(json).asJsonObject.getAsJsonArray("NBest")
        (nBest[0].asJsonObject["Confidence"].asDouble * 100).roundToInt()
    }.getOrDefault(100)
}
