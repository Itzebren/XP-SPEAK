package com.xpspeak.app.feature.games.data.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.Pronunciacion
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Respaldo de Eco Vocal sin conexión (§5.1): SpeechRecognizer de Android,
 * preferentemente offline. No da puntaje fonético, solo qué palabras oyó.
 */
class EvaluadorDispositivo @Inject constructor(@ApplicationContext private val context: Context) {

    class SinReconocedor : Exception("Tu teléfono no tiene reconocimiento de voz disponible.")

    fun disponible(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    // SpeechRecognizer solo funciona en el hilo principal.
    suspend fun evaluar(referencia: String): EvaluacionVoz = withContext(Dispatchers.Main) {
        if (!disponible()) throw SinReconocedor()
        val reconocedor = SpeechRecognizer.createSpeechRecognizer(context)
        try {
            val alternativas = suspendCancellableCoroutine { continuacion ->
                continuacion.invokeOnCancellation { reconocedor.cancel() }
                reconocedor.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(resultados: Bundle) {
                        val textos = resultados.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        val confianzas = resultados.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        continuacion.resume(textos.mapIndexed { i, texto -> texto to (confianzas?.getOrNull(i) ?: -1f) })
                    }

                    override fun onError(error: Int) {
                        when (error) {
                            // No oyó nada entendible: se trata como "no legible", no como falla.
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> continuacion.resume(emptyList())
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                continuacion.resumeWithException(SecurityException("Permite el uso del micrófono para jugar."))
                            else -> continuacion.resumeWithException(Exception("No se pudo usar el reconocimiento de voz ($error)."))
                        }
                    }

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {}
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                reconocedor.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    }
                )
            }
            Pronunciacion.evaluarTranscripcion(referencia, alternativas)
        } finally {
            reconocedor.destroy()
        }
    }
}
