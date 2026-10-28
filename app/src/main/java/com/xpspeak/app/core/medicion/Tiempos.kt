package com.xpspeak.app.core.medicion

import android.os.SystemClock
import android.util.Log

/** Las metas de tiempo del Trabajo Terminal que se miden en el teléfono (Tabla 93). */
enum class Meta(val etiqueta: String, val limiteMs: Long?) {
    /** Retroalimentación fonética y gramatical en 2.5 s o menos. */
    RN05("RN-05 ≤ 2.5 s", 2_500),

    /** Evaluación acústica en 800 ms o menos (riesgo R01). */
    R01("R01 ≤ 800 ms", 800),

    /** "La respuesta visual comienza en pocos segundos": sin límite numérico, solo se registra. */
    RNF02("RNF-02", null)
}

/**
 * Registro de tiempos para las pruebas de rendimiento (Capítulo 6, Tabla 93).
 * Cada medida sale en logcat con su meta y si la cumple:
 *
 *   adb logcat -s XPS-TIEMPOS
 *
 * No guarda nada ni lo manda a ningún servidor.
 */
object Tiempos {
    const val TAG = "XPS-TIEMPOS"

    fun ahora(): Long = SystemClock.elapsedRealtime()

    /** "chat.correccion: 1840 ms · RN-05 ≤ 2.5 s · CUMPLE" (+ nota si la hay). */
    fun linea(medida: String, ms: Long, meta: Meta, nota: String? = null): String {
        val veredicto = meta.limiteMs?.let { if (ms <= it) "CUMPLE" else "NO CUMPLE" }
        return listOfNotNull("$medida: $ms ms", meta.etiqueta, veredicto, nota).joinToString(" · ")
    }

    fun registrar(medida: String, ms: Long, meta: Meta, nota: String? = null) {
        runCatching { Log.i(TAG, linea(medida, ms, meta, nota)) }
    }

    /** Lo que tardó desde [inicio] (de [ahora]). */
    fun desde(medida: String, inicio: Long, meta: Meta, nota: String? = null) =
        registrar(medida, ahora() - inicio, meta, nota)

    /**
     * Milisegundos entre que el usuario dejó de hablar y llegó el resultado de Azure.
     * El audio del micrófono fluye en tiempo real desde que empieza la sesión, así que
     * el fin de la voz en el reloj es [inicioSesion] + (offset + duración) de la frase,
     * que Azure da en ticks de 100 ns.
     */
    fun trasHablar(inicioSesion: Long, llegada: Long, offsetTicks: Long, duracionTicks: Long): Long =
        llegada - inicioSesion - (offsetTicks + duracionTicks) / 10_000
}
