package com.xpspeak.app.feature.voice.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.games.domain.EvaluacionVoz
import com.xpspeak.app.feature.games.domain.FonemaEvaluado
import com.xpspeak.app.feature.games.domain.FuenteEvaluacion
import com.xpspeak.app.feature.games.domain.PalabraEvaluada
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.domain.SilabaEvaluada
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.voice.domain.GuiaSonidos
import com.xpspeak.app.feature.voice.domain.Retroalimentacion

/*
 * RF-09: la retroalimentación de pronunciación, igual en toda la app (Práctica, lecciones,
 * Eco Vocal y chat en vivo). Todo lo que devuelve Azure, ordenado de lo general a lo
 * concreto: puntaje y resumen → la frase palabra por palabra → la palabra tocada por sílabas
 * y sonidos (con cómo corregir cada sonido) → los sonidos a practicar de toda la frase →
 * consejos de ritmo y entonación → los puntajes detallados → lo que se oyó.
 */

/** Mismos colores que Eco Vocal (verde/amarillo/rojo). */
@Composable
fun colorPorNivel(nivel: Pronunciacion.Nivel): Color = when (nivel) {
    Pronunciacion.Nivel.BUENO -> XPVerdeExito
    Pronunciacion.Nivel.REGULAR -> Color(0xFFB7791F)
    Pronunciacion.Nivel.MALO -> MaterialTheme.colorScheme.error
}

@Composable
private fun colorPuntaje(puntaje: Int) = colorPorNivel(Pronunciacion.nivelDe(puntaje))

/**
 * La retroalimentación completa, para poner dentro de cualquier tarjeta u hoja.
 * [onEscuchar] lee despacio una palabra o un ejemplo. Con [compacta] (Eco Vocal, donde la
 * partida sigue) los puntajes detallados empiezan plegados en una sola línea. En pantallas
 * anchas (tablet) va en dos columnas: la frase y la palabra a la izquierda; sonidos,
 * consejos y puntajes a la derecha.
 */
@Composable
fun RetroalimentacionVoz(
    evaluacion: EvaluacionVoz,
    onEscuchar: (String) -> Unit,
    modifier: Modifier = Modifier,
    compacta: Boolean = false
) {
    // Se abre sola la palabra con menor puntaje: siempre se ve cómo se pronunció, sin buscarlo.
    var seleccionada by remember(evaluacion) {
        mutableStateOf(evaluacion.palabras.indices.minByOrNull { evaluacion.palabras[it].puntaje })
    }
    val frase: @Composable () -> Unit = {
        if (evaluacion.palabras.isNotEmpty()) {
            Seccion("Tu frase, palabra por palabra") {
                Leyenda()
                PalabrasColoreadas(evaluacion, seleccionada, onSeleccionar = { seleccionada = if (seleccionada == it) null else it })
                if (evaluacion.palabras.size > 1) {
                    Text(
                        if (seleccionada == null) "Toca una palabra para ver cómo la dijiste." else "Toca otra palabra para ver su detalle.",
                        style = MaterialTheme.typography.bodySmall,
                        color = XPTextoSecundario
                    )
                }
                seleccionada?.let { evaluacion.palabras.getOrNull(it) }?.let { palabra ->
                    DetallePalabra(palabra, onEscuchar = onEscuchar)
                }
            }
        }
    }
    val resto: @Composable () -> Unit = {
        val sonidos = Retroalimentacion.sonidosAPracticar(evaluacion)
        if (sonidos.isNotEmpty() && evaluacion.palabras.size > 1) {
            Seccion("Sonidos para practicar en esta frase") {
                sonidos.forEach { sonido ->
                    SonidoPractica(sonido, onAbrir = {
                        seleccionada = evaluacion.palabras.indexOfFirst { it.palabra == sonido.palabras.first() }.takeIf { it >= 0 }
                    })
                }
            }
        }
        val consejos = Retroalimentacion.consejosFrase(evaluacion)
        if (consejos.isNotEmpty()) {
            Seccion("Para la próxima") {
                consejos.forEach { Consejo(it) }
            }
        }
        val metricas = Retroalimentacion.metricas(evaluacion)
        if (metricas.size > 1) Metricas(metricas, abiertasAlInicio = !compacta)
        if (evaluacion.transcripcion.isNotBlank()) {
            Text("Oímos: “${evaluacion.transcripcion}”", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        }
        NotaFuente(evaluacion)
    }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val espacio = Arrangement.spacedBy(14.dp)
        val dosColumnas = maxWidth >= ANCHO_DOS_COLUMNAS
        Column(verticalArrangement = espacio) {
            Encabezado(evaluacion)
            Retroalimentacion.avisoAudio(evaluacion)?.let { AvisoAudio(it) }
            if (dosColumnas) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column(modifier = Modifier.weight(1.1f), verticalArrangement = espacio) { frase() }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = espacio) { resto() }
                }
            } else {
                frase()
                resto()
            }
        }
    }
}

