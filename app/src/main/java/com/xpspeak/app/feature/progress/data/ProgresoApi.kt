package com.xpspeak.app.feature.progress.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/** Endpoints del módulo de Progreso en backend/ (docs/progreso-diseno.md Fase 2). Exigen el ID token. */
interface ProgresoApi {
    /** Con `etag` (If-None-Match), el servidor responde 304 vacío si nada cambió. */
    @GET("api/progress")
    suspend fun progreso(
        @Header("Authorization") auth: String,
        @Header("If-None-Match") etag: String?,
        @Query("level") nivel: String
    ): Response<ProgresoDto>

    @POST("api/progress")
    suspend fun registrarChat(
        @Header("Authorization") auth: String,
        @Body request: ChatProgresoRequest
    ): Response<ChatProgresoResponse>
}
