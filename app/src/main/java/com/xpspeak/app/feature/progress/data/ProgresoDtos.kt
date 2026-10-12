package com.xpspeak.app.feature.progress.data

import com.google.gson.annotations.SerializedName

/**
 * Contrato de GET /api/progress (docs/progreso-diseno.md Fase 2). Los campos
 * que podrían faltar son nullable: Gson no usa los valores por defecto de Kotlin.
 */
data class ProgresoDto(
    val nivel: String,
    val dominio: DominioDto,
    val xp: XpDto,
    val racha: RachaDto,
    val habilidades: List<HabilidadDto>,
    val insignias: List<InsigniaDto>,
    @SerializedName("sin_actividad") val sinActividad: Boolean
)

/** Lecciones aprobadas del nivel MCER actual. */
data class DominioDto(val aprobadas: Int, val total: Int, val porcentaje: Int)

/** "Nivel 4 · 120/400 para el siguiente" (RF-14). */
data class XpDto(
    val total: Int,
    val nivel: Int,
    @SerializedName("xp_en_nivel") val xpEnNivel: Int,
    @SerializedName("xp_para_siguiente") val xpParaSiguiente: Int
)

/** `ultimoDia` ("2026-10-11"): último día con actividad, en hora de México. */
data class RachaDto(
    val actual: Int,
    val maxima: Int,
    @SerializedName("ultimo_dia") val ultimoDia: String? = null
)

/** `porcentaje` null = "Sin datos" (menos de 5 respuestas). */
data class HabilidadDto(
    val id: String,
    val nombre: String,
    val porcentaje: Int?,
    val sugerencia: String?
)

/**
 * Una insignia del catálogo con el estado del usuario. También es la forma
 * de `insignias_nuevas` (ahí solo vienen id, nombre, descripción, criterio e ícono)
 * y del catálogo empaquetado en assets/progreso/insignias.json.
 */
data class InsigniaDto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val criterio: String?,
    val icono: String?,
    val obtenida: Boolean = false,
    val fecha: Long? = null,
    val retroactiva: Boolean = false,
    val progreso: ProgresoInsigniaDto? = null
)

data class ProgresoInsigniaDto(val actual: Int, val meta: Int)

/** assets/progreso/insignias.json (npm run exportar:insignias). */
data class CatalogoInsigniasDto(val insignias: List<InsigniaDto>)

// Chat — POST /api/progress/chat
data class ChatProgresoRequest(@SerializedName("sesion_id") val sesionId: String)

data class ChatProgresoResponse(
    @SerializedName("insignias_nuevas") val insigniasNuevas: List<InsigniaDto>?,
    @SerializedName("nivel_xp_nuevo") val nivelXpNuevo: Int?,
    val repetido: Boolean
)
