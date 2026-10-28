package com.xpspeak.app.feature.lessons.domain

import com.xpspeak.app.feature.lessons.data.CatalogoDto
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Al completar todas las lecciones de A1, el usuario pasa a A2. */
class AscensoNivelTest {

    private fun catalogo(nivel: String, vararg estados: EstadoLeccion?) = CatalogoDto(
        nivel = nivel,
        lecciones = estados.mapIndexed { i, estado ->
            LeccionResumenDto(id = "$nivel-$i", orden = i + 1, titulo = "L$i", estado = estado, intentos = 1, xpRecompensa = 50, mejorPuntaje = 1.0)
        }
    )

    private val a1Completo = catalogo("A1", EstadoLeccion.COMPLETADA, EstadoLeccion.COMPLETADA, EstadoLeccion.COMPLETADA)

    @Test
    fun `con todo A1 completado pasa a A2`() {
        assertEquals("A2", AscensoNivel.siguiente(a1Completo, sinConexion = false, yaAscendio = false))
    }

    @Test
    fun `con una sola leccion sin completar no asciende`() {
        for (pendiente in listOf(EstadoLeccion.DISPONIBLE, EstadoLeccion.EN_PROGRESO, EstadoLeccion.REPROBADA, EstadoLeccion.BLOQUEADA, null)) {
            val casi = catalogo("A1", EstadoLeccion.COMPLETADA, EstadoLeccion.COMPLETADA, pendiente)
            assertNull("$pendiente", AscensoNivel.siguiente(casi, sinConexion = false, yaAscendio = false))
        }
    }

    @Test
    fun `sin conexion o si ya ascendio antes no vuelve a subir`() {
        assertNull(AscensoNivel.siguiente(a1Completo, sinConexion = true, yaAscendio = false))
        assertNull(AscensoNivel.siguiente(a1Completo, sinConexion = false, yaAscendio = true))
    }

    @Test
    fun `A2 es el ultimo nivel y un catalogo vacio no cuenta como completo`() {
        assertNull(AscensoNivel.siguiente(catalogo("A2", EstadoLeccion.COMPLETADA), sinConexion = false, yaAscendio = false))
        assertNull(AscensoNivel.siguiente(catalogo("A1"), sinConexion = false, yaAscendio = false))
    }
}
