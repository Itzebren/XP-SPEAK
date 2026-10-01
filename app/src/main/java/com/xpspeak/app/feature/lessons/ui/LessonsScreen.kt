package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto

/**
 * RF-10: catálogo de lecciones del nivel del usuario en cuadrícula de 2 columnas
 * (mismo estilo que el catálogo de minijuegos), con su estado de desbloqueo
 * (RN-06) y los conceptos débiles detectados (base RF-13).
 */
@Composable
fun LessonsScreen(
    onAbrirLeccion: (String) -> Unit,
    onAbrirRepaso: () -> Unit,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargar() }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        filaCompleta {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Catálogo de lecciones",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = viewModel::cargar) { Text("Actualizar") }
            }
        }

        if (uiState.nivel.isNotEmpty()) {
            filaCompleta {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            uiState.nivel,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        if (uiState.cargando && uiState.lecciones.isEmpty()) {
            filaCompleta {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                }
            }
        }

        uiState.error?.let { error ->
            filaCompleta { Text(error, color = MaterialTheme.colorScheme.error) }
        }

        if (uiState.sinConexion) {
            filaCompleta {
                Text(
                    "Sin conexión: ves tu último progreso guardado. Las lecciones descargadas funcionan igual.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        if (uiState.intentosPendientes > 0) {
            filaCompleta {
                Text(
                    "${uiState.intentosPendientes} evaluación(es) por enviar: se sincronizan al volver la conexión.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // RN-07: el repaso va antes que las lecciones nuevas.
        if (uiState.repasoPendiente > 0) {
            filaCompleta {
                Card(onClick = onAbrirRepaso, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("🔁 Repaso de hoy", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${uiState.repasoPendiente} concepto(s) por repasar",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        items(uiState.lecciones, key = { it.id }) { leccion ->
            TarjetaLeccion(leccion, onClick = { onAbrirLeccion(leccion.id) })
        }

        if (uiState.conceptosDebiles.isNotEmpty()) {
            filaCompleta {
                Text(
                    "Para repasar (conceptos débiles)",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            items(
                uiState.conceptosDebiles,
                key = { it.conceptoId },
                span = { GridItemSpan(maxLineSpan) }
            ) { concepto ->
                val texto = concepto.titulo ?: "${concepto.en} — ${concepto.es}"
                Text("• $texto  (${(concepto.tasaError * 100).toInt()}% de error)")
            }
        }

        filaCompleta { Box(modifier = Modifier.padding(bottom = 4.dp)) }
    }
}

/** Elemento que ocupa las dos columnas de la cuadrícula (encabezados, avisos). */
private fun LazyGridScope.filaCompleta(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun TarjetaLeccion(leccion: LeccionResumenDto, onClick: () -> Unit) {
    val bloqueada = leccion.estado == EstadoLeccion.BLOQUEADA
    Card(
        onClick = onClick,
        enabled = !bloqueada,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${leccion.orden}. ${leccion.titulo}",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    etiquetaEstado(leccion.estado),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    "${leccion.xpRecompensa} XP" +
                        (leccion.mejorPuntaje?.let { " · mejor: ${(it * 100).toInt()}%" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
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
