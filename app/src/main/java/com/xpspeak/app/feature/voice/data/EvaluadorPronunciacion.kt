package com.xpspeak.app.feature.voice.data

import android.util.Log
import com.microsoft.cognitiveservices.speech.PhraseListGrammar
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.xpspeak.app.feature.games.data.voz.ConversorAzure
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reconocimiento continuo con Azure para hablar libremente (RF-07, RF-09): el chat
 * en vivo y la Ráfaga con voz. La evaluación de una frase de referencia (Eco Vocal,
 * Práctica, lecciones) está en EvaluadorAzure; ambos convierten con ConversorAzure
 * para que la calificación sea la misma en toda la app.
 */
class EvaluadorPronunciacion @Inject constructor(
    private val speechRepository: SpeechRepository
) {
    companion object {
        private const val TAG = "EvaluadorPronunciacion"

        /** Silencio con el que Azure da por terminada una frase en el chat en vivo. */
        private const val PAUSA_FIN_FRASE_MS = 1500L
    }

    /** Nadie habló en el tiempo de espera: el chat en vivo se pausa para no dejar el micrófono abierto. */
    class SinVoz : Exception("Pausé la conversación porque no te escuché. Toca el micrófono para seguir.")

    /**
     * Chat en vivo: escucha una frase libre (sin referencia) y avisa con [onParcial]
     * lo que va oyendo mientras el usuario habla. Termina en la primera pausa con la
     * frase calificada sonido por sonido contra la pronunciación estándar; cancelar la
     * corrutina cierra el micrófono. Si nadie habla en [silencioInicialMs] falla con [SinVoz].
     */
    suspend fun escucharFrase(
        idioma: String = "en-US",
        silencioInicialMs: Long = 15_000,
        onParcial: (String) -> Unit
    ): Result<EvaluacionVoz> = withContext(Dispatchers.IO) {
        runCatching {
            val cred = speechRepository.obtenerCredenciales().getOrThrow()

            val config = SpeechConfig.fromAuthorizationToken(cred.token, cred.region)
            config.speechRecognitionLanguage = idioma
            // En una conversación la pausa natural entre ideas es más larga que al leer una frase.
            config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, PAUSA_FIN_FRASE_MS.toString())

            val audio = AudioConfig.fromDefaultMicrophoneInput()
            val pronConfig = PronunciationAssessmentConfig(
                "",
                PronunciationAssessmentGradingSystem.HundredMark,
                PronunciationAssessmentGranularity.Phoneme,
                false // miscue: sin referencia no hay palabras omitidas ni de más
            )
            // IPA y los sonidos alternativos: lo mismo que Eco Vocal, para detectar sustituciones.
            pronConfig.setPhonemeAlphabet("IPA")
            pronConfig.setNBestPhonemeCount(5)
            val recognizer = SpeechRecognizer(config, audio)
            pronConfig.applyTo(recognizer)
            val medidor = MedidorVoz(recognizer, "voz.chat_en_vivo", PAUSA_FIN_FRASE_MS)

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
                                    runCatching { ConversorAzure.convertir(resultado, referencia = null).also { medidor.registrar(resultado) } }
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
}
