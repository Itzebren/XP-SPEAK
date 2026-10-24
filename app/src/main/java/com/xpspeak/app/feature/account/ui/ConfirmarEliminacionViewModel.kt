package com.xpspeak.app.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.account.data.CuentaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EliminacionUiState(
    val eliminando: Boolean = false,
    val error: String? = null,
    val eliminada: Boolean = false
)

@HiltViewModel
class ConfirmarEliminacionViewModel @Inject constructor(
    private val repositorio: CuentaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EliminacionUiState())
    val uiState: StateFlow<EliminacionUiState> = _uiState.asStateFlow()

    fun eliminar() {
        if (_uiState.value.eliminando) return
        viewModelScope.launch {
            _uiState.value = EliminacionUiState(eliminando = true)
            _uiState.value = repositorio.eliminarCuenta().fold(
                onSuccess = { EliminacionUiState(eliminada = true) },
                onFailure = { EliminacionUiState(error = it.message ?: "No se pudo eliminar la cuenta.") }
            )
        }
    }
}
