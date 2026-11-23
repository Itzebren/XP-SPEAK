package com.xpspeak.app.feature.games.ui.comun

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.xpspeak.app.R
import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.core.ui.componentes.recursoDrawable
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.feature.games.domain.CalculadorXp
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.ResultadoPartida
import com.xpspeak.app.feature.progress.ui.InsigniaNueva
import kotlin.math.roundToInt

/**
 * Marco común de los cuatro juegos: encabezado con el color del juego, carga,
 * "¿Retomar la partida?", resultado final y errores. Cada juego solo dibuja su
 * mecánica en [contenido] y toma sus colores de [LocalTemaJuego].
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
    val tema = temaDe(juego)

    // Un solo SoundPool por partida: así los sonidos ya están cargados en la primera acción.
    CompositionLocalProvider(LocalSonidos provides rememberSonidos(), LocalTemaJuego provides tema) {
        // imePadding: con edge-to-edge (targetSdk 37) adjustResize ya no encoge la pantalla y
        // el teclado tapaba la respuesta escrita de Misión.
        Column(modifier = Modifier.fillMaxSize().background(tema.suave).imePadding()) {
            EncabezadoJuego(juego, onVolver, (fase as? FasePartida.Jugando<E>)?.estado?.let(progreso))
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (fase) {
                    FasePartida.Cargando, FasePartida.Guardando -> Centrado { CircularProgressIndicator(color = tema.texto) }
                    is FasePartida.Retomar -> DialogoRetomar(
                        onRetomar = viewModel::retomar,
                        onNueva = viewModel::empezarDeNuevo
                    )
                    is FasePartida.Jugando -> contenido(fase.estado)
                    is FasePartida.Terminada -> PantallaResultado(fase.resultado, viewModel::jugarOtraVez, onVolver)
                    is FasePartida.SinContenido -> Mensaje(Icons.Filled.Lock, fase.mensaje, onVolver)
                    is FasePartida.Error -> Mensaje(Icons.Filled.ErrorOutline, fase.mensaje, onVolver)
                }
            }
        }
    }
}

/** Encabezado del color del juego con su portada y el avance de la partida. */
@Composable
private fun EncabezadoJuego(juego: Minijuego, onVolver: () -> Unit, progreso: Float?) {
    val tema = LocalTemaJuego.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(tema.acento)
            .statusBarsPadding()
            .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver al catálogo", tint = tema.sobreAcento)
            }
            Text(
                juego.titulo,
                fontFamily = AdlamDisplay,
                style = MaterialTheme.typography.titleLarge,
                color = tema.sobreAcento,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            val portada = recursoDrawable("juego_${juego.id.replace('-', '_')}")
            if (portada != 0) {
                Image(
                    painterResource(portada),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                )
            }
        }
        if (progreso != null) {
            LinearProgressIndicator(
                progress = { progreso.coerceIn(0f, 1f) },
                color = tema.sobreAcento,
                trackColor = tema.sobreAcento.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round,
                modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp).height(8.dp)
            )
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
        confirmButton = { Button(colors = coloresBotonPrincipal(), shape = FormaBoton, onClick = onRetomar) { Text("Retomar") } },
        dismissButton = { TextButton(onClick = onNueva) { Text("Empezar de nuevo") } }
    )
}

@Composable
private fun ColumnScope.Mensaje(icono: ImageVector, mensaje: String, onVolver: () -> Unit) {
    val tema = LocalTemaJuego.current
    Centrado {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = tema.acento, shape = CircleShape, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icono, contentDescription = null, tint = tema.sobreAcento, modifier = Modifier.size(36.dp))
                }
            }
            Text(mensaje, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
            Button(colors = coloresBotonPrincipal(), shape = FormaBoton, onClick = onVolver) { Text("Volver al catálogo") }
        }
    }
}

