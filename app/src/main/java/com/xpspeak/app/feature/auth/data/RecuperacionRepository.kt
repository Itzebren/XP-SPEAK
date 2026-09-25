package com.xpspeak.app.feature.auth.data

import com.google.gson.Gson
import javax.inject.Inject

class RecuperacionRepository @Inject constructor(
    private val api: RecuperacionApi
) {
    suspend fun enviarCodigo(correo: String): Result<String> =
        manejarRespuesta { api.enviarCodigo(EnviarCodigoRequest(correo)) }

    suspend fun verificarCodigo(correo: String, codigo: String, nuevaPassword: String): Result<String> =
        manejarRespuesta {
            api.verificarCodigo(VerificarCodigoRequest(correo, codigo.uppercase(), nuevaPassword))
        }

    private suspend fun manejarRespuesta(
        llamada: suspend () -> retrofit2.Response<RecuperacionResponse>
    ): Result<String> = runCatching {
        val respuesta = llamada()
        if (respuesta.isSuccessful) {
            respuesta.body()?.mensaje ?: "Listo"
        } else {
            val errorBody = respuesta.errorBody()?.string()
            val mensajeError = errorBody?.let {
                runCatching { Gson().fromJson(it, RecuperacionResponse::class.java).error }.getOrNull()
            }
            throw Exception(mensajeError ?: "Ocurrió un error. Intenta de nuevo.")
        }
    }
}
