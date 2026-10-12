package com.xpspeak.app.feature.auth.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * RF-01 (Registro) y RF-02 (Login) vía Firebase Authentication.
 * La app nunca almacena ni maneja contraseñas directamente.
 */
open class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth?
) {
    // Constructor secundario protegido para pruebas unitarias sin Dagger
    protected constructor() : this(null)

    open val usuarioActual: FirebaseUser?
        get() = firebaseAuth?.currentUser

    open suspend fun registrar(correo: String, password: String): Result<FirebaseUser> = runCatching {
        val auth = requireNotNull(firebaseAuth) { "FirebaseAuth no está inicializado" }
        val resultado = auth.createUserWithEmailAndPassword(correo, password).await()
        resultado.user ?: throw IllegalStateException("No se pudo crear el usuario")
    }

    open suspend fun iniciarSesion(correo: String, password: String): Result<FirebaseUser> = runCatching {
        val auth = requireNotNull(firebaseAuth) { "FirebaseAuth no está inicializado" }
        val resultado = auth.signInWithEmailAndPassword(correo, password).await()
        resultado.user ?: throw IllegalStateException("No se pudo iniciar sesión")
    }

    open fun cerrarSesion() {
        firebaseAuth?.signOut()
    }
}
