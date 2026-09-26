package com.xpspeak.app.feature.lessons.data

import com.google.gson.Gson
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Verifica que la app lee y escribe el mismo JSON que el backend de Lecciones. */
class LeccionesContratoTest {

    private val gson = Gson()

    @Test
    fun `lee el estado de cada leccion del catalogo como enum`() {
        val json = """
            {"nivel":"A1","lecciones":[
              {"id":"a1-saludos","orden":1,"titulo":"Saludos","estado":"completada","intentos":2,"xp_recompensa":50,"mejor_puntaje":1},
              {"id":"a1-personal","orden":2,"titulo":"Personal","estado":"en_progreso","intentos":0,"xp_recompensa":60,"mejor_puntaje":null}
            ]}
        """
        val catalogo = gson.fromJson(json, CatalogoDto::class.java)

        assertEquals(EstadoLeccion.COMPLETADA, catalogo.lecciones[0].estado)
        assertEquals(EstadoLeccion.EN_PROGRESO, catalogo.lecciones[1].estado)
        assertEquals(50, catalogo.lecciones[0].xpRecompensa)
        assertNull(catalogo.lecciones[1].mejorPuntaje)
    }

    @Test
    fun `un valor desconocido del backend queda en null y no rompe la app`() {
        val json = """{"tipo":"dictado","enunciado":"Escucha y escribe"}"""
        assertNull(gson.fromJson(json, ItemDto::class.java).tipo)
    }

    @Test
    fun `lee los tipos de seccion y de ejercicio`() {
        val json = """{"tipo":"evaluacion","umbral_aprobacion":0.7,"items":[{"id":"q1","tipo":"emparejar","pares":[{"izq":"Hi!","der":"¡Hola!"}]}]}"""
        val seccion = gson.fromJson(json, SeccionDto::class.java)

        assertEquals(TipoSeccion.EVALUACION, seccion.tipo)
        assertEquals(TipoEjercicio.EMPAREJAR, seccion.items!![0].tipo)
        assertEquals("¡Hola!", seccion.items!![0].pares!![0].der)
    }

    @Test
    fun `cada tipo de respuesta se envia con el formato que espera el servidor`() {
        val request = IntentoRequest(
            attemptId = "b1f2c3d4-0000-4000-8000-000000000000",
            respuestas = listOf(
                RespuestaUsuario.Opcion(1).aDto("q1"),
                RespuestaUsuario.Texto("is").aDto("q2"),
                RespuestaUsuario.Pares(mapOf("Hello!" to "¡Hola!")).aDto("q3")
            )
        )

        assertEquals(
            """{"attempt_id":"b1f2c3d4-0000-4000-8000-000000000000","respuestas":[""" +
                """{"id":"q1","valor":1},{"id":"q2","valor":"is"},{"id":"q3","valor":[["Hello!","¡Hola!"]]}]}""",
            gson.toJson(request)
        )
    }
}
