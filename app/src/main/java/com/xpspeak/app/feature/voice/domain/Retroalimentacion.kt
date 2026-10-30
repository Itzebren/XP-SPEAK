package com.xpspeak.app.feature.voice.domain

import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FonemaEvaluado
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Pronunciacion

/**
 * RF-09: retroalimentación inmediata y efímera de la pronunciación, en tres niveles:
 * frase (puntaje, fluidez, completitud), palabra (verde/amarillo/rojo) y sonido (IPA).
 * Usa los mismos umbrales que Eco Vocal (Pronunciacion) para que los colores
 * signifiquen lo mismo en toda la aplicación. Nada de esto se guarda (RN-04).
 */
object Retroalimentacion {

    /** CU-05 A3: con confianza menor al 30 % se pide repetir en lugar de calificar. */
    fun esLegible(evaluacion: EvaluacionVoz) =
        evaluacion.confianza >= Pronunciacion.CONFIANZA_MINIMA && evaluacion.transcripcion.isNotBlank()

    fun nivelDe(palabra: PalabraEvaluada): Pronunciacion.Nivel =
        if (palabra.error == "Omission") Pronunciacion.Nivel.MALO else Pronunciacion.nivelDe(palabra.puntaje)

    /** Palabras que no salieron bien, en el orden de la frase. */
    fun palabrasAPracticar(evaluacion: EvaluacionVoz): List<PalabraEvaluada> =
        evaluacion.palabras.filter { nivelDe(it) != Pronunciacion.Nivel.BUENO }

    /** Sonidos de la palabra que bajaron su puntaje, del peor al mejor. */
    fun fonemasAPracticar(palabra: PalabraEvaluada): List<FonemaEvaluado> =
        palabra.fonemas
            .filter { Pronunciacion.nivelDe(it.puntaje) != Pronunciacion.Nivel.BUENO }
            .sortedBy { it.puntaje }

    fun descripcion(palabra: PalabraEvaluada): String = when {
        palabra.error == "Omission" -> "No la escuchamos: parece que te la saltaste."
        // AnalisisFonemas detectó el sonido concreto: "/ɹ/ sonó como /d/".
        palabra.sonidos.isNotEmpty() -> "Fíjate en el sonido: " + palabra.sonidos.joinToString("; ") + "."
        nivelDe(palabra) == Pronunciacion.Nivel.BUENO -> "¡Bien pronunciada!"
        nivelDe(palabra) == Pronunciacion.Nivel.REGULAR -> "Casi: se entiende, pero puede sonar más clara."
        else -> "Hay que practicarla: escúchala y repítela despacio."
    }

    fun resumen(evaluacion: EvaluacionVoz): String {
        if (evaluacion.soloTranscrita) {
            return "Te entendimos, pero sin el servicio de voz no podemos calificar la pronunciación."
        }
        val pendientes = palabrasAPracticar(evaluacion)
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
