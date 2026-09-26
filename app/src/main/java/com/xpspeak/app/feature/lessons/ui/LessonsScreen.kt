package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto

/**
 * RF-10: catálogo de lecciones del nivel del usuario con su estado de
 * desbloqueo (RN-06) y los conceptos débiles detectados (base RF-13).
 */
@Composable
fun LessonsScreen(
    onAbrirLeccion: (String) -> Unit,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Lecciones ${uiState.nivel}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::cargar) { Text("Actualizar") }
            }
        }

        if (uiState.cargando && uiState.lecciones.isEmpty()) {
            item { CircularProgressIndicator(modifier = Modifier.padding(24.dp)) }
        }

        uiState.error?.let { error ->
            item { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        items(uiState.lecciones, key = { it.id }) { leccion ->
            TarjetaLeccion(leccion, onClick = { onAbrirLeccion(leccion.id) })
        }

        if (uiState.conceptosDebiles.isNotEmpty()) {
            item {
                Text(
                    "Para repasar (conceptos débiles)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(uiState.conceptosDebiles, key = { it.conceptoId }) { concepto ->
                val texto = concepto.titulo ?: "${concepto.en} — ${concepto.es}"
                Text("• $texto  (${(concepto.tasaError * 100).toInt()}% de error)")
            }
        }
    }
}

@Composable
private fun TarjetaLeccion(leccion: LeccionResumenDto, onClick: () -> Unit) {
    val bloqueada = leccion.estado == EstadoLeccion.BLOQUEADA
    Card(
        onClick = onClick,
        enabled = !bloqueada,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("${leccion.orden}. ${leccion.titulo}", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = onClick, enabled = !bloqueada, label = { Text(etiquetaEstado(leccion.estado)) })
                Text(
                    "  ${leccion.xpRecompensa} XP" +
                        (leccion.mejorPuntaje?.let { " · mejor: ${(it * 100).toInt()}%" } ?: ""),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun etiquetaEstado(estado: EstadoLeccion?) = when (estado) {
    EstadoLeccion.BLOQUEADA -> "🔒 Bloqueada"
    EstadoLeccion.DISPONIBLE -> "Disponible"
    EstadoLeccion.EN_PROGRESO -> "En progreso"
    EstadoLeccion.REPROBADA -> "Repetir"
    EstadoLeccion.COMPLETADA -> "✓ Completada"
    null -> "—"
}
