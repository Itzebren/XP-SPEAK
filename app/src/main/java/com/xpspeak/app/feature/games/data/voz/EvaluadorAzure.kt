package com.xpspeak.app.feature.games.data.voz

import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGradingSystem
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentGranularity
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.audio.AudioStreamFormat
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.Texto
import com.xpspeak.app.feature.voice.data.MedidorVoz
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Azure AI Speech – Pronunciation Assessment (§5.1, PDF §1.2) de una frase de
 * referencia: Eco Vocal, Práctica de pronunciación y las frases de una lección.
 * El micrófono se lee con [MicrofonoPcm] y se transmite a Azure en streaming con el
 * token temporal del backend; no se graba ningún archivo (RN-04). Leer el micrófono
 * en la app (y no dejárselo al SDK) permite mostrar el volumen mientras se habla y
 * cerrar el stream en cuanto el usuario toca «Listo»: Azure califica al instante lo
 * que ya tiene en vez de esperar la pausa de silencio.
 */
class EvaluadorAzure @Inject constructor() {

    /** Error de Azure que no es "no se entendió" (red, token vencido…): se usa el respaldo. */
    class FalloAzure(mensaje: String) : Exception(mensaje)

    private companion object {
        /**
         * Silencio con el que Azure da por terminada la frase. Antes eran 1.5 s más 3 s de
         * cierre, y la calificación tardaba tanto que el usuario repetía la frase creyendo que
         * no se le oía. Una pausa a media oración sigue sin cortarla; y si la frase es de una
         * o dos palabras no hay dónde hacer pausa, así que se cierra antes.
         */
        const val PAUSA_FIN_FRASE_MS = 1000L
        const val PAUSA_FIN_PALABRA_MS = 700L
        const val PALABRAS_CORTA = 2

        fun pausaPara(referencia: String) =
            if (Texto.palabras(referencia).size <= PALABRAS_CORTA) PAUSA_FIN_PALABRA_MS else PAUSA_FIN_FRASE_MS
    }

    suspend fun evaluar(
        referencia: String,
        token: TokenVozDto,
        medida: String = "voz.eco_vocal",
        sesion: SesionEscucha = SesionEscucha {}
    ): EvaluacionVoz = withContext(Dispatchers.IO) {
        val pausaMs = pausaPara(referencia)
        val config = SpeechConfig.fromAuthorizationToken(token.token, token.region).apply {
            speechRecognitionLanguage = "en-US"
            // Con los tiempos de fábrica Azure se rinde si el usuario tarda en empezar y corta en
            // la primera pausa, y todo sale "no legible".
            setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "15000")
            setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, pausaMs.toString())
            setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, pausaMs.toString())
        }
        val formato = AudioStreamFormat.getWaveFormatPCM(MicrofonoPcm.FRECUENCIA_HZ.toLong(), 16, 1)
        val flujo = PushAudioInputStream.create(formato)
        val audio = AudioConfig.fromStreamInput(flujo)
        val reconocedor = SpeechRecognizer(config, audio)
        val medidor = MedidorVoz(reconocedor, medida, pausaMs)
        val evaluacion = PronunciationAssessmentConfig(
            referencia,
            PronunciationAssessmentGradingSystem.HundredMark,
            PronunciationAssessmentGranularity.Phoneme,
            true // miscue: marca palabras omitidas o de más
        )
        // Qué sonido se oyó en lugar de cada sonido esperado (AnalisisFonemas).
        evaluacion.setPhonemeAlphabet("IPA")
        evaluacion.setNBestPhonemeCount(5)
        // Lo que va entendiendo y cuándo detecta voz: la pantalla lo muestra al momento.
        reconocedor.recognizing.addEventListener { _, evento -> sesion.oyendo(evento.result.text.orEmpty()) }
        reconocedor.speechStartDetected.addEventListener { _, _ -> sesion.vozDetectada() }
        reconocedor.speechEndDetected.addEventListener { _, _ -> sesion.calificando() }
        val microfono = MicrofonoPcm(flujo, sesion)
        try {
            evaluacion.applyTo(reconocedor)
            microfono.abrir()
            sesion.alTerminar = microfono::cerrar
            coroutineScope {
                val captura = launch { microfono.transmitir() }
                sesion.escuchando()
                val futuro = reconocedor.recognizeOnceAsync()
                val resultado = try {
                    suspendCancellableCoroutine { continuacion ->
                        continuacion.invokeOnCancellation { runCatching { futuro.cancel(true) } }
                        try {
                            continuacion.resume(futuro.get())
                        } catch (error: ExecutionException) {
                            continuacion.resumeWithException(FalloAzure(error.cause?.message ?: "Azure no respondió"))
                        }
                    }
                } finally {
                    microfono.cerrar()
                    captura.cancel()
                }
                resultado.use {
                    when (it.reason) {
                        ResultReason.RecognizedSpeech -> ConversorAzure.convertir(it, referencia).also { _ -> medidor.registrar(it) }
                        // Silencio o ruido: "no fue legible", se pide repetir (CU-05 A1).
                        ResultReason.NoMatch -> ConversorAzure.noLegible(referencia)
                        else -> throw FalloAzure("Azure canceló el reconocimiento (${it.reason})")
                    }
                }
            }
        } finally {
            microfono.cerrar()
            evaluacion.close()
            reconocedor.close()
            audio.close()
            flujo.close()
            formato.close()
            config.close()
        }
    }
}
