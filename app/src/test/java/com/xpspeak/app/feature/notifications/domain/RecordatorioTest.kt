package com.xpspeak.app.feature.notifications.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RecordatorioTest {

    private val sabado = LocalDate.of(2026, 10, 10)
    private val lunes = LocalDate.of(2026, 10, 12)
    private val ajustes = AjustesNotificacion()

    @Test
    fun `con racha vigente avisa que se puede perder`() {
        assertEquals(
            "¡No pierdas tu racha de 4 días! Practica unos minutos hoy.",
            Recordatorio.mensaje(ajustes, lunes, racha = 4, ultimoDia = "2026-10-11")
        )
    }

    @Test
    fun `sin racha manda el recordatorio diario`() {
        assertEquals("Es hora de practicar tu inglés con XP-SPEAK.", Recordatorio.mensaje(ajustes, lunes, 0, null))
        // Con la alerta de racha apagada también se manda el recordatorio normal.
        assertEquals(
            "Es hora de practicar tu inglés con XP-SPEAK.",
            Recordatorio.mensaje(ajustes.copy(alertasRacha = false), lunes, 4, "2026-10-11")
        )
    }

    @Test
    fun `no avisa si ya practico hoy, si esta apagado o fuera de la frecuencia`() {
        assertNull(Recordatorio.mensaje(ajustes, lunes, 5, "2026-10-12"))
        assertNull(Recordatorio.mensaje(ajustes.copy(activadas = false), lunes, 0, null))
        assertNull(Recordatorio.mensaje(ajustes.copy(recordatorioDiario = false), lunes, 0, null))
        assertNull(Recordatorio.mensaje(ajustes.copy(frecuencia = Frecuencia.ENTRE_SEMANA), sabado, 0, null))
        assertNull(Recordatorio.mensaje(ajustes.copy(frecuencia = Frecuencia.FIN_DE_SEMANA), lunes, 0, null))
    }

    @Test
    fun `frecuencias`() {
        assertTrue(Frecuencia.ENTRE_SEMANA.incluye(DayOfWeek.FRIDAY))
        assertFalse(Frecuencia.ENTRE_SEMANA.incluye(DayOfWeek.SUNDAY))
        assertTrue(Frecuencia.FIN_DE_SEMANA.incluye(DayOfWeek.SATURDAY))
        assertFalse(AjustesNotificacion(recordatorioDiario = false, alertasRacha = false).programadas)
    }

    @Test
    fun `la demora llega a la proxima hora elegida`() {
        val seis = LocalTime.of(18, 0)
        assertEquals(Duration.ofHours(8), Recordatorio.demoraHasta(seis, LocalDateTime.of(2026, 10, 12, 10, 0)))
        assertEquals(Duration.ofHours(23), Recordatorio.demoraHasta(seis, LocalDateTime.of(2026, 10, 12, 19, 0)))
        assertEquals(Duration.ofDays(1), Recordatorio.demoraHasta(seis, LocalDateTime.of(2026, 10, 12, 18, 0)))
    }
}
