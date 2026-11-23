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
import com.xpspeak.app.feature.games.data.voz.EstadoEscucha
import com.xpspeak.app.feature.games.data.voz.EvaluadorVoz
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * [completo] es false mientras la respuesta de la IA sigue llegando (RNF-02).
 * [pronunciacion] solo en mensajes dichos con voz: cómo los pronunció el usuario, por frase,
 * palabra y sonido (RF-09); sin Azure solo queda la transcripción.
 * [correccion] en mensajes del usuario ya revisados: sus errores de gramática o la confirmación
 * de que no hubo (RF-09); no va al historial. [revisando] mientras esa revisión no llega.
 */
data class MensajeUi(
    val texto: String,
    val esUsuario: Boolean,
    val completo: Boolean = true,
    val pronunciacion: EvaluacionVoz? = null,
    val correccion: CorreccionDto? = null,
    val revisando: Boolean = false,
    /** Identifica la burbuja aunque la lista cambie (se quita un turno con «No dije eso»). */
    val id: Long = 0
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
    /** El último mensaje no se pudo enviar (p. ej., sin conexión): se ofrece «Reintentar». */
    val reintentable: Boolean = false,
    /** "Habla con IA" y otras insignias ganadas al conversar (Progreso), para festejarlas. */
    val insigniasNuevas: List<InsigniaDto> = emptyList()
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val progresoRepository: ProgresoRepository,
    private val registroActividad: RegistroActividad,
    private val evaluador: EvaluadorVoz
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /** Una conversación por pantalla de chat; el servidor la registra una sola vez (Progreso). */
    private val sesionId = UUID.randomUUID().toString()
    private var conversacionRegistrada = false
    private var escucha: Job? = null
    /** El envío más reciente: el del último mensaje del usuario. */
    private var envio: Job? = null
    private var siguienteId = 1L

    /** Volumen del micrófono en vivo, para las barras de «te oigo». */
    val estadoEscucha: StateFlow<EstadoEscucha> = evaluador.escucha

    /** Botón «Listo»: envía lo ya dicho sin esperar la pausa de silencio. */
    fun terminarDeHablar() = evaluador.terminar()

    /** Texto y pronunciación del mensaje que falló, para reenviarlo con «Reintentar». */
    private var fallido: Pair<String, EvaluacionVoz?>? = null

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

    private fun enviarTexto(texto: String, pronunciacion: EvaluacionVoz?) {
        val historialParaApi = _uiState.value.mensajes.map {
            MensajeChat(role = if (it.esUsuario) "user" else "assistant", content = it.texto)
        }

        // Las burbujas de este turno, para colgarles la corrección y la respuesta cuando lleguen.
        val idUsuario = siguienteId++
        val idRespuesta = siguienteId++
        _uiState.value = _uiState.value.copy(
            mensajes = _uiState.value.mensajes +
                MensajeUi(texto, esUsuario = true, pronunciacion = pronunciacion, revisando = seRevisa(texto), id = idUsuario),
            entrada = "",
            cargando = true,
            error = null,
            reintentable = false
        )
        fallido = null

        envio = viewModelScope.launch {
            var respuesta = ""
            // La respuesta ya se dio por completa (se lee en voz alta y se puede seguir conversando).
            var entregada = false
            // Tabla 93: desde que se envía el mensaje (con voz, ya evaluada la pronunciación).
            val enviado = Tiempos.ahora()
            fun cambiar(id: Long, cambio: (MensajeUi) -> MensajeUi) {
                _uiState.value = _uiState.value.copy(mensajes = _uiState.value.mensajes.map { if (it.id == id) cambio(it) else it })
            }
            fun conMensajeUsuario(cambio: (MensajeUi) -> MensajeUi) = cambiar(idUsuario, cambio)
            fun entregar() {
                if (entregada) return
                entregada = true
                Tiempos.desde("chat.respuesta_completa", enviado, Meta.RNF02)
                cambiar(idRespuesta) { it.copy(texto = respuesta, completo = true) }
                _uiState.value = _uiState.value.copy(cargando = false)
                viewModelScope.launch { registrarConversacion() }
            }
            try {
                // RF-06/RN-02: se lee en cada envío por si el usuario cambió de nivel.
                repository.enviarMensaje(texto, repository.nivelDelUsuario(), historialParaApi, voz = pronunciacion != null).collect { evento ->
                    when (evento) {
                        is EventoRespuesta.Correccion -> {
                            Tiempos.desde("chat.correccion", enviado, Meta.RN05)
                            conMensajeUsuario { it.copy(correccion = evento.correccion, revisando = false) }
                        }
                        // La corrección puede tardar más que la respuesta: la voz de la IA no la espera.
                        EventoRespuesta.Completa -> if (respuesta.isNotEmpty()) entregar()
                        is EventoRespuesta.Pedazo -> {
                            if (respuesta.isEmpty()) Tiempos.desde("chat.primer_pedazo", enviado, Meta.RNF02)
                            // RNF-02: la burbuja aparece con el primer pedazo y crece con cada uno.
                            val primero = respuesta.isEmpty()
                            respuesta += evento.texto
                            if (primero) {
                                _uiState.value = _uiState.value.copy(
                                    mensajes = _uiState.value.mensajes + MensajeUi(respuesta, esUsuario = false, completo = false, id = idRespuesta)
                                )
                            } else {
                                cambiar(idRespuesta) { it.copy(texto = respuesta) }
                            }
                        }
                    }
                }
                if (respuesta.isEmpty()) throw IllegalStateException("Respuesta vacía del servidor")
                entregar()
                // No llegó revisión (una palabra, español o el corrector falló): se deja de esperar.
                conMensajeUsuario { it.copy(revisando = false) }
            } catch (excepcion: CancellationException) {
                throw excepcion
            } catch (excepcion: Exception) {
                conMensajeUsuario { it.copy(revisando = false) }
                // Se cortó esperando la corrección, con la respuesta ya entregada: no es un envío fallido.
                if (entregada) return@launch
                // Una respuesta cortada no se queda en el historial: confundiría a la IA en el siguiente turno.
                val mensajes = _uiState.value.mensajes.filterNot { it.id == idRespuesta }
                // Sin respuesta que leer, el modo en vivo se quedaría esperando: se pausa.
                fallido = texto to pronunciacion
                _uiState.value = _uiState.value.copy(
                    mensajes = mensajes, cargando = false, enVivo = false,
                    error = ChatRepository.mensajeDeFalla(excepcion), reintentable = true
                )
            }
        }
    }

    /** El servidor solo revisa la gramática de mensajes de dos palabras o más. */
    private fun seRevisa(texto: String) = texto.trim().split(Regex("\\s+")).size >= 2

    /** Reenvía el mensaje que falló: se quita su burbuja para no duplicarla y se manda otra vez. */
    fun reintentar() {
        val (texto, pronunciacion) = fallido ?: return
        if (_uiState.value.cargando) return
        val mensajes = _uiState.value.mensajes
        val sinFallido = if (mensajes.lastOrNull()?.let { it.esUsuario && it.texto == texto } == true) mensajes.dropLast(1) else mensajes
        _uiState.value = _uiState.value.copy(mensajes = sinFallido)
        enviarTexto(texto, pronunciacion)
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

    /**
     * La IA terminó de leer su respuesta en voz alta: le toca al usuario. Se espera un momento
     * a que la bocina calle del todo; si no, el micrófono alcanzaba a oír la cola de la respuesta.
     */
    fun onRespuestaLeida() {
        viewModelScope.launch {
            delay(RESPIRO_TRAS_HABLAR_MS)
            escucharTurno()
        }
    }

    /**
     * «No dije eso»: el reconocedor entendió otra cosa. Se quita ese turno (el mensaje, su
     * respuesta y sus correcciones, que tampoco van ya al historial de la IA) y se vuelve a
     * escuchar. Solo aplica al último mensaje dicho con voz.
     */
    fun descartarUltimoDicho() {
        val mensajes = _uiState.value.mensajes
        val indice = mensajes.indexOfLast { it.esUsuario }
        if (indice < 0 || mensajes[indice].pronunciacion == null) return
        envio?.cancel()
        envio = null
        fallido = null
        val anterior = escucha
        escucha = null
        _uiState.value = _uiState.value.copy(
            mensajes = mensajes.take(indice),
            cargando = false, enVivo = true, escuchando = false, parcial = "",
            error = null, reintentable = false
        )
        viewModelScope.launch {
            // El micrófono anterior debe soltarse antes de abrir el nuevo.
            anterior?.cancelAndJoin()
            escucharTurno()
        }
    }

    /** Un turno del usuario: se oye en vivo, se califica (RF-09) y se envía solo a la IA (RF-07). */
    private fun escucharTurno() {
        val estado = _uiState.value
        if (!estado.enVivo || estado.cargando || escucha?.isActive == true) return
        _uiState.value = estado.copy(escuchando = true, parcial = "")

        escucha = viewModelScope.launch {
            // Con Azure se califica la pronunciación; sin Azure el teléfono solo transcribe.
            val resultado = runCatching {
                evaluador.escucharFrase { parcial ->
                    _uiState.value = _uiState.value.copy(parcial = parcial, error = null)
                }
            }
            _uiState.value = _uiState.value.copy(escuchando = false, parcial = "")
            resultado
                .onSuccess { frase ->
                    if (Retroalimentacion.esLegible(frase)) {
                        enviarTexto(frase.transcripcion.trim(), frase)
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

    private companion object {
        const val RESPIRO_TRAS_HABLAR_MS = 400L
    }

    fun onPermisoMicrofonoDenegado() {
        _uiState.value = _uiState.value.copy(
            error = "Necesitas permitir el micrófono para practicar con voz."
        )
    }
}
