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
            repository.catalogo()
                .onSuccess { catalogo ->
                    _uiState.value = _uiState.value.copy(
                        cargando = false,
                        nivel = catalogo.nivel,
                        lecciones = catalogo.lecciones
                    )
                }
                .onFailure { excepcion ->
                    _uiState.value = _uiState.value.copy(cargando = false, error = excepcion.message)
                }
            repository.conceptosDebiles().onSuccess { dto ->
                _uiState.value = _uiState.value.copy(conceptosDebiles = dto.conceptos)
            }
        }
    }
}
