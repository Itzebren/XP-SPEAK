package com.xpspeak.app.feature.voice.domain

import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FonemaEvaluado
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Pronunciacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-09: reglas de la retroalimentación de pronunciación (frase, palabra y sonido). */
class RetroalimentacionTest {

    private fun evaluacion(vararg palabras: PalabraEvaluada, confianza: Int = 90, texto: String = "hello world") =
        EvaluacionVoz(80, confianza, palabras.toList(), texto, FuenteEvaluacion.AZURE, fluidez = 80, completitud = 80, puntajeGeneral = 80)

    @Test
    fun `una palabra omitida cuenta como mal aunque Azure le de puntaje`() {
        val omitida = PalabraEvaluada("world", 100, "Omission")
        assertEquals(Pronunciacion.Nivel.MALO, Retroalimentacion.nivelDe(omitida))
    }

    @Test
    fun `los umbrales coinciden con Eco Vocal`() {
        assertEquals(Pronunciacion.Nivel.BUENO, Retroalimentacion.nivelDe(PalabraEvaluada("a", 70)))
        assertEquals(Pronunciacion.Nivel.REGULAR, Retroalimentacion.nivelDe(PalabraEvaluada("a", 60)))
        assertEquals(Pronunciacion.Nivel.MALO, Retroalimentacion.nivelDe(PalabraEvaluada("a", 59, "Mispronunciation")))
    }

    @Test
    fun `solo se sugieren las palabras que no salieron bien, en orden`() {
        val r = evaluacion(
            PalabraEvaluada("the", 95),
            PalabraEvaluada("quick", 40, "Mispronunciation"),
            PalabraEvaluada("fox", 65)
        )
        assertEquals(listOf("quick", "fox"), Retroalimentacion.palabrasAPracticar(r).map { it.palabra })
        assertEquals("Practica estas palabras: «quick», «fox».", Retroalimentacion.resumen(r))
    }

    @Test
    fun `sin errores se felicita`() {
        val r = evaluacion(PalabraEvaluada("hello", 90))
        assertEquals("Pronunciaste bien todas las palabras.", Retroalimentacion.resumen(r))
    }

    @Test
    fun `los fonemas debiles van del peor al mejor y se omiten los buenos`() {
        val palabra = PalabraEvaluada(
            "think", 50, "Mispronunciation",
            fonemas = listOf(FonemaEvaluado("θ", 30), FonemaEvaluado("ɪ", 90), FonemaEvaluado("ŋ", 55), FonemaEvaluado("k", 80))
        )
        assertEquals(listOf("θ", "ŋ"), Retroalimentacion.fonemasAPracticar(palabra).map { it.fonema })
    }

    @Test
    fun `la descripcion nombra el sonido sustituido cuando se conoce`() {
        val party = PalabraEvaluada("party", 55, "Mispronunciation", sonidos = listOf("/ɹ/ sonó como /d/"))
        assertEquals("Fíjate en el sonido: /ɹ/ sonó como /d/.", Retroalimentacion.descripcion(party))
        assertEquals("¡Bien pronunciada!", Retroalimentacion.descripcion(PalabraEvaluada("hi", 90)))
    }

    @Test
    fun `con confianza menor al 30 por ciento se pide repetir`() {
        assertFalse(Retroalimentacion.esLegible(evaluacion(confianza = 29)))
        assertTrue(Retroalimentacion.esLegible(evaluacion(confianza = 30)))
        assertFalse(Retroalimentacion.esLegible(evaluacion(confianza = 90, texto = "")))
    }

    @Test
    fun `sin Azure el telefono solo transcribe y el resumen lo dice`() {
        val transcrita = Pronunciacion.soloTranscripcion(listOf("i like coffee" to 0.8f))
        assertTrue(transcrita.soloTranscrita)
        assertEquals("i like coffee", transcrita.transcripcion)
        assertEquals(80, transcrita.confianza)
        assertTrue(Retroalimentacion.esLegible(transcrita))
        assertTrue(Retroalimentacion.resumen(transcrita).contains("sin el servicio de voz"))
        // Si no oyó nada, no es legible y el chat pide repetir.
        assertFalse(Retroalimentacion.esLegible(Pronunciacion.soloTranscripcion(emptyList())))
    }

    @Test
    fun `las frases dependen del nivel`() {
        assertTrue(Retroalimentacion.frasesDe("A2").isNotEmpty())
        assertTrue(Retroalimentacion.frasesDe("A1") != Retroalimentacion.frasesDe("A2"))
        assertEquals(Retroalimentacion.frasesDe("A1"), Retroalimentacion.frasesDe("desconocido"))
    }

    @Test
    fun `los sonidos a practicar juntan los repetidos y van del peor al mejor`() {
        val e = EvaluacionVoz(
            60, 90,
            listOf(
                PalabraEvaluada("the", 40, fonemas = listOf(FonemaEvaluado("ð", 16, "b"), FonemaEvaluado("ə", 90))),
                PalabraEvaluada("this", 50, fonemas = listOf(FonemaEvaluado("ð", 30), FonemaEvaluado("ɪ", 55), FonemaEvaluado("s", 95)))
            ),
            "the this", FuenteEvaluacion.AZURE
        )
        val sonidos = Retroalimentacion.sonidosAPracticar(e)
        assertEquals(listOf("ð", "ɪ"), sonidos.map { it.fonema })
        assertEquals(listOf("the", "this"), sonidos[0].palabras)
        assertEquals("b", sonidos[0].oido)
    }

    @Test
    fun `consejos de frase por completitud, palabras de mas, fluidez y entonacion`() {
        val e = EvaluacionVoz(80, 90, listOf(PalabraEvaluada("hi", 80)), "hi", FuenteEvaluacion.AZURE,
            fluidez = 50, completitud = 60, prosodia = 90, palabrasDeMas = listOf("um"), monotono = true)
        assertEquals(4, Retroalimentacion.consejosFrase(e).size)
    }

    @Test
    fun `con voz baja se le pide acercarse al microfono`() {
        val e = EvaluacionVoz(40, 90, emptyList(), "hi", FuenteEvaluacion.AZURE, vozBaja = true)
        assertTrue(Retroalimentacion.avisoAudio(e)!!.contains("bajito"))
    }

    @Test
    fun `cada sonido del ingles tiene su consejo en espanol`() {
        val ipaAzure = listOf("ɑ", "æ", "ʌ", "ɔ", "aʊ", "ə", "aɪ", "ɛ", "ɝ", "eɪ", "ɪ", "i", "oʊ", "ɔɪ", "ʊ", "u",
            "b", "tʃ", "d", "ð", "f", "g", "h", "dʒ", "k", "l", "m", "n", "ŋ", "p", "ɹ", "s", "ʃ", "t", "θ", "v", "w", "j", "z", "ʒ")
        assertTrue(ipaAzure.filter { GuiaSonidos.consejo(it) == null }.toString(), ipaAzure.all { GuiaSonidos.consejo(it) != null })
    }

    @Test
    fun `la palabra dicha en lugar de otra se explica`() {
        val p = PalabraEvaluada("I", 0, "Omission", dichaEnSuLugar = "the")
        assertEquals("Dijiste «the» en lugar de «I».", Retroalimentacion.descripcion(p))
    }
}
