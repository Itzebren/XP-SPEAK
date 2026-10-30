package com.xpspeak.app.feature.lessons.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.BotonSecundario
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.lessons.data.ItemDto
import com.xpspeak.app.feature.lessons.data.RepasoResponse
import com.xpspeak.app.feature.lessons.ui.components.EjercicioEvaluacion
import com.xpspeak.app.feature.progress.ui.InsigniaNueva
import java.text.DateFormat
import java.util.Date

/**
 * RF-13 / RN-07: sesión de repaso SRS. Un ejercicio por concepto vencido,
 * primero los de mayor tasa de error. Al enviar, SM-2 reprograma cada uno.
 * Misma maquetación que la lección (docs/diseno.md §3): encabezado azul con la
 * mascota, tarjetas azules y botones del PDF.
 */
@Composable
fun RepasoScreen(
    onVolver: () -> Unit,
    viewModel: RepasoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sesion = uiState.sesion
    val resultado = uiState.resultado

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
        EncabezadoXP(titulo = "Repaso de hoy", onVolver = onVolver, mascotaAlInicio = true) {
            sesion?.let {
                Text(
                    when {
                        it.items.isEmpty() -> "Nada pendiente por ahora"
                        it.totalVencidos > it.items.size -> "${it.items.size} de ${it.totalVencidos} conceptos pendientes"
                        else -> "${it.items.size} concepto(s) por repasar"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = XPTextoSecundario
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.cargando) {
                CircularProgressIndicator(color = XPMarino, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (sesion == null) return@Column

            if (sesion.items.isEmpty()) {
                TarjetaXP {
                    Text("¡Nada que repasar por ahora!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        sesion.proximaRevision?.let { "Tu próximo repaso: ${formatearFecha(it)}" }
                            ?: "Termina una lección para empezar a repasar sus conceptos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = XPTextoSecundario
                    )
                }
                BotonPrincipal(texto = "Volver a lecciones", onClick = onVolver)
                return@Column
            }

            Text(
                "Responde los ejercicios de los conceptos que te cuestan más; SM-2 decide cuándo vuelven a salir.",
                style = MaterialTheme.typography.bodyMedium,
                color = XPTextoSecundario
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
                if (sesion.totalVencidos > sesion.items.size) {
                    BotonSecundario(texto = "Seguir repasando", onClick = viewModel::cargar)
                }
                BotonPrincipal(texto = "Volver al catálogo", onClick = onVolver)
            } else {
                // Un ejercicio sin responder no le dice nada a SM-2: se piden todos.
                val faltan = sesion.items.size - uiState.respuestas.size
                BotonPrincipal(
                    texto = if (faltan > 0) "Responde los $faltan ejercicio(s) restantes" else "Enviar repaso",
                    onClick = viewModel::enviar,
                    enabled = faltan == 0,
                    cargando = uiState.enviando
                )
            }
        }
    }
}

/** Puntaje y, por concepto, cuándo vuelve a salir según SM-2. */
@Composable
private fun ResultadoRepaso(resultado: RepasoResponse, items: List<ItemDto>) {
    var festejar by remember(resultado) { mutableStateOf(true) }
    if (festejar) InsigniaNueva(resultado.insigniasNuevas.orEmpty(), resultado.nivelXpNuevo, onCerrar = { festejar = false })
    TarjetaXP {
        Text(
            "${resultado.correctas}/${resultado.total} correctas",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = XPMarino
        )
        resultado.conceptos.forEach { concepto ->
            val nombre = items.firstOrNull { it.conceptoId == concepto.conceptoId }?.concepto
                ?.let { it.titulo ?: it.en }
                ?: concepto.conceptoId
            val cuando = if (concepto.intervalo == 1) "mañana" else "en ${concepto.intervalo} días"
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (concepto.acierto) "✓" else "✗",
                    color = if (concepto.acierto) XPVerdeExito else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text("$nombre · vuelve $cuando", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun formatearFecha(epochMs: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMs))
