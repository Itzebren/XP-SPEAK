package com.xpspeak.app.feature.chat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.actividad.RegistroActividad
import com.xpspeak.app.core.medicion.Meta
import com.xpspeak.app.core.medicion.Tiempos
import com.xpspeak.app.feature.chat.data.ChatRepository
import com.xpspeak.app.feature.chat.data.CorreccionDto
import com.xpspeak.app.feature.chat.data.EventoRespuesta
import com.xpspeak.app.feature.chat.data.MensajeChat
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoRepository
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.ResultadoPronunciacion
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * [completo] es false mientras la respuesta de la IA sigue llegando (RNF-02).
 * [pronunciacion] solo en mensajes dichos con voz: cómo los pronunció el usuario (RF-09).
 * [correccion] solo en mensajes del usuario con errores de gramática (RF-09); no va al historial.
 */
data class MensajeUi(
    val texto: String,
    val esUsuario: Boolean,
    val completo: Boolean = true,
    val pronunciacion: ResultadoPronunciacion? = null,
    val correccion: CorreccionDto? = null
)

data class ChatUiState(
    val mensajes: List<MensajeUi> = emptyList(),
    val entrada: String = "",
    val cargando: Boolean = false,
    /** Modo conversación: escuchar → responder en voz alta → volver a escuchar, sin tocar nada. */
    val enVivo: Boolean = false,
    /** El micrófono está abierto (en vivo, mientras la IA no habla ni piensa). */
    val escuchando: Boolean = false,
    /** Lo que Azure va oyendo mientras el usuario habla. */
    val parcial: String = "",
    val error: String? = null,
    /** "Habla con IA" y otras insignias ganadas al conversar (Progreso), para festejarlas. */
    val insigniasNuevas: List<InsigniaDto> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val progresoRepository: ProgresoRepository,
    private val registroActividad: RegistroActividad,
    private val evaluador: EvaluadorPronunciacion
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Una conversación por pantalla de chat; el servidor la registra una sola vez (Progreso). */
    private val sesionId = UUID.randomUUID().toString()
    private var conversacionRegistrada = false
    private var escucha: Job? = null

    fun onEntradaChange(valor: String) {
        _uiState.value = _uiState.value.copy(entrada = valor)
    }

    fun enviar() {
        val texto = _uiState.value.entrada.trim()
        if (texto.isBlank()) return
        // Escribió en pleno modo en vivo: se cierra el micrófono para no oír la respuesta leída.
        escucha?.cancel()
        _uiState.value = _uiState.value.copy(escuchando = false, parcial = "")
        enviarTexto(texto, pronunciacion = null)
    }

    private fun enviarTexto(texto: String, pronunciacion: ResultadoPronunciacion?) {
        val historialParaApi = _uiState.value.mensajes.map {
            MensajeChat(role = if (it.esUsuario) "user" else "assistant", content = it.texto)
        }

        // Dónde queda el mensaje del usuario, para colgarle su corrección cuando llegue.
        val indiceUsuario = _uiState.value.mensajes.size
        _uiState.value = _uiState.value.copy(
            mensajes = _uiState.value.mensajes + MensajeUi(texto, esUsuario = true, pronunciacion = pronunciacion),
            entrada = "",
            cargando = true,
            error = null
        )

        viewModelScope.launch {
            var respuesta = ""
            // Tabla 93: desde que se envía el mensaje (con voz, ya evaluada la pronunciación).
            val enviado = Tiempos.ahora()
            try {
                // RF-06/RN-02: se lee en cada envío por si el usuario cambió de nivel.
                repository.enviarMensaje(texto, repository.nivelDelUsuario(), historialParaApi).collect { evento ->
                    when (evento) {
                        is EventoRespuesta.Correccion -> {
                            Tiempos.desde("chat.correccion", enviado, Meta.RN05)
                            _uiState.value = _uiState.value.copy(
                                mensajes = _uiState.value.mensajes.mapIndexed { i, mensaje ->
                                    if (i == indiceUsuario) mensaje.copy(correccion = evento.correccion) else mensaje
                                }
                            )
                        }
                        is EventoRespuesta.Pedazo -> {
                            if (respuesta.isEmpty()) Tiempos.desde("chat.primer_pedazo", enviado, Meta.RNF02)
                            // RNF-02: la burbuja aparece con el primer pedazo y crece con cada uno.
                            val anteriores = if (respuesta.isEmpty()) _uiState.value.mensajes else _uiState.value.mensajes.dropLast(1)
                            respuesta += evento.texto
                            _uiState.value = _uiState.value.copy(
                                mensajes = anteriores + MensajeUi(respuesta, esUsuario = false, completo = false)
                            )
                        }
                    }
                }
                if (respuesta.isEmpty()) throw IllegalStateException("Respuesta vacía del servidor")
                Tiempos.desde("chat.respuesta_completa", enviado, Meta.RNF02)
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
                // Sin respuesta que leer, el modo en vivo se quedaría esperando: se pausa.
                _uiState.value = _uiState.value.copy(mensajes = mensajes, cargando = false, enVivo = false, error = excepcion.message)
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

    fun alternarEnVivo() {
        if (_uiState.value.enVivo) {
            detenerEnVivo()
        } else {
            _uiState.value = _uiState.value.copy(enVivo = true, error = null)
            escucharTurno()
        }
    }

    fun detenerEnVivo() {
        escucha?.cancel()
        escucha = null
        _uiState.value = _uiState.value.copy(enVivo = false, escuchando = false, parcial = "")
    }

    /** La IA terminó de leer su respuesta en voz alta: le toca al usuario. */
    fun onRespuestaLeida() {
        escucharTurno()
    }

    /** Un turno del usuario: se oye en vivo, se califica (RF-09) y se envía solo a la IA (RF-07). */
    private fun escucharTurno() {
        val estado = _uiState.value
        if (!estado.enVivo || estado.cargando || escucha?.isActive == true) return
        _uiState.value = estado.copy(escuchando = true, parcial = "")

        escucha = viewModelScope.launch {
            val resultado = evaluador.escucharFrase { parcial ->
                _uiState.value = _uiState.value.copy(parcial = parcial, error = null)
            }
            _uiState.value = _uiState.value.copy(escuchando = false, parcial = "")
            resultado
                .onSuccess { frase ->
                    if (Retroalimentacion.esLegible(frase)) {
                        enviarTexto(frase.textoReconocido.trim(), frase)
                    } else {
                        // CU-05 A3: audio poco claro, se pide repetir en vez de mandar algo que no dijo.
                        _uiState.value = _uiState.value.copy(error = "No te escuché bien. Repítelo, por favor.")
                        escucha = null
                        escucharTurno()
                    }
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(enVivo = false, error = excepcion.message)
                }
        }
    }

    fun onPermisoMicrofonoDenegado() {
        _uiState.value = _uiState.value.copy(
            error = "Necesitas permitir el micrófono para practicar con voz."
        )
    }
}
