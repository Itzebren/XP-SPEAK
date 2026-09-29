package com.xpspeak.app.feature.lessons.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

// Valores que manda el backend. Gson deja en null un valor desconocido,
// así una versión nueva del contenido no rompe una app vieja.
enum class EstadoLeccion {
    @SerializedName("bloqueada") BLOQUEADA,
    @SerializedName("disponible") DISPONIBLE,
    @SerializedName("en_progreso") EN_PROGRESO,
    @SerializedName("reprobada") REPROBADA,
    @SerializedName("completada") COMPLETADA
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
    val lecciones: List<LeccionResumenDto>
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
    val tipo: TipoSeccion?,
    val titulo: String?,
    val cuerpo: String?,
    val explicacion: String?,
    val ejemplos: List<String>?,
    val lineas: List<LineaDialogoDto>?,
    val items: List<ItemDto>?,
    @SerializedName("umbral_aprobacion") val umbralAprobacion: Double?
)

data class LineaDialogoDto(val hablante: String, val en: String, val es: String)

data class ItemDto(
    // vocabulario
    val en: String?,
    val es: String?,
    val nota: String?,
    // evaluación. La clave de respuestas viene en el JSON para calificar sin conexión (§6.2).
    val id: String?,
    val tipo: TipoEjercicio?,
    val enunciado: String?,
    val opciones: List<String>?,
    val pares: List<ParDto>?,
    @SerializedName("concepto_id") val conceptoId: String? = null,
    /** Int (opción múltiple) o String (completar). */
    @SerializedName("respuesta_correcta") val respuestaCorrecta: JsonElement? = null,
    val acepta: List<String>? = null,
    @SerializedName("feedback_error") val feedbackError: String? = null
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
    val respuestas: List<RespuestaDto>
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
    @SerializedName("conceptos_debiles") val conceptosDebiles: List<String>
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

data class ConceptoDebilDto(
    @SerializedName("concepto_id") val conceptoId: String,
    val en: String?,
    val es: String?,
    val titulo: String?,
    val fallos: Int,
    @SerializedName("tasa_error") val tasaError: Double
)

data class ErrorDto(val error: String?, val codigo: String?)
