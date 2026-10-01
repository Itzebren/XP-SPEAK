package com.xpspeak.app.feature.games.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.games.domain.Minijuego

/**
 * Pantalla de un minijuego. Por ahora solo confirma la navegación desde el
 * catálogo; la mecánica de cada juego se implementa según docs/minijuegos-diseno.md.
 */
@Composable
fun MinijuegoScreen(juego: Minijuego?, onVolver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(juego?.titulo ?: "Minijuego", style = MaterialTheme.typography.headlineSmall)
        juego?.let { Text(it.descripcion, style = MaterialTheme.typography.bodyMedium) }
        Text("Próximamente", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onVolver) { Text("Volver") }
    }
}