/** CU-07 pasos 7 y 8: desempeño (precisión y tiempo), XP y racha. */
@Composable
private fun ColumnScope.PantallaResultado(resultado: ResultadoPartida, onOtraVez: () -> Unit, onVolver: () -> Unit) {
    val tema = LocalTemaJuego.current
    val porcentaje = (resultado.precision * 100).roundToInt()
    var festejar by remember(resultado) { mutableStateOf(true) }
    if (festejar) InsigniaNueva(resultado.insigniasNuevas, resultado.nivelXpNuevo, onCerrar = { festejar = false })
    Column(
        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TarjetaJuego {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    when {
                        resultado.precision >= 0.9 -> "¡Excelente!"
                        resultado.precision >= CalculadorXp.PRECISION_PARA_BONO -> "¡Muy bien!"
                        else -> "¡Buen intento!"
                    },
                    fontFamily = AdlamDisplay,
                    style = MaterialTheme.typography.headlineMedium,
                    color = XPMarino,
                    modifier = Modifier.semantics { heading() }
                )
                AnilloPorcentaje(porcentaje, tema.acento)
                Text(
                    "${resultado.aciertos} de ${resultado.total} correctas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = XPTextoSecundario
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Dato(Modifier.weight(1f), "Tiempo", formatearDuracion(resultado.duracionMs)) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = tema.texto, modifier = Modifier.size(28.dp))
            }
            Dato(Modifier.weight(1f), "Racha", Racha.texto(resultado.racha)) {
                Image(painterResource(R.drawable.icono_racha), contentDescription = null, modifier = Modifier.size(28.dp))
            }
        }

        Surface(
            color = XPNaranja.copy(alpha = 0.16f),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, XPNaranja),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.moneda_xp), contentDescription = null, modifier = Modifier.size(40.dp))
                    Text("+${resultado.xp} XP", style = MaterialTheme.typography.headlineLarge, color = XPNaranjaTexto, fontWeight = FontWeight.Bold)
                }
                resultado.nivelXpNuevo?.let {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = XPNaranja)
                        Text("¡Subiste a nivel $it!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (!resultado.confirmada) {
                    Text("Sin conexión: tu XP se confirma al reconectarte.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
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
            TarjetaJuego {
                Text("Para repasar", style = MaterialTheme.typography.titleMedium, color = XPMarino, fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    resultado.paraRepasar.forEachIndexed { i, tema ->
                        Surface(color = TONOS_FICHA[i % TONOS_FICHA.size], shape = RoundedCornerShape(50)) {
                            Text(tema, color = XPMarino, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
                Text("Estos temas saldrán más seguido en tus próximas partidas.", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onVolver,
            shape = FormaBoton,
            border = BorderStroke(2.dp, XPMarino),
            modifier = Modifier.weight(1f).heightIn(min = 54.dp)
        ) { Text("Catálogo", color = XPMarino, fontWeight = FontWeight.Bold) }
        Button(
            colors = coloresBotonPrincipal(),
            shape = FormaBoton,
            onClick = onOtraVez,
            modifier = Modifier.weight(1f).heightIn(min = 54.dp)
        ) { Text("Jugar otra vez", fontWeight = FontWeight.Bold) }
    }
}

/** Precisión como anillo de color con el porcentaje al centro. */
@Composable
private fun AnilloPorcentaje(porcentaje: Int, color: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
        CircularProgressIndicator(
            progress = { porcentaje / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            strokeWidth = 12.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$porcentaje%", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = XPMarino)
            Text("precisión", style = MaterialTheme.typography.labelMedium, color = XPTextoSecundario)
        }
    }
}

@Composable
private fun Dato(modifier: Modifier, etiqueta: String, valor: String, icono: @Composable () -> Unit) {
    Surface(color = XPBlanco, shape = RoundedCornerShape(18.dp), shadowElevation = 2.dp, modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            icono()
            Text(valor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XPMarino)
            Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = XPTextoSecundario)
        }
    }
}

fun formatearDuracion(ms: Long): String {
    val segundos = (ms / 1000).toInt()
    return if (segundos < 60) "$segundos s" else "${segundos / 60} min ${segundos % 60} s"
}

/** Resultado de una acción: acierto o error (CU-07 paso 6). */
enum class Veredicto { ACIERTO, ERROR }

/**
 * Feedback visual, háptico y auditivo de cada acción (§4.5, CU-07 paso 6):
 * verde, vibración corta y sonido de acierto si acierta; color de error, su
 * sonido y la explicación si no. Los errores se explican, no se castigan
 * (filtro afectivo, PDF §2.4). [sonido] en false para avisos que no son una
 * respuesta del jugador (p. ej. el micrófono).
 */
@Composable
fun TarjetaFeedback(veredicto: Veredicto?, titulo: String, detalle: String? = null, sonido: Boolean = true) {
    val haptica = LocalHapticFeedback.current
    val sonidos = LocalSonidos.current
    // Con el detalle en la llave, dos errores seguidos con el mismo título también suenan.
    LaunchedEffect(veredicto, titulo, detalle) {
        when (veredicto) {
            Veredicto.ACIERTO -> haptica.performHapticFeedback(HapticFeedbackType.Confirm)
            Veredicto.ERROR -> haptica.performHapticFeedback(HapticFeedbackType.Reject)
            null -> Unit
        }
        if (sonido && veredicto != null) sonidos?.reproducir(veredicto)
    }
    val color by animateColorAsState(colorDe(veredicto), label = "feedback")
    val icono = when (veredicto) {
        Veredicto.ACIERTO -> Icons.Filled.CheckCircle
        Veredicto.ERROR -> if (sonido) Icons.Filled.Cancel else Icons.Filled.Info
        null -> Icons.Filled.Info
    }
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, color),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icono, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Column {
                Text(titulo, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                if (detalle != null) Text(detalle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun colorDe(veredicto: Veredicto?): Color = when (veredicto) {
    Veredicto.ACIERTO -> XPVerdeExito
    Veredicto.ERROR -> MaterialTheme.colorScheme.error
    null -> LocalTemaJuego.current.texto
}
