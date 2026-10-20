package com.xpspeak.app.feature.chat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.xpspeak.app.feature.auth.data.UsuarioDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepository @Inject constructor(
    private val api: ChatApi,
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao
) {
    private val gson = Gson()

    /** RN-02: la IA responde con la complejidad del nivel elegido al registrarse (guardado en Room). */
    suspend fun nivelDelUsuario(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return "A1"
        return usuarioDao.buscarPorUid(uid)?.nivel ?: "A1"
    }

    /**
     * RNF-02: emite cada pedazo de la respuesta conforme llega del backend y termina
     * cuando llega { fin }. Un error (antes o a media respuesta) se lanza como excepción
     * con el mensaje que manda el servidor.
     */
    fun enviarMensaje(
        mensaje: String,
        nivel: String,
        historial: List<MensajeChat>
    ): Flow<String> = flow {
        val usuario = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay sesión activa")

        val token = usuario.getIdToken(false).await().token
            ?: throw IllegalStateException("No se pudo obtener el token de sesión")

        val respuesta = api.enviarMensaje("Bearer $token", ChatRequest(mensaje, nivel, historial))

        if (!respuesta.isSuccessful) {
            val errorBody = respuesta.errorBody()?.string()
            val mensajeError = errorBody?.let {
                runCatching { gson.fromJson(it, ChatResponse::class.java).error }.getOrNull()
            }
            throw Exception(mensajeError ?: "Ocurrió un error. Intenta de nuevo.")
        }

        val cuerpo = respuesta.body() ?: throw IllegalStateException("Respuesta vacía del servidor")
        // Un backend sin streaming contesta { respuesta } en JSON: se muestra completa de una vez.
        if (cuerpo.contentType()?.subtype == "json") {
            val completa = cuerpo.use { gson.fromJson(it.string(), ChatResponse::class.java)?.respuesta }
            emit(completa ?: throw IllegalStateException("Respuesta vacía del servidor"))
            return@flow
        }
        cuerpo.use {
            val fuente = it.source()
            while (true) {
                val linea = fuente.readUtf8Line() ?: break
                if (!linea.startsWith("data:")) continue
                val evento = runCatching { gson.fromJson(linea.removePrefix("data:").trim(), EventoChat::class.java) }.getOrNull()
                    ?: continue
                when {
                    evento.error != null -> throw Exception(evento.error)
                    evento.fin == true -> return@flow
                    !evento.delta.isNullOrEmpty() -> emit(evento.delta)
                }
            }
        }
        // Sin { fin }: la conexión se cortó a media respuesta.
        throw Exception("Se perdió la conexión con el chatbot. Intenta de nuevo.")
    }.flowOn(Dispatchers.IO)
}
