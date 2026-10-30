package com.xpspeak.app.feature.games.domain

import kotlin.math.roundToInt

/** Quién evaluó: Azure da puntaje fonético; el teléfono solo compara palabras (o solo transcribe). */
enum class FuenteEvaluacion { AZURE, DISPOSITIVO }

/** Un sonido de la palabra en IPA (p. ej. "θ") con su precisión 0–100 (Azure, granularidad Phoneme). */
data class FonemaEvaluado(val fonema: String = "", val puntaje: Int = 0)

data class PalabraEvaluada(
    val palabra: String,
    /** 0–100 */
    val puntaje: Int,
    /** "Omission", "Mispronunciation"… de Azure, o null si se dijo bien. */
    val error: String? = null,
    /** Sonidos mal dichos (IPA), p. ej. "/ɹ/ sonó como /ɾ/"; vacío si no hay detalle. */
    val sonidos: List<String> = emptyList(),
    /** Todos los sonidos de la palabra con su precisión, para el detalle al tocarla. */
    val fonemas: List<FonemaEvaluado> = emptyList()
)

/**
 * Resultado de una evaluación de voz: Eco Vocal (§5.1), Práctica de pronunciación, las
 * frases de una lección y el chat en vivo usan este mismo modelo (RF-09).
 *
 * @param puntaje precisión de los sonidos 0–100 (AnalisisFonemas); es lo que decide el acierto
 * @param confianza 0–100: qué tan seguro está el reconocedor de lo que oyó.
 *   Por debajo de [Pronunciacion.CONFIANZA_MINIMA] el audio no fue legible
 *   (CU-05 A1, Ilustración 40) y se pide repetir sin gastar el intento.
 * @param fluidez, completitud, puntajeGeneral métricas de frase de Azure (0–100); null con el
 *   reconocedor del teléfono. El general (PronScore) mezcla las tres y es solo informativo.
 */
data class EvaluacionVoz(
    val puntaje: Int,
    val confianza: Int,
    val palabras: List<PalabraEvaluada>,
    val transcripcion: String,
    val fuente: FuenteEvaluacion,
    val fluidez: Int? = null,
    val completitud: Int? = null,
    val puntajeGeneral: Int? = null
) {
    /** El teléfono solo transcribió (chat sin Azure): no hay palabras que colorear ni calificar. */
    val soloTranscrita: Boolean get() = fuente == FuenteEvaluacion.DISPOSITIVO && palabras.isEmpty()
}

object Pronunciacion {
    const val UMBRAL_ACIERTO = 70
    const val CONFIANZA_MINIMA = 30
    const val INTENTOS_POR_RONDA = 2
    private const val UMBRAL_PALABRA_REGULAR = 60

    enum class Nivel { BUENO, REGULAR, MALO }

    fun nivelDe(puntaje: Int): Nivel = when {
        puntaje >= UMBRAL_ACIERTO -> Nivel.BUENO
        puntaje >= UMBRAL_PALABRA_REGULAR -> Nivel.REGULAR
        else -> Nivel.MALO
    }

    fun esLegible(evaluacion: EvaluacionVoz) = evaluacion.confianza >= CONFIANZA_MINIMA

    /**
     * Acierto: buen puntaje general y ninguna palabra mal dicha u omitida. Con solo el
     * promedio, una frase larga pasaba aunque una palabra saliera en rojo.
     */
    fun esAcierto(evaluacion: EvaluacionVoz) =
        esLegible(evaluacion) && evaluacion.puntaje >= UMBRAL_ACIERTO && evaluacion.palabras.none(::esPalabraFallida)

    private fun esPalabraFallida(palabra: PalabraEvaluada) =
        palabra.error == "Mispronunciation" || palabra.error == "Omission" || palabra.puntaje < UMBRAL_PALABRA_REGULAR

    /**
     * Respaldo sin conexión: el reconocedor del teléfono no mide fonemas, así
     * que se compara la transcripción con la referencia palabra por palabra.
     * Se usa solo la primera alternativa (lo que el reconocedor cree que se dijo):
     * buscar entre las cinco la más parecida a la frase aprobaba pronunciaciones
     * malas en cuanto alguna alternativa coincidía.
     *
     * @param alternativas lo que oyó el reconocedor y su confianza (0–1, o
     *   negativa si el motor no la reporta), en el orden del reconocedor.
     */
    fun evaluarTranscripcion(referencia: String, alternativas: List<Pair<String, Float>>): EvaluacionVoz {
        val ref = Texto.palabras(referencia)
        // El reconocedor escribe "7" donde la frase dice "seven": se compara la forma hablada.
        fun dichasDe(texto: String) = Texto.numerosEnPalabras(Texto.palabras(texto))
        val texto = alternativas.firstOrNull { it.first.isNotBlank() }?.first
            ?: return EvaluacionVoz(0, 0, ref.map { PalabraEvaluada(it, 0, "Omission") }, "", FuenteEvaluacion.DISPOSITIVO)
        val dichas = Texto.coincidencias(ref, dichasDe(texto))
        return EvaluacionVoz(
            puntaje = (Texto.similitud(ref, dichasDe(texto)) * 100).roundToInt(),
            confianza = confianzaDelAudio(alternativas),
            palabras = ref.mapIndexed { i, palabra ->
                if (dichas[i]) PalabraEvaluada(palabra, 100) else PalabraEvaluada(palabra, 0, "Mispronunciation")
            },
            transcripcion = texto,
            fuente = FuenteEvaluacion.DISPOSITIVO
        )
    }

    /**
     * Chat en vivo sin Azure: el teléfono transcribe lo que se dijo para poder conversar, pero
     * no califica la pronunciación. Sin palabras evaluadas la pantalla no colorea nada.
     */
    fun soloTranscripcion(alternativas: List<Pair<String, Float>>): EvaluacionVoz {
        val texto = alternativas.firstOrNull { it.first.isNotBlank() }?.first.orEmpty()
        return EvaluacionVoz(
            puntaje = 0,
            confianza = if (texto.isBlank()) 0 else confianzaDelAudio(alternativas),
            palabras = emptyList(),
            transcripcion = texto,
            fuente = FuenteEvaluacion.DISPOSITIVO
        )
    }

    /**
     * La legibilidad es del audio, no de la alternativa: Google solo reporta la confianza de la
     * primera y deja las demás en 0.0, así que se toma la mayor. Muchos motores no reportan
     * confianza (-1) o la dejan en 0.0 aunque sí entendieron: si oyó algo, se considera legible.
     */
    private fun confianzaDelAudio(alternativas: List<Pair<String, Float>>): Int {
        val confianza = alternativas.maxOfOrNull { it.second } ?: -1f
        return if (confianza > 0f) (confianza * 100).roundToInt() else 100
    }
}
