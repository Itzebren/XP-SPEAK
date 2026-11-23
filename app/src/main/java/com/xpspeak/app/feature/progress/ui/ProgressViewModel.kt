package com.xpspeak.app.feature.progress.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.feature.progress.data.FuenteProgreso
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoDto
import com.xpspeak.app.feature.progress.data.ProgresoGuardado
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EstadoProgreso {
    data object Cargando : EstadoProgreso

    /**
     * @param desactualizado el último refresco falló: se muestra lo guardado con un aviso
     * @param actualizadoEn cuándo lo confirmó el servidor; null = armado con el perfil local
     */
    data class Listo(val datos: ProgresoDto, val desactualizado: Boolean, val actualizadoEn: Long?) : EstadoProgreso

    /** CU-08 A1: usuario nuevo, indicadores en cero y mensaje motivacional. */
    data class SinActividad(val datos: ProgresoDto, val desactualizado: Boolean, val actualizadoEn: Long?) : EstadoProgreso

    data class Error(val mensaje: String) : EstadoProgreso
}

data class ProgressUiState(
    val estado: EstadoProgreso = EstadoProgreso.Cargando,
    val refrescando: Boolean = false,
    /** Insignia tocada en la cuadrícula: se abre su detalle. */
    val insigniaSeleccionada: InsigniaDto? = null
)

/**
 * Pestaña Progreso (CU-08). Muestra la caché de Room al instante y la
 * refresca con el servidor al entrar y con pull-to-refresh. Sin red ni caché
 * arma lo que se sabe del perfil local y el catálogo empaquetado (RNF-10).
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(private val fuente: FuenteProgreso) : ViewModel() {

    private val _uiState = MutableStateFlow(ProgressUiState())
    val uiState: StateFlow<ProgressUiState> = _uiState.asStateFlow()

    private var guardado: ProgresoGuardado? = null
    private var minimo: ProgresoDto? = null
    /** Mensaje del último refresco fallido; null si fue bien (o aún no termina). */
    private var fallo: String? = null

    init {
        viewModelScope.launch {
            fuente.observar().collect {
                guardado = it
                publicar()
            }
        }
        refrescar()
    }

    fun refrescar() {
        if (_uiState.value.refrescando) return
        _uiState.update { it.copy(refrescando = true) }
        viewModelScope.launch {
            fallo = fuente.refrescar().exceptionOrNull()?.let { it.message ?: MENSAJE_ERROR }
            minimo = if (fallo != null && guardado == null) fuente.estadoMinimo() else null
            _uiState.update { it.copy(refrescando = false) }
            publicar()
        }
    }

    fun seleccionarInsignia(insignia: InsigniaDto?) {
        _uiState.update { it.copy(insigniaSeleccionada = insignia) }
    }

    private fun publicar() {
        val actual = guardado
        val datos = actual?.datos ?: minimo
        val desactualizado = fallo != null
        val estado = when {
            // Un refresco exitoso se refleja cuando Room emite la caché nueva.
            datos == null && fallo == null -> EstadoProgreso.Cargando
            datos == null -> EstadoProgreso.Error(fallo ?: MENSAJE_ERROR)
            datos.sinActividad -> EstadoProgreso.SinActividad(datos, desactualizado, actual?.actualizadoEn)
            else -> EstadoProgreso.Listo(datos, desactualizado, actual?.actualizadoEn)
        }
        _uiState.update { it.copy(estado = estado) }
    }

    private companion object {
        const val MENSAJE_ERROR = "No se pudo cargar tu progreso."
    }
}
