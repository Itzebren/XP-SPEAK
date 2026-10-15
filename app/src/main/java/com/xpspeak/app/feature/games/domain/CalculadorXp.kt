package com.xpspeak.app.feature.games.domain

import kotlin.math.floor

/**
 * XP de una partida (docs/minijuegos-diseno.md §4.3, RF-14 / RN-09). Es la
 * misma fórmula que backend/lib/minijuegos/xp.js: la app la usa para mostrar
 * la XP sin conexión, pero la oficial es la que confirma el servidor.
 *
 *   xp = round(xpBase · precisión) + (precisión ≥ 0.7 ? bono : 0)
 */
object CalculadorXp {
    const val BONO_MAXIMO = 5
    const val PRECISION_PARA_BONO = 0.7
    const val PARTIDAS_CON_XP_COMPLETA = 5
    private const val ACIERTOS_POR_PUNTO_RAFAGA = 5

    data class Xp(val xp: Int, val reducida: Boolean)

    /** @param partidasPrevias partidas del mismo juego ya terminadas hoy */
    fun calcular(juego: Minijuego, aciertos: Int, total: Int, duracionMs: Long, partidasPrevias: Int = 0): Xp {
        val precision = if (total > 0) aciertos.toDouble() / total else 0.0
        // Mismo redondeo que Math.round de JS (mitades hacia arriba).
        val base = floor(juego.xpBase * precision + 0.5).toInt()
        val completa = base + if (precision >= PRECISION_PARA_BONO) bono(juego, aciertos, total, duracionMs) else 0
        val reducida = partidasPrevias >= PARTIDAS_CON_XP_COMPLETA
        return Xp(if (reducida) completa / 2 else completa, reducida)
    }

    fun bono(juego: Minijuego, aciertos: Int, total: Int, duracionMs: Long): Int {
        val msPorRonda = juego.msPorRonda
            ?: return minOf(BONO_MAXIMO, aciertos / ACIERTOS_POR_PUNTO_RAFAGA)
        // 5 puntos en la mitad del tiempo objetivo o menos; 0 en 1.5 veces o más.
        val proporcion = duracionMs.toDouble() / (msPorRonda * total)
        return floor(BONO_MAXIMO * (1.5 - proporcion) + 0.5).toInt().coerceIn(0, BONO_MAXIMO)
    }
}
