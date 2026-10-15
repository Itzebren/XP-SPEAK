package com.xpspeak.app.feature.lessons.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CatalogoLocalTest {

    private val entradas = listOf(
        EntradaPaqueteDto("a1-uno", 1, 1, null, "Uno", 50),
        EntradaPaqueteDto("a1-dos", 1, 2, "a1-uno", "Dos", 60),
        EntradaPaqueteDto("a1-tres", 1, 3, "a1-dos", "Tres", 60)
    )

    private fun estados(catalogo: CatalogoDto) = catalogo.lecciones.map { it.estado }

    @Test
    fun `sin haber hablado con el servidor solo la primera leccion esta disponible`() {
        val catalogo = CatalogoLocal.inicial("A1", entradas)
        assertEquals(listOf(EstadoLeccion.DISPONIBLE, EstadoLeccion.BLOQUEADA, EstadoLeccion.BLOQUEADA), estados(catalogo))
        assertEquals(listOf("Uno", "Dos", "Tres"), catalogo.lecciones.map { it.titulo })
    }

    @Test
    fun `aprobar sin conexion completa la leccion y desbloquea solo la siguiente`() {
        val catalogo = CatalogoLocal.conIntento(CatalogoLocal.inicial("A1", entradas), entradas, "a1-uno", aprobada = true, puntaje = 0.83)
        assertEquals(listOf(EstadoLeccion.COMPLETADA, EstadoLeccion.DISPONIBLE, EstadoLeccion.BLOQUEADA), estados(catalogo))
        assertEquals(1, catalogo.lecciones[0].intentos)
        assertEquals(0.83, catalogo.lecciones[0].mejorPuntaje!!, 0.0001)
    }

    @Test
    fun `reprobar no desbloquea y no le quita lo completado a una leccion ya aprobada`() {
        val inicial = CatalogoLocal.inicial("A1", entradas)
        val reprobada = CatalogoLocal.conIntento(inicial, entradas, "a1-uno", aprobada = false, puntaje = 0.5)
        assertEquals(listOf(EstadoLeccion.REPROBADA, EstadoLeccion.BLOQUEADA, EstadoLeccion.BLOQUEADA), estados(reprobada))

        val aprobada = CatalogoLocal.conIntento(reprobada, entradas, "a1-uno", aprobada = true, puntaje = 1.0)
        val repetida = CatalogoLocal.conIntento(aprobada, entradas, "a1-uno", aprobada = false, puntaje = 0.4)
        assertEquals(EstadoLeccion.COMPLETADA, repetida.lecciones[0].estado)
        assertEquals(1.0, repetida.lecciones[0].mejorPuntaje!!, 0.0001)
        assertEquals(3, repetida.lecciones[0].intentos)
    }
}
