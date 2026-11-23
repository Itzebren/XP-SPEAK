package com.xpspeak.app.feature.games.ui.comun

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.ResultadoPartida
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.random.Random

/** Estado de un juego: se guarda en Room tal cual (Gson), así que solo lleva datos. */
interface EstadoPartida<E> {
    val partidaId: String
    val marcador: Marcador
    fun conMarcador(marcador: Marcador): E
}

sealed interface FasePartida<out E> {
    data object Cargando : FasePartida<Nothing>
    /** CU-07 A1: hay una partida a medias; se pregunta si retomarla (como CU-06 A2). */
    data class Retomar<E>(val guardada: E) : FasePartida<E>
    data class Jugando<E>(val estado: E) : FasePartida<E>
    data object Guardando : FasePartida<Nothing>
    data class Terminada(val resultado: ResultadoPartida) : FasePartida<Nothing>
    /** CU-07 A2: no hay contenido desbloqueado para este juego. */
    data class SinContenido(val mensaje: String) : FasePartida<Nothing>
    data class Error(val mensaje: String) : FasePartida<Nothing>
}

/**
 * Ciclo común de una partida (docs/minijuegos-diseno.md §4.1, Ilustración 20):
 * crear rondas, guardar el avance en Room después de cada acción, contar
 * solo el tiempo con la pantalla activa y cerrar la partida con su XP.
 * Cada juego implementa solo su mecánica.
 */
abstract class PartidaViewModel<E : EstadoPartida<E>>(
    protected val repository: MinijuegosRepository,
    private val juego: Minijuego,
    private val claseEstado: Class<E>
) : ViewModel() {

    private val _fase = MutableStateFlow<FasePartida<E>>(FasePartida.Cargando)
    val fase: StateFlow<FasePartida<E>> = _fase.asStateFlow()

    // Las escrituras van en orden: un guardado tardío no debe revivir una partida ya terminada.
    private val escritura = Mutex()
    private var tiempoBaseMs = 0L
    private var activoDesde: Long? = null

    protected val random: Random = Random.Default
    protected val estadoActual: E? get() = (_fase.value as? FasePartida.Jugando<E>)?.estado

    /** null si no hay contenido suficiente para jugar (CU-07 A2). */
    protected abstract fun crearPartida(contenido: ContenidoJuegos, partidaId: String): E?

    protected open val mensajeSinContenido = "Completa más lecciones para desbloquear este juego."

    /** Al empezar o retomar (p. ej. arrancar el reloj de Ráfaga). */
    protected open fun alJugar(estado: E) {}

    protected open fun alPausar() {}

    init {
        viewModelScope.launch {
            val guardada = runCatching { repository.partidaGuardada(juego, claseEstado) }.getOrNull()
            if (guardada != null) _fase.value = FasePartida.Retomar(guardada) else nuevaPartida()
        }
    }

    fun retomar() {
        (_fase.value as? FasePartida.Retomar<E>)?.let { jugar(it.guardada) }
    }

    fun empezarDeNuevo() {
        viewModelScope.launch {
            escritura.withLock { runCatching { repository.descartarPartida(juego) } }
            nuevaPartida()
        }
    }

    private suspend fun nuevaPartida() {
        _fase.value = FasePartida.Cargando
        runCatching { repository.contenido() }
            .onSuccess { contenido ->
                val estado = crearPartida(contenido, UUID.randomUUID().toString())
                if (estado == null) {
                    _fase.value = FasePartida.SinContenido(mensajeSinContenido)
                } else {
                    jugar(estado)
                    guardar(estado)
                }
            }
            .onFailure { _fase.value = FasePartida.Error(it.message ?: "No se pudo cargar el juego.") }
    }

    private fun jugar(estado: E) {
        tiempoBaseMs = estado.marcador.tiempoJugadoMs
        activoDesde = SystemClock.elapsedRealtime()
        _fase.value = FasePartida.Jugando(estado)
        alJugar(estado)
    }

    /** Cada acción del usuario: actualiza la pantalla y guarda el avance (RNF-10). */
    protected fun actualizar(nuevo: E, guardar: Boolean = true) {
        if (_fase.value !is FasePartida.Jugando<*>) return
        _fase.value = FasePartida.Jugando(nuevo)
        if (guardar) guardar(nuevo)
    }

    private fun guardar(estado: E) {
        val conTiempo = estado.conMarcador(estado.marcador.copy(tiempoJugadoMs = tiempoJugado()))
        viewModelScope.launch {
            escritura.withLock {
                if (_fase.value is FasePartida.Jugando<*>) {
                    runCatching { repository.guardarPartida(juego, conTiempo.partidaId, conTiempo) }
                }
            }
        }
    }

    private fun tiempoJugado(): Long =
        tiempoBaseMs + (activoDesde?.let { SystemClock.elapsedRealtime() - it } ?: 0L)

    /** La app pasó a segundo plano: el tiempo deja de contar y se guarda el avance. */
    fun pausar() {
        val estado = estadoActual ?: return
        if (activoDesde == null) return
        tiempoBaseMs = tiempoJugado()
        activoDesde = null
        alPausar()
        guardar(estado)
    }

    fun reanudar() {
        val estado = estadoActual ?: return
        if (activoDesde != null) return
        activoDesde = SystemClock.elapsedRealtime()
        alJugar(estado)
    }

    /** RN-09: solo aquí se gana XP. */
    protected fun terminar(final: E) {
        val marcador = final.marcador.copy(tiempoJugadoMs = tiempoJugado())
        activoDesde = null
        alPausar()
        _fase.value = FasePartida.Guardando
        viewModelScope.launch {
            escritura.withLock {
                runCatching { repository.terminarPartida(juego, final.partidaId, marcador) }
                    .onSuccess { _fase.value = FasePartida.Terminada(it) }
                    .onFailure { _fase.value = FasePartida.Error(it.message ?: "No se pudo guardar la partida.") }
            }
        }
    }

    fun jugarOtraVez() {
        viewModelScope.launch { nuevaPartida() }
    }
}
