package com.xpspeak.app.feature.auth.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.xpspeak.app.feature.auth.data.PerfilRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NivelUiState(
    val guardando: Boolean = false,
    val listo: Boolean = false
)

@HiltViewModel
class NivelViewModel @Inject constructor(
    private val perfilRepository: PerfilRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(NivelUiState())
    val uiState: StateFlow<NivelUiState> = _uiState.asStateFlow()

    fun seleccionarNivel(nivel: String) {
        val usuario = firebaseAuth.currentUser ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(guardando = true)
            perfilRepository.crearPerfil(
                uid = usuario.uid,
                correo = usuario.email.orEmpty(),
                nivel = nivel
            )
            _uiState.value = _uiState.value.copy(guardando = false, listo = true)
        }
    }
}
