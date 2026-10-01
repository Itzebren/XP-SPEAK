package com.xpspeak.app.feature.games.domain

/**
 * Catálogo de minijuegos (RF-11, CU-07). El diseño de cada uno está en
 * docs/minijuegos-diseno.md.
 */
enum class Minijuego(val id: String, val titulo: String, val descripcion: String) {
    ECO_VOCAL("eco-vocal", "Eco Vocal", "Escucha y repite: pronunciación"),
    MISION_SITUACIONAL("mision-situacional", "Misión Situacional", "Resuelve una situación real"),
    RAFAGA_PALABRAS("rafaga-palabras", "Ráfaga de Palabras", "Vocabulario contra reloj"),
    ORDEN_MAESTRO("orden-maestro", "Orden Maestro", "Ordena la oración");

    companion object {
        fun porId(id: String?): Minijuego? = entries.firstOrNull { it.id == id }
    }
}
