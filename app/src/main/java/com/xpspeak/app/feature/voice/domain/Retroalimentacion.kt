package com.xpspeak.app.feature.voice.domain

import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.FonemaEvaluado
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.PalabraEvaluada
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.ResultadoPronunciacion
import kotlin.math.roundToInt

/**
 * RF-09: retroalimentación inmediata y efímera de la práctica de pronunciación.
 * Usa los mismos umbrales que Eco Vocal (Pronunciacion) para que el verde,
 * amarillo y rojo signifiquen lo mismo en toda la aplicación. Nada de esto se
 * guarda (RN-04).
 */
object Retroalimentacion {

    /** CU-05 A3: con confianza menor al 30 % se pide repetir en lugar de calificar. */
    fun esLegible(resultado: ResultadoPronunciacion) =
        resultado.confianza >= Pronunciacion.CONFIANZA_MINIMA && resultado.textoReconocido.isNotBlank()

    fun nivelDe(palabra: PalabraEvaluada): Pronunciacion.Nivel =
        if (palabra.errorType == "Omission") Pronunciacion.Nivel.MALO
        else Pronunciacion.nivelDe(palabra.puntuacionPrecision.roundToInt())

    /** Palabras que no salieron bien, en el orden de la frase. */
    fun palabrasAPracticar(resultado: ResultadoPronunciacion): List<PalabraEvaluada> =
        resultado.palabras.filter { nivelDe(it) != Pronunciacion.Nivel.BUENO }

    /** Sonidos de la palabra que bajaron su puntaje, del peor al mejor. */
    fun fonemasAPracticar(palabra: PalabraEvaluada): List<FonemaEvaluado> =
        palabra.fonemas
            .filter { Pronunciacion.nivelDe(it.puntuacionPrecision.roundToInt()) != Pronunciacion.Nivel.BUENO }
            .sortedBy { it.puntuacionPrecision }

    fun descripcion(palabra: PalabraEvaluada): String = when {
        palabra.errorType == "Omission" -> "No la escuchamos: parece que te la saltaste."
        nivelDe(palabra) == Pronunciacion.Nivel.BUENO -> "¡Bien pronunciada!"
        nivelDe(palabra) == Pronunciacion.Nivel.REGULAR -> "Casi: se entiende, pero puede sonar más clara."
        else -> "Hay que practicarla: escúchala y repítela despacio."
    }

    fun resumen(resultado: ResultadoPronunciacion): String {
        val pendientes = palabrasAPracticar(resultado)
        return when {
            pendientes.isEmpty() -> "¡Excelente! Pronunciaste bien todas las palabras."
            pendientes.size == 1 -> "Muy bien. Practica la palabra «${pendientes[0].palabra}»."
            else -> "Practica estas palabras: " + pendientes.joinToString(", ") { "«${it.palabra}»" } + "."
        }
    }

    /** Frases de práctica con el vocabulario de las lecciones de cada nivel (RN-02, RN-03). */
    fun frasesDe(nivel: String): List<String> = if (nivel == "A2") FRASES_A2 else FRASES_A1

    private val FRASES_A1 = listOf(
        "Hello, my name is Anna and I am a student.",
        "I live in a small house with my family.",
        "I usually have breakfast at seven o'clock.",
        "My brother likes to play soccer on the weekend.",
        "Can I have a cup of coffee, please?",
        "It is sunny and hot today.",
        "The bank is next to the supermarket.",
        "I am wearing a blue shirt and black shoes."
    )

    private val FRASES_A2 = listOf(
        "Last weekend I went to the beach with my friends.",
        "I'm going to travel to Mexico City next month.",
        "This city is bigger and noisier than my hometown.",
        "I think you should see a doctor about your cough.",
        "Could you tell me how to get to the train station?",
        "She has worked as a nurse for three years.",
        "When I was a child, I lived near the mountains.",
        "I'm sorry, I can't answer the phone right now."
    )
}
