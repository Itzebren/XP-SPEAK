package com.xpspeak.app.feature.lessons.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Endpoints del módulo de Lecciones en backend/ (este mismo repo)
 * (backend/docs/lecciones-diseno.md §6). Todos exigen el ID token de Firebase, menos el
 * manifiesto, que es público.
 */
interface LeccionesApi {
    @GET("api/lessons")
    suspend fun catalogo(
        @Header("Authorization") auth: String,
        @Query("level") nivel: String
    ): Response<CatalogoDto>

    @GET("api/lessons/manifest")
    suspend fun manifiesto(): Response<ManifiestoDto>

    @GET("api/lessons/{id}")
    suspend fun leccion(
        @Header("Authorization") auth: String,
        @Path("id") id: String
    ): Response<LeccionDto>

    @POST("api/lessons/{id}/attempt")
    suspend fun enviarIntento(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body request: IntentoRequest
    ): Response<IntentoResponse>

    @POST("api/lessons/{id}/progress")
    suspend fun guardarAvance(
        @Header("Authorization") auth: String,
        @Path("id") id: String,
        @Body request: AvanceRequest
    ): Response<AvanceResponse>

    @GET("api/srs/session")
    suspend fun sesionRepaso(
        @Header("Authorization") auth: String,
        @Query("level") nivel: String
    ): Response<SesionRepasoDto>

    @POST("api/srs/session")
    suspend fun enviarRepaso(
        @Header("Authorization") auth: String,
        @Body request: RepasoRequest
    ): Response<RepasoResponse>

    @GET("api/srs/review")
    suspend fun conceptosDebiles(
        @Header("Authorization") auth: String,
        @Query("level") nivel: String
    ): Response<ConceptosDebilesDto>
}
