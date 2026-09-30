package com.xpspeak.app.feature.chat.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class MensajeChat(val role: String, val content: String)

data class ChatRequest(
    val mensaje: String,
    val nivel: String = "A1",
    val historial: List<MensajeChat> = emptyList()
)

data class ChatResponse(val respuesta: String? = null, val error: String? = null)

interface ChatApi {
    @POST("api/chat")
    suspend fun enviarMensaje(
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
