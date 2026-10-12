package com.xpspeak.app.feature.chat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val api: ChatApi,
    private val firebaseAuth: FirebaseAuth
) {
    suspend fun enviarMensaje(
        mensaje: String,
        nivel: String,
        historial: List<MensajeChat>
    ): Result<String> = runCatching {
        val usuario = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay sesión activa")

        val token = usuario.getIdToken(false).await().token
            ?: throw IllegalStateException("No se pudo obtener el token de sesión")

        val respuesta = api.enviarMensaje("Bearer $token", ChatRequest(mensaje, nivel, historial))

        if (respuesta.isSuccessful) {
            respuesta.body()?.respuesta ?: throw IllegalStateException("Respuesta vacía del servidor")
        } else {
            val errorBody = respuesta.errorBody()?.string()
            val mensajeError = errorBody?.let {
                runCatching { Gson().fromJson(it, ChatResponse::class.java).error }.getOrNull()
            }
            throw Exception(mensajeError ?: "Ocurrió un error. Intenta de nuevo.")
        }
    }
}
