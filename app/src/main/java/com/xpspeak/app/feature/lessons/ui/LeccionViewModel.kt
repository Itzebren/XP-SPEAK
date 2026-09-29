package com.xpspeak.app.feature.lessons.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.lessons.data.EnvioIntento
import com.xpspeak.app.feature.lessons.data.IntentoResponse
import com.xpspeak.app.feature.lessons.data.LeccionDto
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

    init {
        viewModelScope.launch {
            repository.leccion(leccionId)
                .onSuccess { leccion ->
                    _uiState.value = _uiState.value.copy(cargando = false, leccion = leccion)
                    // RN-12: marca la lección como "en_progreso" al abrir la teoría.
                    repository.guardarAvance(leccionId, 0)
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }

    fun responder(itemId: String, respuesta: RespuestaUsuario) {
        _uiState.value = _uiState.value.copy(respuestas = _uiState.value.respuestas + (itemId to respuesta))
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
