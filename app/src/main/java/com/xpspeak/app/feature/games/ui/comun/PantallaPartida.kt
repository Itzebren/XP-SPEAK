package com.xpspeak.app.feature.games.ui.comun

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.xpspeak.app.core.ui.theme.XPBlueLight
import com.xpspeak.app.core.ui.theme.XPGreenSuccess
import com.xpspeak.app.core.ui.theme.XPOrangeAccent
import com.xpspeak.app.feature.games.domain.CalculadorXp
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.ResultadoPartida
import kotlin.math.roundToInt

/**
 * Marco común de los cuatro juegos: encabezado, carga, "¿Retomar la
 * partida?", resultado final y errores. Cada juego solo dibuja su mecánica
 * en [contenido].
 */
@Composable
fun <E : EstadoPartida<E>> PantallaPartida(
    juego: Minijuego,
    viewModel: PartidaViewModel<E>,
    fase: FasePartida<E>,
    onVolver: () -> Unit,
    progreso: (E) -> Float?,
    contenido: @Composable ColumnScope.(E) -> Unit
) {
    // El tiempo solo cuenta con la pantalla activa; al salir se guarda el avance.
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.pausar() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.reanudar() }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoJuego(juego.titulo, onVolver, (fase as? FasePartida.Jugando<E>)?.estado?.let(progreso))
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (fase) {
                FasePartida.Cargando, FasePartida.Guardando -> Centrado { CircularProgressIndicator() }
                is FasePartida.Retomar -> DialogoRetomar(
                    onRetomar = viewModel::retomar,
                    onNueva = viewModel::empezarDeNuevo
                )
                is FasePartida.Jugando -> contenido(fase.estado)
                is FasePartida.Terminada -> PantallaResultado(fase.resultado, viewModel::jugarOtraVez, onVolver)
                is FasePartida.SinContenido -> Mensaje("🔒", fase.mensaje, onVolver)
                is FasePartida.Error -> Mensaje("⚠️", fase.mensaje, onVolver)
            }
        }
    }
}

@Composable
private fun EncabezadoJuego(titulo: String, onVolver: () -> Unit, progreso: Float?) {
    Column(modifier = Modifier.fillMaxWidth().background(XPBlueLight)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al catálogo")
            }
            Text(titulo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        if (progreso != null) {
            LinearProgressIndicator(progress = { progreso.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ColumnScope.Centrado(contenido: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { contenido() }
}

@Composable
private fun DialogoRetomar(onRetomar: () -> Unit, onNueva: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("¿Retomar la partida?") },
        text = { Text("Tienes una partida sin terminar. Puedes seguir donde te quedaste o empezar una nueva.") },
        confirmButton = { Button(onClick = onRetomar) { Text("Retomar") } },
        dismissButton = { TextButton(onClick = onNueva) { Text("Empezar de nuevo") } }
    )
}

@Composable
private fun ColumnScope.Mensaje(icono: String, mensaje: String, onVolver: () -> Unit) {
    Centrado {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(icono, style = MaterialTheme.typography.displaySmall)
            Text(mensaje, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onVolver) { Text("Volver al catálogo") }
        }
    }
}

/** CU-07 pasos 7 y 8: desempeño (precisión y tiempo), XP y racha. */
@Composable
private fun ColumnScope.PantallaResultado(resultado: ResultadoPartida, onOtraVez: () -> Unit, onVolver: () -> Unit) {
    val porcentaje = (resultado.precision * 100).roundToInt()
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            when {
                resultado.precision >= 0.9 -> "¡Excelente! 🏆"
                resultado.precision >= CalculadorXp.PRECISION_PARA_BONO -> "¡Muy bien! 🎉"
                else -> "¡Buen intento! 💪"
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Fila("Precisión", "$porcentaje% (${resultado.aciertos}/${resultado.total})")
                Fila("Tiempo", formatearDuracion(resultado.duracionMs))
                Fila("Racha", "🔥 ${resultado.racha} ${if (resultado.racha == 1) "día" else "días"}")
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = XPOrangeAccent.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("+${resultado.xp} XP", style = MaterialTheme.typography.headlineLarge, color = XPOrangeAccent, fontWeight = FontWeight.Bold)
                if (!resultado.confirmada) {
                    Text(
                        "Sin conexión: tu XP se confirma al reconectarte.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
                if (resultado.xpReducida) {
                    Text(
                        "Ya jugaste mucho este juego hoy, así que la XP es la mitad. ¡Prueba una lección!",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        if (resultado.paraRepasar.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Para repasar", style = MaterialTheme.typography.titleMedium)
                    resultado.paraRepasar.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                    Text(
                        "Estos temas saldrán más seguido en tus próximas partidas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(onClick = onVolver, modifier = Modifier.weight(1f)) { Text("Catálogo") }
        Button(onClick = onOtraVez, modifier = Modifier.weight(1f)) { Text("Jugar otra vez") }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyLarge)
        Text(valor, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

fun formatearDuracion(ms: Long): String {
    val segundos = (ms / 1000).toInt()
    return if (segundos < 60) "$segundos s" else "${segundos / 60} min ${segundos % 60} s"
}

/** Resultado de una acción: acierto o error (CU-07 paso 6). */
enum class Veredicto { ACIERTO, ERROR }

/**
 * Feedback visual y háptico de cada acción (§4.5): verde y vibración corta
 * si acierta; color de error y la explicación si no. Los errores se explican,
 * no se castigan (filtro afectivo, PDF §2.4).
 */
@Composable
fun TarjetaFeedback(veredicto: Veredicto?, titulo: String, detalle: String? = null) {
    val haptica = LocalHapticFeedback.current
    LaunchedEffect(veredicto, titulo) {
        when (veredicto) {
            Veredicto.ACIERTO -> haptica.performHapticFeedback(HapticFeedbackType.Confirm)
            Veredicto.ERROR -> haptica.performHapticFeedback(HapticFeedbackType.Reject)
            null -> Unit
        }
    }
    val color by animateColorAsState(colorDe(veredicto), label = "feedback")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(titulo, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (detalle != null) {
                Spacer(Modifier.height(4.dp))
                Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun colorDe(veredicto: Veredicto?): Color = when (veredicto) {
    Veredicto.ACIERTO -> XPGreenSuccess
    Veredicto.ERROR -> MaterialTheme.colorScheme.error
    null -> MaterialTheme.colorScheme.primary
}
