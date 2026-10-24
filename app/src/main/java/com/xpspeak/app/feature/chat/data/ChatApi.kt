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
    /** RNF-02: el backend contesta por text/event-stream conforme Azure genera el texto. */
    val stream: Boolean = true
)

/** Cuerpo de error JSON (antes de empezar el flujo). */
data class ChatResponse(val respuesta: String? = null, val error: String? = null)

/** Un evento "data:" del flujo: { delta }, { fin: true } o { error, codigo }. */
data class EventoChat(
    val delta: String? = null,
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
