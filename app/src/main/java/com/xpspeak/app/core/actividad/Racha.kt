package com.xpspeak.app.core.actividad

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

    /** La racha que se muestra hoy: si el último día activo fue antes de ayer, ya se perdió. */
    fun vigente(rachaActual: Int, ultimoDia: String?, hoy: LocalDate): Int {
        val ultimo = ultimoDia?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return 0
        return if (ultimo == hoy || ultimo == hoy.minusDays(1)) rachaActual else 0
    }

    /**
     * Los 7 círculos del Inicio (Ilustración 32), de lunes a domingo de la semana de [hoy]:
     * `true` en los días que cubre la racha vigente.
     */
    fun semana(rachaActual: Int, ultimoDia: String?, hoy: LocalDate): List<Boolean> {
        val racha = vigente(rachaActual, ultimoDia, hoy)
        val ultimo = ultimoDia?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val lunes = hoy.minusDays(hoy.dayOfWeek.value - 1L)
        return (0L..6L).map { i ->
            val dia = lunes.plusDays(i)
            racha > 0 && ultimo != null && !dia.isAfter(ultimo) && dia.isAfter(ultimo.minusDays(racha.toLong()))
        }
    }

    /** "1 día", "5 días". */
    fun texto(dias: Int): String = if (dias == 1) "1 día" else "$dias días"
}
