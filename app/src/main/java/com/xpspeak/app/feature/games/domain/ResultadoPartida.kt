package com.xpspeak.app.feature.games.domain

import com.xpspeak.app.feature.progress.data.InsigniaDto

/**
 * Lo que se muestra al terminar (CU-07 pasos 7 y 8).
 *
 * @param confirmada el servidor ya registró la partida y la XP es la oficial;
 *   si es false, se muestra la XP calculada en el teléfono y se confirma al sincronizar.
 * @param paraRepasar conceptos con errores, ya con texto para mostrar.
 * @param insigniasNuevas insignias ganadas con esta partida (Progreso), para festejarlas.
 * @param nivelXpNuevo nivel de experiencia alcanzado con esta partida, o null.
 */
data class ResultadoPartida(
    val juego: Minijuego,
    val aciertos: Int,
    val total: Int,
    val duracionMs: Long,
    val xp: Int,
    val xpReducida: Boolean,
    val confirmada: Boolean,
    val racha: Int,
    val paraRepasar: List<String>,
    val insigniasNuevas: List<InsigniaDto> = emptyList(),
    val nivelXpNuevo: Int? = null
) {
    val precision: Double get() = if (total == 0) 0.0 else aciertos.toDouble() / total
}
