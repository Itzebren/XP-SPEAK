package com.xpspeak.app.feature.chat.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica que la app lee los mismos eventos que manda backend/api/chat.js. */
class ChatContratoTest {

    private val gson = Gson()

    @Test
    fun `lee la correccion del flujo (RF-09)`() {
        val linea = """{"correccion":{"corregido":"I went to the park yesterday.","errores":[""" +
            """{"original":"I go","correccion":"I went","explicacion":"Con \"yesterday\" se usa el pasado."}]}}"""
        val evento = gson.fromJson(linea, EventoChat::class.java)

        assertNull(evento.delta)
        assertEquals("I went to the park yesterday.", evento.correccion!!.corregido)
        assertEquals("I went", evento.correccion!!.errores.single().correccion)
        assertEquals("Con \"yesterday\" se usa el pasado.", evento.correccion!!.errores.single().explicacion)
    }

    @Test
    fun `lee el tipo de cada error, y sin tipo (backend anterior) queda en null`() {
        val linea = """{"correccion":{"corregido":"He goes.","correcta":false,"errores":[""" +
            """{"original":"He go","correccion":"He goes","tipo":"Conjugación","explicacion":"Con he se agrega -s."},""" +
            """{"original":"a apple","correccion":"an apple","explicacion":"Antes de vocal va an."}]}}"""
        val correccion = gson.fromJson(linea, EventoChat::class.java).correccion!!

        assertEquals(listOf("Conjugación", null), correccion.errores.map { it.tipo })
        assertTrue(correccion.seMuestra)
    }

    @Test
    fun `un mensaje revisado sin errores se confirma, y sin revision no se muestra nada`() {
        val bien = gson.fromJson("""{"correccion":{"corregido":null,"errores":[],"correcta":true}}""", EventoChat::class.java).correccion!!
        assertTrue(bien.correcta)
        assertTrue(bien.errores.isEmpty())
        assertTrue(bien.seMuestra)

        // Un backend anterior no manda "correcta": sin errores no hay nada que mostrar.
        val anterior = gson.fromJson("""{"corregido":"I like pizza."}""", CorreccionDto::class.java)
        assertEquals(false, anterior.seMuestra)
    }

    @Test
    fun `la respuesta completa llega como evento propio antes del fin`() {
        assertEquals(true, gson.fromJson("""{"completa":true}""", EventoChat::class.java).completa)
        assertNull(gson.fromJson("""{"fin":true}""", EventoChat::class.java).completa)
    }

    @Test
    fun `el mensaje dicho con voz se avisa al servidor`() {
        assertTrue(gson.toJson(ChatRequest("I go there", "A1", emptyList(), voz = true)).contains("\"voz\":true"))
        assertTrue(gson.toJson(ChatRequest("I go there")).contains("\"voz\":false"))
    }

    @Test
    fun `un pedazo de texto no trae correccion`() {
        val evento = gson.fromJson("""{"delta":"Hi!"}""", EventoChat::class.java)
        assertEquals("Hi!", evento.delta)
        assertNull(evento.correccion)
    }

    @Test
    fun `sin errores la lista queda vacia y no en null`() {
        val correccion = gson.fromJson("""{"corregido":"I like pizza."}""", CorreccionDto::class.java)
        assertTrue(correccion.errores.isEmpty())
    }

    @Test
    fun `la respuesta JSON sin streaming tambien trae la correccion`() {
        val json = """{"respuesta":"Nice!","correccion":{"corregido":"He goes.","errores":[{"original":"He go","correccion":"He goes","explicacion":"Con he se agrega -s."}]}}"""
        val respuesta = gson.fromJson(json, ChatResponse::class.java)
        assertEquals("Nice!", respuesta.respuesta)
        assertEquals("He go", respuesta.correccion!!.errores.single().original)
    }

    @Test
    fun `un error del servidor se muestra con su mensaje, y sin cuerpo con uno segun el codigo`() {
        assertEquals("El chat no está disponible por ahora", ChatRepository.mensajeDeError(503, """{"error":"El chat no está disponible por ahora","codigo":"CHAT_NO_CONFIGURADO"}"""))
        assertEquals("El chat no está disponible por ahora. Intenta más tarde.", ChatRepository.mensajeDeError(503, "Service Unavailable"))
        assertEquals("Ocurrió un error (500). Intenta de nuevo.", ChatRepository.mensajeDeError(500, null))
    }

    @Test
    fun `sin conexion se explica en espanol y no con el mensaje tecnico de red`() {
        val red = java.net.ConnectException("Failed to connect to localhost/127.0.0.1:3000")
        assertEquals(ChatRepository.SIN_CONEXION, ChatRepository.mensajeDeFalla(red))
        assertEquals(ChatRepository.SIN_CONEXION, ChatRepository.mensajeDeFalla(RuntimeException("envuelto", java.net.UnknownHostException("xp-speak"))))
        assertEquals("El chat no está disponible por ahora", ChatRepository.mensajeDeFalla(Exception("El chat no está disponible por ahora")))
    }
}
