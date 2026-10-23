package com.xpspeak.app.feature.games.ui.comun

import org.junit.Assert.assertEquals
import org.junit.Test

class LocutorIdiomaTest {

    private fun idiomas(texto: String) = fragmentosPorIdioma(texto).map { if (it.esEspanol) "es" else "en" }

    @Test
    fun `una respuesta en ingles se lee toda en ingles`() {
        assertEquals(listOf("en", "en"), idiomas("Great job! What do you like to eat for breakfast?"))
    }

    @Test
    fun `una explicacion en espanol se lee con voz en espanol`() {
        assertEquals(
            listOf("es", "es"),
            idiomas("¡Claro! La palabra \"breakfast\" significa desayuno en español.")
        )
    }

    @Test
    fun `un mensaje mixto cambia de voz por oracion`() {
        assertEquals(listOf("es", "en"), idiomas("Se dice así, con el verbo to be. I am a student."))
    }

    @Test
    fun `una oracion sin pistas sigue el idioma de la anterior`() {
        assertEquals(listOf("es", "es"), idiomas("Muy bien, lo dijiste correctamente. OK!"))
        assertEquals(listOf("en", "en"), idiomas("That is correct. OK!"))
    }
}
