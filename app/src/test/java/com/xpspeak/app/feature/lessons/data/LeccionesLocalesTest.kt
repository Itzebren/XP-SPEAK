package com.xpspeak.app.feature.lessons.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Las lecciones reales del APK (assets/lecciones/contenido.json, RNF-08 / RN-11). */
class LeccionesLocalesTest {

    private val contenido = File("src/main/assets/lecciones/contenido.json").reader()
        .use { Gson().fromJson(it, ContenidoEmpaquetado::class.java) }

    @Test
    fun `el APK trae las lecciones de A1 y A2 con su evaluacion`() {
        assertEquals(setOf("A1", "A2"), contenido.niveles.keys)
        assertTrue(contenido.niveles.values.all { it.isNotEmpty() })
        assertEquals(contenido.niveles.values.flatten().map { it.id }, contenido.lecciones.map { it.id })
        contenido.lecciones.forEach { leccion ->
            val evaluacion = leccion.secciones.single { it.tipo == TipoSeccion.EVALUACION }
            assertTrue("${leccion.id} sin reactivos", !evaluacion.items.isNullOrEmpty())
            // La clave de respuestas debe venir para calificar sin conexión (CalificadorLocal).
            assertTrue(evaluacion.items!!.all { it.tipo != null && (it.respuestaCorrecta != null || it.pares != null) })
        }
    }

    @Test
    fun `sin progreso solo la primera leccion de cada nivel esta disponible`() {
        for (nivel in listOf("A1", "A2")) {
            val catalogo = LeccionesLocales.catalogoInicial(contenido, nivel)
            val disponibles = catalogo.lecciones.filter { it.estado == EstadoLeccion.DISPONIBLE }

            assertEquals(1, disponibles.size)
            assertEquals(1, disponibles.single().orden)
            assertTrue(catalogo.lecciones.all { it.intentos == 0 && it.mejorPuntaje == null })
        }
    }

    @Test
    fun `un nivel desconocido da un catalogo vacio`() {
        assertTrue(LeccionesLocales.catalogoInicial(contenido, "B1").lecciones.isEmpty())
    }
}
