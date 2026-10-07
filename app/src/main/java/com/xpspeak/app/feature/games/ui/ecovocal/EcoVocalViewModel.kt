package com.xpspeak.app.feature.games.ui.ecovocal

import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.data.voz.EvaluadorVoz
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Mazo
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.domain.RondaEco
import com.xpspeak.app.feature.games.ui.comun.EstadoPartida
import com.xpspeak.app.feature.games.ui.comun.PartidaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RevisionEco(val acierto: Boolean, val definitiva: Boolean)

data class EstadoEco(
    override val partidaId: String,
    override val marcador: Marcador,
    val mazo: Mazo<RondaEco>,
    val dificultad: Dificultad,
    val ronda: RondaEco,
    val numeroRonda: Int,
    val intento: Int = 1,
    /** Último intento evaluado; si no fue legible, `revision` queda en null y se repite. */
    val evaluacion: EvaluacionVoz? = null,
    val revision: RevisionEco? = null
) : EstadoPartida<EstadoEco> {
    override fun conMarcador(marcador: Marcador) = copy(marcador = marcador)
}

/**
 * Eco Vocal (§5.1): escuchar la frase, repetirla y recibir el puntaje de
 * pronunciación. ≥ 70 es acierto; hay 2 intentos por frase. Si el audio no
 * fue legible (confianza < 30%) se pide repetir sin gastar el intento.
 */
@HiltViewModel
class EcoVocalViewModel @Inject constructor(
    repository: MinijuegosRepository,
    private val evaluador: EvaluadorVoz
) : PartidaViewModel<EstadoEco>(repository, Minijuego.ECO_VOCAL, EstadoEco::class.java) {

    private val _escuchando = MutableStateFlow(false)
    val escuchando: StateFlow<Boolean> = _escuchando.asStateFlow()

    private val _aviso = MutableStateFlow<String?>(null)
    /** Problema con el micrófono o el reconocedor (no es un error de pronunciación). */
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    private var escucha: Job? = null

    override fun crearPartida(contenido: ContenidoJuegos, partidaId: String): EstadoEco? {
        val mazo = GeneradorRondas.rondasEco(contenido, random)
        if (mazo.restantes < GeneradorRondas.RONDAS_ECO) return null
        val dificultad = Dificultad()
        val (ronda, resto) = mazo.sacar(dificultad.nivel) ?: return null
        return EstadoEco(partidaId, Marcador(), resto, dificultad, ronda, numeroRonda = 1)
    }

    override fun alPausar() {
        escucha?.cancel()
    }

    fun hablar() {
        val estado = estadoActual ?: return
        if (estado.revision != null || _escuchando.value) return
        _aviso.value = null
        escucha = viewModelScope.launch {
            _escuchando.value = true
            try {
                evaluar(estado, evaluador.evaluar(estado.ronda.en))
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                _aviso.value = "Permite el uso del micrófono para jugar Eco Vocal."
            } catch (error: Exception) {
                _aviso.value = error.message ?: "No se pudo escuchar. Intenta de nuevo."
            } finally {
                _escuchando.value = false
            }
        }
    }

    fun permisoDenegado() {
        _aviso.value = "Eco Vocal necesita el micrófono. Puedes darle permiso en los ajustes del teléfono."
    }

    private fun evaluar(estado: EstadoEco, evaluacion: EvaluacionVoz) {
        val actual = estadoActual ?: return
        if (actual.ronda != estado.ronda || actual.intento != estado.intento) return
        // CU-05 A1 / Ilustración 40: audio no legible, se repite sin gastar el intento.
        if (!Pronunciacion.esLegible(evaluacion)) {
            actualizar(actual.copy(evaluacion = evaluacion), guardar = false)
            return
        }
        val acierto = Pronunciacion.esAcierto(evaluacion)
        val definitiva = acierto || actual.intento >= Pronunciacion.INTENTOS_POR_RONDA
        var marcador = actual.marcador.conSenal(actual.ronda.conceptoId, acierto)
        if (definitiva) marcador = marcador.conRonda(acierto)
        actualizar(
            actual.copy(
                marcador = marcador,
                dificultad = if (definitiva) actual.dificultad.registrar(acierto) else actual.dificultad,
                evaluacion = evaluacion,
                revision = RevisionEco(acierto, definitiva)
            )
        )
    }

    fun continuar() {
        val estado = estadoActual ?: return
        val revision = estado.revision ?: return
        if (!revision.definitiva) {
            actualizar(estado.copy(intento = estado.intento + 1, evaluacion = null, revision = null))
        } else {
            siguienteRonda(estado)
        }
    }

    /** Si el micrófono no funciona, la frase cuenta como no lograda y se sigue. */
    fun saltar() {
        val estado = estadoActual ?: return
        escucha?.cancel()
        _aviso.value = null
        siguienteRonda(
            estado.copy(
                marcador = estado.marcador.conRonda(acierto = false),
                dificultad = estado.dificultad.registrar(false)
            )
        )
    }

    private fun siguienteRonda(estado: EstadoEco) {
        val siguiente = estado.mazo.sacar(estado.dificultad.nivel)
        if (estado.numeroRonda >= GeneradorRondas.RONDAS_ECO || siguiente == null) {
            terminar(estado)
            return
        }
        val (ronda, resto) = siguiente
        actualizar(
            estado.copy(mazo = resto, ronda = ronda, numeroRonda = estado.numeroRonda + 1, intento = 1, evaluacion = null, revision = null)
        )
    }
}
