package com.xpspeak.app.feature.games.domain

/**
 * Catálogo de minijuegos (RF-11, CU-07). El diseño de cada uno está en
 * docs/minijuegos-diseno.md. `xpBase` y `msPorRonda` deben coincidir con
 * backend/lib/minijuegos/xp.js: el servidor recalcula la XP con los mismos valores.
 *
 * @param msPorRonda tiempo objetivo por ronda para el bono de rapidez; null en
 *   Ráfaga, que ya va contra reloj.
 */
enum class Minijuego(
    val id: String,
    val titulo: String,
    val descripcion: String,
    val xpBase: Int,
    val msPorRonda: Long?
) {
    ECO_VOCAL("eco-vocal", "Eco Vocal", "Escucha y repite: pronunciación", 15, 20_000),
    MISION_SITUACIONAL("mision-situacional", "Misión Situacional", "Resuelve una situación real", 15, 20_000),
    RAFAGA_PALABRAS("rafaga-palabras", "Ráfaga de Palabras", "Vocabulario contra reloj", 10, null),
    ORDEN_MAESTRO("orden-maestro", "Orden Maestro", "Ordena la oración", 10, 15_000);

    companion object {
        fun porId(id: String?): Minijuego? = entries.firstOrNull { it.id == id }
    }
}
