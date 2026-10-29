package com.xpspeak.app.feature.auth.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** RNF-06: 8 caracteres, una letra y un número; el mensaje dice solo lo que falta. */
class PasswordValidatorTest {

    @Test
    fun `una contrasena completa es valida y no tiene mensaje`() {
        assertTrue(PasswordValidator.esValida("ingles2026"))
        assertNull(PasswordValidator.mensajeError("Speak2024!"))
    }

    @Test
    fun `el mensaje dice solo lo que falta`() {
        assertEquals("Tu contraseña debe contener al menos: un número.", PasswordValidator.mensajeError("password"))
        assertEquals("Tu contraseña debe contener al menos: una letra.", PasswordValidator.mensajeError("12345678"))
        assertEquals("Tu contraseña debe contener al menos: 8 caracteres.", PasswordValidator.mensajeError("abc123"))
        assertEquals("Tu contraseña debe contener al menos: 8 caracteres y un número.", PasswordValidator.mensajeError("ingles"))
        assertEquals(
            "Tu contraseña debe contener al menos: 8 caracteres, una letra y un número.",
            PasswordValidator.mensajeError("")
        )
        assertFalse(PasswordValidator.esValida("!!!!!!!!"))
    }
}
