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

    /** Sonidos de la palabra que bajaron su puntaje o que sonaron como otro, del peor al mejor. */
    fun fonemasAPracticar(palabra: PalabraEvaluada): List<FonemaEvaluado> =
        palabra.fonemas
            .filter(::esFonemaDebil)
            .sortedWith(compareBy({ it.oido == null }, { it.puntaje }))

    private fun esFonemaDebil(f: FonemaEvaluado) = Pronunciacion.nivelDe(f.puntaje) != Pronunciacion.Nivel.BUENO || f.oido != null

    fun descripcion(palabra: PalabraEvaluada): String = when {
        palabra.error == "Omission" && palabra.dichaEnSuLugar != null ->
            "Dijiste «${palabra.dichaEnSuLugar}» en lugar de «${palabra.palabra}»."
        palabra.error == "Omission" -> "No la escuchamos: parece que te la saltaste."
        // AnalisisFonemas detectó el sonido concreto: "/ɹ/ sonó como /d/".
        palabra.sonidos.isNotEmpty() -> "Fíjate en el sonido: " + palabra.sonidos.joinToString("; ") + "."
        nivelDe(palabra) == Pronunciacion.Nivel.BUENO -> "¡Bien pronunciada!"
        nivelDe(palabra) == Pronunciacion.Nivel.REGULAR -> "Casi: se entiende, pero puede sonar más clara."
        else -> "Hay que practicarla: escúchala y repítela despacio."
    }

    /** Pausas mal puestas en la palabra (prosodia de Azure). */
    fun notasRitmo(palabra: PalabraEvaluada): List<String> = listOfNotNull(
        "Hiciste una pausa antes de esta palabra; dila unida a la anterior.".takeIf { palabra.pausaInesperada },
        "Aquí va una pequeña pausa (por ejemplo, en una coma).".takeIf { palabra.faltaPausa }
    )

    /** Encabezado de la tarjeta según el puntaje: lo primero que se lee. */
    fun titulo(evaluacion: EvaluacionVoz): String = when {
        evaluacion.soloTranscrita -> "Te entendimos"
        evaluacion.puntaje >= 90 && palabrasAPracticar(evaluacion).isEmpty() -> "¡Excelente pronunciación!"
        Pronunciacion.esAcierto(evaluacion) -> "¡Bien hecho!"
        evaluacion.puntaje >= 60 -> "¡Vas bien! Puedes mejorar"
        else -> "Sigue practicando"
    }

    /** Una métrica de frase con lo que significa, para que el número no quede suelto. */
    data class Metrica(val nombre: String, val valor: Int, val significado: String)

    fun metricas(evaluacion: EvaluacionVoz): List<Metrica> {
        if (evaluacion.soloTranscrita) return emptyList()
        return listOfNotNull(
            Metrica("Precisión", evaluacion.puntaje, "Qué tan claros salieron tus sonidos."),
            evaluacion.fluidez?.let { Metrica("Fluidez", it, "Si hablaste de corrido, sin pausas de más.") },
            evaluacion.completitud?.let { Metrica("Completitud", it, "Cuántas palabras de la frase dijiste.") },
            evaluacion.prosodia?.let { Metrica("Entonación", it, "Ritmo, acento y melodía, como un nativo.") },
            evaluacion.puntajeGeneral?.let { Metrica("General", it, "Todo junto, según el servicio de voz.") }
        )
    }

    /** Un sonido que conviene practicar, con dónde salió mal y qué se oyó en su lugar. */
    data class SonidoAPracticar(val fonema: String, val puntaje: Int, val oido: String?, val palabras: List<String>)

    /** Los sonidos más débiles de toda la frase (peor primero), juntando los repetidos. */
    fun sonidosAPracticar(evaluacion: EvaluacionVoz, maximo: Int = 3): List<SonidoAPracticar> =
        evaluacion.palabras
            .flatMap { p -> p.fonemas.filter(::esFonemaDebil).map { p.palabra to it } }
            .groupBy { it.second.fonema }
            .map { (fonema, casos) ->
                val peor = casos.minBy { it.second.puntaje }.second
                SonidoAPracticar(fonema, peor.puntaje, casos.firstNotNullOfOrNull { it.second.oido }, casos.map { it.first }.distinct())
            }
            .sortedBy { it.puntaje }
            .take(maximo)

    /** Qué hacer para subir el puntaje de la frase, además de las palabras en rojo. */
    fun consejosFrase(evaluacion: EvaluacionVoz): List<String> {
        if (evaluacion.soloTranscrita) return emptyList()
        return listOfNotNull(
            evaluacion.completitud?.takeIf { it < Pronunciacion.UMBRAL_ACIERTO }
                ?.let { "Te faltaron palabras: di la frase completa." },
            evaluacion.palabrasDeMas.takeIf { it.isNotEmpty() }
                ?.let { "Dijiste de más: " + it.joinToString(", ") { p -> "«$p»" } + "." },
            evaluacion.fluidez?.takeIf { it < Pronunciacion.UMBRAL_ACIERTO }
                ?.let { "Intenta decirla de corrido, sin detenerte entre palabras." },
            "Tu entonación sonó plana: sube y baja la voz y marca la sílaba fuerte de cada palabra."
                // Una o dos palabras no tienen «melodía» de frase que corregir.
                .takeIf { evaluacion.palabras.size >= 3 && (evaluacion.monotono || (evaluacion.prosodia ?: 100) < Pronunciacion.UMBRAL_ACIERTO) }
        )
    }

    /** Si el audio llegó con ruido o muy bajo, se dice para que el usuario no crea que fue su pronunciación. */
    fun avisoAudio(evaluacion: EvaluacionVoz): String? = when {
        evaluacion.vozBaja && Pronunciacion.esAcierto(evaluacion) ->
            "Te oímos bajito, pero se entendió bien. Para un puntaje más preciso, acerca el micrófono a un palmo de tu boca."
        evaluacion.vozBaja -> "Te oímos muy bajito. Acerca el micrófono (a un palmo de tu boca) y habla un poco más fuerte."
        evaluacion.audioDificil -> "Había mucho ruido de fondo. En un lugar más callado tu puntaje será más preciso."
        else -> null
    }

    fun resumen(evaluacion: EvaluacionVoz): String {
        if (evaluacion.soloTranscrita) {
            return "Te entendimos, pero sin el servicio de voz no podemos calificar la pronunciación."
        }
        val pendientes = palabrasAPracticar(evaluacion)
        return when {
            pendientes.isEmpty() -> "Pronunciaste bien todas las palabras."
            pendientes.size == 1 && evaluacion.puntaje >= Pronunciacion.UMBRAL_ACIERTO -> "Muy bien. Practica la palabra «${pendientes[0].palabra}»."
            pendientes.size == 1 -> "Practica la palabra «${pendientes[0].palabra}»."
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
