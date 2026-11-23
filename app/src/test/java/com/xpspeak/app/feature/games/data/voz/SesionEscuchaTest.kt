package com.xpspeak.app.feature.games.data.voz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Lo que ve el usuario mientras habla: volumen, lo oído y el botón «Listo» (PanelEscucha). */
class SesionEscuchaTest {

    private val publicados = mutableListOf<EstadoEscucha>()
    private val sesion = SesionEscucha { publicados += it }

    @Test
    fun `un intento pasa por preparando, escuchando y calificando`() {
        sesion.preparando()
        sesion.escuchando()
        sesion.nivel(0.5f)
        sesion.oyendo("pants")
        sesion.calificando()
        assertEquals(
            listOf(
                EstadoEscucha.Preparando,
                EstadoEscucha.Escuchando(),
                EstadoEscucha.Escuchando(nivel = 0.5f),
                EstadoEscucha.Escuchando(nivel = 0.5f, oyendo = "pants", hablo = true),
                EstadoEscucha.Calificando
            ),
            publicados
        )
    }

    @Test
    fun `el volumen y lo oido solo cuentan con el microfono abierto`() {
        sesion.preparando()
        sesion.nivel(0.9f)
        sesion.oyendo("hello")
        assertEquals(listOf(EstadoEscucha.Preparando), publicados)
    }

    @Test
    fun `listo cierra el microfono y pasa a calificar`() {
        var cerrado = false
        sesion.escuchando()
        sesion.alTerminar = { cerrado = true }
        sesion.terminar()
        assertTrue(cerrado)
        assertTrue(sesion.fueTerminada)
        assertEquals(EstadoEscucha.Calificando, publicados.last())
        // Ya no hay micrófono abierto: el volumen que llegue tarde no revive la escucha.
        sesion.nivel(0.3f)
        assertEquals(EstadoEscucha.Calificando, publicados.last())
    }

    @Test
    fun `si listo llega antes de que el evaluador abra el microfono, se cierra al abrirlo`() {
        sesion.escuchando()
        sesion.terminar()
        var cerrado = false
        sesion.alTerminar = { cerrado = true }
        assertTrue(cerrado)
    }

    @Test
    fun `el mismo nivel repetido no se vuelve a publicar`() {
        sesion.escuchando()
        sesion.nivel(0.2f)
        sesion.nivel(0.2f)
        assertEquals(2, publicados.size)
        assertFalse(sesion.fueTerminada)
    }
}
