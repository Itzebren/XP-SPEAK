package com.xpspeak.app.feature.chat.data

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Streaming

data class MensajeChat(val role: String, val content: String)

data class ChatRequest(
    val mensaje: String,
    val nivel: String = "A1",
    val historial: List<MensajeChat> = emptyList(),
    /** El mensaje se dictó: el corrector no marca ortografía ni palabras que suenan igual. */
    val voz: Boolean = false,
    /** RNF-02: el backend contesta por text/event-stream conforme Azure genera el texto. */
    val stream: Boolean = true
)

/** Respuesta JSON (backend sin streaming) o cuerpo de error antes de empezar el flujo. */
data class ChatResponse(
    val respuesta: String? = null,
    val correccion: CorreccionDto? = null,
    val error: String? = null
)

/**
 * RF-09: la revisión del mensaje del usuario, que el backend pide aparte de la respuesta.
 * Con errores trae la oración [corregido] y cada error; si se revisó y está bien, solo
 * [correcta]. Es efímera: solo se muestra, no se guarda ni vuelve a la IA en el historial.
 */
data class CorreccionDto(
    val corregido: String? = null,
    val errores: List<ErrorGramaticalDto> = emptyList(),
    val correcta: Boolean = false
) {
    /** Hay algo que mostrar: errores, o la confirmación de que no hubo. */
    val seMuestra: Boolean get() = errores.isNotEmpty() || correcta
}

/** [tipo] es la etiqueta del error («Tiempo verbal», «Conjugación»…); un backend anterior no la manda. */
data class ErrorGramaticalDto(val original: String, val correccion: String, val explicacion: String, val tipo: String? = null)

/**
 * Un evento "data:" del flujo: { delta }, { correccion }, { completa: true } cuando la respuesta
 * ya llegó entera (la corrección puede seguir en camino), { fin: true } o { error, codigo }.
 */
data class EventoChat(
    val delta: String? = null,
    val correccion: CorreccionDto? = null,
    val completa: Boolean? = null,
    val fin: Boolean? = null,
    val error: String? = null,
    val codigo: String? = null
)

interface ChatApi {
    /** @Streaming: Retrofit entrega el cuerpo sin leerlo, para ir mostrando cada pedazo. */
    @Streaming
    @POST("api/chat")
    suspend fun enviarMensaje(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ResponseBody>
}
