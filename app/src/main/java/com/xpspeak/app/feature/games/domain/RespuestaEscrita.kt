package com.xpspeak.app.feature.games.domain

/**
 * Respuesta escrita en Misión Situacional (§5.2 paso 3): a qué opción del paso
 * se parece lo que escribió el usuario. La correcta se compara con su frase y
 * con sus [OpcionMision.aceptadas] ("No, thanks." vale igual que
 * "No, I don't. Thank you!"), y las contracciones cuentan como su forma larga.
 */
object RespuestaEscrita {
    /** Parecido mínimo con una frase para darla por dicha (tolera una palabra distinta en frases largas). */
    const val SIMILITUD_MINIMA = 0.8

    /** Índice de la opción a la que corresponde [texto], o null si no se parece a ninguna. */
    fun opcionDe(opciones: List<OpcionMision>, texto: String): Int? {
        val escrito = sinContracciones(Texto.palabras(texto))
        if (escrito.isEmpty()) return null
        return opciones.indices
            .map { i -> i to parecido(opciones[i], escrito) }
            .filter { (_, parecido) -> parecido >= SIMILITUD_MINIMA }
            // Empate: gana la correcta (una aceptada puede parecerse a una incorrecta).
            .maxWithOrNull(compareBy({ it.second }, { opciones[it.first].correcta }))
            ?.first
    }

    private fun parecido(opcion: OpcionMision, escrito: List<String>): Double {
        val frases = listOf(opcion.en) + if (opcion.correcta) opcion.aceptadas.orEmpty() else emptyList()
        return frases.maxOf { Texto.similitud(sinContracciones(Texto.palabras(it)), escrito) }
    }

    fun sinContracciones(palabras: List<String>): List<String> = palabras.flatMap { CONTRACCIONES[it] ?: listOf(it) }

    private val CONTRACCIONES: Map<String, List<String>> = mapOf(
        "i'm" to "i am", "you're" to "you are", "we're" to "we are", "they're" to "they are",
        "it's" to "it is", "that's" to "that is", "what's" to "what is", "there's" to "there is",
        "he's" to "he is", "she's" to "she is", "name's" to "name is",
        "i've" to "i have", "you've" to "you have", "we've" to "we have", "they've" to "they have",
        "i'd" to "i would", "you'd" to "you would", "i'll" to "i will", "you'll" to "you will",
        "don't" to "do not", "doesn't" to "does not", "didn't" to "did not",
        "isn't" to "is not", "aren't" to "are not", "wasn't" to "was not", "weren't" to "were not",
        "haven't" to "have not", "hasn't" to "has not", "can't" to "can not", "cannot" to "can not",
        "won't" to "will not", "shouldn't" to "should not", "couldn't" to "could not"
    ).mapValues { it.value.split(' ') }
}
