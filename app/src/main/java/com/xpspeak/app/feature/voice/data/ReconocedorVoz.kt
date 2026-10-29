package com.xpspeak.app.feature.voice.data

import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** RF-07: reconoce una frase hablada en inglés y la devuelve como texto. */
class ReconocedorVoz @Inject constructor(
    private val speechRepository: SpeechRepository
) {
    suspend fun reconocerUnaVez(idioma: String = "en-US"): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cred = speechRepository.obtenerCredenciales().getOrThrow()

                val config = SpeechConfig.fromAuthorizationToken(cred.token, cred.region)
                config.speechRecognitionLanguage = idioma
                val audio = AudioConfig.fromDefaultMicrophoneInput()
                val reconocedor = SpeechRecognizer(config, audio)

                try {
                    val resultado = reconocedor.recognizeOnceAsync().get()
                    when (resultado.reason) {
                        ResultReason.RecognizedSpeech -> resultado.text
                        ResultReason.NoMatch ->
                            throw Exception("No te escuché bien. Intenta de nuevo.")
                        ResultReason.Canceled -> {
                            val detalle = CancellationDetails.fromResult(resultado)
                            throw Exception("Error de voz: ${detalle.errorDetails}")
                        }
                        else -> throw Exception("No se pudo reconocer el audio.")
                    }
                } finally {
                    reconocedor.close()
                    audio.close()
                    config.close()
                }
            }
        }
}
