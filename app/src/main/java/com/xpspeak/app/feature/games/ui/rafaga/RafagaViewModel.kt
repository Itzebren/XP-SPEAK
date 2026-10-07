package com.xpspeak.app.feature.games.ui.rafaga

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.RondaRafaga
import com.xpspeak.app.feature.games.ui.comun.EstadoPartida
import com.xpspeak.app.feature.games.ui.comun.PartidaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/** El par que se acaba de responder, para mostrar el feedback medio segundo. */
data class ParRespondido(val acierto: Boolean, val en: String, val esCorrecta: String, val coincidian: Boolean)

data class EstadoRafaga(
    override val partidaId: String,
    override val marcador: Marcador,
    val pares: List<RondaRafaga>,
    val indice: Int = 0,
    val dificultad: Dificultad = Dificultad(),
    val msRestantes: Long = RafagaViewModel.MS_INICIALES,
    val combo: Int = 0,
    val mejorCombo: Int = 0,
    val puntos: Int = 0,
    val respondido: ParRespondido? = null
) : EstadoPartida<EstadoRafaga> {
    override fun conMarcador(marcador: Marcador) = copy(marcador = marcador)

    val par: RondaRafaga? get() = pares.getOrNull(indice)
    val multiplicador: Int get() = if (combo >= RafagaViewModel.COMBO_PARA_DOBLE) 2 else 1
}

/**
 * Ráfaga de Palabras (§5.3), variante de la Ilustración 37: aparece una
 * palabra con una traducción y el usuario dice si coinciden o la manda al
 * bote. 60 s en el reloj; acertar suma 1 s, fallar resta 3 s.
 */
@HiltViewModel
class RafagaViewModel @Inject constructor(
    repository: MinijuegosRepository
) : PartidaViewModel<EstadoRafaga>(repository, Minijuego.RAFAGA_PALABRAS, EstadoRafaga::class.java) {

    private var reloj: Job? = null

    override fun crearPartida(contenido: ContenidoJuegos, partidaId: String): EstadoRafaga? {
        val pares = GeneradorRondas.paresRafaga(contenido, random)
        return if (pares.isEmpty()) null else EstadoRafaga(partidaId, Marcador(), pares)
    }

    override fun alJugar(estado: EstadoRafaga) {
        reloj?.cancel()
        reloj = viewModelScope.launch {
            var anterior = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_MS)
                val ahora = SystemClock.elapsedRealtime()
                val actual = estadoActual ?: break
                // El reloj se detiene mientras se muestra el feedback del par.
                if (actual.respondido == null) {
                    val restantes = actual.msRestantes - (ahora - anterior)
                    if (restantes <= 0) {
                        terminar(actual.copy(msRestantes = 0))
                        break
                    }
                    actualizar(actual.copy(msRestantes = restantes), guardar = false)
                }
                anterior = ahora
            }
        }
        // Retomada justo durante el feedback: se pasa al siguiente par.
        if (estado.respondido != null) siguiente()
    }

    override fun alPausar() {
        reloj?.cancel()
        reloj = null
    }

    fun responder(coinciden: Boolean) {
        val estado = estadoActual ?: return
        val par = estado.par ?: return
        if (estado.respondido != null) return
        val acierto = coinciden == par.muestraCorrecta
        val combo = if (acierto) estado.combo + 1 else 0
        val nuevo = estado.copy(
            marcador = estado.marcador.conRonda(acierto).conSenal(par.conceptoId, acierto),
            dificultad = estado.dificultad.registrar(acierto),
            msRestantes = (estado.msRestantes + if (acierto) MS_POR_ACIERTO else -MS_POR_ERROR).coerceAtLeast(0),
            combo = combo,
            mejorCombo = maxOf(estado.mejorCombo, combo),
            puntos = estado.puntos + if (acierto) estado.copy(combo = combo).multiplicador else 0,
            respondido = ParRespondido(acierto, par.en, par.esCorrecta, par.muestraCorrecta)
        )
        actualizar(nuevo)
        viewModelScope.launch {
            delay(if (acierto) PAUSA_ACIERTO_MS else PAUSA_ERROR_MS)
            siguiente()
        }
    }

    private fun siguiente() {
        val estado = estadoActual ?: return
        if (estado.respondido == null) return
        val avanzado = estado.copy(indice = estado.indice + 1, respondido = null)
        when {
            avanzado.msRestantes <= 0 || avanzado.par == null -> terminar(avanzado)
            else -> actualizar(avanzado, guardar = false)
        }
    }

    companion object {
        const val MS_INICIALES = 60_000L
        // 1 s y no 2: con 2 s quien contesta rápido nunca ve el reloj llegar a 0.
        const val MS_POR_ACIERTO = 1_000L
        const val MS_POR_ERROR = 3_000L
        const val COMBO_PARA_DOBLE = 5
        private const val TICK_MS = 100L
        private const val PAUSA_ACIERTO_MS = 300L
        // Error: medio segundo largo para leer la respuesta correcta (§5.3).
        private const val PAUSA_ERROR_MS = 1_200L
    }
}
