package com.xpspeak.app.feature.games.data.voz

import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Qué hace el micrófono en este momento. Lo publica [EvaluadorVoz] y lo muestran igual
 * todas las pantallas con voz (Eco Vocal, Práctica, Pronunciación), para que el usuario
 * sepa cuándo hablar, vea que se le oye y pueda terminar sin esperar la pausa.
 */
sealed interface EstadoEscucha {
    object Inactivo : EstadoEscucha

    /** Pidiendo el token o abriendo el micrófono: todavía no sirve hablar. */
    object Preparando : EstadoEscucha

    /**
     * Micrófono abierto. [nivel] es el volumen de entrada (0–1) para las barras; [oyendo], lo que
     * el reconocedor lleva entendido; [hablo], si ya detectó voz (antes el usuario repetía la
     * frase varias veces porque no sabía si lo estaba oyendo).
     */
    data class Escuchando(val nivel: Float = 0f, val oyendo: String = "", val hablo: Boolean = false) : EstadoEscucha

    /** Terminó de hablar (pausa o botón «Listo»): se espera la calificación. */
    object Calificando : EstadoEscucha
}

/**
 * Un intento de voz de principio a fin. La crea [EvaluadorVoz] y se la pasa al evaluador
 * (Azure o el teléfono), que avisa por aquí lo que va oyendo y recibe la orden de terminar
 * antes de tiempo ([terminar], el botón «Listo»).
 */
class SesionEscucha(private val publicar: (EstadoEscucha) -> Unit) {
    private var actual: EstadoEscucha.Escuchando? = null

    /** El usuario pidió terminar; el evaluador que llegue después ya no debe esperar la pausa. */
    @Volatile var fueTerminada = false
        private set

    /** Lo pone el evaluador cuando abre el micrófono: cerrarlo y calificar lo que ya se oyó. */
    var alTerminar: (() -> Unit)? = null
        set(value) {
            field = value
            if (fueTerminada) value?.invoke()
        }

    fun preparando() = publicar(EstadoEscucha.Preparando)

    fun escuchando() = synchronized(this) {
        EstadoEscucha.Escuchando().also { actual = it; publicar(it) }
    }

    fun nivel(valor: Float) = cambiar { it.copy(nivel = valor.coerceIn(0f, 1f)) }

    fun oyendo(texto: String) = cambiar { it.copy(oyendo = texto, hablo = it.hablo || texto.isNotBlank()) }

    fun vozDetectada() = cambiar { it.copy(hablo = true) }

    fun calificando() = synchronized(this) {
        actual = null
        publicar(EstadoEscucha.Calificando)
    }

    /** Botón «Listo»: el usuario ya dijo la frase y no quiere esperar la pausa de silencio. */
    fun terminar() {
        fueTerminada = true
        calificando()
        alTerminar?.invoke()
    }

    private inline fun cambiar(f: (EstadoEscucha.Escuchando) -> EstadoEscucha.Escuchando) = synchronized(this) {
        val estado = actual ?: return
        val nuevo = f(estado)
        if (nuevo != estado) {
            actual = nuevo
            publicar(nuevo)
        }
    }
}

/** Volumen del micrófono en 0–1 para las barras de «te escucho». */
object NivelAudio {
    /** Silencio de cuarto (≈ -55 dBFS) → 0; voz normal (≈ -25 dBFS) → 0.5; a tope → 1. */
    private const val PISO_DBFS = 55.0

    /** PCM de 16 bits (lo que se manda a Azure). */
    fun deMuestras(muestras: ShortArray, leidas: Int): Float {
        if (leidas <= 0) return 0f
        var suma = 0.0
        for (i in 0 until leidas) {
            val m = muestras[i].toDouble()
            suma += m * m
        }
        val rms = sqrt(suma / leidas)
        if (rms < 1.0) return 0f
        return deDbfs(20 * log10(rms / 32768.0))
    }

    fun deDbfs(db: Double): Float = ((db + PISO_DBFS) / PISO_DBFS).toFloat().coerceIn(0f, 1f)

    /** `onRmsChanged` del reconocedor de Android reporta más o menos de -2 a 10 dB. */
    fun deRmsAndroid(rmsDb: Float): Float = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
}
