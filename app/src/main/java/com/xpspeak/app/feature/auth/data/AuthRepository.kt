package com.xpspeak.app.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * RF-01 (Registro) y RF-02 (Login) vía Firebase Authentication.
 * La app nunca almacena ni maneja contraseñas directamente.
 */
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val usuarioActual: FirebaseUser?
        get() = firebaseAuth.currentUser

    suspend fun registrar(correo: String, password: String): Result<FirebaseUser> = runCatching {
        val resultado = firebaseAuth.createUserWithEmailAndPassword(correo, password).await()
        resultado.user ?: throw IllegalStateException("No se pudo crear el usuario")
    }

    suspend fun iniciarSesion(correo: String, password: String): Result<FirebaseUser> = runCatching {
        val resultado = firebaseAuth.signInWithEmailAndPassword(correo, password).await()
        resultado.user ?: throw IllegalStateException("No se pudo iniciar sesión")
    }

    fun cerrarSesion() = firebaseAuth.signOut()
}
