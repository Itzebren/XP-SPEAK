package com.xpspeak.app.feature.lessons.data

import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import org.junit.Assert.assertEquals
import org.junit.Test

class RespuestasCodecTest {

    @Test
    fun `las respuestas de los tres tipos vuelven igual al retomar la leccion`() {
        val respuestas = mapOf(
            "q1" to RespuestaUsuario.Opcion(2),
            "q2" to RespuestaUsuario.Texto("I am Ana"),
            "q3" to RespuestaUsuario.Pares(mapOf("Hello!" to "¡Hola!", "Bye!" to "¡Adiós!"))
        )
        assertEquals(respuestas, RespuestasCodec.decodificar(RespuestasCodec.codificar(respuestas)))
    }

    @Test
    fun `un texto danado o de otra version no rompe la pantalla`() {
        assertEquals(emptyMap<String, RespuestaUsuario>(), RespuestasCodec.decodificar("no es json"))
        assertEquals(
            mapOf("q1" to RespuestaUsuario.Opcion(0)),
            RespuestasCodec.decodificar("""[{"id":"q1","tipo":"opcion","indice":0},{"id":"q2","tipo":"dictado"}]""")
        )
    }
}
