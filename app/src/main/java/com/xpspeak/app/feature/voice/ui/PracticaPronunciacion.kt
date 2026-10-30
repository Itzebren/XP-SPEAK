package com.xpspeak.app.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPFondo
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.games.data.voz.EvaluadorVoz
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Práctica de una frase desde cualquier pantalla (RF-09): las tarjetas y diálogos de una
 * lección, la oración de Orden Maestro o lo que dijiste en Misión Situacional abren una
 * hoja con la frase, su audio y la retroalimentación completa al repetirla.
 */
data class PracticaVozUiState(
    /** Frase abierta en la hoja; null = hoja cerrada. */
    val frase: String? = null,
    val evaluando: Boolean = false,
    val evaluacion: EvaluacionVoz? = null,
    val error: String? = null
)

@HiltViewModel
class PracticaVozViewModel @Inject constructor(private val evaluador: EvaluadorVoz) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticaVozUiState())
    val uiState: StateFlow<PracticaVozUiState> = _uiState.asStateFlow()

    private var escucha: Job? = null

    fun abrir(frase: String) {
        escucha?.cancel()
        _uiState.value = PracticaVozUiState(frase = frase)
    }

    /** RF-09: la retroalimentación es efímera; al cerrar se descarta. */
    fun cerrar() {
        escucha?.cancel()
        _uiState.value = PracticaVozUiState()
    }

    fun hablar() {
        val frase = _uiState.value.frase ?: return
        if (_uiState.value.evaluando) return
        _uiState.value = _uiState.value.copy(evaluando = true, error = null, evaluacion = null)
        escucha = viewModelScope.launch {
            try {
                val evaluacion = evaluador.evaluar(frase, medida = "voz.practica")
                _uiState.value = if (Retroalimentacion.esLegible(evaluacion)) {
                    _uiState.value.copy(evaluando = false, evaluacion = evaluacion)
                } else {
                    // CU-05 A3: no se califica un audio que no se entendió.
                    _uiState.value.copy(evaluando = false, error = "No te entendimos bien. Acércate al micrófono y repite la frase.")
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: SecurityException) {
                _uiState.value = _uiState.value.copy(evaluando = false, error = "Permite el uso del micrófono para practicar.")
            } catch (error: Exception) {
                _uiState.value = _uiState.value.copy(evaluando = false, error = error.message ?: "No se pudo escuchar. Intenta de nuevo.")
            }
        }
    }

    fun permisoDenegado() {
        _uiState.value = _uiState.value.copy(error = "Necesitas permitir el micrófono para practicar con voz.")
    }
}

/** Botón de micrófono pequeño para las tarjetas y líneas que se pueden practicar. */
@Composable
fun BotonPronunciar(onClick: () -> Unit, modifier: Modifier = Modifier, tinte: androidx.compose.ui.graphics.Color = XPMarino) {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp)) {
        Icon(Icons.Filled.Mic, contentDescription = "Practicar pronunciación", tint = tinte)
    }
}

/** La hoja de práctica; se muestra mientras el ViewModel tenga una frase abierta. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaPracticaPronunciacion(viewModel: PracticaVozViewModel, locutor: Locutor) {
    val uiState by viewModel.uiState.collectAsState()
    val frase = uiState.frase ?: return
    val context = LocalContext.current
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) viewModel.hablar() else viewModel.permisoDenegado()
    }
    val hablar = {
        locutor.callar()
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) viewModel.hablar() else pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
    }

    ModalBottomSheet(
        onDismissRequest = viewModel::cerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = XPFondo
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Practica la pronunciación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XPMarino)
            Surface(color = XPBlanco, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Lee en voz alta:", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
                    Text(frase, style = MaterialTheme.typography.headlineSmall, color = XPTexto)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { locutor.decir(frase) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Text("  Escuchar")
                        }
                        OutlinedButton(onClick = { locutor.decir(frase, lento = true) }) {
                            Icon(Icons.Filled.SlowMotionVideo, contentDescription = null)
                            Text("  Más lento")
                        }
                    }
                }
            }
            BotonPrincipal(
                texto = if (uiState.evaluacion == null) "🎙 Hablar" else "🎙 Intentar de nuevo",
                onClick = hablar,
                cargando = uiState.evaluando
            )
            if (uiState.evaluando) {
                Text("Escuchando… lee la frase completa y haz una pausa al terminar.", style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario)
            }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            uiState.evaluacion?.let { PanelPronunciacion(it, locutor) }
            Surface(color = XPAzulClaro, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Tu voz va directo al servicio de evaluación y no se guarda en ningún lado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = XPMarino,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
