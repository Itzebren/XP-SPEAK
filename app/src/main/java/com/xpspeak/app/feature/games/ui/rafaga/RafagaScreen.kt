package com.xpspeak.app.feature.games.ui.rafaga

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.ChipDato
import com.xpspeak.app.feature.games.ui.comun.LocalTemaJuego
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
        val tema = LocalTemaJuego.current
        val par = estado.par ?: return@PantallaPartida
        val respondido = estado.respondido
        val segundos = (estado.msRestantes + 999) / 1000
        val urgente = segundos <= 10
        val conBono = estado.multiplicador > 1

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ChipDato(
                Icons.Filled.Timer, "$segundos s",
                fondo = if (urgente) MaterialTheme.colorScheme.error else XPBlanco,
                contenido = if (urgente) XPBlanco else XPMarino
            )
            ChipDato(Icons.Filled.Star, "${estado.puntos}", fondo = tema.acento, contenido = tema.sobreAcento)
            ChipDato(
                Icons.Filled.Bolt,
                if (conBono) "Combo ${estado.combo} · x2" else "Combo ${estado.combo}",
                fondo = if (conBono) XPNaranja else XPBlanco,
                contenido = XPMarino
            )
        }

        Text(
            "¿La traducción es correcta?",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = XPMarino,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        // Tarjeta a dos colores: la palabra en inglés arriba y la traducción a juzgar abajo.
        Surface(shape = RoundedCornerShape(24.dp), shadowElevation = 3.dp, color = XPBlanco, modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(tema.acento).padding(top = 20.dp, bottom = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(par.en, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = tema.sobreAcento)
                        FilledIconButton(
                            onClick = { locutor.decir(par.en) },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = XPBlanco, contentColor = XPMarino)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar")
                        }
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(top = 0.dp).size(40.dp).clip(CircleShape).background(XPMarino)
                ) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = null, tint = XPBlanco)
                }
                Text(
                    par.mostrada(estado.dificultad.nivel),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = XPMarino,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 22.dp)
                )
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
        Surface(color = XPBlanco, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
                FilledIconToggleButton(
                    checked = microfono.activo,
                    onCheckedChange = { onMicClick() },
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = tema.suave,
                        contentColor = XPMarino,
                        checkedContainerColor = tema.acento,
                        checkedContentColor = tema.sobreAcento
                    )
                ) {
                    Icon(
                        if (microfono.activo) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = if (microfono.activo) "Dejar de responder con voz" else "Responder con voz"
                    )
                }
                Text(
                    when {
                        microfono.error != null -> microfono.error!!
                        !microfono.activo -> "Toca el micrófono para responder con voz"
                        microfono.oyendo.isNotBlank() -> "Oímos: \"${microfono.oyendo}\""
                        else -> "Di \"yes\" si coinciden o \"no\" para el bote"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (microfono.error != null) MaterialTheme.colorScheme.error else XPTextoSecundario,
                    modifier = Modifier.padding(start = 8.dp, end = 12.dp)
                )
            }
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
                Text("  Al bote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Button(
                shape = FormaBoton,
                onClick = { viewModel.responder(coinciden = true) },
                enabled = respondido == null,
                modifier = Modifier.weight(1f).height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = XPVerdeExito)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null)
                Text("  Coinciden", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}
