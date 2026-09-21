package com.xpspeak.app.feature.auth.domain

/**
 * RNF-06 Seguridad: Firebase por defecto solo exige 6 caracteres.
 * Este validador corre en la UI ANTES de mandar la contraseña a Firebase,
 * para exigir un mínimo más estricto.
 */
object PasswordValidator {
    fun esValida(password: String): Boolean {
        val tieneLongitudMinima = password.length >= 8
        val tieneLetra = password.any { it.isLetter() }
        val tieneNumero = password.any { it.isDigit() }
        return tieneLongitudMinima && tieneLetra && tieneNumero
    }

    fun mensajeError(): String =
        "La contraseña debe tener al menos 8 caracteres, incluir una letra y un número."
}
