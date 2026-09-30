package com.xpspeak.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.chat.data.ChatRepository
import com.xpspeak.app.feature.chat.data.MensajeChat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MensajeUi(val texto: String, val esUsuario: Boolean)

data class ChatUiState(
    val mensajes: List<MensajeUi> = emptyList(),
    val entrada: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // TODO: cuando conectemos el módulo de Perfil (RF-04/RF-05), tomar el
    // nivel real guardado del usuario en vez de este valor fijo.
    private val nivelUsuario = "A1"

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
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }
}
