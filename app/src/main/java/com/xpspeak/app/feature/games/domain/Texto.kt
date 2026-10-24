package com.xpspeak.app.feature.games.domain

import java.text.Normalizer

/** Comparación de textos tolerante a mayúsculas, apóstrofos y puntuación. */
object Texto {

    /** Igual que normalizarTexto del backend: minúsculas, apóstrofo recto y sin puntuación final. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFC)
            .lowercase()
            .replace(Regex("[‘’ʼ]"), "'")
            .replace(Regex("\\s+"), " ")
            .trim()
            .replace(Regex("[.!?¡¿]+$"), "")
            .trim()

    /** Palabras sin ninguna puntuación, para comparar lo que se dijo o escribió con la referencia. */
    fun palabras(texto: String): List<String> =
        normalizar(texto)
            .replace(Regex("[^\\p{L}\\p{N}' ]"), " ")
            .split(' ')
            .map { it.trim('\'') }
            .filter { it.isNotEmpty() }

    /**
     * Qué palabras de la referencia aparecen, en orden, en lo dicho (subsecuencia
     * común más larga). Sirve para colorear palabra por palabra en Eco Vocal y
     * para aceptar respuestas escritas con pequeñas diferencias en Misión.
     */
    fun coincidencias(referencia: List<String>, dicho: List<String>): List<Boolean> {
        val n = referencia.size
        val m = dicho.size
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (referencia[i] == dicho[j]) lcs[i + 1][j + 1] + 1 else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val marcadas = BooleanArray(n)
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                referencia[i] == dicho[j] -> { marcadas[i] = true; i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> i++
                else -> j++
            }
        }
        return marcadas.toList()
    }

    /** 0.0–1.0: palabras de la referencia dichas en orden, penalizando las que sobran. */
    fun similitud(referencia: String, dicho: String): Double {
        val ref = palabras(referencia)
        val dichas = palabras(dicho)
        if (ref.isEmpty()) return 0.0
        val aciertos = coincidencias(ref, dichas).count { it }
        return aciertos.toDouble() / maxOf(ref.size, dichas.size)
    }
}
