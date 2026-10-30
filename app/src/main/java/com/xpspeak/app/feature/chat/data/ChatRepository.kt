package com.xpspeak.app.feature.chat.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.xpspeak.app.feature.auth.data.UsuarioDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Lo que va llegando de una vuelta del chat. */
sealed interface EventoRespuesta {
    /** Un pedazo de la respuesta de la IA (RNF-02). */
    data class Pedazo(val texto: String) : EventoRespuesta

    /** Los errores del mensaje del usuario (RF-09); puede llegar antes, durante o después de la respuesta. */
    data class Correccion(val correccion: CorreccionDto) : EventoRespuesta
}

class ChatRepository @Inject constructor(
    private val api: ChatApi,
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao
) {
    private val gson = Gson()

    companion object {
        /**
         * El mensaje que manda el servidor ({ error, codigo }) o, si no se puede leer, uno
         * según el código HTTP (QA 29-oct: un 503 salía como "Ocurrió un error").
         */
        fun mensajeDeError(codigo: Int, cuerpo: String?): String {
            val delServidor = cuerpo?.let {
                runCatching { JsonParser.parseString(it).asJsonObject.get("error")?.takeIf { e -> e.isJsonPrimitive }?.asString }.getOrNull()
            }?.takeIf { it.isNotBlank() }
            return delServidor ?: when (codigo) {
                503 -> "El chat no está disponible por ahora. Intenta más tarde."
                429 -> "Has conversado mucho por hoy. Intenta más tarde."
                401 -> "Tu sesión venció. Vuelve a iniciar sesión."
                else -> "Ocurrió un error ($codigo). Intenta de nuevo."
            }
        }
    }

    /** RN-02: la IA responde con la complejidad del nivel elegido al registrarse (guardado en Room). */
    suspend fun nivelDelUsuario(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return "A1"
        return usuarioDao.buscarPorUid(uid)?.nivel ?: "A1"
    }

    /**
     * RNF-02: emite cada pedazo de la respuesta conforme llega del backend, y la
     * corrección del mensaje si la hay (RF-09); termina cuando llega { fin }. Un error
     * (antes o a media respuesta) se lanza como excepción con el mensaje que manda el servidor.
     */
    fun enviarMensaje(
        mensaje: String,
        nivel: String,
        historial: List<MensajeChat>
    ): Flow<EventoRespuesta> = flow {
        val usuario = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay sesión activa")

        val token = usuario.getIdToken(false).await().token
            ?: throw IllegalStateException("No se pudo obtener el token de sesión")

        val respuesta = api.enviarMensaje("Bearer $token", ChatRequest(mensaje, nivel, historial))

        if (!respuesta.isSuccessful) {
            throw Exception(mensajeDeError(respuesta.code(), respuesta.errorBody()?.string()))
        }

        val cuerpo = respuesta.body() ?: throw IllegalStateException("Respuesta vacía del servidor")
        // Un backend sin streaming contesta { respuesta } en JSON: se muestra completa de una vez.
        if (cuerpo.contentType()?.subtype == "json") {
            val completa = cuerpo.use { gson.fromJson(it.string(), ChatResponse::class.java) }
            emit(EventoRespuesta.Pedazo(completa?.respuesta ?: throw IllegalStateException("Respuesta vacía del servidor")))
            completa.correccion?.takeIf { it.errores.isNotEmpty() }?.let { emit(EventoRespuesta.Correccion(it)) }
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
                    !evento.delta.isNullOrEmpty() -> emit(EventoRespuesta.Pedazo(evento.delta))
                    !evento.correccion?.errores.isNullOrEmpty() -> emit(EventoRespuesta.Correccion(evento.correccion!!))
                }
            }
        }
        // Sin { fin }: la conexión se cortó a media respuesta.
        throw Exception("Se perdió la conexión con el chatbot. Intenta de nuevo.")
    }.flowOn(Dispatchers.IO)
}
