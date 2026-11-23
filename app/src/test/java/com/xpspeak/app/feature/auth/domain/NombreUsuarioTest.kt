package com.xpspeak.app.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NombreUsuarioTest {

    @Test
    fun `acepta nombres normales, con acentos y espacios`() {
        assertNull(NombreUsuario.validar("Itzel P."))
        assertNull(NombreUsuario.validar("  Íñigo  "))
        assertNull(NombreUsuario.validar("Al"))
    }

    @Test
    fun `rechaza nombres vacios, cortos, largos o sin letras`() {
        assertEquals("Tu nombre debe tener al menos 2 letras.", NombreUsuario.validar(" a "))
        assertEquals("Tu nombre debe tener al menos 2 letras.", NombreUsuario.validar(""))
        assertEquals("Tu nombre puede tener hasta 30 caracteres.", NombreUsuario.validar("a".repeat(31)))
        assertEquals("Tu nombre debe incluir letras.", NombreUsuario.validar("1234"))
    }
}
