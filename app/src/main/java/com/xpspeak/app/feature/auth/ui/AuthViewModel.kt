package com.xpspeak.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.xpspeak.app.feature.auth.data.AuthRepository
import com.xpspeak.app.feature.auth.domain.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, REGISTRO }

data class AuthUiState(
    val modo: AuthMode = AuthMode.LOGIN,
    val correo: String = "",
    val password: String = "",
    val confirmarPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val exito: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun cambiarModo(modo: AuthMode) {
        _uiState.value = _uiState.value.copy(modo = modo, error = null)
    }

    fun onCorreoChange(valor: String) {
        _uiState.value = _uiState.value.copy(correo = valor, error = null)
    }

    fun onPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(password = valor, error = null)
    }

    fun onConfirmarPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(confirmarPassword = valor, error = null)
    }

    fun enviar() {
        val estado = _uiState.value

        if (estado.correo.isBlank() || !estado.correo.contains("@")) {
            _uiState.value = estado.copy(error = "Ingresa un correo válido.")
            return
        }

        if (estado.modo == AuthMode.REGISTRO) {
            if (!PasswordValidator.esValida(estado.password)) {
                _uiState.value = estado.copy(error = PasswordValidator.mensajeError())
                return
            }
            if (estado.password != estado.confirmarPassword) {
                _uiState.value = estado.copy(error = "Las contraseñas no coinciden.")
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)

            val resultado = if (estado.modo == AuthMode.REGISTRO) {
                repository.registrar(estado.correo, estado.password)
            } else {
                repository.iniciarSesion(estado.correo, estado.password)
            }

            resultado
                .onSuccess {
                    _uiState.value = _uiState.value.copy(cargando = false, exito = true)
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = mapearError(excepcion))
                }
        }
    }

    private fun mapearError(excepcion: Throwable): String = when (excepcion) {
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        else -> "Ocurrió un error. Intenta de nuevo."
    }
}
