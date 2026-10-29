package com.xpspeak.app.feature.games.data.voz

import android.util.Log
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.voice.data.SpeechRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Elige quién evalúa cada intento de Eco Vocal: Azure si hay red y el
 * backend tiene el recurso configurado; si no, el reconocedor del teléfono.
 * El token sale del mismo SpeechRepository que usa el chat: en debug el
 * backend de minijuegos es el local (localhost:3000), que no tiene Azure,
 * y Eco Vocal terminaba siempre en el reconocedor del teléfono.
 */
@Singleton
class EvaluadorVoz @Inject constructor(
    private val speechRepository: SpeechRepository,
    private val azure: EvaluadorAzure,
    private val dispositivo: EvaluadorDispositivo
) {
    private val pidiendoToken = Mutex()
    /** Sin red o sin Azure en el backend: no se vuelve a preguntar en cada intento. */
    private var sinAzureHasta = 0L

    suspend fun evaluar(referencia: String): EvaluacionVoz {
        val vigente = tokenVigente() ?: return dispositivo.evaluar(referencia)
        return try {
            azure.evaluar(referencia, vigente)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            // Token rechazado, red caída a media frase o falla del SDK: se usa el respaldo.
            Log.w("EvaluadorVoz", "Azure falló, se usa el reconocedor del teléfono: ${error.message}")
            dispositivo.evaluar(referencia)
        }
    }

    // SpeechRepository ya reutiliza el token mientras no esté por vencer.
    private suspend fun tokenVigente(): TokenVozDto? = pidiendoToken.withLock {
        val ahora = System.currentTimeMillis()
        if (ahora < sinAzureHasta) return@withLock null
        speechRepository.obtenerCredenciales()
            .onFailure {
                Log.w("EvaluadorVoz", "Sin token de Azure, se usa el reconocedor del teléfono: ${it.message}")
                sinAzureHasta = ahora + REINTENTO_MS
            }
            .getOrNull()
            ?.let { TokenVozDto(it.token, it.region, expiraEn = ahora + VIGENCIA_MINIMA_MS) }
    }

    /** Fuera de una ronda (al empezar la partida): ¿se usará Azure? Para avisar al usuario. */
    suspend fun azureDisponible(): Boolean = tokenVigente() != null

    private companion object {
        const val REINTENTO_MS = 2 * 60_000L
        // SpeechRepository entrega tokens de a lo más 8 min de uso; quedan ≥ 2 min.
        const val VIGENCIA_MINIMA_MS = 2 * 60_000L
    }
}
