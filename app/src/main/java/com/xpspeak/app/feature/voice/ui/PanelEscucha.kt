package com.xpspeak.app.feature.voice.ui

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.feature.games.data.voz.EstadoEscucha
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.games.ui.comun.LocutorApp
import kotlinx.coroutines.delay

/**
 * Lo que ve el usuario mientras el micrófono está abierto (RF-09, CU-05): barras que se
 * mueven con su voz, lo que el reconocedor va entendiendo y un botón «Listo» para calificar
 * sin esperar la pausa. Es el mismo panel en Eco Vocal, Práctica y Pronunciación.
 *
 * @param conBoton false cuando la pantalla ya tiene su propio botón de terminar (Eco Vocal).
 */
@Composable
fun PanelEscucha(
    estado: EstadoEscucha,
    onTerminar: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = XPMarino,
    conBoton: Boolean = true
) {
    AvisoInicioEscucha(estado)
    when (estado) {
        EstadoEscucha.Inactivo -> Unit
        EstadoEscucha.Preparando -> Espera("Preparando el micrófono…", color, modifier)
        EstadoEscucha.Calificando -> Espera("Calificando tu pronunciación…", color, modifier)
        is EstadoEscucha.Escuchando -> Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BarrasNivel(estado.nivel, color)
            Text(
                if (estado.hablo) "Te oigo…" else "Te escucho. Di la frase ahora.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (estado.oyendo.isNotBlank()) {
                Text("“${estado.oyendo}”", style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario, textAlign = TextAlign.Center)
            }
            Text(
                if (estado.hablo) "Al terminar haz una pausa, o toca «Listo»." else "Cuando acabes, haz una pausa o toca «Listo».",
                style = MaterialTheme.typography.bodySmall,
                color = XPTextoSecundario,
                textAlign = TextAlign.Center
            )
            if (conBoton) {
                Button(onClick = onTerminar, shape = FormaBoton, colors = coloresBotonPrincipal(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Text("  Listo, calificar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Espera(texto: String, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = color)
        Text("  $texto", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}

/** Siete barras que suben con el volumen del micrófono: la señal de "sí te estoy oyendo". */
@Composable
fun BarrasNivel(nivel: Float, color: Color, modifier: Modifier = Modifier) {
    val suave by animateFloatAsState(nivel, animationSpec = tween(90), label = "nivel")
    // Las barras del centro suben más que las de las orillas, como un ecualizador.
    val pesos = listOf(0.45f, 0.7f, 0.9f, 1f, 0.9f, 0.7f, 0.45f)
    Row(
        modifier = modifier.height(36.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        pesos.forEach { peso ->
            val altura = 10.dp + 26.dp * (suave * peso).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height(altura)
                    .background(color.copy(alpha = if (suave > 0.05f) 1f else 0.45f), RoundedCornerShape(4.dp))
            )
        }
    }
}

/** Un toquecito de vibración y un bip cortos al abrirse el micrófono: "ya puedes hablar". */
@Composable
private fun AvisoInicioEscucha(estado: EstadoEscucha) {
    val context = LocalContext.current
    val escuchando = estado is EstadoEscucha.Escuchando
    LaunchedEffect(escuchando) {
        if (!escuchando) return@LaunchedEffect
        runCatching {
            context.getSystemService(Vibrator::class.java)?.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
        }
        val tono = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 60) }.getOrNull() ?: return@LaunchedEffect
        try {
            tono.startTone(ToneGenerator.TONE_PROP_BEEP, 90)
            delay(200)
        } finally {
            tono.release()
        }
    }
}

/**
 * Si la voz del teléfono no funciona (sin voz en inglés, motor caído), se dice en pantalla
 * en lugar de que «Escuchar» quede mudo, con acceso a los ajustes de voz y a reintentar.
 */
@Composable
fun AvisoVoz(locutor: Locutor, modifier: Modifier = Modifier) {
    val problema by locutor.problema.collectAsState()
    val mensaje = problema ?: return
    val context = LocalContext.current
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.VolumeOff, contentDescription = null, tint = XPNaranjaTexto, modifier = Modifier.size(22.dp))
                Text("  $mensaje", style = MaterialTheme.typography.bodySmall, color = XPNaranjaTexto, modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = {
                    runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }) { Text("Ajustes de voz", color = XPMarino, fontWeight = FontWeight.Bold) }
                TextButton(onClick = { LocutorApp.reiniciar(context) }) { Text("Reintentar", color = XPMarino, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
