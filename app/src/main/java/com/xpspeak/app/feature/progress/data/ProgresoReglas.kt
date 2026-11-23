package com.xpspeak.app.feature.progress.data

import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import java.time.LocalDate

/**
 * Reglas del módulo de Progreso que la app aplica sin el servidor
 * (docs/progreso-diseno.md "Reglas del módulo" y Fase 3).
 */
object ProgresoReglas {
    private const val XP_BASE = 50

    /** Las 4 habilidades, en el orden del servidor, para el estado sin red. */
    private val HABILIDADES = listOf(
        "vocabulario" to "Vocabulario",
        "gramatica" to "Gramática",
        "pronunciacion" to "Pronunciación",
        "comunicacion" to "Comunicación"
    )

    /** XP acumulada para llegar al nivel n: 50 · n · (n − 1). */
    fun xpParaNivel(nivel: Int): Int = XP_BASE * nivel * (nivel - 1)

    /** Mismo cálculo que backend/lib/progreso/niveles.js. */
    fun nivelPorXp(xp: Int): XpDto {
        val total = xp.coerceAtLeast(0)
        var nivel = 1
        while (xpParaNivel(nivel + 1) <= total) nivel++
        val base = xpParaNivel(nivel)
        return XpDto(total = total, nivel = nivel, xpEnNivel = total - base, xpParaSiguiente = xpParaNivel(nivel + 1) - base)
    }

    /**
     * Regla de sincronización de la racha: la del servidor solo reemplaza la
     * local si no hay partidas ni intentos sin enviar. Si los hay, la local ya
     * cuenta lo hecho sin conexión y el servidor todavía no.
     */
    fun debeSobrescribirRacha(pendientes: Int): Boolean = pendientes == 0

    /**
     * Sin red y sin caché: lo que se sabe del perfil local, con las insignias
     * del catálogo empaquetado en gris (todavía no se sabe cuáles se obtuvieron).
     */
    fun estadoMinimo(usuario: UsuarioEntity?, catalogo: List<InsigniaDto>, hoy: LocalDate): ProgresoDto {
        val xp = usuario?.xp ?: 0
        val racha = usuario?.let { Racha.vigente(it.racha, it.ultimoDiaActivo, hoy) } ?: 0
        return ProgresoDto(
            nivel = usuario?.nivel ?: "A1",
            dominio = DominioDto(aprobadas = 0, total = 0, porcentaje = 0),
            xp = nivelPorXp(xp),
            racha = RachaDto(actual = racha, maxima = racha, ultimoDia = usuario?.ultimoDiaActivo),
            habilidades = HABILIDADES.map { (id, nombre) -> HabilidadDto(id, nombre, porcentaje = null, sugerencia = null) },
            insignias = catalogo.map { it.copy(obtenida = false, fecha = null, retroactiva = false, progreso = null) },
            sinActividad = xp == 0 && usuario?.ultimoDiaActivo == null
        )
    }
}
