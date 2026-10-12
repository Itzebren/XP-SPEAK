package com.xpspeak.app.feature.games.ui.rafaga

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.XPGreenSuccess
import com.xpspeak.app.core.ui.theme.XPOrangeAccent
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@Composable
fun RafagaScreen(onVolver: () -> Unit, viewModel: RafagaViewModel = hiltViewModel()) {
    val fase by viewModel.fase.collectAsState()
    val locutor = rememberLocutor()

    PantallaPartida(
        juego = Minijuego.RAFAGA_PALABRAS,
        viewModel = viewModel,
        fase = fase,
        onVolver = onVolver,
        progreso = { it.msRestantes.toFloat() / RafagaViewModel.MS_INICIALES }
    ) { estado ->
        val par = estado.par ?: return@PantallaPartida
        val respondido = estado.respondido
        val segundos = (estado.msRestantes + 999) / 1000

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "⏱ $segundos s",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (segundos <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text("⭐ ${estado.puntos}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Text(
            if (estado.multiplicador > 1) "🔥 Combo ${estado.combo} · ¡puntos x2!" else "Combo ${estado.combo}",
            style = MaterialTheme.typography.labelLarge,
            color = if (estado.multiplicador > 1) XPOrangeAccent else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "¿La traducción es correcta?",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(par.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { locutor.decir(par.en) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar")
                    }
                }
                Text("↓", style = MaterialTheme.typography.titleLarge)
                Text(par.mostrada(estado.dificultad.nivel), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            }
        }

        if (respondido != null) {
            if (respondido.acierto) {
                TarjetaFeedback(Veredicto.ACIERTO, "¡Bien! +1 s")
            } else {
                TarjetaFeedback(Veredicto.ERROR, "−3 s", "${respondido.en} = ${respondido.esCorrecta}")
            }
        } else {
            Spacer(Modifier.height(72.dp))
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.responder(coinciden = false) },
                enabled = respondido == null,
                modifier = Modifier.weight(1f).height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text("  Al bote", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                onClick = { viewModel.responder(coinciden = true) },
                enabled = respondido == null,
                modifier = Modifier.weight(1f).height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XPGreenSuccess)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  Coinciden", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
