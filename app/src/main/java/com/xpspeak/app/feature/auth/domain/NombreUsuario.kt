package com.xpspeak.app.feature.auth.domain

/** Nombre de usuario de Crear cuenta (Ilustración 29) y Ajustes de perfil (Ilustración 35). */
object NombreUsuario {
    const val MIN = 2
    const val MAX = 30

    /** @return el mensaje de error, o null si el nombre sirve. */
    fun validar(nombre: String): String? {
        val limpio = nombre.trim()
        return when {
            limpio.length < MIN -> "Tu nombre debe tener al menos $MIN letras."
            limpio.length > MAX -> "Tu nombre puede tener hasta $MAX caracteres."
            limpio.none { it.isLetter() } -> "Tu nombre debe incluir letras."
            else -> null
        }
    }
}
