package com.xpspeak.app.feature.games.ui.ecovocal

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.ui.comun.ChipDato
import com.xpspeak.app.feature.games.ui.comun.EtiquetaJuego
import com.xpspeak.app.feature.games.ui.comun.LocalTemaJuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.TarjetaJuego
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
        val tema = LocalTemaJuego.current
        val ronda = estado.ronda
        val evaluacion = estado.evaluacion
        val revision = estado.revision

        // §5.1 paso 1: la frase se escucha al aparecer.
        LaunchedEffect(ronda) { locutor.decir(ronda.en) }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChipDato(Icons.Filled.RecordVoiceOver, "Frase ${estado.numeroRonda} de ${GeneradorRondas.RONDAS_ECO}", XPBlanco, tema.texto)
                ChipDato(Icons.Filled.Replay, "Intento ${estado.intento} de ${Pronunciacion.INTENTOS_POR_RONDA}", XPBlanco, tema.texto)
            }

            if (evaluacion != null && revision == null) {
                AudioNoLegible(evaluacion)
            }

            TarjetaJuego(borde = tema.acento) {
                EtiquetaJuego("Repite en voz alta")
                Text(ronda.en, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = XPMarino)
                ronda.es?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = XPTextoSecundario) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { locutor.decir(ronda.en) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = tema.acento, contentColor = tema.sobreAcento)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text("  Escuchar", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { locutor.decir(ronda.en, lento = true) },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(2.dp, tema.acento)
                    ) {
                        Icon(Icons.Filled.SlowMotionVideo, contentDescription = null, tint = tema.texto, modifier = Modifier.size(20.dp))
                        Text("  Más lento", color = tema.texto, fontWeight = FontWeight.Bold)
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

            // Un aviso del micrófono no es una respuesta equivocada: sin sonido de error.
            aviso?.let { TarjetaFeedback(Veredicto.ERROR, "Micrófono", it, sonido = false) }
        }

        if (revision != null) {
            Button(
                colors = coloresBotonPrincipal(),
                shape = FormaBoton,
                onClick = viewModel::continuar,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
            ) {
                Text(if (revision.definitiva) "Continuar" else "Intentar de nuevo", fontWeight = FontWeight.Bold)
            }
        } else {
            BotonMicrofono(escuchando, onClick = hablar)
            // Si el micrófono no capta nada (o falla), el usuario no debe quedar atorado en la frase.
            if (aviso != null || evaluacion != null) {
                OutlinedButton(
                    onClick = viewModel::saltar,
                    shape = FormaBoton,
                    border = BorderStroke(2.dp, XPMarino),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Saltar esta frase", color = XPMarino) }
            }
        }
    }
}

/** Micrófono grande del color del juego; mientras escucha, un halo late a su alrededor. */
@Composable
private fun BotonMicrofono(escuchando: Boolean, onClick: () -> Unit) {
    val tema = LocalTemaJuego.current
    val latido = rememberInfiniteTransition(label = "microfono")
    val escala by latido.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
        label = "halo"
    )
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(if (escuchando) escala else 1.1f)
                    .background(tema.acento.copy(alpha = if (escuchando) 0.3f else 0.18f), CircleShape)
            )
            FilledIconButton(
                onClick = onClick,
                enabled = !escuchando,
                modifier = Modifier.size(88.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = tema.acento,
                    contentColor = tema.sobreAcento,
                    disabledContainerColor = tema.acento,
                    disabledContentColor = tema.sobreAcento
                )
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Hablar", modifier = Modifier.size(44.dp))
            }
        }
        Text(
            if (escuchando) "Escuchando… di la frase" else "Toca y repite la frase",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = tema.texto
        )
    }
}

/** Ilustración 40: "¡Pronunciación incorrecta!" con el índice de confianza bajo. */
@Composable
private fun AudioNoLegible(evaluacion: EvaluacionVoz) {
    TarjetaJuego(borde = MaterialTheme.colorScheme.error) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painterResource(R.drawable.mascota_preocupada),
                contentDescription = null,
                modifier = Modifier.size(width = 64.dp, height = 70.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("¡Pronunciación incorrecta!", color = XPNaranjaTexto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Tu índice de confianza es del ${evaluacion.confianza}%, la respuesta no fue legible.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(14.dp)) {
                Text(
                    "${evaluacion.confianza}%",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Column {
                Text("Índice de confianza", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
                Text("Bajo", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Text("Por favor, repite la frase.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Puntaje en anillo y palabras coloreadas (verde/amarillo/rojo) según la evaluación. */
@Composable
private fun PuntajePronunciacion(evaluacion: EvaluacionVoz) {
    val color = colorPorPuntaje(evaluacion.puntaje)
    TarjetaJuego {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(84.dp)) {
                CircularProgressIndicator(
                    progress = { evaluacion.puntaje / 100f },
                    color = color,
                    trackColor = color.copy(alpha = 0.18f),
                    strokeWidth = 9.dp,
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.fillMaxSize()
                )
                Text("${evaluacion.puntaje}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
            }
            Column {
                Text("Precisión", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XPMarino)
                Text("de 100 puntos", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            evaluacion.palabras.forEach { palabra ->
                Text(
                    palabra.palabra,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .background(colorPorPuntaje(palabra.puntaje), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
        if (evaluacion.transcripcion.isNotBlank()) {
            Text("Oímos: “${evaluacion.transcripcion}”", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        }
        if (evaluacion.fuente == FuenteEvaluacion.DISPOSITIVO) {
            Text(
                "Evaluación básica del teléfono (sin conexión con el servicio de voz): revisa qué palabras se entendieron, no los sonidos.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Start,
                color = XPTextoSecundario
            )
        }
    }
}

@Composable
private fun colorPorPuntaje(puntaje: Int): Color = when (Pronunciacion.nivelDe(puntaje)) {
    Pronunciacion.Nivel.BUENO -> XPVerdeExito
    Pronunciacion.Nivel.REGULAR -> Color(0xFFB7791F)
    Pronunciacion.Nivel.MALO -> MaterialTheme.colorScheme.error
}
