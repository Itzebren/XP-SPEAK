package com.xpspeak.app.feature.games.data

import com.google.gson.annotations.SerializedName
import com.xpspeak.app.feature.progress.data.InsigniaDto
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
    val conceptos: List<ConceptoResultadoDto>,
    /** Cuándo terminó la partida (ms epoch): una jugada sin conexión cuenta para ese día (RN-08). */
    @SerializedName("completado_en") val completadoEn: Long? = null
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
    val repetido: Boolean,
    /** Progreso: insignias ganadas en vivo (para festejarlas) y nivel de XP alcanzado; faltan en un backend viejo. */
    @SerializedName("insignias_nuevas") val insigniasNuevas: List<InsigniaDto>? = null,
    @SerializedName("nivel_xp_nuevo") val nivelXpNuevo: Int? = null
)

/** Token de Azure AI Speech de 10 minutos; la llave nunca llega a la app (RNF-06). */
data class TokenVozDto(
    val token: String,
    val region: String,
    @SerializedName("expira_en") val expiraEn: Long
)