/** Desde este ancho (tablet, teléfono horizontal) la retroalimentación va en dos columnas. */
private val ANCHO_DOS_COLUMNAS = 600.dp

/** Puntaje, título y resumen en una línea de lectura. */
@Composable
private fun Encabezado(evaluacion: EvaluacionVoz) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!evaluacion.soloTranscrita) AnilloPuntaje(evaluacion.puntaje)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(Retroalimentacion.titulo(evaluacion), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XPMarino)
            if (!evaluacion.soloTranscrita) {
                Text("Pronunciación: ${evaluacion.puntaje} de 100", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
            }
            Text(Retroalimentacion.resumen(evaluacion), style = MaterialTheme.typography.bodyMedium, color = XPTexto)
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = XPMarino)
        contenido()
    }
}

/** Qué significa cada color, una sola vez. */
@Composable
private fun Leyenda() {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(Pronunciacion.Nivel.BUENO to "Bien", Pronunciacion.Nivel.REGULAR to "Casi", Pronunciacion.Nivel.MALO to "A practicar").forEach { (nivel, texto) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).background(colorPorNivel(nivel), CircleShape))
                Text(" $texto", style = MaterialTheme.typography.labelMedium, color = XPTextoSecundario)
            }
        }
    }
}

@Composable
private fun AvisoAudio(texto: String) {
    Surface(color = Color(0xFFFFF4E0), shape = MaterialTheme.shapes.small, border = BorderStroke(1.dp, Color(0xFFFFC107))) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = XPNaranjaTexto, modifier = Modifier.size(22.dp))
            Text("  $texto", style = MaterialTheme.typography.bodySmall, color = XPTexto)
        }
    }
}

@Composable
private fun Consejo(texto: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = Color(0xFFB7791F), modifier = Modifier.size(18.dp))
        Text("  $texto", style = MaterialTheme.typography.bodyMedium, color = XPTexto)
    }
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
            val nivel = Retroalimentacion.nivelDe(palabra)
            Text(
                palabra.palabra,
                color = Color.White,
                style = if (compacto) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textDecoration = if (omitida) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .background(colorPorNivel(nivel), RoundedCornerShape(8.dp))
                    .then(if (seleccionada == i) Modifier.border(BorderStroke(3.dp, XPMarino), RoundedCornerShape(8.dp)) else Modifier)
                    .semantics { contentDescription = "${palabra.palabra}, ${palabra.puntaje} de 100" }
                    .clickable(onClickLabel = "Ver detalle") { onSeleccionar(i) }
                    .padding(horizontal = if (compacto) 6.dp else 10.dp, vertical = if (compacto) 2.dp else 6.dp)
            )
        }
    }
}

/**
 * Nivel palabra: puntaje, qué pasó, el ritmo, sus sílabas, cada sonido en IPA y cómo
 * corregir los que salieron mal.
 */
