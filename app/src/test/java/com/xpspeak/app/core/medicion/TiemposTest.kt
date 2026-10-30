package com.xpspeak.app.core.medicion

import org.junit.Assert.assertEquals
import org.junit.Test

/** Las líneas que se leen con `adb logcat -s XPS-TIEMPOS` para la Tabla 93. */
class TiemposTest {

    @Test
    fun `marca si cumple la meta, incluido el limite exacto`() {
        assertEquals("chat.correccion: 1840 ms · RN-05 ≤ 2.5 s · CUMPLE", Tiempos.linea("chat.correccion", 1840, Meta.RN05))
        assertEquals("x: 2500 ms · RN-05 ≤ 2.5 s · CUMPLE", Tiempos.linea("x", 2500, Meta.RN05))
        assertEquals("x: 801 ms · R01 ≤ 800 ms · NO CUMPLE", Tiempos.linea("x", 801, Meta.R01))
    }

    @Test
    fun `sin limite numerico solo registra, con la nota al final`() {
        assertEquals(
            "chat.primer_pedazo: 950 ms · RNF-02 · con nota",
            Tiempos.linea("chat.primer_pedazo", 950, Meta.RNF02, "con nota")
        )
    }

    @Test
    fun `cuenta desde que termino la voz, con offset y duracion en ticks de 100 ns`() {
        // La sesión empezó en t = 10 000 ms; la frase va de 1.2 s a 3.0 s del audio y el resultado llegó en t = 14 700 ms.
        val latencia = Tiempos.trasHablar(inicioSesion = 10_000, llegada = 14_700, offsetTicks = 12_000_000, duracionTicks = 18_000_000)
        assertEquals(1_700, latencia)
    }
}
