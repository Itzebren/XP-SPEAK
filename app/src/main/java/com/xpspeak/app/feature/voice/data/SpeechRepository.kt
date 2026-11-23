package com.xpspeak.app.feature.voice.data

import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class SpeechCredenciales(val token: String, val region: String)

/**
 * Pide al backend un token temporal de Azure Speech (dura 10 min) y lo
 * reutiliza ~8 min para no llamar al servidor en cada frase.
 */
@Singleton
class SpeechRepository @Inject constructor(
    private val api: SpeechApi,
    private val firebaseAuth: FirebaseAuth
) {
    private var cache: SpeechCredenciales? = null
    private var obtenidoEn: Long = 0L

    suspend fun obtenerCredenciales(): Result<SpeechCredenciales> = runCatching {
        val vigente = cache
        if (vigente != null && System.currentTimeMillis() - obtenidoEn < 8 * 60 * 1000) {
            return@runCatching vigente
        }

        val usuario = firebaseAuth.currentUser
            ?: throw IllegalStateException("No hay sesión activa")
        val idToken = usuario.getIdToken(false).await().token
            ?: throw IllegalStateException("No se pudo obtener el token de sesión")

        val respuesta = api.obtenerToken("Bearer $idToken")
        if (!respuesta.isSuccessful) {
            val mensaje = respuesta.errorBody()?.string()?.let {
                runCatching { Gson().fromJson(it, SpeechTokenResponse::class.java).error }.getOrNull()
            }
            throw Exception(mensaje ?: "No se pudo preparar el reconocimiento de voz.")
        }

        val cuerpo = respuesta.body()
        val token = cuerpo?.token
        val region = cuerpo?.region
        if (token == null || region == null) throw IllegalStateException("Respuesta de voz incompleta")

        SpeechCredenciales(token, region).also {
            cache = it
            obtenidoEn = System.currentTimeMillis()
        }
    }
}
