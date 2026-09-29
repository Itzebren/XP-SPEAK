package com.xpspeak.app.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.auth.data.AuthRepository
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountUiState(
    val correo: String = "",
    val usuario: UsuarioEntity? = null
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
        val correo = user?.email.orEmpty()
        _uiState.value = AccountUiState(correo = correo)

        user?.uid?.let { uid ->
            viewModelScope.launch {
                usuarioDao.observarUsuario(uid).collect { perfil ->
                    _uiState.value = _uiState.value.copy(usuario = perfil)
                }
            }
        }
    }

    fun cerrarSesion() {
        authRepository.cerrarSesion()
    }
}
