package com.xpspeak.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.actividad.RegistroActividad
import com.xpspeak.app.feature.chat.data.ChatRepository
import com.xpspeak.app.feature.chat.data.MensajeChat
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoRepository
import com.xpspeak.app.feature.voice.data.ReconocedorVoz
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/** [completo] es false mientras la respuesta de la IA sigue llegando (RNF-02). */
data class MensajeUi(val texto: String, val esUsuario: Boolean, val completo: Boolean = true)

data class ChatUiState(
    val mensajes: List<MensajeUi> = emptyList(),
    val entrada: String = "",
    val cargando: Boolean = false,
    val escuchando: Boolean = false,
    val error: String? = null,
    /** "Habla con IA" y otras insignias ganadas al conversar (Progreso), para festejarlas. */
    val insigniasNuevas: List<InsigniaDto> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val progresoRepository: ProgresoRepository,
    private val registroActividad: RegistroActividad,
    private val reconocedor: ReconocedorVoz
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

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
            var respuesta = ""
            try {
                // RF-06/RN-02: se lee en cada envío por si el usuario cambió de nivel.
                repository.enviarMensaje(texto, repository.nivelDelUsuario(), historialParaApi).collect { pedazo ->
                    // RNF-02: la burbuja aparece con el primer pedazo y crece con cada uno.
                    val anteriores = if (respuesta.isEmpty()) _uiState.value.mensajes else _uiState.value.mensajes.dropLast(1)
                    respuesta += pedazo
                    _uiState.value = _uiState.value.copy(
                        mensajes = anteriores + MensajeUi(respuesta, esUsuario = false, completo = false)
                    )
                }
                if (respuesta.isEmpty()) throw IllegalStateException("Respuesta vacía del servidor")
                _uiState.value = _uiState.value.copy(
                    mensajes = _uiState.value.mensajes.dropLast(1) + MensajeUi(respuesta, esUsuario = false),
                    cargando = false
                )
                registrarConversacion()
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (excepcion: Exception) {
                // Una respuesta cortada no se queda en el historial: confundiría a la IA en el siguiente turno.
                val mensajes = if (respuesta.isEmpty()) _uiState.value.mensajes else _uiState.value.mensajes.dropLast(1)
                _uiState.value = _uiState.value.copy(mensajes = mensajes, cargando = false, error = excepcion.message)
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

    fun escucharVoz() {
        if (_uiState.value.escuchando || _uiState.value.cargando) return
        _uiState.value = _uiState.value.copy(escuchando = true, error = null)

        viewModelScope.launch {
            reconocedor.reconocerUnaVez()
                .onSuccess { texto ->
                    _uiState.value = _uiState.value.copy(entrada = texto, escuchando = false)
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(escuchando = false, error = excepcion.message)
                }
        }
    }

    fun onPermisoMicrofonoDenegado() {
        _uiState.value = _uiState.value.copy(
            error = "Necesitas permitir el micrófono para practicar con voz."
        )
    }
}
