package com.xpspeak.app.feature.lessons.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.lessons.data.EnvioIntento
import com.xpspeak.app.feature.lessons.data.ErrorDelServidor
import com.xpspeak.app.feature.lessons.data.IntentoResponse
import com.xpspeak.app.feature.lessons.data.LeccionDto
import com.xpspeak.app.feature.lessons.data.LeccionEnCurso
import com.xpspeak.app.feature.lessons.data.LeccionesRepository
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LeccionUiState(
    val cargando: Boolean = true,
    val leccion: LeccionDto? = null,
    val respuestas: Map<String, RespuestaUsuario> = emptyMap(), // id del ítem → respuesta
    /** Un id por intento: si se reintenta el envío tras un error, el servidor no duplica XP (§6.2). */
    val attemptId: String = UUID.randomUUID().toString(),
    val enviando: Boolean = false,
    val resultado: IntentoResponse? = null,
    /** Calificado en el teléfono sin conexión; el XP se confirma al sincronizar. */
    val pendiente: Boolean = false,
    /** CU-06 A2: hay una sesión anterior sin terminar; se pregunta si retomarla. */
    val porRetomar: LeccionEnCurso? = null,
    /** Desplazamiento al que la pantalla debe volver al retomar (se consume una vez). */
    val posicionPorRestaurar: Int? = null,
    /** RN-07: es un tema nuevo y antes hay que repasar los conceptos débiles. */
    val requiereRepaso: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class LeccionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: LeccionesRepository
) : ViewModel() {

    private val leccionId: String = checkNotNull(savedStateHandle["id"])

    private val _uiState = MutableStateFlow(LeccionUiState())
    val uiState: StateFlow<LeccionUiState> = _uiState.asStateFlow()

    /** Último desplazamiento de la pantalla, para guardarlo junto con las respuestas. */
    private var posicion = 0

    init {
        viewModelScope.launch {
            repository.leccion(leccionId)
                .onSuccess { leccion ->
                    val enCurso = repository.leccionEnCurso(leccionId)
                        ?.takeIf { it.respuestas.isNotEmpty() || it.posicion > 0 }
                    _uiState.value = _uiState.value.copy(cargando = false, leccion = leccion, porRetomar = enCurso)
                    // RN-12: marca la lección como "en_progreso" al abrir la teoría. RN-07: si es un tema
                    // nuevo y toca repasar, el servidor responde 409. Sin conexión no pasa nada.
                    repository.guardarAvance(leccionId, 0).onFailure { error ->
                        if ((error as? ErrorDelServidor)?.esRepasoPendiente == true) {
                            _uiState.value = _uiState.value.copy(requiereRepaso = true, porRetomar = null)
                        }
                    }
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }

    /** CU-06 A2: vuelve a poner las respuestas y la posición de la sesión anterior. */
    fun retomar() {
        val enCurso = _uiState.value.porRetomar ?: return
        posicion = enCurso.posicion
        _uiState.value = _uiState.value.copy(
            respuestas = enCurso.respuestas,
            attemptId = enCurso.attemptId,
            porRetomar = null,
            posicionPorRestaurar = enCurso.posicion
        )
    }

    fun empezarDeNuevo() {
        _uiState.value = _uiState.value.copy(porRetomar = null)
        viewModelScope.launch { repository.descartarEnCurso(leccionId) }
    }

    fun posicionRestaurada() {
        _uiState.value = _uiState.value.copy(posicionPorRestaurar = null)
    }

    fun responder(itemId: String, respuesta: RespuestaUsuario) {
        _uiState.value = _uiState.value.copy(respuestas = _uiState.value.respuestas + (itemId to respuesta))
        guardarEnCurso()
    }

    /** La pantalla lo llama al pasar a segundo plano: si el sistema cierra la app, se retoma aquí. */
    fun guardarPosicion(nueva: Int) {
        posicion = nueva
        guardarEnCurso()
    }

    /** RN-12: cada cambio queda en Room; un cierre inesperado no pierde lo contestado. */
    private fun guardarEnCurso() {
        val estado = _uiState.value
        // Mientras se pregunta si retomar no se pisa la sesión anterior; con resultado ya no hay nada a medias.
        if (estado.leccion == null || estado.porRetomar != null || estado.resultado != null || estado.requiereRepaso) return
        viewModelScope.launch {
            repository.guardarEnCurso(leccionId, LeccionEnCurso(estado.attemptId, estado.respuestas, posicion))
        }
    }

    fun reintentar() {
        _uiState.value = _uiState.value.copy(
            respuestas = emptyMap(),
            attemptId = UUID.randomUUID().toString(),
            resultado = null,
            pendiente = false,
            error = null
        )
    }

    fun enviar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, error = null)
            val estado = _uiState.value
            repository.enviarIntento(leccionId, estado.attemptId, estado.respuestas)
                .onSuccess { envio ->
                    // Calificado (o en la cola de envío): ya no hay una lección a medias que retomar.
                    repository.descartarEnCurso(leccionId)
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        resultado = envio.resultado,
                        pendiente = envio is EnvioIntento.Pendiente
                    )
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(enviando = false, error = excepcion.message)
                }
        }
    }
}
