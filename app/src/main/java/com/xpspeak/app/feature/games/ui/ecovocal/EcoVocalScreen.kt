package com.xpspeak.app.feature.games.ui.ecovocal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.XPGreenSuccess
import com.xpspeak.app.core.ui.theme.XPOrangeAccent
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@Composable
fun EcoVocalScreen(onVolver: () -> Unit, viewModel: EcoVocalViewModel = hiltViewModel()) {
    val fase by viewModel.fase.collectAsState()
    val escuchando by viewModel.escuchando.collectAsState()
    val aviso by viewModel.aviso.collectAsState()
    val locutor = rememberLocutor()
    val context = LocalContext.current

    // RECORD_AUDIO está en el manifiesto, pero desde Android 6 se pide al usarlo.
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) viewModel.hablar() else viewModel.permisoDenegado()
    }
    val hablar = {
        locutor.callar()
        val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) viewModel.hablar() else pedirPermiso.launch(Manifest.permission.RECORD_AUDIO)
    }

    PantallaPartida(
        juego = Minijuego.ECO_VOCAL,
        viewModel = viewModel,
        fase = fase,
        onVolver = onVolver,
        progreso = { (it.numeroRonda - 1).toFloat() / GeneradorRondas.RONDAS_ECO }
    ) { estado ->
        val ronda = estado.ronda
        val evaluacion = estado.evaluacion
        val revision = estado.revision

        // §5.1 paso 1: la frase se escucha al aparecer.
        LaunchedEffect(ronda) { locutor.decir(ronda.en) }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Frase ${estado.numeroRonda} de ${GeneradorRondas.RONDAS_ECO} · intento ${estado.intento} de ${Pronunciacion.INTENTOS_POR_RONDA}",
                style = MaterialTheme.typography.labelLarge
            )

            if (evaluacion != null && revision == null) {
                AudioNoLegible(evaluacion)
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Enunciado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(ronda.en, style = MaterialTheme.typography.headlineSmall)
                    ronda.es?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    Row {
                        TextButton(onClick = { locutor.decir(ronda.en) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                            Text(" Escuchar")
                        }
                        TextButton(onClick = { locutor.decir(ronda.en, lento = true) }) { Text("🐢 Más lento") }
                    }
                }
            }

            if (evaluacion != null && revision != null) {
                PuntajePronunciacion(evaluacion)
                when {
                    revision.acierto -> TarjetaFeedback(Veredicto.ACIERTO, "¡Muy bien pronunciado!")
                    !revision.definitiva -> TarjetaFeedback(
                        Veredicto.ERROR,
                        "Casi. Fíjate en las palabras en rojo.",
                        "Escucha la frase otra vez y vuelve a intentarlo."
                    )
                    else -> TarjetaFeedback(Veredicto.ERROR, "Sigue practicando esta frase.", "La verás de nuevo en otras partidas.")
                }
            }

            aviso?.let { TarjetaFeedback(Veredicto.ERROR, "Micrófono", it) }
        }

        if (revision != null) {
            Button(onClick = viewModel::continuar, modifier = Modifier.fillMaxWidth()) {
                Text(if (revision.definitiva) "Continuar" else "Intentar de nuevo")
            }
        } else {
            BotonMicrofono(escuchando, onClick = hablar)
            // Si el micrófono no capta nada (o falla), el usuario no debe quedar atorado en la frase.
            if (aviso != null || evaluacion != null) {
                OutlinedButton(onClick = viewModel::saltar, modifier = Modifier.fillMaxWidth()) { Text("Saltar esta frase") }
            }
        }
    }
}

@Composable
private fun BotonMicrofono(escuchando: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            FilledIconButton(
                onClick = onClick,
                enabled = !escuchando,
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = XPOrangeAccent)
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Hablar", modifier = Modifier.size(44.dp))
            }
            if (escuchando) CircularProgressIndicator(modifier = Modifier.size(96.dp))
        }
        Text(if (escuchando) "Escuchando… di la frase" else "Toca y repite la frase", style = MaterialTheme.typography.labelLarge)
    }
}

/** Ilustración 40: "¡Pronunciación incorrecta!" con el índice de confianza bajo. */
@Composable
private fun AudioNoLegible(evaluacion: EvaluacionVoz) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "¡PRONUNCIACIÓN INCORRECTA!",
            color = XPOrangeAccent,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            "Tu índice de confianza es del ${evaluacion.confianza}%, la respuesta no fue legible.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.error, RoundedCornerShape(8.dp)).padding(12.dp)
            ) {
                Text("${evaluacion.confianza}%", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.size(16.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("Índice de confianza:", style = MaterialTheme.typography.titleMedium)
                Text("BAJO", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
    Text("Por favor, repite la frase:", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
}

/** Puntaje y palabras coloreadas (verde/amarillo/rojo) según la evaluación. */
@Composable
private fun PuntajePronunciacion(evaluacion: EvaluacionVoz) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${evaluacion.puntaje}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = colorPorPuntaje(evaluacion.puntaje)
                )
                Text(" / 100  precisión", style = MaterialTheme.typography.titleMedium)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                evaluacion.palabras.forEach { palabra ->
                    Text(
                        palabra.palabra,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .background(colorPorPuntaje(palabra.puntaje), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            if (evaluacion.transcripcion.isNotBlank()) {
                Text("Oímos: “${evaluacion.transcripcion}”", style = MaterialTheme.typography.bodySmall)
            }
            if (evaluacion.fuente == FuenteEvaluacion.DISPOSITIVO) {
                Text(
                    "Evaluación básica del teléfono (sin conexión con el servicio de voz): revisa qué palabras se entendieron, no los sonidos.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun colorPorPuntaje(puntaje: Int): Color = when (Pronunciacion.nivelDe(puntaje)) {
    Pronunciacion.Nivel.BUENO -> XPGreenSuccess
    Pronunciacion.Nivel.REGULAR -> Color(0xFFF9A825)
    Pronunciacion.Nivel.MALO -> MaterialTheme.colorScheme.error
}
