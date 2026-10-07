package com.xpspeak.app.feature.games.ui.mision

import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.Mision
import com.xpspeak.app.feature.games.domain.OpcionMision
import com.xpspeak.app.feature.games.domain.PasoMision
import com.xpspeak.app.feature.games.domain.Texto
import com.xpspeak.app.feature.games.ui.comun.EstadoPartida
import com.xpspeak.app.feature.games.ui.comun.PartidaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

data class LineaChat(val deNpc: Boolean, val en: String, val es: String? = null)

data class EstadoMision(
    override val partidaId: String,
    override val marcador: Marcador,
    val mision: Mision,
    /** Orden barajado de las opciones de cada paso (se guarda para que no cambie al retomar). */
    val ordenOpciones: List<List<Int>>,
    val paso: Int = 0,
    val historial: List<LineaChat> = emptyList(),
    val cumplidos: List<String> = emptyList(),
    /** Opciones ya intentadas (incorrectas) en el paso actual. */
    val falladas: List<Int> = emptyList(),
    val feedback: String? = null,
    val dificultad: Dificultad = Dificultad(),
    /** Tras 3 aciertos seguidos la respuesta se escribe en vez de elegirse (§5.2 paso 3). */
    val escribiendo: Boolean = false
) : EstadoPartida<EstadoMision> {
    override fun conMarcador(marcador: Marcador) = copy(marcador = marcador)

    val pasoActual: PasoMision get() = mision.pasos[paso]
    val correcta: OpcionMision get() = pasoActual.opciones.first { it.correcta }
    val opciones: List<Pair<Int, OpcionMision>> get() = ordenOpciones[paso].map { it to pasoActual.opciones[it] }
}

/**
 * Misión Situacional (§5.2): una conversación guiada con un personaje. Cada
 * respuesta correcta avanza la conversación y puede cumplir un objetivo; una
 * incorrecta explica por qué y el personaje no entiende. Desempeño = pasos
 * resueltos al primer intento.
 */
@HiltViewModel
class MisionViewModel @Inject constructor(
    repository: MinijuegosRepository
) : PartidaViewModel<EstadoMision>(repository, Minijuego.MISION_SITUACIONAL, EstadoMision::class.java) {

    private var ultimaMision: String? = null

    override val mensajeSinContenido =
        "Aún no hay misiones para tus lecciones. Completa más lecciones para desbloquearlas."

    override fun crearPartida(contenido: ContenidoJuegos, partidaId: String): EstadoMision? {
        val mision = GeneradorRondas.elegirMision(contenido, random, evitar = ultimaMision) ?: return null
        ultimaMision = mision.id
        return EstadoMision(
            partidaId = partidaId,
            marcador = Marcador(),
            mision = mision,
            ordenOpciones = mision.pasos.map { paso -> paso.opciones.indices.shuffled(random) },
            historial = listOf(LineaChat(deNpc = true, en = mision.pasos[0].npc, es = mision.pasos[0].npcEs))
        )
    }

    fun elegir(indice: Int) {
        val estado = estadoActual ?: return
        if (indice in estado.falladas) return
        val opcion = estado.pasoActual.opciones[indice]
        if (opcion.correcta) acertar(estado) else fallar(estado, indice, opcion.feedback)
    }

    /** Compara lo escrito con la respuesta correcta, tolerando mayúsculas, puntuación y una palabra distinta. */
    fun escribir(texto: String) {
        val estado = estadoActual ?: return
        if (texto.isBlank()) return
        val (indice, opcion) = estado.opciones.maxBy { (_, opcion) -> Texto.similitud(opcion.en, texto) }
        when {
            opcion.correcta && Texto.similitud(opcion.en, texto) >= SIMILITUD_ESCRITA -> acertar(estado, texto.trim())
            !opcion.correcta && Texto.similitud(opcion.en, texto) >= SIMILITUD_ESCRITA -> fallar(estado, indice, opcion.feedback)
            else -> fallar(estado, null, "No es lo que se esperaba. Inténtalo otra vez o elige una opción.")
        }
    }

    fun verOpciones() {
        val estado = estadoActual ?: return
        actualizar(estado.copy(escribiendo = false), guardar = false)
    }

    private fun fallar(estado: EstadoMision, indice: Int?, feedback: String?) {
        actualizar(
            estado.copy(
                // La señal del SRS va al concepto que practica la respuesta correcta.
                marcador = estado.marcador.conSenal(estado.correcta.conceptoId, acierto = false),
                falladas = if (indice != null) estado.falladas + indice else estado.falladas,
                feedback = feedback ?: "Esa respuesta no encaja aquí.",
                historial = estado.historial + LineaChat(deNpc = true, en = "Sorry?", es = "¿Perdón?")
            )
        )
    }

    private fun acertar(estado: EstadoMision, dicho: String = estado.correcta.en) {
        val alPrimerIntento = estado.falladas.isEmpty() && estado.feedback == null
        val marcador = estado.marcador.conSenal(estado.correcta.conceptoId, acierto = true).conRonda(alPrimerIntento)
        val dificultad = estado.dificultad.registrar(alPrimerIntento)
        val cumplidos = estado.cumplidos + listOfNotNull(estado.correcta.cumple)
        val historial = estado.historial + LineaChat(deNpc = false, en = dicho)

        val siguiente = estado.paso + 1
        if (siguiente >= estado.mision.pasos.size) {
            terminar(estado.copy(marcador = marcador, cumplidos = cumplidos, historial = historial))
            return
        }
        val npc = estado.mision.pasos[siguiente]
        actualizar(
            estado.copy(
                marcador = marcador,
                paso = siguiente,
                historial = historial + LineaChat(deNpc = true, en = npc.npc, es = npc.npcEs),
                cumplidos = cumplidos,
                falladas = emptyList(),
                feedback = null,
                dificultad = dificultad,
                escribiendo = dificultad.nivel >= Dificultad.MEDIO
            )
        )
    }

    private companion object {
        const val SIMILITUD_ESCRITA = 0.8
    }
}
