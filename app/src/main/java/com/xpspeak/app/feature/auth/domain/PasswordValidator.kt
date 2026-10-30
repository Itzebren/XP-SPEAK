package com.xpspeak.app.feature.auth.domain

/**
 * RNF-06 Seguridad: Firebase por defecto solo exige 6 caracteres.
 * Este validador corre en la UI ANTES de mandar la contraseña a Firebase,
 * para exigir un mínimo más estricto.
 */
object PasswordValidator {
    const val LONGITUD_MINIMA = 8

    fun esValida(password: String): Boolean = faltantes(password).isEmpty()

    /** Lo que le falta a la contraseña, en palabras: "8 caracteres", "una letra", "un número". */
    fun faltantes(password: String): List<String> = buildList {
        if (password.length < LONGITUD_MINIMA) add("$LONGITUD_MINIMA caracteres")
        if (password.none { it.isLetter() }) add("una letra")
        if (password.none { it.isDigit() }) add("un número")
    }

    /** "Tu contraseña debe contener al menos: una letra y un número.", o null si ya es válida. */
    fun mensajeError(password: String): String? {
        val faltan = faltantes(password).ifEmpty { return null }
        val lista = if (faltan.size == 1) faltan[0] else faltan.dropLast(1).joinToString(", ") + " y " + faltan.last()
        return "Tu contraseña debe contener al menos: $lista."
    }
}
