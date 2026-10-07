package com.xpspeak.app.feature.games.domain

/** Aciertos y errores de un concepto en una partida: lo que alimenta el SRS (RF-13). */
data class ConteoConcepto(val aciertos: Int = 0, val errores: Int = 0)

/**
 * Desempeño acumulado de una partida (CU-07 paso 7: precisión y tiempo).
 * Inmutable para que el estado de cada juego se pueda guardar en Room tal cual.
 *
 * @param tiempoJugadoMs solo el tiempo con la pantalla activa: una partida
 *   retomada al día siguiente no cuenta las horas que estuvo cerrada.
 */
data class Marcador(
    val aciertos: Int = 0,
    val total: Int = 0,
    val conceptos: Map<String, ConteoConcepto> = emptyMap(),
    val tiempoJugadoMs: Long = 0
) {
    /** Una ronda terminada. */
    fun conRonda(acierto: Boolean): Marcador =
        copy(aciertos = aciertos + if (acierto) 1 else 0, total = total + 1)

    /** Señal del SRS; un concepto puede sumar varios errores en la misma ronda (segundo intento). */
    fun conSenal(conceptoId: String?, acierto: Boolean): Marcador {
        if (conceptoId == null) return this
        val previo = conceptos[conceptoId] ?: ConteoConcepto()
        val nuevo = if (acierto) previo.copy(aciertos = previo.aciertos + 1) else previo.copy(errores = previo.errores + 1)
        return copy(conceptos = conceptos + (conceptoId to nuevo))
    }

    fun conTiempo(ms: Long): Marcador = copy(tiempoJugadoMs = tiempoJugadoMs + ms.coerceAtLeast(0))

    val precision: Double get() = if (total == 0) 0.0 else aciertos.toDouble() / total
}
