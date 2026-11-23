package com.xpspeak.app.feature.progress.data

import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProgresoReglasTest {

    private val hoy = LocalDate.of(2026, 10, 11)

    // Mismos casos que backend/test/progreso.test.js.
    @Test
    fun `el nivel por XP coincide con el del servidor`() {
        assertEquals(listOf(0, 100, 300, 600, 1000), (1..5).map(ProgresoReglas::xpParaNivel))
        assertEquals(XpDto(0, 1, 0, 100), ProgresoReglas.nivelPorXp(0))
        assertEquals(XpDto(99, 1, 99, 100), ProgresoReglas.nivelPorXp(99))
        assertEquals(XpDto(100, 2, 0, 200), ProgresoReglas.nivelPorXp(100))
        assertEquals(XpDto(720, 4, 120, 400), ProgresoReglas.nivelPorXp(720))
        assertEquals(1, ProgresoReglas.nivelPorXp(-5).nivel)
    }

    @Test
    fun `la racha del servidor solo se aplica sin actividades pendientes`() {
        assertTrue(ProgresoReglas.debeSobrescribirRacha(pendientes = 0))
        assertFalse(ProgresoReglas.debeSobrescribirRacha(pendientes = 2))
    }

    @Test
    fun `la racha vigente se pierde despues de un dia sin actividad`() {
        assertEquals(4, Racha.vigente(4, "2026-10-11", hoy))
        assertEquals(4, Racha.vigente(4, "2026-10-10", hoy))
        assertEquals(0, Racha.vigente(4, "2026-10-09", hoy))
        assertEquals(0, Racha.vigente(4, null, hoy))
        assertEquals("1 día", Racha.texto(1))
        assertEquals("0 días", Racha.texto(0))
    }

    @Test
    fun `estado minimo sin red con el perfil local y las insignias en gris`() {
        val usuario = UsuarioEntity(uid = "u", correo = "a@b.c", nivel = "A2", xp = 320, racha = 3, ultimoDiaActivo = "2026-10-10")
        val catalogo = listOf(
            InsigniaDto("primer-logro", "Primer Logro", "…", "…", "estrella", obtenida = true, progreso = ProgresoInsigniaDto(1, 1))
        )
        val dto = ProgresoReglas.estadoMinimo(usuario, catalogo, hoy)

        assertEquals("A2", dto.nivel)
        assertEquals(XpDto(320, 3, 20, 300), dto.xp)
        assertEquals(3, dto.racha.actual)
        assertEquals(0, dto.dominio.total)
        assertEquals(4, dto.habilidades.size)
        assertTrue(dto.habilidades.all { it.porcentaje == null })
        assertFalse(dto.insignias.single().obtenida)
        assertNull(dto.insignias.single().progreso)
        assertFalse(dto.sinActividad)
    }

    @Test
    fun `estado minimo de un usuario nuevo es sin actividad`() {
        val dto = ProgresoReglas.estadoMinimo(UsuarioEntity(uid = "u", correo = "a@b.c", nivel = "A1"), emptyList(), hoy)
        assertTrue(dto.sinActividad)
        assertEquals(0, dto.racha.actual)
        assertTrue(ProgresoReglas.estadoMinimo(null, emptyList(), hoy).sinActividad)
    }
}
