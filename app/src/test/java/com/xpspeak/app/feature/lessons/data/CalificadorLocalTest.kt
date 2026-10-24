package com.xpspeak.app.feature.lessons.data

import com.google.gson.Gson
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mismos casos que test/calificador.test.js del backend: sin conexión la app
 * debe calificar igual que el servidor. Usa la evaluación real de
 * a1-saludos-presentaciones tal como la manda GET /api/lessons/:id.
 */
class CalificadorLocalTest {

    private val saludos = Gson().fromJson(
        """
        {"tipo":"evaluacion","umbral_aprobacion":0.7,"items":[
          {"id":"q1","tipo":"opcion_multiple","concepto_id":"voc.saludos.good_morning","enunciado":"?",
           "opciones":["Good night!","Good morning!","Goodbye!"],"respuesta_correcta":1},
          {"id":"q2","tipo":"completar","concepto_id":"gram.verb_to_be_present","enunciado":"My name ___ Laura.",
           "respuesta_correcta":"is","acepta":["is"],
           "feedback_error":"'My name' es singular (3ª persona), así que usamos 'is'."},
          {"id":"q3","tipo":"completar","concepto_id":"gram.verb_to_be_present","enunciado":"I ___ a new student.",
           "respuesta_correcta":"am","acepta":["am"]},
          {"id":"q4","tipo":"opcion_multiple","concepto_id":"gram.subject_pronouns","enunciado":"?",
           "opciones":["He","She","They","I"],"respuesta_correcta":1},
          {"id":"q5","tipo":"opcion_multiple","concepto_id":"voc.saludos.nice_to_meet_you","enunciado":"?",
           "opciones":["See you later!","Nice to meet you!","Good evening!"],"respuesta_correcta":1},
          {"id":"q6","tipo":"emparejar","enunciado":"Une cada expresión con su significado.","pares":[
            {"concepto_id":"voc.saludos.hello","izq":"Hello!","der":"¡Hola!"},
            {"concepto_id":"voc.saludos.goodbye","izq":"Goodbye!","der":"¡Adiós!"},
            {"concepto_id":"voc.saludos.how_are_you","izq":"How are you?","der":"¿Cómo estás?"},
            {"concepto_id":"voc.saludos.see_you_later","izq":"See you later!","der":"¡Nos vemos luego!"}]}
        ]}
        """,
        SeccionDto::class.java
    )

    private val paresCorrectos = mapOf(
        "Hello!" to "¡Hola!",
        "Goodbye!" to "¡Adiós!",
        "How are you?" to "¿Cómo estás?",
        "See you later!" to "¡Nos vemos luego!"
    )

    private fun correctas(): Map<String, RespuestaUsuario> = mapOf(
        "q1" to RespuestaUsuario.Opcion(1),
        "q2" to RespuestaUsuario.Texto("is"),
        "q3" to RespuestaUsuario.Texto("am"),
        "q4" to RespuestaUsuario.Opcion(1),
        "q5" to RespuestaUsuario.Opcion(1),
        "q6" to RespuestaUsuario.Pares(paresCorrectos)
    )

    private fun feedback(r: IntentoResponse, id: String) = r.feedback.first { it.id == id }

    @Test
    fun `todas correctas - puntaje 1 y aprobada`() {
        val r = CalificadorLocal.calificar(saludos, correctas())
        assertEquals(1.0, r.puntaje, 0.0)
        assertTrue(r.aprobada)
        assertEquals(6, r.correctas)
        assertEquals(emptyList<String>(), r.conceptosDebiles)
        assertTrue(r.feedback.all { it.correcta && it.mensaje == null })
    }

    @Test
    fun `umbral 70 por ciento - 5 de 6 aprueba y 4 de 6 no (RN-06)`() {
        val cinco = correctas() + ("q1" to RespuestaUsuario.Opcion(0))
        assertTrue(CalificadorLocal.calificar(saludos, cinco).aprobada)

        val cuatro = cinco + ("q2" to RespuestaUsuario.Texto("are"))
        val r = CalificadorLocal.calificar(saludos, cuatro)
        assertEquals(0.6667, r.puntaje, 0.0)
        assertFalse(r.aprobada)
    }

    @Test
    fun `umbral exacto - 7 de 10 aprueba pese al punto flotante`() {
        val items = (1..10).map {
            ItemDto(
                en = null, es = null, nota = null, id = "q$it", tipo = TipoEjercicio.OPCION_MULTIPLE,
                enunciado = "?", opciones = listOf("a", "b"), pares = null,
                conceptoId = "voc.x.y", respuestaCorrecta = Gson().toJsonTree(0)
            )
        }
        val evaluacion = SeccionDto(TipoSeccion.EVALUACION, null, null, null, null, null, items, 0.7)
        val respuestas = items.mapIndexed { i, item -> item.id!! to RespuestaUsuario.Opcion(if (i < 7) 0 else 1) }.toMap()

        val r = CalificadorLocal.calificar(evaluacion, respuestas)
        assertEquals(0.7, r.puntaje, 0.0)
        assertTrue(r.aprobada)
    }

    @Test
    fun `respuesta incorrecta trae mensaje y respuesta correcta (CU-06 A1)`() {
        val q2 = feedback(CalificadorLocal.calificar(saludos, correctas() + ("q2" to RespuestaUsuario.Texto("are"))), "q2")
        assertFalse(q2.correcta)
        assertEquals("is", q2.respuestaCorrecta)
        assertTrue(q2.mensaje!!.contains("is"))
    }

    @Test
    fun `items sin responder cuentan como incorrectos pero no marcan conceptos debiles`() {
        val r = CalificadorLocal.calificar(saludos, mapOf("q1" to RespuestaUsuario.Opcion(0)))
        assertEquals(0, r.correctas)
        assertEquals(6, r.total)
        assertFalse(r.aprobada)
        assertEquals(listOf("voc.saludos.good_morning"), r.conceptosDebiles)
    }

    @Test
    fun `completar normaliza mayusculas, espacios, puntuacion y apostrofos`() {
        assertEquals("is", CalificadorLocal.normalizarTexto("  IS. "))
        assertEquals("o'clock", CalificadorLocal.normalizarTexto("o’clock"))

        val r = CalificadorLocal.calificar(saludos, correctas() + ("q2" to RespuestaUsuario.Texto("  Is! ")))
        assertTrue(feedback(r, "q2").correcta)
    }

    @Test
    fun `emparejar exige todos los pares y marca como debil solo el concepto del par fallado`() {
        val cruzados = paresCorrectos + mapOf("Goodbye!" to "¿Cómo estás?", "How are you?" to "¡Adiós!")
        val r = CalificadorLocal.calificar(saludos, correctas() + ("q6" to RespuestaUsuario.Pares(cruzados)))

        val q6 = feedback(r, "q6")
        assertFalse(q6.correcta)
        assertEquals(4, (q6.respuestaCorrecta as List<*>).size)
        assertEquals(setOf("voc.saludos.goodbye", "voc.saludos.how_are_you"), r.conceptosDebiles.toSet())
    }

    @Test
    fun `emparejar con pares de mas no es correcto`() {
        val deMas = paresCorrectos + ("Hi!" to "¡Hola! (informal)")
        val r = CalificadorLocal.calificar(saludos, correctas() + ("q6" to RespuestaUsuario.Pares(deMas)))
        assertFalse(feedback(r, "q6").correcta)
    }
}
