package com.xpspeak.app.feature.auth.data

import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Sesión guardada que Firebase ya no acepta (p. ej. venía del emulador de Auth o la cuenta
 * se borró): la app debe pedir entrar de nuevo, pero sin red debe seguir funcionando offline.
 */
class AuthRepositoryTest {

    @Test
    fun `refresh token rechazado como error interno es sesion caducada`() {
        // Así lo reporta el SDK en el logcat: "An internal error has occurred. [ INVALID_REFRESH_TOKEN ]"
        val error = FirebaseException("An internal error has occurred. [ INVALID_REFRESH_TOKEN ]")
        assertTrue(AuthRepository.esSesionCaducada(error))
    }

    @Test
    fun `usuario invalido es sesion caducada`() {
        val error = FirebaseAuthInvalidUserException("ERROR_USER_TOKEN_EXPIRED", "The user's credential is no longer valid.")
        assertTrue(AuthRepository.esSesionCaducada(error))
    }

    @Test
    fun `sin red no se cierra la sesion`() {
        val error = FirebaseNetworkException("A network error (such as timeout, interrupted connection or unreachable host) has occurred.")
        assertFalse(AuthRepository.esSesionCaducada(error))
    }

    @Test
    fun `otros errores no cierran la sesion`() {
        assertFalse(AuthRepository.esSesionCaducada(IllegalStateException("No hay sesión guardada.")))
    }
}
