package com.xpspeak.app.feature.games.domain

/**
 * Dificultad adaptativa común (docs/minijuegos-diseno.md §4.4, teoría del
 * flujo): 3 aciertos seguidos suben un nivel y 2 errores seguidos lo bajan,
 * para que el reto no aburra ni frustre.
 */
data class Dificultad(val nivel: Int = FACIL, val aciertosSeguidos: Int = 0, val erroresSeguidos: Int = 0) {

    fun registrar(acierto: Boolean): Dificultad =
        if (acierto) {
            val seguidos = aciertosSeguidos + 1
            if (seguidos >= ACIERTOS_PARA_SUBIR && nivel < DIFICIL) Dificultad(nivel + 1) else copy(aciertosSeguidos = seguidos, erroresSeguidos = 0)
        } else {
            val seguidos = erroresSeguidos + 1
            if (seguidos >= ERRORES_PARA_BAJAR && nivel > FACIL) Dificultad(nivel - 1) else copy(aciertosSeguidos = 0, erroresSeguidos = seguidos)
        }

    companion object {
        const val FACIL = 0
        const val MEDIO = 1
        const val DIFICIL = 2
        private const val ACIERTOS_PARA_SUBIR = 3
        private const val ERRORES_PARA_BAJAR = 2
    }
}
