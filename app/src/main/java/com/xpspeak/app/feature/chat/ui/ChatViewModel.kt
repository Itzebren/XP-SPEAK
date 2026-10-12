package com.xpspeak.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.actividad.RegistroActividad
import com.xpspeak.app.feature.chat.data.ChatRepository
import com.xpspeak.app.feature.chat.data.MensajeChat
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class MensajeUi(val texto: String, val esUsuario: Boolean)

data class ChatUiState(
    val mensajes: List<MensajeUi> = emptyList(),
    val entrada: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    /** "Habla con IA" y otras insignias ganadas al conversar (Progreso), para festejarlas. */
    val insigniasNuevas: List<InsigniaDto> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val progresoRepository: ProgresoRepository,
    private val registroActividad: RegistroActividad
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // TODO: cuando conectemos el módulo de Perfil (RF-04/RF-05), tomar el
    // nivel real guardado del usuario en vez de este valor fijo.
    private val nivelUsuario = "A1"

    /** Una conversación por pantalla de chat; el servidor la registra una sola vez (Progreso). */
    private val sesionId = UUID.randomUUID().toString()
    private var conversacionRegistrada = false

    fun onEntradaChange(valor: String) {
        _uiState.value = _uiState.value.copy(entrada = valor)
    }

    fun enviar() {
        val texto = _uiState.value.entrada.trim()
        if (texto.isBlank()) return

        val historialParaApi = _uiState.value.mensajes.map {
            MensajeChat(role = if (it.esUsuario) "user" else "assistant", content = it.texto)
        }

        _uiState.value = _uiState.value.copy(
            mensajes = _uiState.value.mensajes + MensajeUi(texto, esUsuario = true),
            entrada = "",
            cargando = true,
            error = null
        )

        viewModelScope.launch {
            repository.enviarMensaje(texto, nivelUsuario, historialParaApi)
                .onSuccess { respuesta ->
                    _uiState.value = _uiState.value.copy(
                        mensajes = _uiState.value.mensajes + MensajeUi(respuesta, esUsuario = false),
                        cargando = false
                    )
                    registrarConversacion()
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }

    /** RN-08: la primera respuesta de la IA cuenta como actividad del día (racha) y para "Habla con IA". */
    private suspend fun registrarConversacion() {
        if (conversacionRegistrada) return
        conversacionRegistrada = true
        registroActividad.registrarUsuarioActual()
        val nuevas = progresoRepository.registrarChat(sesionId).filterNot { it.retroactiva }
        if (nuevas.isNotEmpty()) _uiState.value = _uiState.value.copy(insigniasNuevas = nuevas)
    }

    fun cerrarFestejo() {
        _uiState.value = _uiState.value.copy(insigniasNuevas = emptyList())
    }
}
