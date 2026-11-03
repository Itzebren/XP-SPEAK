package com.xpspeak.app.feature.games.data.voz

import android.util.Log
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion
import com.xpspeak.app.feature.voice.data.SpeechRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Único punto de entrada de la voz en la app (RF-07, RF-09): elige quién evalúa cada
 * intento. Azure si hay red y el backend tiene el recurso configurado; si no, el
 * reconocedor del teléfono. Lo usan Eco Vocal, la Práctica de pronunciación, las frases
 * de una lección y el chat en vivo, así ninguna pantalla se queda sin voz porque Azure
 * no esté disponible.
 */
@Singleton
class EvaluadorVoz @Inject constructor(
    private val speechRepository: SpeechRepository,
    private val azure: EvaluadorAzure,
    private val azureLibre: EvaluadorPronunciacion,
    private val dispositivo: EvaluadorDispositivo
) {
    private val pidiendoToken = Mutex()
    /** Sin red o sin Azure en el backend: no se vuelve a preguntar en cada intento. */
    private var sinAzureHasta = 0L

    private val _escucha = MutableStateFlow<EstadoEscucha>(EstadoEscucha.Inactivo)
    /** Qué hace el micrófono ahora (una sola fuente para todas las pantallas con voz). */
    val escucha: StateFlow<EstadoEscucha> = _escucha.asStateFlow()
    @Volatile private var sesionActual: SesionEscucha? = null

    /**
     * Califica cómo se dijo [referencia]. Con Azure: fonema por fonema, fluidez y
     * completitud; con el teléfono: qué palabras se entendieron. Mientras tanto publica en
     * [escucha] el volumen y lo que va oyendo; [terminar] cierra el micrófono antes de la pausa.
     * @param medida nombre de la medición de latencia para la Tabla 93.
     */
    suspend fun evaluar(referencia: String, medida: String = "voz.eco_vocal"): EvaluacionVoz {
        val sesion = nuevaSesion()
        sesion.preparando()
        try {
            val vigente = tokenVigente() ?: return dispositivo.evaluar(referencia, sesion)
            return try {
                azure.evaluar(referencia, vigente, medida, sesion)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                // Token rechazado, red caída a media frase o falla del SDK: se usa el respaldo,
                // con una sesión nueva porque el micrófono se vuelve a abrir desde cero.
                Log.w(TAG, "Azure falló, se usa el reconocedor del teléfono: ${error.message}")
                dispositivo.evaluar(referencia, nuevaSesion())
            }
        } finally {
            sesionActual = null
            _escucha.value = EstadoEscucha.Inactivo
        }
    }

    /** Botón «Listo»: deja de escuchar y califica lo que ya se dijo. */
    fun terminar() {
        sesionActual?.terminar()
    }

    private fun nuevaSesion() = SesionEscucha { _escucha.value = it }.also { sesionActual = it }

    /**
     * Chat en vivo: una frase libre, avisando con [onParcial] lo que se va oyendo. Con Azure
     * se califica la pronunciación; sin Azure el teléfono solo transcribe para poder seguir
     * la conversación (la evaluación queda marcada como solo transcrita). El silencio del
     * usuario ([EvaluadorPronunciacion.SinVoz]) no es una falla de Azure: se propaga para
     * que el chat se pause.
     */
    suspend fun escucharFrase(onParcial: (String) -> Unit): EvaluacionVoz {
        if (tokenVigente() == null) return dispositivo.transcribir(onParcial)
        return azureLibre.escucharFrase(onParcial = onParcial).getOrElse { error ->
            if (error is CancellationException || error is EvaluadorPronunciacion.SinVoz) throw error
            Log.w(TAG, "Azure falló en el chat, el teléfono solo transcribe: ${error.message}")
            dispositivo.transcribir(onParcial)
        }
    }

    // SpeechRepository ya reutiliza el token mientras no esté por vencer.
    private suspend fun tokenVigente(): TokenVozDto? = pidiendoToken.withLock {
        val ahora = System.currentTimeMillis()
        if (ahora < sinAzureHasta) return@withLock null
        speechRepository.obtenerCredenciales()
            .onFailure {
                Log.w(TAG, "Sin token de Azure, se usa el reconocedor del teléfono: ${it.message}")
                sinAzureHasta = ahora + REINTENTO_MS
            }
            .getOrNull()
            ?.let { TokenVozDto(it.token, it.region, expiraEn = ahora + VIGENCIA_MINIMA_MS) }
    }

    /** Fuera de una ronda (al empezar la partida): ¿se usará Azure? Para avisar al usuario. */
    suspend fun azureDisponible(): Boolean = tokenVigente() != null

    private companion object {
        const val TAG = "EvaluadorVoz"
        const val REINTENTO_MS = 2 * 60_000L
        // SpeechRepository entrega tokens de a lo más 8 min de uso; quedan ≥ 2 min.
        const val VIGENCIA_MINIMA_MS = 2 * 60_000L
    }
}
