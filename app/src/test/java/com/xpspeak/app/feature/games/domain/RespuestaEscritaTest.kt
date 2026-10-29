package com.xpspeak.app.feature.games.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Respuestas escritas en Misión: la correcta, sus formas aceptadas y contracciones. */
class RespuestaEscritaTest {

    // Último paso de "En la cafetería": "Do you want any fruit?"
    private val fruta = listOf(
        OpcionMision("Yes, I want any fruit.", correcta = false, feedback = "some"),
        OpcionMision(
            "No, I don't. Thank you!", correcta = true,
            aceptadas = listOf("No, thank you.", "No, thanks.", "No, I don't.", "No, thank you very much.")
        ),
        OpcionMision("No, I'm not fruit.", correcta = false, feedback = "sin sentido")
    )

    @Test
    fun `acepta la frase de la opcion y cualquiera de sus formas aceptadas`() {
        assertEquals(1, RespuestaEscrita.opcionDe(fruta, "No, I don't. Thank you!"))
        assertEquals(1, RespuestaEscrita.opcionDe(fruta, "no thank you"))
        assertEquals(1, RespuestaEscrita.opcionDe(fruta, "No, thanks"))
        assertEquals(1, RespuestaEscrita.opcionDe(fruta, "NO, THANK YOU VERY MUCH!!"))
    }

    @Test
    fun `las contracciones valen igual que su forma larga`() {
        assertEquals(1, RespuestaEscrita.opcionDe(fruta, "No, I do not. Thank you!"))
        val nombre = listOf(
            OpcionMision("My name are Diego.", correcta = false, feedback = "is"),
            OpcionMision("My name is Diego.", correcta = true, aceptadas = listOf("I'm Diego.")),
            OpcionMision("I'm fine, thank you.", correcta = false, feedback = "nombre")
        )
        assertEquals(1, RespuestaEscrita.opcionDe(nombre, "I am Diego"))
        assertEquals(1, RespuestaEscrita.opcionDe(nombre, "My name's Diego."))
    }

    @Test
    fun `escribir una opcion incorrecta la elige para explicar el error`() {
        assertEquals(0, RespuestaEscrita.opcionDe(fruta, "Yes, I want any fruit"))
        assertEquals(2, RespuestaEscrita.opcionDe(fruta, "No, I'm not fruit"))
    }

    @Test
    fun `algo que no se parece a ninguna respuesta no cuenta`() {
        assertNull(RespuestaEscrita.opcionDe(fruta, "I like pizza"))
        assertNull(RespuestaEscrita.opcionDe(fruta, "   "))
    }
}
