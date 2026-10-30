package com.xpspeak.app.feature.lessons.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import com.xpspeak.app.feature.progress.data.InsigniaDto

// Valores que manda el backend. Gson deja en null un valor desconocido,
// así una versión nueva del contenido no rompe una app vieja.
enum class EstadoLeccion {
    @SerializedName("bloqueada") BLOQUEADA,
    @SerializedName("disponible") DISPONIBLE,
    @SerializedName("en_progreso") EN_PROGRESO,
    @SerializedName("reprobada") REPROBADA,
    @SerializedName("completada") COMPLETADA,
    /** RN-07: tema nuevo que espera a que se repasen los conceptos débiles vencidos. */
    @SerializedName("repaso_pendiente") REPASO_PENDIENTE
}

enum class TipoSeccion {
    @SerializedName("introduccion") INTRODUCCION,
    @SerializedName("vocabulario") VOCABULARIO,
    @SerializedName("gramatica") GRAMATICA,
    @SerializedName("dialogo") DIALOGO,
    @SerializedName("evaluacion") EVALUACION
}

enum class TipoEjercicio {
    @SerializedName("opcion_multiple") OPCION_MULTIPLE,
    @SerializedName("completar") COMPLETAR,
    @SerializedName("emparejar") EMPAREJAR
}

// Catálogo — GET /api/lessons?level=A1
data class CatalogoDto(
    val nivel: String,
    val lecciones: List<LeccionResumenDto>,
    /** RN-07: conceptos que hay que repasar antes de empezar un tema nuevo. */
    @SerializedName("repaso_requerido") val repasoRequerido: Int = 0
)

data class LeccionResumenDto(
    val id: String,
    val orden: Int,
    val titulo: String,
    val estado: EstadoLeccion?,
    val intentos: Int,
    @SerializedName("xp_recompensa") val xpRecompensa: Int,
    @SerializedName("mejor_puntaje") val mejorPuntaje: Double?
)

// Contenido — GET /api/lessons/:id (esquema §4.2). Una sola clase por sección
// e ítem con campos opcionales: cada `tipo` usa solo los suyos.
data class LeccionDto(
    val id: String,
    @SerializedName("modulo_tematico") val moduloTematico: String,
    @SerializedName("nivel_mcer") val nivelMcer: String,
    @SerializedName("can_do") val canDo: List<String>,
    val secciones: List<SeccionDto>
)

data class SeccionDto(
    val tipo: TipoSeccion? = null,
    val titulo: String? = null,
    val cuerpo: String? = null,
    val explicacion: String? = null,
    val ejemplos: List<String>? = null,
    val lineas: List<LineaDialogoDto>? = null,
    val items: List<ItemDto>? = null,
    @SerializedName("umbral_aprobacion") val umbralAprobacion: Double? = null
)

data class LineaDialogoDto(val hablante: String, val en: String, val es: String)

data class ItemDto(
    // vocabulario
    val en: String? = null,
    val es: String? = null,
    val nota: String? = null,
    // evaluación. La clave de respuestas viene en el JSON para calificar sin conexión (§6.2).
    val id: String? = null,
    val tipo: TipoEjercicio? = null,
    val enunciado: String? = null,
    val opciones: List<String>? = null,
    val pares: List<ParDto>? = null,
    @SerializedName("concepto_id") val conceptoId: String? = null,
    /** Int (opción múltiple) o String (completar). */
    @SerializedName("respuesta_correcta") val respuestaCorrecta: JsonElement? = null,
    val acepta: List<String>? = null,
    @SerializedName("feedback_error") val feedbackError: String? = null,
    /** Solo en la sesión de repaso: el concepto que repasa este ejercicio. */
    val concepto: ConceptoDebilDto? = null
)

data class ParDto(
    val izq: String,
    val der: String,
    @SerializedName("concepto_id") val conceptoId: String? = null
)

// Manifiesto — GET /api/lessons/manifest (público). Dice qué versión de cada
// lección hay en el servidor para refrescar solo lo que cambió en la caché.
data class ManifiestoDto(
    @SerializedName("version_contenido") val versionContenido: String,
    val niveles: Map<String, List<EntradaManifiestoDto>>
)

data class EntradaManifiestoDto(val id: String, val version: Int)

// Intento — POST /api/lessons/:id/attempt
data class IntentoRequest(
    @SerializedName("attempt_id") val attemptId: String,
    val respuestas: List<RespuestaDto>,
    /** Cuándo se contestó (ms epoch): un intento hecho sin conexión cuenta para ese día (RN-08). */
    @SerializedName("completado_en") val completadoEn: Long? = null
)

