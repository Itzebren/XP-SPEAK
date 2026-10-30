package com.xpspeak.app.feature.games.ui.orden

import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Mazo
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.RondaOrden
import com.xpspeak.app.feature.games.domain.Texto
import com.xpspeak.app.feature.games.ui.comun.EstadoPartida
import com.xpspeak.app.feature.games.ui.comun.PartidaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Resultado de armar la oración; `definitiva` = ya no hay otro intento en esta ronda. */
data class RevisionOrden(val correcta: Boolean, val posicionesMal: List<Int>, val definitiva: Boolean)

data class EstadoOrden(
    override val partidaId: String,
    override val marcador: Marcador,
    val mazo: Mazo<RondaOrden>,
    val dificultad: Dificultad,
    val ronda: RondaOrden,
    val numeroRonda: Int,
    /** Índices de `ronda.fichas` en el orden en que se colocaron. */
    val colocadas: List<Int> = emptyList(),
    val intento: Int = 1,
    val revision: RevisionOrden? = null
) : EstadoPartida<EstadoOrden> {
    override fun conMarcador(marcador: Marcador) = copy(marcador = marcador)
}

/**
 * Orden Maestro (§5.4): armar la oración tocando las fichas. Un segundo
 * intento por oración; la dificultad adaptativa elige oraciones más largas.
 */
@HiltViewModel
class OrdenMaestroViewModel @Inject constructor(
    repository: MinijuegosRepository
) : PartidaViewModel<EstadoOrden>(repository, Minijuego.ORDEN_MAESTRO, EstadoOrden::class.java) {

    override fun crearPartida(contenido: ContenidoJuegos, partidaId: String): EstadoOrden? {
        val mazo = GeneradorRondas.rondasOrden(contenido, random)
        if (mazo.restantes < GeneradorRondas.RONDAS_ORDEN) return null
        val dificultad = Dificultad()
        val (ronda, resto) = mazo.sacar(dificultad.nivel) ?: return null
        return EstadoOrden(partidaId, Marcador(), resto, dificultad, ronda, numeroRonda = 1)
    }

    fun colocar(indiceFicha: Int) {
        val estado = estadoActual ?: return
        if (estado.revision != null || indiceFicha in estado.colocadas) return
        val colocadas = estado.colocadas + indiceFicha
        if (colocadas.size == estado.ronda.fichas.size) revisar(estado.copy(colocadas = colocadas))
        else actualizar(estado.copy(colocadas = colocadas), guardar = false)
    }

    fun quitar(posicion: Int) {
        val estado = estadoActual ?: return
        if (estado.revision != null) return
        actualizar(estado.copy(colocadas = estado.colocadas.filterIndexed { i, _ -> i != posicion }), guardar = false)
    }

    private fun revisar(estado: EstadoOrden) {
        val ronda = estado.ronda
        val armada = estado.colocadas.map { ronda.fichas[it] }
        val correcta = GeneradorRondas.ordenCorrecto(armada, ronda.solucion)
        val definitiva = correcta || estado.intento >= INTENTOS
        val posicionesMal = armada.indices.filter { Texto.normalizar(armada[it]) != Texto.normalizar(ronda.solucion[it]) }
        var marcador = estado.marcador.conSenal(ronda.conceptoId, correcta)
        if (definitiva) marcador = marcador.conRonda(correcta)
        actualizar(
            estado.copy(
                marcador = marcador,
                dificultad = if (definitiva) estado.dificultad.registrar(correcta) else estado.dificultad,
                revision = RevisionOrden(correcta, posicionesMal, definitiva)
            )
        )
    }

    /** Tras la revisión: segundo intento, siguiente oración o fin de la partida. */
    fun continuar() {
        val estado = estadoActual ?: return
        val revision = estado.revision ?: return
        if (!revision.definitiva) {
            actualizar(estado.copy(colocadas = emptyList(), intento = estado.intento + 1, revision = null))
            return
        }
        val siguiente = estado.mazo.sacar(estado.dificultad.nivel)
        if (estado.numeroRonda >= GeneradorRondas.RONDAS_ORDEN || siguiente == null) {
            terminar(estado)
            return
        }
        val (ronda, resto) = siguiente
        actualizar(
            estado.copy(mazo = resto, ronda = ronda, numeroRonda = estado.numeroRonda + 1, colocadas = emptyList(), intento = 1, revision = null)
        )
    }

    private companion object {
        const val INTENTOS = 2
    }
}
