package com.xpspeak.app.feature.progress.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.feature.progress.data.InsigniaDto

/**
 * Festejo de lo ganado al terminar una actividad (RN-09): las insignias nuevas
 * y la subida de nivel de XP. Lo usan el resultado de lección, de minijuego,
 * de repaso y el chat. Las insignias `retroactiva` no se festejan.
 */
@Composable
fun InsigniaNueva(insignias: List<InsigniaDto>, nivelXpNuevo: Int?, onCerrar: () -> Unit) {
    val enVivo = insignias.filterNot { it.retroactiva }
    if (enVivo.isEmpty() && nivelXpNuevo == null) return

    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = { TextButton(onClick = onCerrar) { Text("¡Genial!") } },
        title = {
            Text(
                when {
                    enVivo.size > 1 -> "¡Nuevas insignias! 🎉"
                    enVivo.size == 1 -> "¡Nueva insignia! 🎉"
                    else -> "¡Subiste de nivel! 🎉"
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                nivelXpNuevo?.let {
                    Text(
                        "¡Subiste a nivel $it!",
                        style = MaterialTheme.typography.titleMedium,
                        color = XPNaranjaTexto,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                enVivo.forEach { insignia ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        IconoInsignia(insignia, obtenida = true, tamano = 56.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(insignia.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(insignia.descripcion, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    )
}
