package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.DistintivoNivel
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.recursoDrawable
import com.xpspeak.app.core.ui.theme.FormaPortada
import com.xpspeak.app.core.ui.theme.XPAmarillo
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionResumenDto
import com.xpspeak.app.feature.lessons.domain.RepasoPrimero
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal

/**
 * RF-10: catálogo de lecciones del nivel del usuario en cuadrícula de 2 columnas
 * (Ilustración 38, portada en res/drawable/leccion_<id>), con su estado de desbloqueo
 * (RN-06) y los conceptos débiles detectados (base RF-13).
 */
@Composable
fun LessonsScreen(
    onAbrirLeccion: (String) -> Unit,
    onAbrirRepaso: () -> Unit,
    viewModel: LessonsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var avisoRepaso by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.cargar() }

    if (avisoRepaso) {
        DialogoRepasoPrimero(
            conceptos = uiState.repasoPrioritario,
            onRepasar = { avisoRepaso = false; onAbrirRepaso() },
            onCerrar = { avisoRepaso = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoXP(
            "Catálogo de lecciones",
            acciones = { TextButton(onClick = viewModel::cargar) { Text("Actualizar", color = XPMarino) } }
        ) {
            if (uiState.nivel.isNotEmpty()) DistintivoNivel(uiState.nivel, tamano = 36.dp)
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    Card(
                        onClick = onAbrirRepaso,
                        colors = CardDefaults.cardColors(containerColor = PISTA_NARANJA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Repaso de hoy", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${uiState.repasoPendiente} concepto(s) por repasar",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (uiState.repasoPrioritario > 0) {
                                Text(
                                    "Termínalo para empezar lecciones nuevas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            items(uiState.lecciones, key = { it.id }) { leccion ->
                TarjetaLeccion(leccion, onClick = {
                    if (RepasoPrimero.retiene(leccion, uiState.repasoPrioritario)) avisoRepaso = true
                    else onAbrirLeccion(leccion.id)
                })
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

        }
    }
}

/** RN-07: se tocó una lección nueva con conceptos de alta tasa de error pendientes. */
@Composable
private fun DialogoRepasoPrimero(conceptos: Int, onRepasar: () -> Unit, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Primero, un repaso") },
        text = {
            Text(
                "Tienes $conceptos concepto(s) que te han costado y toca repasar hoy. " +
                    "Repásalos antes de empezar un tema nuevo; las lecciones que ya empezaste siguen abiertas."
            )
        },
        confirmButton = { Button(colors = coloresBotonPrincipal(), shape = FormaBoton, onClick = onRepasar) { Text("Repasar ahora") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Ahora no") } }
    )
}

/** Elemento que ocupa las dos columnas de la cuadrícula (encabezados, avisos). */
private fun LazyGridScope.filaCompleta(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun TarjetaLeccion(leccion: LeccionResumenDto, onClick: () -> Unit) {
    val bloqueada = leccion.estado == EstadoLeccion.BLOQUEADA
    Column(
        modifier = Modifier
            .clickable(enabled = !bloqueada, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            val portada = recursoDrawable("leccion_${leccion.id.replace('-', '_')}")
            Image(
                painter = painterResource(if (portada != 0) portada else R.drawable.inicio_lecciones),
                contentDescription = null,
                colorFilter = if (bloqueada) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
                alpha = if (bloqueada) 0.55f else 1f,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(FormaPortada)
            )
            ChipEstado(leccion.estado, modifier = Modifier.padding(bottom = 8.dp))
        }
        Text(
            "${leccion.orden}. ${leccion.titulo}",
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center
        )
        Text(
            "${leccion.xpRecompensa} XP" +
                (leccion.mejorPuntaje?.let { " · mejor: ${(it * 100).toInt()} %" } ?: ""),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Estado de la lección en texto y color (nunca solo color, RNF-01). */
@Composable
private fun ChipEstado(estado: EstadoLeccion?, modifier: Modifier = Modifier) {
    val (texto, fondo, tinta) = when (estado) {
        EstadoLeccion.BLOQUEADA -> Triple("Bloqueada", XPTextoSecundario, XPBlanco)
        EstadoLeccion.DISPONIBLE -> Triple("Disponible", XPBlanco, XPMarino)
        EstadoLeccion.EN_PROGRESO -> Triple("En progreso", XPNaranja, XPMarino)
        EstadoLeccion.REPROBADA -> Triple("Repetir", XPAmarillo, XPMarino)
        EstadoLeccion.COMPLETADA -> Triple("Completada", XPVerdeExito, XPBlanco)
        null -> return
    }
    Surface(color = fondo, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            when (estado) {
                EstadoLeccion.BLOQUEADA -> Icon(Icons.Filled.Lock, contentDescription = null, tint = tinta, modifier = Modifier.size(14.dp))
                EstadoLeccion.COMPLETADA -> Icon(Icons.Filled.Check, contentDescription = null, tint = tinta, modifier = Modifier.size(14.dp))
                else -> Unit
            }
            Text(texto, color = tinta, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

/** Fondo naranja claro del aviso de repaso. */
private val PISTA_NARANJA = Color(0xFFFFE0B8)
