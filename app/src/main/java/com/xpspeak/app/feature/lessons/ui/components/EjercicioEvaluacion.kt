package com.xpspeak.app.feature.lessons.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.lessons.data.FeedbackDto
import com.xpspeak.app.feature.lessons.data.ItemDto
import com.xpspeak.app.feature.lessons.data.TipoEjercicio
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario

/**
 * Un ítem de la evaluación (CU-06 pasos 6–8). Se pinta verde/rojo según el
 * `feedback` del servidor. Para un tipo de ejercicio nuevo: agrégalo a
 * TipoEjercicio, a RespuestaUsuario y a este `when`.
 */
@Composable
fun EjercicioEvaluacion(
    item: ItemDto,
    respuesta: RespuestaUsuario?,
    feedback: FeedbackDto?,
    onResponder: (RespuestaUsuario) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = coloresSegun(feedback)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(item.enunciado.orEmpty(), fontWeight = FontWeight.Medium)
            when (item.tipo) {
                TipoEjercicio.OPCION_MULTIPLE -> OpcionMultiple(item, respuesta as? RespuestaUsuario.Opcion, onResponder)
                TipoEjercicio.COMPLETAR -> Completar(respuesta as? RespuestaUsuario.Texto, onResponder)
                TipoEjercicio.EMPAREJAR -> Emparejar(item, respuesta as? RespuestaUsuario.Pares, onResponder)
                null -> Text("Actualiza la app para responder este ejercicio.")
            }
            // CU-06 A1: nota breve que explica el error.
            feedback?.takeIf { !it.correcta }?.mensaje?.let {
                Text("✗ $it", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

@Composable
private fun coloresSegun(feedback: FeedbackDto?) = when (feedback?.correcta) {
    true -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    false -> CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    null -> CardDefaults.cardColors()
}

@Composable
private fun OpcionMultiple(
    item: ItemDto,
    respuesta: RespuestaUsuario.Opcion?,
    onResponder: (RespuestaUsuario) -> Unit
) {
    item.opciones.orEmpty().forEachIndexed { indice, opcion ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = respuesta?.indice == indice,
                onClick = { onResponder(RespuestaUsuario.Opcion(indice)) }
            )
            Text(opcion)
        }
    }
}

@Composable
private fun Completar(respuesta: RespuestaUsuario.Texto?, onResponder: (RespuestaUsuario) -> Unit) {
    OutlinedTextField(
        value = respuesta?.texto.orEmpty(),
        onValueChange = { onResponder(RespuestaUsuario.Texto(it)) },
        label = { Text("Tu respuesta") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Para cada palabra de la izquierda, el usuario elige su pareja entre las opciones revueltas. */
@Composable
private fun Emparejar(
    item: ItemDto,
    respuesta: RespuestaUsuario.Pares?,
    onResponder: (RespuestaUsuario) -> Unit
) {
    val pares = item.pares.orEmpty()
    val elegidos = respuesta?.elegidos.orEmpty()
    val opcionesRevueltas = remember(item.id) { pares.map { it.der }.shuffled() }

    pares.forEach { par ->
        Text(par.izq, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            opcionesRevueltas.forEach { der ->
                FilterChip(
                    selected = elegidos[par.izq] == der,
                    onClick = { onResponder(RespuestaUsuario.Pares(elegidos + (par.izq to der))) },
                    label = { Text(der) }
                )
            }
        }
    }
}
