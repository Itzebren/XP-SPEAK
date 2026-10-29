package com.xpspeak.app.feature.games.domain

/** Ráfaga de Palabras con voz: "yes" acepta el par y "no" lo manda al bote. */
object RespuestaVoz {

    private val ACEPTAR = setOf("yes", "yeah", "yep", "yup", "correct", "right", "true", "match", "si", "sí")
    private val DESCARTAR = setOf("no", "nope", "nah", "wrong", "false", "trash", "bote")

    /** Lo que se le pide a Azure que espere (PhraseListGrammar). */
    val FRASES = listOf("yes", "no", "correct", "wrong")

    /** true = coinciden, false = al bote, null = no dijo ninguna de las dos. Cuenta la primera que dijo. */
    fun interpretar(texto: String): Boolean? {
        for (palabra in Texto.palabras(texto)) {
            if (palabra in ACEPTAR) return true
            if (palabra in DESCARTAR) return false
        }
        return null
    }
}
