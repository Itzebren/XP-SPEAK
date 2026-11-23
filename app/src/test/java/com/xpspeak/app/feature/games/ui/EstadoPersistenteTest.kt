package com.xpspeak.app.feature.games.ui

import com.google.gson.Gson
import com.xpspeak.app.feature.games.domain.BancoMinijuegos
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.ui.ecovocal.EstadoEco
import com.xpspeak.app.feature.games.ui.mision.EstadoMision
import com.xpspeak.app.feature.games.ui.mision.LineaChat
import com.xpspeak.app.feature.games.ui.orden.EstadoOrden
import com.xpspeak.app.feature.games.ui.rafaga.EstadoRafaga
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * RNF-10 / CU-07 A1: el estado de cada juego se guarda en Room como JSON
 * (Gson) y debe volver idéntico al retomar la partida.
 */
class EstadoPersistenteTest {

    private val gson = Gson()
    private val banco = File("src/main/assets/minijuegos/banco.json").reader().use { gson.fromJson(it, BancoMinijuegos::class.java) }
    private val contenido = ContenidoJuegos("A1", banco.lecciones.filter { it.nivel == "A1" }, banco.misiones.filter { it.nivel == "A1" })
    private val marcador = Marcador(aciertos = 2, total = 3, tiempoJugadoMs = 4_200).conSenal("gram.some_any", false)

    private inline fun <reified E> idaYVuelta(estado: E) = assertEquals(estado, gson.fromJson(gson.toJson(estado), E::class.java))

    @Test
    fun `orden maestro`() {
        val mazo = GeneradorRondas.rondasOrden(contenido, Random(1))
        val (ronda, resto) = mazo.sacar(0)!!
        idaYVuelta(EstadoOrden("id", marcador, resto, Dificultad(1, 2, 0), ronda, numeroRonda = 3, colocadas = listOf(2, 0)))
    }

    @Test
    fun `rafaga`() {
        idaYVuelta(EstadoRafaga("id", marcador, GeneradorRondas.paresRafaga(contenido, Random(1)), indice = 4, msRestantes = 31_000, combo = 3))
    }

    @Test
    fun `mision`() {
        val mision = banco.misiones.first()
        idaYVuelta(
            EstadoMision(
                "id", marcador, mision, mision.pasos.map { listOf(2, 0, 1) }, paso = 1,
                historial = listOf(LineaChat(true, "Hi!", "¡Hola!"), LineaChat(false, "Hello!")),
                cumplidos = listOf("saludar"), falladas = listOf(1), feedback = "Así no.", escribiendo = true
            )
        )
    }

    @Test
    fun `eco vocal`() {
        val (ronda, resto) = GeneradorRondas.rondasEco(contenido, Random(1)).sacar(0)!!
        val evaluacion = EvaluacionVoz(82, 95, listOf(PalabraEvaluada("hello", 82, "Mispronunciation")), "hello", FuenteEvaluacion.AZURE)
        idaYVuelta(EstadoEco("id", marcador, resto, Dificultad(), ronda, numeroRonda = 2, intento = 2, evaluacion = evaluacion))
    }
}
