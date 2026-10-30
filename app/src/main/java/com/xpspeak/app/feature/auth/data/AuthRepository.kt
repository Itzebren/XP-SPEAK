package com.xpspeak.app.feature.auth.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
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

    private companion object {
        /** Logcat: fallos al enviar el correo de verificación (RN-01). */
        const val TAG = "XPS-AUTH"
    }

    open val usuarioActual: FirebaseUser?
        get() = firebaseAuth?.currentUser

    /** [nombre] es el nombre de usuario de la Ilustración 29; se guarda como displayName. */
    open suspend fun registrar(correo: String, password: String, nombre: String? = null): Result<FirebaseUser> = runCatching {
        val auth = requireNotNull(firebaseAuth) { "FirebaseAuth no está inicializado" }
        val resultado = auth.createUserWithEmailAndPassword(correo, password).await()
        val usuario = resultado.user ?: throw IllegalStateException("No se pudo crear el usuario")
        if (!nombre.isNullOrBlank()) {
            usuario.updateProfile(userProfileChangeRequest { displayName = nombre.trim() }).await()
        }
        // RN-01: si el envío falla, la cuenta ya existe; se guarda el motivo para avisarlo
        // y el usuario puede reenviarlo desde el aviso.
        falloEnvioVerificacion = runCatching { usuario.sendEmailVerification().await() }
            .onFailure { Log.w(TAG, "No se envió la verificación al registrarse", it) }
            .exceptionOrNull()
        usuario
    }

    /** Por qué no salió el correo de verificación al registrarse, o null si se envió. */
    open var falloEnvioVerificacion: Throwable? = null
        protected set

    /** RN-01: vuelve a mandar el enlace de verificación al correo de la sesión actual. */
    open suspend fun reenviarVerificacion(): Result<Unit> = runCatching {
        val usuario = requireNotNull(usuarioActual) { "Inicia sesión para verificar tu correo." }
        usuario.sendEmailVerification().await()
        Unit
    }.onFailure { Log.w(TAG, "No se reenvió la verificación", it) }

    /** Recarga el usuario desde Firebase: `isEmailVerified` no cambia solo al abrir el enlace. */
    open suspend fun correoVerificado(): Result<Boolean> = runCatching {
        val usuario = requireNotNull(usuarioActual) { "Inicia sesión para verificar tu correo." }
        usuario.reload().await()
        usuarioActual?.isEmailVerified == true
    }

    /** Ajustes de perfil (Ilustración 35): cambia el nombre que se muestra. */
    open suspend fun cambiarNombre(nombre: String): Result<Unit> = runCatching {
        val usuario = requireNotNull(usuarioActual) { "Inicia sesión para cambiar tu nombre." }
        usuario.updateProfile(userProfileChangeRequest { displayName = nombre.trim() }).await()
    }

    /**
     * Ajustes de perfil: Firebase manda un enlace al correo nuevo y lo cambia hasta que
     * el usuario lo confirma, para que nadie se adueñe de la cuenta con un correo ajeno.
     */
    open suspend fun cambiarCorreo(nuevo: String): Result<Unit> = runCatching {
        val usuario = requireNotNull(usuarioActual) { "Inicia sesión para cambiar tu correo." }
        usuario.verifyBeforeUpdateEmail(nuevo).await()
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
