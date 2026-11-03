package com.xpspeak.app.feature.games.data.voz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

    /** El paquete de inglés sin conexión no está instalado: se reintenta con el reconocedor en línea. */
    private class SinIdiomaOffline : Exception()

    // SpeechRecognizer solo funciona en el hilo principal.
    suspend fun evaluar(referencia: String, sesion: SesionEscucha? = null): EvaluacionVoz = withContext(Dispatchers.Main) {
        Pronunciacion.evaluarTranscripcion(referencia, alternativas(sesion = sesion))
    }

    /**
     * Chat en vivo sin Azure: solo transcribe lo que se dijo, avisando con [onParcial] mientras
     * habla, para poder conversar. No califica la pronunciación.
     */
    suspend fun transcribir(onParcial: (String) -> Unit = {}): EvaluacionVoz = withContext(Dispatchers.Main) {
        Pronunciacion.soloTranscripcion(alternativas(onParcial))
    }

    private suspend fun alternativas(onParcial: (String) -> Unit = {}, sesion: SesionEscucha? = null): List<Pair<String, Float>> {
        if (!disponible()) throw SinReconocedor()
        return try {
            escuchar(offline = true, onParcial, sesion)
        } catch (error: SinIdiomaOffline) {
            escuchar(offline = false, onParcial, sesion)
        }
    }

    /** [sesion]: a quién avisar el volumen y lo que se va oyendo; el botón «Listo» corta la escucha. */
    private suspend fun escuchar(offline: Boolean, onParcial: (String) -> Unit, sesion: SesionEscucha?): List<Pair<String, Float>> {
        val reconocedor = SpeechRecognizer.createSpeechRecognizer(context)
        try {
            return suspendCancellableCoroutine { continuacion ->
                continuacion.invokeOnCancellation { reconocedor.cancel() }
                // Lo último que oyó a media frase: algunos motores terminan en NO_MATCH aunque ya
                // hubieran entendido, y sin esto la frase dicha salía como "no legible".
                var parcial: String? = null
                reconocedor.setRecognitionListener(object : RecognitionListener {
                    override fun onResults(resultados: Bundle) {
                        val textos = resultados.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                        val confianzas = resultados.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        val alternativas = textos.mapIndexed { i, texto -> texto to (confianzas?.getOrNull(i) ?: -1f) }
                        continuacion.resume(alternativas.ifEmpty { listOfNotNull(parcial?.let { it to -1f }) })
                    }

                    override fun onError(error: Int) {
                        when (error) {
                            // No oyó nada entendible: se trata como "no legible", no como falla.
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                continuacion.resume(listOfNotNull(parcial?.let { it to -1f }))
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                continuacion.resumeWithException(SecurityException("Permite el uso del micrófono para jugar."))
                            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                                continuacion.resumeWithException(
                                    if (offline) SinIdiomaOffline()
                                    else Exception("Tu teléfono no tiene reconocimiento de voz en inglés.")
                                )
                            else -> continuacion.resumeWithException(Exception("No se pudo usar el reconocimiento de voz ($error)."))
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.firstOrNull()?.takeIf { it.isNotBlank() }?.let { parcial = it; onParcial(it); sesion?.oyendo(it) }
                    }

                    override fun onReadyForSpeech(params: Bundle?) { sesion?.escuchando() }
                    override fun onBeginningOfSpeech() { sesion?.vozDetectada() }
                    override fun onRmsChanged(rmsdB: Float) { sesion?.nivel(NivelAudio.deRmsAndroid(rmsdB)) }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { sesion?.calificando() }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                reconocedor.startListening(
                    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, offline)
                        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                        // Una pausa corta a media oración no da la frase por terminada.
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                    }
                )
                // «Listo»: stopListening entrega lo que ya oyó (o NO_MATCH si no oyó nada).
                // Puede llegar desde otro hilo y SpeechRecognizer solo acepta el principal.
                sesion?.alTerminar = {
                    Handler(Looper.getMainLooper()).post { if (continuacion.isActive) runCatching { reconocedor.stopListening() } }
                }
            }
        } finally {
            reconocedor.destroy()
        }
    }
}
