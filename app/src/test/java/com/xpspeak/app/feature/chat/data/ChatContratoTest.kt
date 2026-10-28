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
}
