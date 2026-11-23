package com.xpspeak.app.feature.lessons.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.medicion.Meta
import com.xpspeak.app.core.medicion.Tiempos
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    private val guardandoBorrador = Mutex()

    init {
        viewModelScope.launch {
            repository.leccion(leccionId)
                .onSuccess { leccion ->
                    // RN-12/RNF-10: si la evaluación quedó a medias, se retoma con las mismas respuestas.
                    val borrador = repository.borrador(leccionId)
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        leccion = leccion,
                        respuestas = borrador?.second ?: emptyMap(),
                        attemptId = borrador?.first ?: _uiState.value.attemptId
                    )
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
        // Al escribir en "completar" llega un cambio por tecla: el candado evita que un guardado
        // viejo termine después de uno nuevo, y cada uno lee el estado más reciente.
        viewModelScope.launch {
            guardandoBorrador.withLock {
                val estado = _uiState.value
                if (estado.resultado == null) repository.guardarBorrador(leccionId, estado.attemptId, estado.respuestas)
            }
        }
    }

    fun reintentar() {
        viewModelScope.launch { guardandoBorrador.withLock { repository.descartarBorrador(leccionId) } }
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
            val enviado = Tiempos.ahora()
            repository.enviarIntento(leccionId, estado.attemptId, estado.respuestas)
                .onSuccess { envio ->
                    // RN-05: la retroalimentación gramatical de la evaluación (sin red se califica en el teléfono).
                    Tiempos.desde(
                        "leccion.calificacion", enviado, Meta.RN05,
                        if (envio is EnvioIntento.Pendiente) "sin conexión, calificada en el teléfono" else null
                    )
                    _uiState.value = _uiState.value.copy(
                        enviando = false,
                        resultado = envio.resultado,
                        pendiente = envio is EnvioIntento.Pendiente
                    )
                    // Ya calificado (o en la cola offline): no hay nada que retomar.
                    guardandoBorrador.withLock { repository.descartarBorrador(leccionId) }
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(enviando = false, error = excepcion.message)
                }
        }
    }
}
