package com.xpspeak.app.feature.voice.data

import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.POST

data class SpeechTokenResponse(
    val token: String? = null,
    val region: String? = null,
    val error: String? = null
)

interface SpeechApi {
    @POST("api/speech-token")
    suspend fun obtenerToken(@Header("Authorization") auth: String): Response<SpeechTokenResponse>
}
