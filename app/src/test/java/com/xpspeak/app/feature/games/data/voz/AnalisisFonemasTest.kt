package com.xpspeak.app.feature.games.data.voz

import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.Pronunciacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Calificación sonido por sonido con el JSON detallado de Azure. */
class AnalisisFonemasTest {

    private fun fonema(esperado: String, precision: Int, vararg candidatos: Pair<String, Int>) =
        """{"Phoneme":"$esperado","PronunciationAssessment":{"AccuracyScore":$precision,
           "NBestPhonemes":[${candidatos.joinToString(",") { """{"Phoneme":"${it.first}","Score":${it.second}}""" }}]}}"""

    private fun palabra(texto: String, precision: Int, error: String = "None", vararg fonemas: String) =
        """{"Word":"$texto","PronunciationAssessment":{"AccuracyScore":$precision,"ErrorType":"$error"},
           "Phonemes":[${fonemas.joinToString(",")}]}"""

    private fun json(vararg palabras: String) =
        """{"NBest":[{"Confidence":0.9,"PronunciationAssessment":{"AccuracyScore":88,"FluencyScore":91,"CompletenessScore":100,"PronScore":90},
           "Words":[${palabras.joinToString(",")}]}]}"""

    private fun evaluacion(json: String): EvaluacionVoz {
        val palabras = AnalisisFonemas.palabras(json)!!
        return EvaluacionVoz(AnalisisFonemas.puntaje(palabras), 90, palabras, "", FuenteEvaluacion.AZURE)
    }

    @Test
    fun `party con la r en espanol queda en rojo aunque Azure le de 95 a la palabra`() {
        val resultado = evaluacion(json(palabra("party", 95, "None",
            fonema("p", 100, "p" to 100),
            fonema("ɑ", 90, "ɑ" to 90, "a" to 70),
            fonema("ɹ", 72, "d" to 88, "ɹ" to 72),
            fonema("t", 85, "t" to 85),
            fonema("i", 98, "i" to 98)
        )))
        val party = resultado.palabras.single()
        assertEquals(AnalisisFonemas.TOPE_PALABRA_CON_ERROR, party.puntaje)
        assertEquals("Mispronunciation", party.error)
        assertEquals(listOf("/ɹ/ sonó como /d/"), party.sonidos)
        assertFalse(Pronunciacion.esAcierto(resultado))
    }

    @Test
    fun `bien pronunciada conserva su puntaje y acierta`() {
        val resultado = evaluacion(json(palabra("party", 95, "None",
            fonema("p", 100, "p" to 100),
            fonema("ɑ", 92, "ɑ" to 92, "ʌ" to 85),
            fonema("ɹ", 90, "ɹ" to 90),
            fonema("t", 88, "t" to 88),
            fonema("i", 97, "i" to 97)
        )))
        assertEquals(95, resultado.palabras.single().puntaje)
        assertTrue(resultado.palabras.single().sonidos.isEmpty())
        assertTrue(Pronunciacion.esAcierto(resultado))
    }

    @Test
    fun `un candidato apenas mejor que el esperado no cuenta como sustitucion`() {
        val resultado = evaluacion(json(palabra("hi", 90, "None",
            fonema("h", 90, "h" to 90),
            fonema("aɪ", 84, "a" to 88, "aɪ" to 84)
        )))
        assertTrue(resultado.palabras.single().sonidos.isEmpty())
    }

    @Test
    fun `un sonido con precision baja se marca aunque no haya otro candidato`() {
        val resultado = evaluacion(json(palabra("three", 80, "None",
            fonema("θ", 40),
            fonema("ɹ", 90),
            fonema("i", 95)
        )))
        assertEquals(listOf("/θ/ poco claro"), resultado.palabras.single().sonidos)
    }

    @Test
    fun `las palabras de mas no cuentan y las omitidas se conservan`() {
        val resultado = evaluacion(json(
            palabra("good", 95, "None", fonema("ɡ", 95, "ɡ" to 95)),
            palabra("very", 90, "Insertion"),
            palabra("morning", 0, "Omission")
        ))
        assertEquals(listOf("good", "morning"), resultado.palabras.map { it.palabra })
        assertEquals("Omission", resultado.palabras[1].error)
        assertEquals(48, resultado.puntaje)
    }

    @Test
    fun `sin fonemas en el json se usa la calificacion del sdk`() {
        assertNull(AnalisisFonemas.palabras("""{"NBest":[{"Words":[]}]}"""))
        assertNull(AnalisisFonemas.palabras("no es json"))
    }

    @Test
    fun `la evaluacion completa trae confianza, fluidez, completitud y los fonemas de cada palabra`() {
        val evaluacion = AnalisisFonemas.evaluar(json(palabra("three", 80, "None",
            fonema("θ", 40),
            fonema("ɹ", 90),
            fonema("i", 95)
        )), transcripcion = "three")!!
        assertEquals(90, evaluacion.confianza)
        assertEquals(91, evaluacion.fluidez)
        assertEquals(100, evaluacion.completitud)
        assertEquals(90, evaluacion.puntajeGeneral)
        assertEquals("three", evaluacion.transcripcion)
        assertEquals(FuenteEvaluacion.AZURE, evaluacion.fuente)
        assertEquals(listOf("θ" to 40, "ɹ" to 90, "i" to 95), evaluacion.palabras.single().fonemas.map { it.fonema to it.puntaje })
        // La palabra queda en rojo por el sonido poco claro, y el puntaje de la frase lo refleja.
        assertEquals(AnalisisFonemas.TOPE_PALABRA_CON_ERROR, evaluacion.puntaje)
        assertNull(AnalisisFonemas.evaluar("""{"NBest":[{"Words":[]}]}""", ""))
    }

    /** Respuesta real de Azure Speech (recurso de producción, REST, 29-oct-2026) a "Good morning! My name is Laura." */
    @Test
    fun `una respuesta real de Azure se lee completa con seis palabras, fonemas, fluidez y confianza`() {
        val json = javaClass.classLoader!!.getResource("azure-pronunciation-real.json")!!.readText()
        val evaluacion = AnalisisFonemas.evaluar(json, "Good morning. My name is Laura.")!!

        assertEquals(listOf("good", "morning", "my", "name", "is", "laura"), evaluacion.palabras.map { it.palabra })
        assertEquals(98, evaluacion.puntaje)
        assertEquals(99, evaluacion.confianza)
        assertEquals(97, evaluacion.fluidez)
        assertEquals(100, evaluacion.completitud)
        assertEquals(98, evaluacion.puntajeGeneral)
        assertTrue(evaluacion.palabras.all { it.sonidos.isEmpty() && it.error == null })
        assertEquals(listOf("g" to 100, "ʊ" to 100, "d" to 83), evaluacion.palabras[0].fonemas.map { it.fonema to it.puntaje })
        assertTrue(Pronunciacion.esAcierto(evaluacion))
    }
}
