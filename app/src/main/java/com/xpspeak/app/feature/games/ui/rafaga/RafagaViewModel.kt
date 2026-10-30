package com.xpspeak.app.feature.games.ui.rafaga

import android.os.SystemClock
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.games.data.MinijuegosRepository
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.Dificultad
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.RespuestaVoz
import com.xpspeak.app.feature.games.domain.RondaRafaga
import com.xpspeak.app.feature.games.ui.comun.EstadoPartida
import com.xpspeak.app.feature.games.ui.comun.PartidaViewModel
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/** El micrófono de Ráfaga: no se guarda con la partida, cada sesión decide si juega con voz. */
data class MicrofonoRafaga(
    val activo: Boolean = false,
    /** Lo que Azure va oyendo, para que el usuario vea que lo escucha. */
    val oyendo: String = "",
    val error: String? = null
)

/**
 * Ráfaga de Palabras (§5.3), variante de la Ilustración 37: aparece una
 * palabra con una traducción y el usuario dice si coinciden o la manda al
 * bote. 60 s en el reloj; acertar suma 1 s, fallar resta 3 s. También se
 * puede responder en voz alta ("yes" / "no") con el Azure Speech del chat.
 */
@HiltViewModel
class RafagaViewModel @Inject constructor(
    repository: MinijuegosRepository,
    private val evaluador: EvaluadorPronunciacion
) : PartidaViewModel<EstadoRafaga>(repository, Minijuego.RAFAGA_PALABRAS, EstadoRafaga::class.java) {

    private var reloj: Job? = null
    private var escucha: Job? = null
    /** Inicio (en el audio) de la última frase que ya se usó: los parciales y el final llegan con el mismo. */
    private var fraseUsada: Long? = null

    private val _microfono = MutableStateFlow(MicrofonoRafaga())
    val microfono: StateFlow<MicrofonoRafaga> = _microfono.asStateFlow()

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
        if (_microfono.value.activo) escuchar()
    }

    override fun alPausar() {
        reloj?.cancel()
        reloj = null
        // En segundo plano o al terminar no se deja el micrófono abierto.
        escucha?.cancel()
        escucha = null
        _microfono.value = _microfono.value.copy(oyendo = "")
    }

    fun alternarVoz() {
        if (_microfono.value.activo) {
            escucha?.cancel()
            escucha = null
            _microfono.value = MicrofonoRafaga()
        } else {
            _microfono.value = MicrofonoRafaga(activo = true)
            if (estadoActual != null) escuchar()
        }
    }

    fun onPermisoMicrofonoDenegado() {
        _microfono.value = MicrofonoRafaga(error = "Necesitas permitir el micrófono para responder con voz.")
    }

    private fun escuchar() {
        if (escucha?.isActive == true) return
        // Cada reconocedor cuenta el audio desde cero.
        fraseUsada = null
        escucha = viewModelScope.launch {
            val resultado = evaluador.escucharContinuo(RespuestaVoz.FRASES) { inicio, texto ->
                // Llega en un hilo del SDK: se procesa en el principal, igual que un toque.
                viewModelScope.launch { oir(inicio, texto) }
            }
            // Solo vuelve si Azure canceló (sin red, token rechazado…): se apaga el micrófono.
            resultado.exceptionOrNull()?.let {
                // QA 29-oct: el mensaje de Azure ya termina en punto; sin trimEnd salía "voz.. Sigue".
                _microfono.value = MicrofonoRafaga(error = "${(it.message ?: "No pude usar el micrófono").trimEnd('.')}. Sigue con los botones.")
                escucha = null
            }
        }
    }

    private fun oir(inicio: Long, texto: String) {
        if (!_microfono.value.activo) return
        _microfono.value = _microfono.value.copy(oyendo = texto)
        if (inicio == fraseUsada) return
        val coinciden = RespuestaVoz.interpretar(texto) ?: return
        // Dicha durante el feedback también se consume: si no, su resultado final contestaría el par siguiente.
        fraseUsada = inicio
        responder(coinciden)
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
