package com.xpspeak.app.feature.lessons.data

import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import java.text.Normalizer
import kotlin.math.roundToLong

/**
 * Calificación sin conexión (backend/docs/lecciones-diseno.md §6.2). Es la misma lógica
 * que lib/lecciones/calificador.js del backend: si cambia allá, cambia aquí.
 * Solo sirve para dar el resultado al instante; el XP y el desbloqueo los
 * decide el servidor cuando se sincroniza el intento.
 */
internal object CalificadorLocal {

    fun calificar(evaluacion: SeccionDto, respuestas: Map<String, RespuestaUsuario>): IntentoResponse {
        val items = evaluacion.items.orEmpty()
        val feedback = mutableListOf<FeedbackDto>()
        val conceptosConFallo = linkedSetOf<String>()
        var correctas = 0

        for (item in items) {
            val itemId = item.id ?: continue
            val respuesta = respuestas[itemId]
            val (correcta, senales, respuestaCorrecta) = when (item.tipo) {
                TipoEjercicio.OPCION_MULTIPLE -> opcionMultiple(item, respuesta)
                TipoEjercicio.COMPLETAR -> completar(item, respuesta)
                TipoEjercicio.EMPAREJAR -> emparejar(item, respuesta)
                null -> Resultado(false, emptyList(), null)
            }
            if (correcta) correctas++
            feedback += FeedbackDto(
                id = itemId,
                correcta = correcta,
                mensaje = if (correcta) null else item.feedbackError,
                respuestaCorrecta = if (correcta) null else respuestaCorrecta
            )
            // Un ítem sin responder cuenta como error, pero no dice que el concepto sea débil.
            if (respuesta == null) continue
            senales.filter { !it.second }.mapNotNullTo(conceptosConFallo) { it.first }
        }

        val total = items.size
        val puntaje = if (total == 0) 0.0 else correctas.toDouble() / total
        val umbral = evaluacion.umbralAprobacion ?: 0.7
        return IntentoResponse(
            puntaje = (puntaje * 10000).roundToLong() / 10000.0,
            correctas = correctas,
            total = total,
            // Tolerancia para que 7/10 contra 0.70 no falle por redondeo.
            aprobada = puntaje >= umbral - 1e-9,
            estado = null,
            repetido = false,
            sugerencia = null,
            feedback = feedback,
            xpGanado = 0,
            desbloqueadaSiguiente = null,
            conceptosDebiles = conceptosConFallo.toList()
        )
    }

    /** Minúsculas, espacios colapsados, apóstrofo tipográfico → recto y sin signo final. */
    fun normalizarTexto(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFC)
            .lowercase()
            .replace(Regex("[‘’ʼ]"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(Regex("[.!?¡¿]+$"), "")
            .trim()

    /** senales: (concepto_id, acierto) para el SRS. */
    private data class Resultado(
        val correcta: Boolean,
        val senales: List<Pair<String?, Boolean>>,
        val respuestaCorrecta: Any?
    )

    private fun opcionMultiple(item: ItemDto, respuesta: RespuestaUsuario?): Resultado {
        val clave = runCatching { item.respuestaCorrecta?.asInt }.getOrNull()
        val correcta = respuesta is RespuestaUsuario.Opcion && respuesta.indice == clave
        return Resultado(correcta, listOf(item.conceptoId to correcta), clave)
    }

    private fun completar(item: ItemDto, respuesta: RespuestaUsuario?): Resultado {
        val clave = runCatching { item.respuestaCorrecta?.asString }.getOrNull()
        val aceptadas = (listOfNotNull(clave) + item.acepta.orEmpty()).map(::normalizarTexto).toSet()
        val correcta = respuesta is RespuestaUsuario.Texto && normalizarTexto(respuesta.texto) in aceptadas
        return Resultado(correcta, listOf(item.conceptoId to correcta), clave)
    }

    /** Correcto solo si todos los pares están bien y no sobra ninguno; la señal SRS es por par. */
    private fun emparejar(item: ItemDto, respuesta: RespuestaUsuario?): Resultado {
        val pares = item.pares.orEmpty()
        val elegidos = (respuesta as? RespuestaUsuario.Pares)?.elegidos.orEmpty()
            .entries.associate { normalizarTexto(it.key) to normalizarTexto(it.value) }
        val senales = pares.map { par ->
            par.conceptoId to (elegidos[normalizarTexto(par.izq)] == normalizarTexto(par.der))
        }
        val correcta = respuesta is RespuestaUsuario.Pares &&
            respuesta.elegidos.size == pares.size && senales.all { it.second }
        return Resultado(correcta, senales, pares.map { listOf(it.izq, it.der) })
    }
}
