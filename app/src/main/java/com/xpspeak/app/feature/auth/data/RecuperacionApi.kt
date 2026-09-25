package com.xpspeak.app.feature.auth.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class EnviarCodigoRequest(val correo: String)
data class VerificarCodigoRequest(val correo: String, val codigo: String, val nuevaPassword: String)
data class RecuperacionResponse(val mensaje: String? = null, val error: String? = null)

interface RecuperacionApi {
    @POST("api/send-reset-code")
    suspend fun enviarCodigo(@Body request: EnviarCodigoRequest): Response<RecuperacionResponse>

    @POST("api/verify-reset-code")
    suspend fun verificarCodigo(@Body request: VerificarCodigoRequest): Response<RecuperacionResponse>
}
