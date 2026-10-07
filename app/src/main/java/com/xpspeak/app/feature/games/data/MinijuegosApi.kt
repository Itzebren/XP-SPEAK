package com.xpspeak.app.feature.games.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

/** Endpoints de minijuegos en backend/ (docs/minijuegos-diseno.md §6). Exigen el ID token de Firebase. */
interface MinijuegosApi {
    @POST("api/games/result")
    suspend fun enviarResultado(
        @Header("Authorization") auth: String,
        @Body request: ResultadoRequest
    ): Response<ResultadoResponse>

    @GET("api/speech/token")
    suspend fun tokenVoz(@Header("Authorization") auth: String): Response<TokenVozDto>
}

data class ResultadoRequest(
    @SerializedName("partida_id") val partidaId: String,
    val juego: String,
    val aciertos: Int,
    val total: Int,
    @SerializedName("duracion_ms") val duracionMs: Long,
    val conceptos: List<ConceptoResultadoDto>
)

data class ConceptoResultadoDto(
    @SerializedName("concepto_id") val conceptoId: String,
    val aciertos: Int,
    val errores: Int
)

data class ResultadoResponse(
    @SerializedName("xp_ganado") val xpGanado: Int,
    @SerializedName("xp_reducida") val xpReducida: Boolean,
    @SerializedName("partidas_hoy") val partidasHoy: Int,
    val repetido: Boolean
)

/** Token de Azure AI Speech de 10 minutos; la llave nunca llega a la app (RNF-06). */
data class TokenVozDto(
    val token: String,
    val region: String,
    @SerializedName("expira_en") val expiraEn: Long
)