@Composable
fun DetallePalabra(palabra: PalabraEvaluada, onEscuchar: (String) -> Unit, modifier: Modifier = Modifier) {
    val color = colorPorNivel(Retroalimentacion.nivelDe(palabra))
    Surface(
        color = XPBlanco,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("«${palabra.palabra}»", style = MaterialTheme.typography.titleLarge, color = XPMarino, fontWeight = FontWeight.Bold)
                    Text(Retroalimentacion.descripcion(palabra), color = XPTexto, style = MaterialTheme.typography.bodyMedium)
                }
                Pastilla("${palabra.puntaje}", color)
            }
            TextButton(onClick = { onEscuchar(palabra.palabra) }, modifier = Modifier.height(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("  Escúchala despacio", fontWeight = FontWeight.Bold)
            }
            Retroalimentacion.notasRitmo(palabra).forEach { Consejo(it) }
            if (palabra.silabas.size > 1) {
                Text("Por sílabas", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
                Silabas(palabra.silabas)
            }
            if (palabra.fonemas.isNotEmpty()) {
                Text("Sonido por sonido", style = MaterialTheme.typography.labelLarge, color = XPTextoSecundario)
                Fonemas(palabra.fonemas)
            }
            Retroalimentacion.fonemasAPracticar(palabra).take(2).forEach { ConsejoSonido(it, onEscuchar) }
        }
    }
}

