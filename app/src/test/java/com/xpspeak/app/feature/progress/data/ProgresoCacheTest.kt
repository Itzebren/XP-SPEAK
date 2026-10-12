package com.xpspeak.app.feature.progress.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

/** La caché de Room guarda ProgresoDto como JSON: debe volver idéntico (ProgresoCacheEntity.json). */
class ProgresoCacheTest {

    @Test
    fun `ida y vuelta con Gson conserva todo el progreso`() {
        val original = ProgresoDto(
            nivel = "A1",
            dominio = DominioDto(3, 12, 25),
            xp = XpDto(720, 4, 120, 400),
            racha = RachaDto(5, 9, "2026-10-11"),
            habilidades = listOf(
                HabilidadDto("vocabulario", "Vocabulario", 82, "Juega Ráfaga de Palabras"),
                HabilidadDto("pronunciacion", "Pronunciación", null, "Juega Eco Vocal")
            ),
            insignias = listOf(
                InsigniaDto("racha-7", "Racha de 7 Días", "…", "Practica 7 días seguidos.", "fuego", false, null, false, ProgresoInsigniaDto(5, 7)),
                InsigniaDto("primer-logro", "Primer Logro", "…", "…", "estrella", true, 1791770068735, true, ProgresoInsigniaDto(1, 1))
            ),
            sinActividad = false
        )
        val gson = Gson()
        assertEquals(original, gson.fromJson(gson.toJson(original), ProgresoDto::class.java))
    }
}
