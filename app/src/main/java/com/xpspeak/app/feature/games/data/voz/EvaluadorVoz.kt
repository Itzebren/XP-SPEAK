package com.xpspeak.app.feature.games.data.voz

import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.data.TokenVozDto
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Elige quién evalúa cada intento de Eco Vocal: Azure si hay red y el
 * backend tiene el recurso configurado; si no, el reconocedor del teléfono.
 * El token se reutiliza mientras no esté por vencer.
 */
@Singleton
class EvaluadorVoz @Inject constructor(
    private val repository: MinijuegosRepository,
    private val azure: EvaluadorAzure,
    private val dispositivo: EvaluadorDispositivo
) {
    private val pidiendoToken = Mutex()
    private var token: TokenVozDto? = null
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
            token = null
            dispositivo.evaluar(referencia)
        }
    }

    private suspend fun tokenVigente(): TokenVozDto? = pidiendoToken.withLock {
        val ahora = System.currentTimeMillis()
        token?.takeIf { it.expiraEn - MARGEN_MS > ahora }?.let { return@withLock it }
        if (ahora < sinAzureHasta) return@withLock null
        runCatching { repository.tokenVoz() }.getOrNull()
            .also { token = it }
            .also { if (it == null) sinAzureHasta = ahora + REINTENTO_MS }
    }

    /** Fuera de una ronda (al empezar la partida): ¿se usará Azure? Para avisar al usuario. */
    suspend fun azureDisponible(): Boolean = tokenVigente() != null

    private companion object {
        const val MARGEN_MS = 60_000L
        const val REINTENTO_MS = 2 * 60_000L
    }
}
