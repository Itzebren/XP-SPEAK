package com.xpspeak.app.feature.lessons.domain

import com.xpspeak.app.feature.lessons.data.CatalogoDto
import com.xpspeak.app.feature.lessons.data.EstadoLeccion

/**
 * Al completar todas las lecciones de A1, el usuario pasa a A2 (RN-02: el nivel
 * rige el catálogo, el chat y los minijuegos). El nivel elegido al registrarse
 * es el punto de partida; este ascenso es el único cambio automático.
 *
 * Solo con el catálogo recién confirmado por el servidor: el guardado sin
 * conexión puede no reflejar evaluaciones todavía en cola. Y solo una vez por
 * cuenta: si después el usuario vuelve a A1 desde Cuenta para repasar, se respeta.
 */
object AscensoNivel {
    /** A2 es el último nivel del alcance del Trabajo Terminal (A1 y A2). */
    private val SIGUIENTE = mapOf("A1" to "A2")

    /** El nivel al que se asciende, o null si no corresponde. */
    fun siguiente(catalogo: CatalogoDto, sinConexion: Boolean, yaAscendio: Boolean): String? {
        if (sinConexion || yaAscendio || catalogo.lecciones.isEmpty()) return null
        if (catalogo.lecciones.any { it.estado != EstadoLeccion.COMPLETADA }) return null
        return SIGUIENTE[catalogo.nivel]
    }
}
