package com.xpspeak.app.feature.games.ui.rafaga

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.xpspeak.app.core.ui.theme.FormaBoton
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@Composable
fun RafagaScreen(onVolver: () -> Unit, viewModel: RafagaViewModel = hiltViewModel()) {
    val fase by viewModel.fase.collectAsState()
    val microfono by viewModel.microfono.collectAsState()
    val locutor = rememberLocutor()

    val context = LocalContext.current
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.alternarVoz() else viewModel.onPermisoMicrofonoDenegado()
    }
    val onMicClick = {
        val tienePermiso = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (microfono.activo || tienePermiso) viewModel.alternarVoz()
        else permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

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
            color = if (estado.multiplicador > 1) XPNaranjaTexto else MaterialTheme.colorScheme.onSurfaceVariant
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

        // Responder con voz: Azure Speech, el mismo del chat.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            FilledIconToggleButton(checked = microfono.activo, onCheckedChange = { onMicClick() }) {
                Icon(
                    if (microfono.activo) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = if (microfono.activo) "Dejar de responder con voz" else "Responder con voz"
                )
            }
            Text(
                when {
                    microfono.error != null -> microfono.error!!
                    !microfono.activo -> "Toca el micrófono para responder con voz"
                    microfono.oyendo.isNotBlank() -> "🎤 \"${microfono.oyendo}\""
                    else -> "Di \"yes\" si coinciden o \"no\" para el bote"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (microfono.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                shape = FormaBoton,
                onClick = { viewModel.responder(coinciden = false) },
                enabled = respondido == null,
                modifier = Modifier.weight(1f).height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Text("  Al bote", style = MaterialTheme.typography.titleMedium)
            }
            Button(
                shape = FormaBoton,
                onClick = { viewModel.responder(coinciden = true) },
                enabled = respondido == null,
                modifier = Modifier.weight(1f).height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XPVerdeExito)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  Coinciden", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
