package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.xpspeak.app.feature.lessons.data.ItemDto
import com.xpspeak.app.feature.lessons.data.RepasoResponse
import com.xpspeak.app.feature.lessons.ui.components.EjercicioEvaluacion
import java.text.DateFormat
import java.util.Date

/**
 * RF-13 / RN-07: sesión de repaso SRS. Un ejercicio por concepto vencido,
 * primero los de mayor tasa de error. Al enviar, SM-2 reprograma cada uno.
 */
@Composable
fun RepasoScreen(
    onVolver: () -> Unit,
    viewModel: RepasoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sesion = uiState.sesion
    val resultado = uiState.resultado

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedButton(onClick = onVolver) { Text("← Lecciones") }
        Text("Repaso de hoy", style = MaterialTheme.typography.headlineSmall)

        if (uiState.cargando) CircularProgressIndicator()
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (sesion == null) return@Column

        if (sesion.items.isEmpty()) {
            Text("¡Nada que repasar por ahora! 🎉", style = MaterialTheme.typography.titleMedium)
            sesion.proximaRevision?.let { Text("Tu próximo repaso: ${formatearFecha(it)}") }
                ?: Text("Termina una lección para empezar a repasar sus conceptos.")
            return@Column
        }

        Text(
            "${sesion.items.size} concepto(s) por repasar" +
                if (sesion.totalVencidos > sesion.items.size) " (de ${sesion.totalVencidos} pendientes)" else "",
            style = MaterialTheme.typography.bodyMedium
        )

        sesion.items.forEach { item ->
            val itemId = item.id ?: return@forEach
            EjercicioEvaluacion(
                item = item,
                respuesta = uiState.respuestas[itemId],
                feedback = resultado?.feedback?.find { it.id == itemId },
                onResponder = { if (resultado == null) viewModel.responder(itemId, it) }
            )
        }

        if (resultado != null) {
            ResultadoRepaso(resultado, sesion.items)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (sesion.totalVencidos > sesion.items.size) {
                    OutlinedButton(onClick = viewModel::cargar) { Text("Seguir repasando") }
                }
                Button(onClick = onVolver) { Text("Volver al catálogo") }
            }
        } else {
            // Un ejercicio sin responder no le dice nada a SM-2: se piden todos.
            val faltan = sesion.items.size - uiState.respuestas.size
            Button(
                onClick = viewModel::enviar,
                enabled = !uiState.enviando && faltan == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    when {
                        uiState.enviando -> "Calificando…"
                        faltan > 0 -> "Responde los $faltan ejercicio(s) restantes"
                        else -> "Enviar repaso"
                    }
                )
            }
        }
    }
}

/** Puntaje y, por concepto, cuándo vuelve a salir según SM-2. */
@Composable
private fun ResultadoRepaso(resultado: RepasoResponse, items: List<ItemDto>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${resultado.correctas}/${resultado.total} correctas", style = MaterialTheme.typography.titleLarge)
            resultado.conceptos.forEach { concepto ->
                val nombre = items.firstOrNull { it.conceptoId == concepto.conceptoId }?.concepto
                    ?.let { it.titulo ?: it.en }
                    ?: concepto.conceptoId
                val cuando = if (concepto.intervalo == 1) "mañana" else "en ${concepto.intervalo} días"
                Text("${if (concepto.acierto) "✓" else "✗"} $nombre — vuelve $cuando")
            }
        }
    }
}

private fun formatearFecha(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))
