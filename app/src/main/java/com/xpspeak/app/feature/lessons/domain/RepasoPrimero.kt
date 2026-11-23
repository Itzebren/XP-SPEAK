package com.xpspeak.app.feature.lessons.domain

import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto
import com.xpspeak.app.feature.lessons.data.SesionRepasoDto

/**
 * RN-07: los conceptos con alta tasa de error que SM-2 marca para hoy se repasan
 * antes de empezar un tema nuevo. Solo retiene lecciones "disponibles" (nunca
 * empezadas): una lección en progreso, reprobada o completada se puede seguir
 * abriendo. Al terminar el repaso, SM-2 reprograma los conceptos para otro día,
 * así que el candado se quita solo aunque se hayan vuelto a fallar.
 *
 * Sin conexión no hay sesión de repaso, así que tampoco hay candado: el repaso
 * requiere el servidor y no debe impedir estudiar.
 */
object RepasoPrimero {
    /** El mismo umbral que backend/lib/lecciones/srs.js para un concepto "débil". */
    const val UMBRAL_TASA_ERROR = 0.3

    /** Conceptos vencidos hoy que ya se fallaron y tienen tasa de error alta. */
    fun prioritarios(sesion: SesionRepasoDto): Int = sesion.items
        .mapNotNull { it.concepto }
        .distinctBy { it.conceptoId }
        .count { it.fallos > 0 && it.tasaError >= UMBRAL_TASA_ERROR }

    fun retiene(leccion: LeccionResumenDto, prioritarios: Int): Boolean =
        prioritarios > 0 && leccion.estado == EstadoLeccion.DISPONIBLE
}
