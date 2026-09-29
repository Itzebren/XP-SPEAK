package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.lessons.data.TipoSeccion
import com.xpspeak.app.feature.lessons.ui.components.EjercicioEvaluacion
import com.xpspeak.app.feature.lessons.ui.components.ResultadoIntento
import com.xpspeak.app.feature.lessons.ui.components.SeccionTeoria

/**
 * CU-06: teoría de la lección + evaluación corta. La calificación la hace el
 * servidor (autoritativa); sin conexión se califica en el teléfono y el
 * intento se envía después (§6.2).
 */
@Composable
fun LeccionScreen(
    onVolver: () -> Unit,
    viewModel: LeccionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val leccion = uiState.leccion
    val resultado = uiState.resultado

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onVolver) { Text("← Lecciones") }

        if (uiState.cargando) CircularProgressIndicator()
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (leccion == null) return@Column

        Text(leccion.moduloTematico, style = MaterialTheme.typography.headlineSmall)
        leccion.canDo.forEach { Text("✓ $it", style = MaterialTheme.typography.bodySmall) }

        leccion.secciones.forEach { seccion ->
            if (seccion.tipo != TipoSeccion.EVALUACION) {
                SeccionTeoria(seccion)
                return@forEach
            }
            Text(
                "Evaluación (mínimo ${((seccion.umbralAprobacion ?: 0.7) * 100).toInt()}%)",
                style = MaterialTheme.typography.titleLarge
            )
            seccion.items.orEmpty().forEach { item ->
                val itemId = item.id ?: return@forEach
                EjercicioEvaluacion(
                    item = item,
                    respuesta = uiState.respuestas[itemId],
                    feedback = resultado?.feedback?.find { it.id == itemId },
                    onResponder = { viewModel.responder(itemId, it) }
                )
            }
        }

        if (resultado != null) {
            ResultadoIntento(
                resultado,
                pendiente = uiState.pendiente,
                onReintentar = viewModel::reintentar,
                onVolver = onVolver
            )
        } else {
            Button(
                onClick = viewModel::enviar,
                enabled = !uiState.enviando && uiState.respuestas.isNotEmpty(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (uiState.enviando) "Calificando…" else "Enviar evaluación") }
        }
    }
}
