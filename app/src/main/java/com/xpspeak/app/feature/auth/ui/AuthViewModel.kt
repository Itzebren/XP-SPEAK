package com.xpspeak.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val correo: String = "",
    val nivel: String = "A1",
    val cargando: Boolean = false,
    val registroExitoso: Boolean = false
)

/**
 * ViewModel de ejemplo. Demuestra el flujo completo:
 * UI (Compose) --dispara evento--> ViewModel --llama--> Repository --usa--> DAO/Room
 * y la UI se actualiza automáticamente al observar el StateFlow.
 *
 * La validación real de RF-01/RF-02/RF-03 (formato de correo, contraseña,
 * recuperación de acceso, etc.) se construye en la Fase 2.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onCorreoChange(valor: String) {
        _uiState.value = _uiState.value.copy(correo = valor)
    }

    fun onNivelChange(valor: String) {
        _uiState.value = _uiState.value.copy(nivel = valor)
    }

    fun registrarDemo() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            repository.registrarUsuarioDemo(_uiState.value.correo, _uiState.value.nivel)
            _uiState.value = _uiState.value.copy(cargando = false, registroExitoso = true)
        }
    }
}
