package com.xpspeak.app.feature.games.data.voz

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Control automático de ganancia (AGC) por software para el PCM que se manda a Azure.
 *
 * El micrófono se abre con la fuente VOICE_RECOGNITION, que en Android viene SIN ganancia
 * automática. En un teléfono pegado a la boca no se nota, pero en una tablet (Xiaomi Pad 6,
 * 10-nov-2026) la voz llegaba a ~1 % del nivel normal: con la misma voz nativa, Azure pasaba
 * de 100 puntos a 49, y por debajo de eso devolvía texto vacío y confianza 0 («no legible»).
 *
 * Por cada bloque (50 ms) se estima el ruido de fondo; si el bloque es voz (bastante más fuerte
 * que el ruido), se busca la ganancia que deja la voz en [OBJETIVO_RMS] sin pasar de
 * [GANANCIA_MAXIMA] ni saturar el pico. La ganancia baja de inmediato y sube con suavidad, y se
 * interpola muestra a muestra para que no haya chasquidos. En silencio se conserva la ganancia
 * para no subir el ruido entre palabras. Un audio que ya llega con buen volumen queda intacto.
 */
class GananciaAutomatica {
    /** Ganancia aplicada en el último bloque (1 = sin cambio). */
    var ganancia = 1.0
        private set

    /** Ruido de fondo estimado (RMS en unidades de PCM de 16 bits). */
    private var ruido = -1.0

    /** RMS más alto de la voz ANTES de la ganancia. */
    var rmsVozOriginal = 0.0
        private set

    /**
     * RMS más alto de cualquier bloque ANTES de la ganancia: sirve para avisar «te oímos muy
     * bajito» aun cuando la voz apenas supera el ruido y no se reconoce como voz.
     */
    var rmsMaximoOriginal = 0.0
        private set

    /** Procesa [leidas] muestras de [muestras] en su lugar. */
    fun procesar(muestras: ShortArray, leidas: Int) {
        if (leidas <= 0) return
        var suma = 0.0
        var pico = 0
        for (i in 0 until leidas) {
            val m = muestras[i].toInt()
            suma += m.toDouble() * m
            pico = maxOf(pico, abs(m))
        }
        val rms = sqrt(suma / leidas)
        rmsMaximoOriginal = maxOf(rmsMaximoOriginal, rms)

        // Ruido de fondo: baja rápido hacia los bloques más callados y sube muy despacio.
        ruido = when {
            ruido < 0 -> rms
            rms < ruido -> rms
            else -> ruido + (rms - ruido) * SUBIDA_RUIDO
        }.coerceAtLeast(RUIDO_MINIMO)

        val anterior = ganancia
        val esVoz = rms > ruido * VOZ_SOBRE_RUIDO && rms > RMS_MINIMO_VOZ
        if (esVoz) {
            rmsVozOriginal = maxOf(rmsVozOriginal, rms)
            val deseada = (OBJETIVO_RMS / rms).coerceIn(1.0, GANANCIA_MAXIMA)
            val sinSaturar = if (pico > 0) PICO_MAXIMO / pico else GANANCIA_MAXIMA
            val meta = minOf(deseada, sinSaturar).coerceAtLeast(1.0)
            ganancia = if (meta < ganancia) meta else ganancia + (meta - ganancia) * SUBIDA_GANANCIA
        } else if (pico > 0 && ganancia * pico > PICO_MAXIMO) {
            // Un golpe o una palabra muy fuerte entre silencios: nunca saturar.
            ganancia = maxOf(1.0, PICO_MAXIMO / pico)
        }

        if (anterior == 1.0 && ganancia == 1.0) return
        for (i in 0 until leidas) {
            val g = anterior + (ganancia - anterior) * (i + 1) / leidas
            muestras[i] = (muestras[i] * g).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
    }

    companion object {
        /** Voz normal (≈ -20 dBFS), lo que da un teléfono a 15–20 cm. */
        const val OBJETIVO_RMS = 3200.0
        /** Hasta +30 dB: más que eso solo sube ruido. */
        const val GANANCIA_MAXIMA = 32.0
        /** El pico amplificado se queda en ≈ -1 dBFS. */
        const val PICO_MAXIMO = 29_000.0
        /** Un bloque es voz si su RMS es ≥ 2.5 veces el ruido de fondo (≈ 8 dB). */
        const val VOZ_SOBRE_RUIDO = 2.5
        /** Por debajo de esto (≈ -78 dBFS) no hay nada que amplificar. */
        const val RMS_MINIMO_VOZ = 4.0
        private const val RUIDO_MINIMO = 1.0
        private const val SUBIDA_RUIDO = 0.02
        private const val SUBIDA_GANANCIA = 0.5

        /** Voz original por debajo de ≈ -42 dBFS: se avisa al usuario que se acerque. */
        const val RMS_VOZ_BAJA = 250.0
    }
}
