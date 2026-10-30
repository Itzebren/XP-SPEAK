package com.xpspeak.app.feature.lessons.ui.components

import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.core.ui.theme.FormaBoton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.lessons.data.IntentoResponse
import com.xpspeak.app.feature.lessons.data.nombreLegible
import com.xpspeak.app.feature.progress.ui.InsigniaNueva

/** Resultado de la evaluación: puntaje, XP (RF-14), desbloqueo (RN-06) y conceptos a reforzar. */
@Composable
fun ResultadoIntento(
    resultado: IntentoResponse,
    pendiente: Boolean,
    onReintentar: () -> Unit,
    onVolver: () -> Unit
) {
    // Progreso: festeja las insignias y la subida de nivel que confirmó el servidor (no las de un intento en cola).
    var festejar by remember(resultado) { mutableStateOf(!pendiente) }
    if (festejar) InsigniaNueva(resultado.insigniasNuevas.orEmpty(), resultado.nivelXpNuevo, onCerrar = { festejar = false })

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (resultado.aprobada) "¡Aprobada! 🎉" else "Aún no — repasa y vuelve a intentarlo",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Puntaje: ${(resultado.puntaje * 100).toInt()}% (${resultado.correctas}/${resultado.total})")
            if (pendiente) {
                // El XP y el desbloqueo los decide el servidor al recibir el intento.
                Text(
                    "Sin conexión: tu evaluación se enviará al reconectarte y ahí se sumará tu XP.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text("XP ganado: ${resultado.xpGanado}")
                resultado.nivelXpNuevo?.let { Text("⭐ ¡Subiste a nivel $it!") }
            }
            resultado.desbloqueadaSiguiente?.let { Text("🔓 Desbloqueaste: ${resultado.desbloqueadaTitulo ?: nombreLegible(it)}") }
            if (resultado.conceptosDebiles.isNotEmpty()) {
                // Con nombre (del servidor) y no con el id interno (QA 29-oct).
                val nombres = resultado.conceptosDebilesDetalle?.map { it.nombre } ?: resultado.conceptosDebiles.map(::nombreLegible)
                Text("Conceptos a reforzar: ${nombres.joinToString()}")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReintentar) { Text("Intentar de nuevo") }
        Button(colors = coloresBotonPrincipal(), shape = FormaBoton, onClick = onVolver) { Text("Volver al catálogo") }
    }
}
