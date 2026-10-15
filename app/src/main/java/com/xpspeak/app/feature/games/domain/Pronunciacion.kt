package com.xpspeak.app.feature.games.domain

import kotlin.math.roundToInt

/** Quién evaluó: Azure da puntaje fonético; el teléfono solo compara palabras. */
enum class FuenteEvaluacion { AZURE, DISPOSITIVO }

data class PalabraEvaluada(
    val palabra: String,
    /** 0–100 */
    val puntaje: Int,
    /** "Omission", "Mispronunciation"… de Azure, o null si se dijo bien. */
    val error: String? = null
)

/**
 * Resultado de un intento de Eco Vocal (§5.1).
 *
 * @param puntaje precisión 0–100
 * @param confianza 0–100: qué tan seguro está el reconocedor de lo que oyó.
 *   Por debajo de [Pronunciacion.CONFIANZA_MINIMA] el audio no fue legible
 *   (CU-05 A1, Ilustración 40) y se pide repetir sin gastar el intento.
 */
data class EvaluacionVoz(
    val puntaje: Int,
    val confianza: Int,
    val palabras: List<PalabraEvaluada>,
    val transcripcion: String,
    val fuente: FuenteEvaluacion
)

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

    fun esAcierto(evaluacion: EvaluacionVoz) = esLegible(evaluacion) && evaluacion.puntaje >= UMBRAL_ACIERTO

    /**
     * Respaldo sin conexión: el reconocedor del teléfono no mide fonemas, así
     * que se compara la transcripción con la referencia palabra por palabra
     * y se elige la alternativa que más se parece.
     *
     * @param alternativas lo que oyó el reconocedor y su confianza (0–1, o
     *   negativa si el motor no la reporta).
     */
    fun evaluarTranscripcion(referencia: String, alternativas: List<Pair<String, Float>>): EvaluacionVoz {
        val ref = Texto.palabras(referencia)
        val (texto, confianza) = alternativas.maxByOrNull { Texto.similitud(referencia, it.first) }
            ?: return EvaluacionVoz(0, 0, ref.map { PalabraEvaluada(it, 0, "Omission") }, "", FuenteEvaluacion.DISPOSITIVO)
        val dichas = Texto.coincidencias(ref, Texto.palabras(texto))
        return EvaluacionVoz(
            puntaje = (Texto.similitud(referencia, texto) * 100).roundToInt(),
            // Muchos motores no reportan confianza: si oyó algo, se considera legible.
            confianza = if (confianza >= 0f) (confianza * 100).roundToInt() else 100,
            palabras = ref.mapIndexed { i, palabra ->
                if (dichas[i]) PalabraEvaluada(palabra, 100) else PalabraEvaluada(palabra, 0, "Mispronunciation")
            },
            transcripcion = texto,
            fuente = FuenteEvaluacion.DISPOSITIVO
        )
    }
}
