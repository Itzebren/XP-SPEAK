package com.xpspeak.app.feature.lessons.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import java.util.UUID
import javax.inject.Inject

/**
 * RF-10/RF-12/RF-13: consume el backend de Lecciones. El servidor saca el uid
 * del ID token de Firebase, así que cada llamada manda "Bearer <token>".
 */
class LeccionesRepository @Inject constructor(
    private val api: LeccionesApi,
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao
) {
    /** RN-02: el catálogo depende del nivel elegido al registrarse (guardado en Room). */
    suspend fun nivelDelUsuario(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return "A1"
        return usuarioDao.buscarPorUid(uid)?.nivel ?: "A1"
    }

    suspend fun catalogo(): Result<CatalogoDto> = llamar { api.catalogo(it, nivelDelUsuario()) }

    suspend fun leccion(id: String): Result<LeccionDto> = llamar { api.leccion(it, id) }

    suspend fun guardarAvance(id: String, seccion: Int): Result<AvanceResponse> =
        llamar { api.guardarAvance(it, id, AvanceRequest(seccion)) }

    suspend fun conceptosDebiles(): Result<ConceptosDebilesDto> =
        llamar { api.conceptosDebiles(it, nivelDelUsuario()) }

    /** Cada envío lleva un attempt_id nuevo; el servidor lo usa para no duplicar XP (§6.2). */
    suspend fun enviarIntento(id: String, respuestas: Map<String, RespuestaUsuario>): Result<IntentoResponse> {
        val request = IntentoRequest(
            attemptId = UUID.randomUUID().toString(),
            respuestas = respuestas.map { (itemId, respuesta) -> respuesta.aDto(itemId) }
        )
        return llamar { api.enviarIntento(it, id, request) }
    }

    private suspend fun <T> llamar(peticion: suspend (String) -> Response<T>): Result<T> = runCatching {
        val usuario = firebaseAuth.currentUser ?: throw Exception("Inicia sesión para ver tus lecciones.")
        val token = usuario.getIdToken(false).await().token
            ?: throw Exception("No se pudo obtener la sesión. Vuelve a iniciar sesión.")

        val respuesta = peticion("Bearer $token")
        if (respuesta.isSuccessful) {
            respuesta.body() ?: throw Exception("Respuesta vacía del servidor")
        } else {
            val errorBody = respuesta.errorBody()?.string()
            val mensajeError = errorBody?.let {
                runCatching { Gson().fromJson(it, ErrorDto::class.java).error }.getOrNull()
            }
            throw Exception(mensajeError ?: "Ocurrió un error (${respuesta.code()}). Intenta de nuevo.")
        }
    }
}
