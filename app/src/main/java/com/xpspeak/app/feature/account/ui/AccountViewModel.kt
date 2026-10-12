package com.xpspeak.app.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.xpspeak.app.feature.auth.data.AuthRepository
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import com.xpspeak.app.feature.auth.domain.NombreUsuario
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val correo: String = "",
    /** displayName de Firebase: el nombre de usuario de Crear cuenta. */
    val nombre: String = "",
    val usuario: UsuarioEntity? = null,
    val guardando: Boolean = false,
    /** Confirmación o error del último cambio de nombre o correo. */
    val mensaje: String? = null,
    val esError: Boolean = false
)

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val usuarioDao: UsuarioDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountUiState())
    val uiState: StateFlow<AccountUiState> = _uiState.asStateFlow()

    init {
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val user = authRepository.usuarioActual
        _uiState.value = AccountUiState(correo = user?.email.orEmpty(), nombre = user?.displayName.orEmpty())

        user?.uid?.let { uid ->
            viewModelScope.launch {
                usuarioDao.observarUsuario(uid).collect { perfil ->
                    _uiState.value = _uiState.value.copy(usuario = perfil)
                }
            }
        }
    }

    fun guardarNombre(nombre: String) {
        NombreUsuario.validar(nombre)?.let { return avisar(it, error = true) }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(guardando = true, mensaje = null)
            authRepository.cambiarNombre(nombre)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(nombre = nombre.trim())
                    avisar("Guardamos tu nombre.")
                }
                .onFailure { avisar("No se pudo guardar tu nombre. Revisa tu conexión.", error = true) }
        }
    }

    fun cambiarCorreo(nuevo: String) {
        val correo = nuevo.trim()
        if (!correo.contains("@") || correo.contains(" ")) return avisar("Ingresa un correo válido.", error = true)
        if (correo.equals(_uiState.value.correo, ignoreCase = true)) return avisar("Ese ya es tu correo.", error = true)
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(guardando = true, mensaje = null)
            authRepository.cambiarCorreo(correo)
                .onSuccess { avisar("Te enviamos un enlace a $correo. Tu correo cambia cuando lo abras.") }
                .onFailure { avisar(mensajeErrorCorreo(it), error = true) }
        }
    }

    fun cambiarNivel(nivel: String) {
        val uid = authRepository.usuarioActual?.uid ?: return
        viewModelScope.launch { usuarioDao.actualizarNivel(uid, nivel) }
    }

    fun cambiarAvatar(avatar: String) {
        val uid = authRepository.usuarioActual?.uid ?: return
        viewModelScope.launch { usuarioDao.actualizarAvatar(uid, avatar) }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }

    private fun avisar(mensaje: String, error: Boolean = false) {
        _uiState.value = _uiState.value.copy(guardando = false, mensaje = mensaje, esError = error)
    }

    private fun mensajeErrorCorreo(error: Throwable): String = when (error) {
        is FirebaseAuthRecentLoginRequiredException -> "Por seguridad, cierra sesión y vuelve a entrar antes de cambiar tu correo."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthInvalidCredentialsException -> "Ingresa un correo válido."
        else -> "No se pudo cambiar tu correo. Revisa tu conexión."
    }
}