@Composable
private fun Pastilla(texto: String, color: Color) {
    Surface(color = color, shape = RoundedCornerShape(12.dp)) {
        Text(
            texto,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** «wee · kend»: cómo se escribe cada sílaba, cómo suena y qué tan bien salió. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Silabas(silabas: List<SilabaEvaluada>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        silabas.forEach { silaba ->
            val color = colorPuntaje(silaba.puntaje)
            Surface(color = color.copy(alpha = 0.10f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.5.dp, color)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text(silaba.grafema.ifBlank { silaba.silaba }, fontWeight = FontWeight.Bold, color = XPTexto, style = MaterialTheme.typography.titleSmall)
                    Text("/${silaba.silaba}/", fontFamily = FontFamily.Default, color = XPTextoSecundario, style = MaterialTheme.typography.bodySmall)
                    Text("${silaba.puntaje}", fontWeight = FontWeight.Bold, color = color, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** Cada sonido en IPA con su puntaje; si se oyó otro, «/ð/ → /b/». */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Fonemas(fonemas: List<FonemaEvaluado>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        fonemas.forEach { fonema ->
            val color = colorPuntaje(fonema.puntaje)
            val malo = Pronunciacion.nivelDe(fonema.puntaje) != Pronunciacion.Nivel.BUENO || fonema.oido != null
            Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, color)) {
                Text(
                    buildString {
                        append("/${fonema.fonema}/")
                        fonema.oido?.let { append(" → /$it/") }
                        append("  ${fonema.puntaje}")
                    },
                    fontFamily = FontFamily.Default,
                    fontWeight = if (malo) FontWeight.Bold else FontWeight.Normal,
                    color = color,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/** Cómo corregir un sonido de la palabra: qué se oyó, cómo se hace y un ejemplo para escuchar. */
@Composable
private fun ConsejoSonido(fonema: FonemaEvaluado, onEscuchar: (String) -> Unit) {
    val consejo = GuiaSonidos.consejo(fonema.fonema)
    TarjetaSonido(
        fonema = fonema.fonema,
        puntaje = fonema.puntaje,
        que = fonema.oido?.let { GuiaSonidos.confusion(fonema.fonema, it) },
        como = consejo?.comoSeHace,
        ejemplo = consejo?.ejemplo,
        onEscuchar = onEscuchar
    )
}

/** Un renglón por sonido débil de la frase; al tocarlo se abre la palabra donde salió peor. */
@Composable
private fun SonidoPractica(sonido: Retroalimentacion.SonidoAPracticar, onAbrir: () -> Unit) {
    val color = colorPuntaje(sonido.puntaje)
    Surface(
        color = XPBlanco,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().clickable(onClickLabel = "Ver la palabra", onClick = onAbrir)
    ) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("/${sonido.fonema}/", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XPMarino, modifier = Modifier.width(56.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "En " + sonido.palabras.joinToString(", ") { "«$it»" } + (sonido.oido?.let { " · sonó como /$it/" } ?: ""),
                    style = MaterialTheme.typography.bodyMedium,
                    color = XPTexto
                )
                Text("Toca para ver cómo corregirlo", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
            }
            Pastilla("${sonido.puntaje}", color)
        }
    }
}

@Composable
private fun TarjetaSonido(fonema: String, puntaje: Int, que: String?, como: String?, ejemplo: String?, onEscuchar: (String) -> Unit) {
    val color = colorPuntaje(puntaje)
    Surface(color = XPAzulClaro.copy(alpha = 0.35f), shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                Text("/$fonema/", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = XPMarino, fontFamily = FontFamily.Default)
                Text("$puntaje", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = color)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                que?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = XPTexto, fontWeight = FontWeight.SemiBold) }
                como?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = XPTexto) }
                ejemplo?.let { palabras ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Como en: $palabras", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario, modifier = Modifier.weight(1f))
                        IconButton(onClick = { onEscuchar(palabras.replace(",", ".")) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar ejemplos", tint = XPMarino, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

/** Nivel frase: cada métrica con barra y qué significa; plegable para no saturar. */
@Composable
private fun Metricas(metricas: List<Retroalimentacion.Metrica>, abiertasAlInicio: Boolean) {
    var abiertas by remember { mutableStateOf(abiertasAlInicio) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClickLabel = if (abiertas) "Ocultar" else "Mostrar") { abiertas = !abiertas }
        ) {
            Text("Tus puntajes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = XPMarino, modifier = Modifier.weight(1f))
            Icon(if (abiertas) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = XPMarino)
        }
        if (abiertas) {
            metricas.forEach { BarraMetrica(it) }
        } else {
            // Plegadas: todos los números en una línea; al tocar el título se ven con su explicación.
            Text(
                metricas.joinToString(" · ") { "${it.nombre} ${it.valor}" },
                style = MaterialTheme.typography.bodyMedium,
                color = XPTextoSecundario
            )
        }
    }
}

@Composable
private fun BarraMetrica(metrica: Retroalimentacion.Metrica) {
    val color = colorPuntaje(metrica.valor)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(metrica.nombre, modifier = Modifier.weight(1f), color = XPTexto, fontWeight = FontWeight.SemiBold)
            Text("${metrica.valor}/100", fontWeight = FontWeight.Bold, color = color)
        }
        LinearProgressIndicator(
            progress = { metrica.valor / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.15f),
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxWidth().height(8.dp)
        )
        Text(metrica.significado, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
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
    val color = colorPuntaje(puntaje)
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(84.dp)) {
        CircularProgressIndicator(
            progress = { puntaje / 100f },
            color = color,
            trackColor = color.copy(alpha = 0.18f),
            strokeWidth = 9.dp,
            strokeCap = StrokeCap.Round,
            modifier = Modifier.fillMaxSize()
        )
        Text("$puntaje", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
    }
}

/**
 * La retroalimentación completa en una tarjeta (Práctica de pronunciación y la hoja de
 * práctica de las lecciones). Se reinicia con cada evaluación.
 */
@Composable
fun PanelPronunciacion(evaluacion: EvaluacionVoz, locutor: Locutor, modifier: Modifier = Modifier) {
    TarjetaXP(modifier = modifier) {
        RetroalimentacionVoz(evaluacion, onEscuchar = { locutor.decir(it, lento = true) })
        Text(
            "Tu audio no se guarda: esta evaluación desaparece al salir.",
            style = MaterialTheme.typography.bodySmall,
            color = XPTextoSecundario
        )
    }
}

/**
 * Versión corta para el chat: puntaje, métricas en una línea y la frase coloreada; el botón
 * abre la retroalimentación completa en [onVerDetalle].
 */
@Composable
fun ResumenPronunciacion(evaluacion: EvaluacionVoz, onVerDetalle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.widthIn(max = 320.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pastilla("${evaluacion.puntaje}", colorPuntaje(evaluacion.puntaje))
            Text(
                buildString {
                    append("Pronunciación")
                    evaluacion.fluidez?.let { append(" · Fluidez $it") }
                    evaluacion.prosodia?.let { append(" · Entonación $it") }
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = XPMarino
            )
        }
        PalabrasColoreadas(evaluacion, seleccionada = null, onSeleccionar = { onVerDetalle() }, alineacion = Alignment.End, compacto = true)
        TextButton(onClick = onVerDetalle) {
            Text(
                Retroalimentacion.palabrasAPracticar(evaluacion).size.let { if (it == 0) "Ver cómo pronunciaste" else "Ver cómo mejorar ($it)" },
                fontWeight = FontWeight.Bold,
                color = XPMarino
            )
        }
    }
}
