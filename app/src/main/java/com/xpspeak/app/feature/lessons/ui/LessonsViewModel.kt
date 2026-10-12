package com.xpspeak.app.feature.lessons.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.lessons.data.ConceptoDebilDto
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto
import com.xpspeak.app.feature.lessons.data.LeccionesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LessonsUiState(
    val cargando: Boolean = false,
    val nivel: String = "",
    val lecciones: List<LeccionResumenDto> = emptyList(),
    val conceptosDebiles: List<ConceptoDebilDto> = emptyList(),
    /** El catálogo es el último guardado: no hay conexión con el servidor. */
    val sinConexion: Boolean = false,
    /** Intentos calificados sin conexión que aún no llegan al servidor. */
    val intentosPendientes: Int = 0,
    /** Conceptos que SM-2 marca para repasar hoy (RF-13). */
    val repasoPendiente: Int = 0,
    val error: String? = null
)

@HiltViewModel
class LessonsViewModel @Inject constructor(
    private val repository: LeccionesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonsUiState())
    val uiState: StateFlow<LessonsUiState> = _uiState.asStateFlow()

    /** Se llama cada vez que se entra a la pantalla, para reflejar desbloqueos nuevos. */
    fun cargar() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val catalogo = repository.catalogo()
                .onSuccess { catalogo ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        nivel = catalogo.dto.nivel,
                        lecciones = catalogo.dto.lecciones,
                        sinConexion = catalogo.sinConexion,
                        intentosPendientes = catalogo.intentosPendientes
                    )
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
                .getOrNull()
            if (catalogo == null || catalogo.sinConexion) return@launch

            repository.conceptosDebiles().onSuccess { dto ->
                _uiState.value = _uiState.value.copy(conceptosDebiles = dto.conceptos)
            }
            repository.sesionRepaso().onSuccess { sesion ->
                _uiState.value = _uiState.value.copy(repasoPendiente = sesion.totalVencidos)
            }
            // En segundo plano: deja descargadas las lecciones del nivel para usarlas sin conexión.
            repository.sincronizarContenido()
        }
    }
}
