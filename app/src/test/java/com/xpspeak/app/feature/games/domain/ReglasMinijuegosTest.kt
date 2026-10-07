package com.xpspeak.app.feature.games.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/** Reglas puras del motor común (docs/minijuegos-diseno.md §4). */
class ReglasMinijuegosTest {

    // Mismos casos que backend/test/minijuegos.test.js: la XP local debe coincidir con la del servidor.
    @Test
    fun `la XP coincide con la formula del servidor`() {
        assertEquals(CalculadorXp.Xp(15, false), CalculadorXp.calcular(Minijuego.ORDEN_MAESTRO, 8, 8, 60_000))
        assertEquals(13, CalculadorXp.calcular(Minijuego.ORDEN_MAESTRO, 8, 8, 120_000).xp)
        assertEquals(10, CalculadorXp.calcular(Minijuego.ORDEN_MAESTRO, 8, 8, 600_000).xp)
        assertEquals(5, CalculadorXp.calcular(Minijuego.ORDEN_MAESTRO, 4, 8, 10_000).xp)
        assertEquals(20, CalculadorXp.calcular(Minijuego.ECO_VOCAL, 8, 8, 80_000).xp)
        assertEquals(12, CalculadorXp.calcular(Minijuego.RAFAGA_PALABRAS, 20, 25, 90_000).xp)
        assertEquals(15, CalculadorXp.calcular(Minijuego.RAFAGA_PALABRAS, 60, 60, 180_000).xp)
        assertEquals(
            CalculadorXp.Xp(7, true),
            CalculadorXp.calcular(Minijuego.ORDEN_MAESTRO, 8, 8, 60_000, partidasPrevias = CalculadorXp.PARTIDAS_CON_XP_COMPLETA)
        )
    }

    @Test
    fun `una partida sin rondas da 0 XP`() {
        assertEquals(0, CalculadorXp.calcular(Minijuego.RAFAGA_PALABRAS, 0, 0, 60_000).xp)
    }

    @Test
    fun `la dificultad sube con 3 aciertos seguidos y baja con 2 errores`() {
        var dificultad = Dificultad()
        repeat(2) { dificultad = dificultad.registrar(true) }
        assertEquals(Dificultad.FACIL, dificultad.nivel)
        dificultad = dificultad.registrar(true)
        assertEquals(Dificultad.MEDIO, dificultad.nivel)

        dificultad = dificultad.registrar(false)
        assertEquals(Dificultad.MEDIO, dificultad.nivel)
        dificultad = dificultad.registrar(false)
        assertEquals(Dificultad.FACIL, dificultad.nivel)

        // No baja de fácil ni sube de difícil.
        repeat(5) { dificultad = dificultad.registrar(false) }
        assertEquals(Dificultad.FACIL, dificultad.nivel)
        repeat(20) { dificultad = dificultad.registrar(true) }
        assertEquals(Dificultad.DIFICIL, dificultad.nivel)
    }

    @Test
    fun `la racha sube una vez por dia y se reinicia si se salta un dia`() {
        val hoy = LocalDate.of(2026, 10, 6)
        assertEquals(1, Racha.siguiente(0, null, hoy))
        assertEquals(4, Racha.siguiente(3, "2026-10-05", hoy))
        assertEquals(3, Racha.siguiente(3, "2026-10-06", hoy))
        assertEquals(1, Racha.siguiente(9, "2026-10-03", hoy))
        assertEquals(1, Racha.siguiente(0, "2026-10-06", hoy))
    }

    @Test
    fun `el marcador acumula rondas, senales por concepto y tiempo`() {
        val marcador = Marcador()
            .conRonda(true).conSenal("voc.a", true)
            .conRonda(false).conSenal("voc.a", false).conSenal("voc.a", false)
            .conSenal(null, true)
            .conTiempo(1_500).conTiempo(-10)
        assertEquals(1, marcador.aciertos)
        assertEquals(2, marcador.total)
        assertEquals(ConteoConcepto(aciertos = 1, errores = 2), marcador.conceptos["voc.a"])
        assertEquals(1, marcador.conceptos.size)
        assertEquals(1_500, marcador.tiempoJugadoMs)
    }

    @Test
    fun `el texto se compara sin mayusculas, apostrofos ni puntuacion`() {
        assertEquals("i'm fine, thank you", Texto.normalizar("  I’m fine,  thank you! "))
        assertEquals(listOf("i'm", "fine", "thank", "you"), Texto.palabras("I'm fine, thank you."))
        assertEquals(1.0, Texto.similitud("Can I pay by card?", "can i pay by card"), 0.0)
        assertEquals(0.8, Texto.similitud("Can I pay by card?", "can I pay with card"), 0.0)
        assertEquals(listOf(true, false, true), Texto.coincidencias(listOf("my", "name", "is"), listOf("my", "is")))
    }

    @Test
    fun `sin conexion la pronunciacion se evalua por palabras con la mejor alternativa`() {
        val evaluacion = Pronunciacion.evaluarTranscripcion(
            "Nice to meet you",
            listOf("nice to eat you" to 0.4f, "nice to meet you" to 0.9f)
        )
        assertEquals(100, evaluacion.puntaje)
        assertEquals(90, evaluacion.confianza)
        assertTrue(Pronunciacion.esAcierto(evaluacion))
        assertEquals(FuenteEvaluacion.DISPOSITIVO, evaluacion.fuente)

        val parcial = Pronunciacion.evaluarTranscripcion("Nice to meet you", listOf("nice to eat you" to -1f))
        assertEquals(75, parcial.puntaje)
        assertEquals(100, parcial.confianza)
        assertEquals("Mispronunciation", parcial.palabras[2].error)
        assertTrue(Pronunciacion.esAcierto(parcial))
    }

    @Test
    fun `si no se oyo nada el audio no es legible y no cuenta como intento`() {
        val evaluacion = Pronunciacion.evaluarTranscripcion("Hello, how are you?", emptyList())
        assertFalse(Pronunciacion.esLegible(evaluacion))
        assertFalse(Pronunciacion.esAcierto(evaluacion))
        assertEquals(4, evaluacion.palabras.size)
    }
}
