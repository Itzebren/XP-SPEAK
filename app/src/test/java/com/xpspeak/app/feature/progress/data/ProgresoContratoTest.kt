package com.xpspeak.app.feature.progress.data

import com.google.gson.Gson
import com.xpspeak.app.feature.games.data.ResultadoResponse
import com.xpspeak.app.feature.lessons.data.IntentoResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Verifica que la app lee el mismo JSON que produce el backend de Progreso
 * (respuestas reales de backend/api/progress y backend/api/games/result).
 */
class ProgresoContratoTest {

    private val gson = Gson()

    @Test
    fun `lee GET api progress`() {
        val json = """
            {"nivel":"A1","dominio":{"aprobadas":0,"total":12,"porcentaje":0},
             "xp":{"total":15,"nivel":1,"xp_en_nivel":15,"xp_para_siguiente":100},
             "racha":{"actual":1,"maxima":1,"ultimo_dia":"2026-10-11"},
             "habilidades":[{"id":"vocabulario","nombre":"Vocabulario","porcentaje":null,"sugerencia":"Juega Ráfaga de Palabras"},
                            {"id":"gramatica","nombre":"Gramática","porcentaje":100,"sugerencia":"Juega Orden Maestro"}],
             "insignias":[{"id":"primer-logro","nombre":"Primer Logro","descripcion":"¡Diste el primer paso en tu aprendizaje!",
                           "criterio":"Termina tu primera actividad.","icono":"estrella","obtenida":true,"fecha":1791770068735,
                           "retroactiva":false,"progreso":{"actual":1,"meta":1}},
                          {"id":"racha-7","nombre":"Racha de 7 Días","descripcion":"Una semana entera practicando sin faltar un día.",
                           "criterio":"Practica 7 días seguidos.","icono":"fuego","obtenida":false,"fecha":null,
                           "retroactiva":false,"progreso":{"actual":1,"meta":7}}],
             "sin_actividad":false}
        """
        val dto = gson.fromJson(json, ProgresoDto::class.java)

        assertEquals("A1", dto.nivel)
        assertEquals(12, dto.dominio.total)
        assertEquals(XpDto(total = 15, nivel = 1, xpEnNivel = 15, xpParaSiguiente = 100), dto.xp)
        assertEquals(RachaDto(actual = 1, maxima = 1, ultimoDia = "2026-10-11"), dto.racha)
        assertNull(dto.habilidades[0].porcentaje)
        assertEquals(100, dto.habilidades[1].porcentaje)
        assertTrue(dto.insignias[0].obtenida)
        assertEquals(1791770068735L, dto.insignias[0].fecha)
        assertFalse(dto.insignias[1].obtenida)
        assertEquals(ProgresoInsigniaDto(1, 7), dto.insignias[1].progreso)
        assertFalse(dto.sinActividad)
    }

    @Test
    fun `lee las insignias nuevas del resultado de un minijuego`() {
        val json = """
            {"partida_id":"6f1c2a9e-3b4d-4e5f-8a7b-9c0d1e2f3a4b","juego":"orden-maestro","aciertos":8,"total":8,"precision":1,
             "xp_ganado":15,"xp_reducida":false,"partidas_hoy":1,
             "insignias_nuevas":[{"id":"primer-logro","nombre":"Primer Logro","descripcion":"¡Diste el primer paso en tu aprendizaje!",
                                  "criterio":"Termina tu primera actividad.","icono":"estrella"},
                                 {"id":"estrella-minijuego","nombre":"Estrella del Minijuego","descripcion":"Una partida perfecta.",
                                  "criterio":"Logra 100 % de precisión.","icono":"juego"}],
             "nivel_xp_nuevo":null,"repetido":false}
        """
        val respuesta = gson.fromJson(json, ResultadoResponse::class.java)

        assertEquals(15, respuesta.xpGanado)
        assertEquals(listOf("primer-logro", "estrella-minijuego"), respuesta.insigniasNuevas!!.map { it.id })
        assertFalse(respuesta.insigniasNuevas!![0].retroactiva)
        assertNull(respuesta.nivelXpNuevo)
    }

    @Test
    fun `un backend viejo sin los campos de progreso no rompe el resultado de la leccion`() {
        val json = """
            {"attempt_id":"x","leccion_id":"a1-saludos","puntaje":1,"correctas":6,"total":6,"aprobada":true,"estado":"completada",
             "xp_ganado":50,"desbloqueada_siguiente":null,"feedback":[],"conceptos_debiles":[],"sugerencia":null,"repetido":false}
        """
        val respuesta = gson.fromJson(json, IntentoResponse::class.java)
        assertNull(respuesta.insigniasNuevas)
        assertNull(respuesta.nivelXpNuevo)

        val conProgreso = gson.fromJson(json.replace("\"repetido\":false", "\"repetido\":false,\"nivel_xp_nuevo\":2,\"insignias_nuevas\":[]"), IntentoResponse::class.java)
        assertEquals(2, conProgreso.nivelXpNuevo)
        assertEquals(emptyList<InsigniaDto>(), conProgreso.insigniasNuevas)
    }

    @Test
    fun `el catalogo empaquetado tiene las 9 insignias`() {
        // Las pruebas unitarias corren con el módulo app/ como directorio de trabajo.
        val archivo = File("src/main/assets/${CatalogoInsigniasLocal.RUTA}")
        val insignias = archivo.reader().use(CatalogoInsigniasLocal::leer)

        assertEquals(9, insignias.size)
        assertEquals(9, insignias.map { it.id }.toSet().size)
        assertTrue(insignias.all { it.nombre.isNotBlank() && it.descripcion.isNotBlank() && it.icono != null })
        assertTrue(insignias.none { it.obtenida })
    }
}
