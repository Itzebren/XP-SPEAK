package com.xpspeak.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.auth.data.RecuperacionRepository
import com.xpspeak.app.feature.auth.domain.PasswordValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PasoRecuperacion { CORREO, CODIGO }

data class RecuperarUiState(
    val paso: PasoRecuperacion = PasoRecuperacion.CORREO,
    val correo: String = "",
    val codigo: String = "",
    val nuevaPassword: String = "",
    val confirmarPassword: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val completado: Boolean = false
)

@HiltViewModel
class RecuperarAccesoViewModel @Inject constructor(
    private val repository: RecuperacionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecuperarUiState())
    val uiState: StateFlow<RecuperarUiState> = _uiState.asStateFlow()

    fun onCorreoChange(valor: String) {
        _uiState.value = _uiState.value.copy(correo = valor, error = null)
    }

    fun onCodigoChange(valor: String) {
        _uiState.value = _uiState.value.copy(codigo = valor, error = null)
    }

    fun onNuevaPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(nuevaPassword = valor, error = null)
    }

    fun onConfirmarPasswordChange(valor: String) {
        _uiState.value = _uiState.value.copy(confirmarPassword = valor, error = null)
    }

    fun solicitarCodigo() {
        val correo = _uiState.value.correo
        if (correo.isBlank() || !correo.contains("@")) {
            _uiState.value = _uiState.value.copy(error = "Ingresa un correo válido.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            repository.enviarCodigo(correo)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(cargando = false, paso = PasoRecuperacion.CODIGO)
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }

    fun confirmarNuevaPassword() {
        val estado = _uiState.value

        if (estado.codigo.isBlank()) {
            _uiState.value = estado.copy(error = "Ingresa el código que recibiste por correo.")
            return
        }
        if (!PasswordValidator.esValida(estado.nuevaPassword)) {
            _uiState.value = estado.copy(error = PasswordValidator.mensajeError())
            return
        }
        if (estado.nuevaPassword != estado.confirmarPassword) {
            _uiState.value = estado.copy(error = "Las contraseñas no coinciden.")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            repository.verificarCodigo(estado.correo, estado.codigo, estado.nuevaPassword)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(cargando = false, completado = true)
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
        }
    }
}
