package com.xpspeak.app.feature.games.data.voz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

/** La voz que llega casi muda (tablet, VOICE_RECOGNITION sin AGC) sube a nivel normal. */
class GananciaAutomaticaTest {

    private fun tono(amplitud: Double, n: Int = 800) =
        ShortArray(n) { (amplitud * sin(2 * PI * 220 * it / 16_000.0)).toInt().toShort() }

    private fun rms(m: ShortArray) = sqrt(m.sumOf { it.toDouble() * it } / m.size)

    /** Medio segundo de silencio con ruido de cuarto y después voz de [amplitud]; devuelve el último bloque. */
    private fun procesar(agc: GananciaAutomatica, amplitud: Double): ShortArray {
        repeat(10) { agc.procesar(ShortArray(800) { i -> ((i % 7) - 3).toShort() }, 800) }
        var ultimo = ShortArray(0)
        repeat(20) { ultimo = tono(amplitud).also { agc.procesar(it, it.size) } }
        return ultimo
    }

    @Test
    fun `una voz casi muda se amplifica a nivel de voz normal`() {
        val agc = GananciaAutomatica()
        val salida = procesar(agc, amplitud = 100.0) // ≈ -50 dBFS
        assertTrue("rms ${rms(salida)}", rms(salida) > 2000)
        assertTrue(agc.rmsVozOriginal < GananciaAutomatica.RMS_VOZ_BAJA)
    }

    @Test
    fun `una voz con buen volumen no se toca`() {
        val agc = GananciaAutomatica()
        val salida = procesar(agc, amplitud = 8000.0)
        assertEquals(1.0, agc.ganancia, 0.0)
        assertEquals(rms(tono(8000.0)), rms(salida), 1.0)
    }

    @Test
    fun `nunca satura`() {
        val agc = GananciaAutomatica()
        val salida = procesar(agc, amplitud = 2500.0)
        assertTrue(salida.all { abs(it.toInt()) <= GananciaAutomatica.PICO_MAXIMO.toInt() + 1 })
    }

    @Test
    fun `en silencio no sube el ruido`() {
        val agc = GananciaAutomatica()
        repeat(30) { agc.procesar(ShortArray(800) { i -> ((i % 7) - 3).toShort() }, 800) }
        assertEquals(1.0, agc.ganancia, 0.0)
    }
}
