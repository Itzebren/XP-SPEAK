package com.xpspeak.app.feature.games.data.voz

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

/** Las barras de «te escucho» van de 0 (silencio) a 1 (a tope) con cualquiera de los dos micrófonos. */
class NivelAudioTest {

    private fun senoide(amplitud: Double, muestras: Int = 800) =
        ShortArray(muestras) { (amplitud * 32767 * sin(2 * PI * it / 40)).toInt().toShort() }

    @Test
    fun `el silencio es cero`() {
        assertEquals(0f, NivelAudio.deMuestras(ShortArray(800), 800), 0f)
        assertEquals(0f, NivelAudio.deMuestras(ShortArray(0), 0), 0f)
    }

    @Test
    fun `una voz normal queda a media barra y un grito la llena`() {
        // -25 dBFS ≈ amplitud 0.08 de una senoide (rms = amplitud/√2).
        val normal = NivelAudio.deMuestras(senoide(0.08), 800)
        assertEquals(0.5f, normal, 0.08f)
        val grito = NivelAudio.deMuestras(senoide(1.0), 800)
        assertEquals(0.95f, grito, 0.05f)
    }

    @Test
    fun `solo se leen las muestras recibidas`() {
        val mitad = senoide(1.0, 400)
        val completo = ShortArray(800).also { mitad.copyInto(it) }
        assertEquals(NivelAudio.deMuestras(mitad, 400), NivelAudio.deMuestras(completo, 400), 0.001f)
    }

    @Test
    fun `el rms del reconocedor de Android se acota a 0 y 1`() {
        assertEquals(0f, NivelAudio.deRmsAndroid(-5f), 0f)
        assertEquals(0.5f, NivelAudio.deRmsAndroid(4f), 0.001f)
        assertEquals(1f, NivelAudio.deRmsAndroid(12f), 0f)
    }
}
