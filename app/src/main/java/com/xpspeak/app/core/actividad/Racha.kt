package com.xpspeak.app.feature.games.domain

import java.time.LocalDate

/**
 * RN-08: la racha sube con al menos una actividad válida (lección, chat o
 * minijuego) por día. Varias actividades el mismo día no la suben de nuevo;
 * un día sin actividad la reinicia.
 */
object Racha {
    /** @param ultimoDia último día con actividad (ISO, "2026-10-06"), o null si nunca hubo */
    fun siguiente(rachaActual: Int, ultimoDia: String?, hoy: LocalDate): Int {
        val ultimo = ultimoDia?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 1
        return when {
            ultimo == hoy -> maxOf(rachaActual, 1)
            ultimo == hoy.minusDays(1) -> rachaActual + 1
            else -> 1
        }
    }
}
