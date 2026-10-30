package com.xpspeak.app.feature.account.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.account.data.CuentaRepository
import com.xpspeak.app.feature.auth.data.UsuarioDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EliminacionUiState(
    /** Nivel MCER actual del usuario, para decir qué progreso se pierde (QA 29-oct: decía siempre A1). */
    val nivel: String = "A1",
    val eliminando: Boolean = false,
    val error: String? = null,
    val eliminada: Boolean = false
)

@HiltViewModel
class ConfirmarEliminacionViewModel @Inject constructor(
    private val repositorio: CuentaRepository,
    sesion: SesionActual,
    usuarioDao: UsuarioDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EliminacionUiState())
    val uiState: StateFlow<EliminacionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val nivel = sesion.uid?.let { usuarioDao.buscarPorUid(it)?.nivel } ?: "A1"
            _uiState.value = _uiState.value.copy(nivel = nivel)
        }
    }

    fun eliminar() {
        if (_uiState.value.eliminando) return
        viewModelScope.launch {
            val nivel = _uiState.value.nivel
            _uiState.value = EliminacionUiState(nivel = nivel, eliminando = true)
            _uiState.value = repositorio.eliminarCuenta().fold(
                onSuccess = { EliminacionUiState(nivel = nivel, eliminada = true) },
                onFailure = { EliminacionUiState(nivel = nivel, error = it.message ?: "No se pudo eliminar la cuenta.") }
            )
        }
    }
}