/** `valor`: Int (opción múltiple), String (completar) o List<List<String>> (emparejar). */
data class RespuestaDto(val id: String, val valor: Any)

data class IntentoResponse(
    val puntaje: Double,
    val correctas: Int,
    val total: Int,
    val aprobada: Boolean,
    val estado: EstadoLeccion?,
    val repetido: Boolean,
    val sugerencia: String?,
    val feedback: List<FeedbackDto>,
    @SerializedName("xp_ganado") val xpGanado: Int,
    @SerializedName("desbloqueada_siguiente") val desbloqueadaSiguiente: String?,
    @SerializedName("conceptos_debiles") val conceptosDebiles: List<String>,
    /** Nombres de los conceptos débiles y de la lección desbloqueada (faltan en un backend viejo o sin conexión). */
    @SerializedName("conceptos_debiles_detalle") val conceptosDebilesDetalle: List<ConceptoNombreDto>? = null,
    @SerializedName("desbloqueada_titulo") val desbloqueadaTitulo: String? = null,
    /** Progreso: insignias ganadas en vivo (para festejarlas) y nivel de XP alcanzado; faltan en un backend viejo. */
    @SerializedName("insignias_nuevas") val insigniasNuevas: List<InsigniaDto>? = null,
    @SerializedName("nivel_xp_nuevo") val nivelXpNuevo: Int? = null
)

data class FeedbackDto(
    val id: String,
    val correcta: Boolean,
    val mensaje: String?,
    @SerializedName("respuesta_correcta") val respuestaCorrecta: Any?
)

// Avance — POST /api/lessons/:id/progress
data class AvanceRequest(@SerializedName("seccion_actual") val seccionActual: Int)
data class AvanceResponse(val estado: EstadoLeccion?)

// SRS — GET /api/srs/review
data class ConceptosDebilesDto(val conceptos: List<ConceptoDebilDto>)

/** Nombre legible de un concepto: el título de la regla gramatical o la palabra en inglés. */
data class ConceptoNombreDto(
    @SerializedName("concepto_id") val conceptoId: String,
    val titulo: String? = null,
    val en: String? = null,
    val es: String? = null
) {
    val nombre: String get() = titulo ?: en?.let { if (es != null) "$it ($es)" else it } ?: nombreLegible(conceptoId)
}

/** "gram.verb_to_be_present" → "verb to be present": por si el servidor no mandó el nombre. */
fun nombreLegible(conceptoId: String): String = conceptoId.substringAfterLast('.').replace('_', ' ')

data class ConceptoDebilDto(
    @SerializedName("concepto_id") val conceptoId: String,
    val en: String?,
    val es: String?,
    val titulo: String?,
    val fallos: Int,
    @SerializedName("tasa_error") val tasaError: Double
)

// Sesión de repaso — GET/POST /api/srs/session (SM-2, RF-13)
data class SesionRepasoDto(
    @SerializedName("total_vencidos") val totalVencidos: Int,
    /** Cuándo vuelve a haber repaso (ms epoch) si hoy no toca nada. */
    @SerializedName("proxima_revision") val proximaRevision: Long?,
    /** Un ejercicio por concepto; su id es "<lección>:<ítem>[:<par>]". */
    val items: List<ItemDto>
)

data class RepasoRequest(
    @SerializedName("attempt_id") val attemptId: String,
    val respuestas: List<RespuestaDto>
)

data class RepasoResponse(
    val puntaje: Double,
    val correctas: Int,
    val total: Int,
    val feedback: List<FeedbackDto>,
    val conceptos: List<ConceptoReprogramadoDto>,
    val repetido: Boolean,
    /** Progreso: insignias ganadas en vivo (para festejarlas) y nivel de XP alcanzado; faltan en un backend viejo. */
    @SerializedName("insignias_nuevas") val insigniasNuevas: List<InsigniaDto>? = null,
    @SerializedName("nivel_xp_nuevo") val nivelXpNuevo: Int? = null
)

data class ConceptoReprogramadoDto(
    @SerializedName("concepto_id") val conceptoId: String,
    val acierto: Boolean,
    /** Días hasta el siguiente repaso según SM-2. */
    val intervalo: Int,
    @SerializedName("proxima_revision") val proximaRevision: Long
)

data class ErrorDto(val error: String?, val codigo: String?)
