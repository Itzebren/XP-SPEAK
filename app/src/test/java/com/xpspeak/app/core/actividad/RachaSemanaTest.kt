package com.xpspeak.app.core.actividad

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RachaSemanaTest {

    private val domingo = LocalDate.of(2026, 10, 11)
    private val jueves = LocalDate.of(2026, 10, 8)

    @Test
    fun `una racha larga llena toda la semana hasta hoy`() {
        assertEquals(List(7) { true }, Racha.semana(10, "2026-10-11", domingo))
    }

    @Test
    fun `solo se marcan los dias que cubre la racha`() {
        // Racha de 3 que terminó ayer (sábado): jueves, viernes y sábado.
        assertEquals(listOf(false, false, false, true, true, true, false), Racha.semana(3, "2026-10-10", domingo))
    }

    @Test
    fun `los dias que faltan de la semana quedan vacios`() {
        assertEquals(listOf(true, true, true, true, false, false, false), Racha.semana(5, "2026-10-08", jueves))
    }

    @Test
    fun `sin racha vigente no se marca nada`() {
        assertEquals(List(7) { false }, Racha.semana(4, "2026-10-08", domingo))
        assertEquals(List(7) { false }, Racha.semana(0, null, domingo))
    }
}
