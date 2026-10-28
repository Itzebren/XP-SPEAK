package com.xpspeak.app.feature.lessons.domain

import com.xpspeak.app.feature.lessons.data.ConceptoDebilDto
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.ItemDto
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto
import com.xpspeak.app.feature.lessons.data.SesionRepasoDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** RN-07: el repaso de conceptos con alta tasa de error va antes de un tema nuevo. */
class RepasoPrimeroTest {

    private fun concepto(id: String, fallos: Int, tasaError: Double) =
        ConceptoDebilDto(conceptoId = id, en = null, es = null, titulo = null, fallos = fallos, tasaError = tasaError)

    private fun sesion(vararg conceptos: ConceptoDebilDto) =
        SesionRepasoDto(totalVencidos = conceptos.size, proximaRevision = null, items = conceptos.map { ItemDto(id = it.conceptoId, concepto = it) })

    private fun leccion(estado: EstadoLeccion) =
        LeccionResumenDto(id = "a1-x", orden = 2, titulo = "X", estado = estado, intentos = 0, xpRecompensa = 50, mejorPuntaje = null)

    @Test
    fun `cuenta solo los conceptos vencidos ya fallados con tasa de error alta`() {
        val vencidos = sesion(
            concepto("voc.a", fallos = 2, tasaError = 0.5),
            concepto("voc.b", fallos = 1, tasaError = 0.3),
            // Vencido por el calendario de SM-2, pero se ha acertado: no frena el avance.
            concepto("voc.c", fallos = 0, tasaError = 0.0),
            concepto("voc.d", fallos = 1, tasaError = 0.2)
        )
        assertEquals(2, RepasoPrimero.prioritarios(vencidos))
        assertEquals(0, RepasoPrimero.prioritarios(SesionRepasoDto(0, null, emptyList())))
    }

    @Test
    fun `un concepto con dos ejercicios cuenta una vez`() {
        val repetido = concepto("gram.be", fallos = 3, tasaError = 0.6)
        assertEquals(1, RepasoPrimero.prioritarios(sesion(repetido, repetido)))
    }

    @Test
    fun `con repaso prioritario solo se retienen las lecciones nunca empezadas`() {
        assertTrue(RepasoPrimero.retiene(leccion(EstadoLeccion.DISPONIBLE), prioritarios = 1))
        for (estado in listOf(EstadoLeccion.EN_PROGRESO, EstadoLeccion.REPROBADA, EstadoLeccion.COMPLETADA, EstadoLeccion.BLOQUEADA)) {
            assertFalse(estado.name, RepasoPrimero.retiene(leccion(estado), prioritarios = 1))
        }
    }

    @Test
    fun `sin repaso prioritario no se retiene nada`() {
        assertFalse(RepasoPrimero.retiene(leccion(EstadoLeccion.DISPONIBLE), prioritarios = 0))
    }
}
