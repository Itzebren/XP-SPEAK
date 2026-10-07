package com.xpspeak.app.feature.games.domain

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

/** Generación de rondas con el banco real del APK (assets/minijuegos/banco.json). */
class GeneradorRondasTest {

    private val banco: BancoMinijuegos =
        File("src/main/assets/minijuegos/banco.json").reader().use { Gson().fromJson(it, BancoMinijuegos::class.java) }

    /** Lo mínimo que puede tener un usuario: solo la primera lección de A1. */
    private val primeraLeccion = ContenidoJuegos(
        nivel = "A1",
        lecciones = banco.lecciones.filter { it.id == "a1-saludos-presentaciones" },
        misiones = banco.misiones.filter { it.leccionId == "a1-saludos-presentaciones" }
    )
    private val todoA1 = ContenidoJuegos("A1", banco.lecciones.filter { it.nivel == "A1" }, banco.misiones.filter { it.nivel == "A1" })

    @Test
    fun `el banco trae las 24 lecciones y las misiones`() {
        assertEquals(24, banco.lecciones.size)
        assertTrue(banco.misiones.size >= 6)
        banco.misiones.forEach { mision ->
            mision.pasos.forEach { paso -> assertEquals(1, paso.opciones.count { it.correcta }) }
        }
    }

    @Test
    fun `con una sola leccion ya alcanza para una partida de cada juego`() {
        val random = Random(1)
        assertTrue(GeneradorRondas.rondasOrden(primeraLeccion, random).restantes >= GeneradorRondas.RONDAS_ORDEN)
        assertTrue(GeneradorRondas.rondasEco(primeraLeccion, random).restantes >= GeneradorRondas.RONDAS_ECO)
        assertEquals(GeneradorRondas.PARES_RAFAGA, GeneradorRondas.paresRafaga(primeraLeccion, random).size)
        assertNotNull(GeneradorRondas.elegirMision(primeraLeccion, random))
    }

    @Test
    fun `orden maestro desordena las fichas y no delata la primera con la mayuscula`() {
        repeat(20) { semilla ->
            var mazo = GeneradorRondas.rondasOrden(todoA1, Random(semilla))
            while (true) {
                val (ronda, resto) = mazo.sacar(Dificultad.FACIL) ?: break
                mazo = resto
                assertTrue(ronda.solucion.size in 3..9)
                assertEquals(ronda.solucion.sorted(), ronda.fichas.sorted())
                assertNotEquals("las fichas no deben venir ya ordenadas", ronda.solucion, ronda.fichas)
                assertTrue(GeneradorRondas.ordenCorrecto(ronda.solucion, ronda.solucion))
                val primera = ronda.solucion.first().trimEnd(',', '!')
                assertFalse("'$primera' delata el inicio de la oración", primera in INICIOS_DE_ORACION)
                assertFalse(ronda.fichas.last().endsWith(".") && ronda.fichas.last() == ronda.solucion.last() + ".")
            }
        }
    }

    @Test
    fun `la dificultad elige oraciones mas largas y el mazo usa el nivel cercano si se agota`() {
        val mazo = GeneradorRondas.rondasOrden(todoA1, Random(3))
        val (facil, _) = mazo.sacar(Dificultad.FACIL)!!
        val (dificil, _) = mazo.sacar(Dificultad.DIFICIL)!!
        assertTrue(facil.solucion.size <= 4)
        assertTrue(dificil.solucion.size >= 7)

        val soloFaciles = Mazo(listOf(listOf("a"), emptyList(), emptyList()))
        assertEquals("a", soloFaciles.sacar(Dificultad.DIFICIL)?.first)
    }

    @Test
    fun `dos fichas con la misma palabra son intercambiables`() {
        assertTrue(GeneradorRondas.ordenCorrecto(listOf("is", "that", "the", "bank,", "the", "park"), listOf("is", "that", "the", "bank,", "the", "park")))
        assertFalse(GeneradorRondas.ordenCorrecto(listOf("that", "is"), listOf("is", "that")))
    }

    @Test
    fun `rafaga nunca muestra como distractor la traduccion correcta`() {
        val pares = GeneradorRondas.paresRafaga(todoA1, Random(7))
        pares.forEach { par ->
            val correctas = GeneradorRondas.sentidos(par.esCorrecta)
            assertTrue(GeneradorRondas.sentidos(par.distractorFacil).none { it in correctas })
            assertTrue("${par.en}: ${par.distractorDificil}", GeneradorRondas.sentidos(par.distractorDificil).none { it in correctas })
            assertEquals(par.esCorrecta, par.copy(muestraCorrecta = true).mostrada(Dificultad.DIFICIL))
        }
        // Aproximadamente la mitad de los pares coinciden.
        assertTrue(pares.count { it.muestraCorrecta } in 40..80)
    }

    @Test
    fun `las traducciones se comparan sin notas ni alternativas`() {
        assertEquals(setOf("hola"), GeneradorRondas.sentidos("¡Hola! (informal)"))
        assertEquals(setOf("hola"), GeneradorRondas.sentidos("¡Hola!"))
        assertEquals(setOf("chamarra", "chaqueta"), GeneradorRondas.sentidos("chamarra / chaqueta"))
    }

    @Test
    fun `los conceptos debiles salen antes con mas frecuencia`() {
        val palabras = (1..10).map { "voc.$it" }
        var primerosPrioritarios = 0
        repeat(2_000) { semilla ->
            val orden = GeneradorRondas.ponderar(palabras, setOf("voc.1"), Random(semilla)) { it }
            if (orden.first() == "voc.1") primerosPrioritarios++
        }
        // Sin ponderar sería ~10 %; con peso 2 es ~18 %.
        assertTrue("salió primero $primerosPrioritarios veces", primerosPrioritarios in 280..450)
    }

    @Test
    fun `eco vocal va de palabras sueltas a frases`() {
        val mazo = GeneradorRondas.rondasEco(todoA1, Random(5))
        assertTrue(mazo.porNivel[0].all { Texto.palabras(it.en).size <= 2 })
        assertTrue(mazo.porNivel[2].all { Texto.palabras(it.en).size in 6..8 })
        assertTrue(mazo.porNivel[0].none { '…' in it.en })
    }

    private companion object {
        // Palabras que solo llevan mayúscula por iniciar la oración.
        val INICIOS_DE_ORACION = setOf(
            "Can", "Do", "Does", "Did", "Is", "Are", "Was", "What", "Where", "When", "How", "The", "This", "That",
            "These", "Those", "My", "There", "She", "He", "We", "They", "You", "It", "Yes", "No", "Hello", "Hi", "Go"
        )
    }
}
