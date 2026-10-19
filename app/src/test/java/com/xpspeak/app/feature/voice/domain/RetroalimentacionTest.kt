package com.xpspeak.app.feature.voice.domain

import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.FonemaEvaluado
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.PalabraEvaluada
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.ResultadoPronunciacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-09: reglas de la retroalimentación de la práctica de pronunciación. */
class RetroalimentacionTest {

    private fun resultado(vararg palabras: PalabraEvaluada, confianza: Int = 90, texto: String = "hello world") =
        ResultadoPronunciacion(texto, 80.0, 80.0, 80.0, 80.0, palabras.toList(), confianza)

    @Test
    fun `una palabra omitida cuenta como mal aunque Azure le de puntaje`() {
        val omitida = PalabraEvaluada("world", 100.0, "Omission")
        assertEquals(Pronunciacion.Nivel.MALO, Retroalimentacion.nivelDe(omitida))
    }

    @Test
    fun `los umbrales coinciden con Eco Vocal`() {
        assertEquals(Pronunciacion.Nivel.BUENO, Retroalimentacion.nivelDe(PalabraEvaluada("a", 70.0, "None")))
        assertEquals(Pronunciacion.Nivel.REGULAR, Retroalimentacion.nivelDe(PalabraEvaluada("a", 60.0, "None")))
        assertEquals(Pronunciacion.Nivel.MALO, Retroalimentacion.nivelDe(PalabraEvaluada("a", 59.0, "Mispronunciation")))
    }

    @Test
    fun `solo se sugieren las palabras que no salieron bien, en orden`() {
        val r = resultado(
            PalabraEvaluada("the", 95.0, "None"),
            PalabraEvaluada("quick", 40.0, "Mispronunciation"),
            PalabraEvaluada("fox", 65.0, "None")
        )
        assertEquals(listOf("quick", "fox"), Retroalimentacion.palabrasAPracticar(r).map { it.palabra })
        assertEquals("Practica estas palabras: «quick», «fox».", Retroalimentacion.resumen(r))
    }

    @Test
    fun `sin errores se felicita`() {
        val r = resultado(PalabraEvaluada("hello", 90.0, "None"))
        assertEquals("¡Excelente! Pronunciaste bien todas las palabras.", Retroalimentacion.resumen(r))
    }

    @Test
    fun `los fonemas debiles van del peor al mejor y se omiten los buenos`() {
        val palabra = PalabraEvaluada(
            "think", 50.0, "Mispronunciation",
            listOf(FonemaEvaluado("θ", 30.0), FonemaEvaluado("ɪ", 90.0), FonemaEvaluado("ŋ", 55.0), FonemaEvaluado("k", 80.0))
        )
        assertEquals(listOf("θ", "ŋ"), Retroalimentacion.fonemasAPracticar(palabra).map { it.fonema })
    }

    @Test
    fun `con confianza menor al 30 por ciento se pide repetir`() {
        assertFalse(Retroalimentacion.esLegible(resultado(confianza = 29)))
        assertTrue(Retroalimentacion.esLegible(resultado(confianza = 30)))
        assertFalse(Retroalimentacion.esLegible(resultado(confianza = 90, texto = "")))
    }

    @Test
    fun `las frases dependen del nivel`() {
        assertTrue(Retroalimentacion.frasesDe("A2").isNotEmpty())
        assertTrue(Retroalimentacion.frasesDe("A1") != Retroalimentacion.frasesDe("A2"))
        assertEquals(Retroalimentacion.frasesDe("A1"), Retroalimentacion.frasesDe("desconocido"))
    }
}
