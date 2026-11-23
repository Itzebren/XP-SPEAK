package com.xpspeak.app.feature.auth.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** QA 29-oct (H8): cuándo se usa el enlace de Firebase en vez del código por correo. */
class RecuperacionRepositoryTest {

    @Test
    fun `un 503 del envio de correo o su codigo activan el respaldo de Firebase`() {
        val cuerpo = """{"error":"No pudimos enviarte el correo por ahora. Intenta más tarde.","codigo":"CORREO_NO_DISPONIBLE"}"""
        assertTrue(RecuperacionRepository.esServicioNoDisponible(503, RecuperacionRepository.codigoDe(cuerpo)))
        assertTrue(RecuperacionRepository.esServicioNoDisponible(500, "CORREO_NO_CONFIGURADO"))
        assertTrue(RecuperacionRepository.esServicioNoDisponible(503, null))
        assertEquals("No pudimos enviarte el correo por ahora. Intenta más tarde.", RecuperacionRepository.mensajeDe(cuerpo))
    }

    @Test
    fun `otros errores siguen mostrandose tal cual`() {
        assertFalse(RecuperacionRepository.esServicioNoDisponible(400, RecuperacionRepository.codigoDe("""{"error":"Correo inválido"}""")))
        assertFalse(RecuperacionRepository.esServicioNoDisponible(500, null))
        assertNull(RecuperacionRepository.mensajeDe("Internal Server Error"))
        assertNull(RecuperacionRepository.codigoDe(null))
    }
}
