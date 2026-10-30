package com.xpspeak.app.feature.voice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.voice.domain.Retroalimentacion

/*
 * RF-09: la retroalimentación de pronunciación, igual en toda la app (Práctica, lecciones,
 * Eco Vocal, Misión, Orden Maestro y chat): puntaje de la frase, palabras en verde, amarillo
 * o rojo, y al tocar una, sus sonidos en IPA con el que se oyó en su lugar.
 */

/** Mismos colores que Eco Vocal (verde/amarillo/rojo). */
@Composable
fun colorPorNivel(nivel: Pronunciacion.Nivel): Color = when (nivel) {
    Pronunciacion.Nivel.BUENO -> XPVerdeExito
    Pronunciacion.Nivel.REGULAR -> Color(0xFFB7791F)
    Pronunciacion.Nivel.MALO -> MaterialTheme.colorScheme.error
}

/** Las palabras de la frase coloreadas; la tocada se enmarca. Las omitidas van tachadas. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PalabrasColoreadas(
    evaluacion: EvaluacionVoz,
    seleccionada: Int?,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    alineacion: Alignment.Horizontal = Alignment.Start,
    compacto: Boolean = false
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp, alineacion),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
    ) {
        evaluacion.palabras.forEachIndexed { i, palabra ->
            val omitida = palabra.error == "Omission"
            Text(
                palabra.palabra,
                color = Color.White,
                style = if (compacto) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (omitida) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .background(colorPorNivel(Retroalimentacion.nivelDe(palabra)), RoundedCornerShape(8.dp))
                    .then(if (seleccionada == i) Modifier.border(BorderStroke(2.dp, XPMarino), RoundedCornerShape(8.dp)) else Modifier)
                    .clickable(onClickLabel = "Ver detalle") { onSeleccionar(i) }
                    .padding(horizontal = if (compacto) 6.dp else 8.dp, vertical = if (compacto) 2.dp else 4.dp)
            )
        }
    }
}

/** Nivel sonido: puntaje de la palabra, qué pasó y cada fonema IPA con su precisión. */
@Composable
fun DetallePalabra(palabra: PalabraEvaluada, onEscuchar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "«${palabra.palabra}»  ${palabra.puntaje}/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorPorNivel(Retroalimentacion.nivelDe(palabra)),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEscuchar) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text(" Escuchar")
                }
            }
            Text(Retroalimentacion.descripcion(palabra), color = XPTexto)
            if (palabra.fonemas.isNotEmpty()) {
                val debiles = Retroalimentacion.fonemasAPracticar(palabra).map { it.fonema }.toSet()
                Text("Sonidos:", style = MaterialTheme.typography.labelLarge, color = XPTexto)
                FlowRowFonemas(palabra, debiles)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowRowFonemas(palabra: PalabraEvaluada, debiles: Set<String>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        palabra.fonemas.forEach { fonema ->
            val color = colorPorNivel(Pronunciacion.nivelDe(fonema.puntaje))
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, color)) {
                Text(
                    "/${fonema.fonema}/ ${fonema.puntaje}",
                    fontFamily = FontFamily.Default,
                    fontWeight = if (fonema.fonema in debiles) FontWeight.Bold else FontWeight.Normal,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** Nivel frase: precisión, fluidez, completitud y el general de Azure (cuando los hay). */
@Composable
fun MetricasFrase(evaluacion: EvaluacionVoz, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Metrica("Precisión de los sonidos", evaluacion.puntaje)
        evaluacion.fluidez?.let { Metrica("Fluidez", it) }
        evaluacion.completitud?.let { Metrica("Completitud", it) }
        evaluacion.puntajeGeneral?.let { Metrica("General (Azure)", it) }
    }
}

@Composable
private fun Metrica(nombre: String, valor: Int) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(nombre, modifier = Modifier.weight(1f), color = XPTexto)
        Text("$valor/100", fontWeight = FontWeight.Bold, color = colorPorNivel(Pronunciacion.nivelDe(valor)))
    }
}

/** Aviso cuando calificó el teléfono y no Azure. */
@Composable
fun NotaFuente(evaluacion: EvaluacionVoz) {
    if (evaluacion.fuente != FuenteEvaluacion.DISPOSITIVO) return
    Text(
        if (evaluacion.soloTranscrita) {
            "Sin conexión con el servicio de voz: el teléfono solo transcribió lo que dijiste."
        } else {
            "Evaluación básica del teléfono (sin conexión con el servicio de voz): revisa qué palabras se entendieron, no los sonidos."
        },
        style = MaterialTheme.typography.bodySmall,
        color = XPTextoSecundario
    )
}

/** Puntaje de la frase en anillo. */
@Composable
fun AnilloPuntaje(puntaje: Int, modifier: Modifier = Modifier) {
    val color = colorPorNivel(Pronunciacion.nivelDe(puntaje))
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(84.dp)) {
        CircularProgressIndicator(
            progress = { puntaje / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            strokeWidth = 9.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )
        Text("$puntaje", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color)
    }
}

/**
 * La retroalimentación completa en una tarjeta: frase, palabras y sonidos. [locutor] lee
 * despacio la palabra tocada. La selección vive aquí; se reinicia con cada evaluación.
 */
@Composable
fun PanelPronunciacion(evaluacion: EvaluacionVoz, locutor: Locutor, modifier: Modifier = Modifier) {
    var seleccionada by remember(evaluacion) { mutableStateOf<Int?>(null) }
    TarjetaXP(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (!evaluacion.soloTranscrita) AnilloPuntaje(evaluacion.puntaje)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (evaluacion.soloTranscrita) "Te entendimos" else "Precisión de 100 puntos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = XPMarino
                )
                Text(Retroalimentacion.resumen(evaluacion), style = MaterialTheme.typography.bodyMedium, color = XPTexto)
            }
        }
        if (evaluacion.palabras.isNotEmpty()) {
            Text("Toca una palabra para ver sus sonidos:", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
            PalabrasColoreadas(evaluacion, seleccionada, onSeleccionar = { seleccionada = if (seleccionada == it) null else it })
        }
        seleccionada?.let { evaluacion.palabras.getOrNull(it) }?.let { palabra ->
            DetallePalabra(palabra, onEscuchar = { locutor.decir(palabra.palabra, lento = true) })
        }
        if (!evaluacion.soloTranscrita) MetricasFrase(evaluacion)
        if (evaluacion.transcripcion.isNotBlank()) {
            Text("Oímos: “${evaluacion.transcripcion}”", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        }
        NotaFuente(evaluacion)
        Text(
            "Tu audio no se guarda: esta evaluación desaparece al salir.",
            style = MaterialTheme.typography.bodySmall,
            color = XPTextoSecundario
        )
    }
}
