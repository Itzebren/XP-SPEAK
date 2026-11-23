package com.xpspeak.app.core.sesion

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Usuario con sesión y su ID token de Firebase para el backend. Es una
 * interfaz para que los repositorios nuevos se prueben con fakes, sin Firebase.
 */
interface SesionActual {
    /** UID de Firebase, o null si no hay sesión. */
    val uid: String?

    /** "Bearer <ID token>" para el header Authorization. */
    suspend fun autorizacion(): String
}

class SesionFirebase @Inject constructor(private val firebaseAuth: FirebaseAuth) : SesionActual {
    override val uid: String? get() = firebaseAuth.currentUser?.uid

    override suspend fun autorizacion(): String {
        val usuario = firebaseAuth.currentUser ?: throw Exception("Inicia sesión para ver tu progreso.")
        val token = usuario.getIdToken(false).await().token
            ?: throw Exception("No se pudo obtener la sesión. Vuelve a iniciar sesión.")
        return "Bearer $token"
    }
}
