package com.xpspeak.app.feature.lessons.ui.components

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.lessons.data.IntentoResponse

/** Resultado de la evaluación: puntaje, XP (RF-14), desbloqueo (RN-06) y conceptos a reforzar. */
@Composable
fun ResultadoIntento(resultado: IntentoResponse, onReintentar: () -> Unit, onVolver: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (resultado.aprobada) "¡Aprobada! 🎉" else "Aún no — repasa y vuelve a intentarlo",
                style = MaterialTheme.typography.titleLarge
            )
            Text("Puntaje: ${(resultado.puntaje * 100).toInt()}% (${resultado.correctas}/${resultado.total})")
            Text("XP ganado: ${resultado.xpGanado}")
            resultado.desbloqueadaSiguiente?.let { Text("🔓 Desbloqueaste: $it") }
            if (resultado.conceptosDebiles.isNotEmpty()) {
                Text("Conceptos a reforzar: ${resultado.conceptosDebiles.joinToString()}")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onReintentar) { Text("Intentar de nuevo") }
        Button(onClick = onVolver) { Text("Volver al catálogo") }
    }
}
