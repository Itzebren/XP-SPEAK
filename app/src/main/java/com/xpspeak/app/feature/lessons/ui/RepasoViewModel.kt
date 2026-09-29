package com.xpspeak.app.feature.lessons.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.lessons.data.LeccionesRepository
import com.xpspeak.app.feature.lessons.data.RepasoResponse
import com.xpspeak.app.feature.lessons.data.SesionRepasoDto
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class RepasoUiState(
    val cargando: Boolean = true,
    val sesion: SesionRepasoDto? = null,
    val respuestas: Map<String, RespuestaUsuario> = emptyMap(), // id del ejercicio → respuesta
    /** Un id por sesión: reenviar tras un error no aplica dos veces el repaso. */
    val attemptId: String = UUID.randomUUID().toString(),
    val enviando: Boolean = false,
    val resultado: RepasoResponse? = null,
    val error: String? = null
)

/** RF-13: sesión de repaso con los conceptos que SM-2 programó para hoy. */
@HiltViewModel
class RepasoViewModel @Inject constructor(
    private val repository: LeccionesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepasoUiState())
    val uiState: StateFlow<RepasoUiState> = _uiState.asStateFlow()

    init {
        cargar()
    }

    /** Pide la sesión de hoy; al terminar una, trae la siguiente tanda si quedan vencidos. */
    fun cargar() {
        viewModelScope.launch {
            _uiState.value = RepasoUiState(cargando = true)
            repository.sesionRepaso()
                .onSuccess { sesion -> _uiState.value = _uiState.value.copy(cargando = false, sesion = sesion) }
                .onFailure { excepcion -> _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message) }
        }
    }

    fun responder(itemId: String, respuesta: RespuestaUsuario) {
        _uiState.value = _uiState.value.copy(respuestas = _uiState.value.respuestas + (itemId to respuesta))
    }

    fun enviar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviando = true, error = null)
            val estado = _uiState.value
            repository.enviarRepaso(estado.attemptId, estado.respuestas)
                .onSuccess { resultado -> _uiState.value = _uiState.value.copy(enviando = false, resultado = resultado) }
                .onFailure { excepcion -> _uiState.value = _uiState.value.copy(enviando = false, error = excepcion.message) }
        }
    }
}
