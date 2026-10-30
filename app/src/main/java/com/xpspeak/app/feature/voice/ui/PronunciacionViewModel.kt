package com.xpspeak.app.feature.voice.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.games.data.voz.EvaluadorVoz
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PronunciacionUiState(
    val frases: List<String> = Retroalimentacion.frasesDe("A1"),
    val indiceFrase: Int = 0,
    val evaluando: Boolean = false,
    val resultado: EvaluacionVoz? = null,
    val error: String? = null
) {
    val textoReferencia: String get() = frases[indiceFrase % frases.size]
}

@HiltViewModel
class PronunciacionViewModel @Inject constructor(
    private val evaluador: EvaluadorVoz,
    private val sesion: SesionActual,
    private val usuarioDao: UsuarioDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(PronunciacionUiState())
    val uiState: StateFlow<PronunciacionUiState> = _uiState.asStateFlow()

    init {
        // RN-02: las frases siguen el nivel elegido al registrarse.
        viewModelScope.launch {
            val nivel = sesion.uid?.let { usuarioDao.buscarPorUid(it)?.nivel } ?: "A1"
            _uiState.value = _uiState.value.copy(frases = Retroalimentacion.frasesDe(nivel), indiceFrase = 0)
        }
    }

    fun iniciarEvaluacion() {
        if (_uiState.value.evaluando) return
        _uiState.value = _uiState.value.copy(evaluando = true, error = null, resultado = null)

        viewModelScope.launch {
            try {
                // Con Azure: fonema por fonema; sin Azure: el reconocedor del teléfono.
                val resultado = evaluador.evaluar(_uiState.value.textoReferencia, medida = "voz.pronunciacion")
                _uiState.value = if (Retroalimentacion.esLegible(resultado)) {
                    _uiState.value.copy(evaluando = false, resultado = resultado)
                } else {
                    // CU-05 A3: no se califica un audio que no se entendió.
                    _uiState.value.copy(evaluando = false, error = "No te entendimos bien. Acércate al micrófono y repite la frase.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                _uiState.value = _uiState.value.copy(evaluando = false, error = "Permite el uso del micrófono para practicar.")
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(evaluando = false, error = error.message)
            }
        }
    }

    fun siguienteFrase() {
        if (_uiState.value.evaluando) return
        // RF-09: la retroalimentación es efímera; al cambiar de frase se descarta.
        _uiState.value = _uiState.value.copy(
            indiceFrase = (_uiState.value.indiceFrase + 1) % _uiState.value.frases.size,
            resultado = null,
            error = null
        )
    }
}
