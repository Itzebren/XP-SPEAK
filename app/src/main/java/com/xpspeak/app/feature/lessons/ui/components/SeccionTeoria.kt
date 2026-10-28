package com.xpspeak.app.feature.lessons.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.componentes.DistintivoNivel
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPGris
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.lessons.data.SeccionDto
import com.xpspeak.app.feature.lessons.data.TipoSeccion

/** Tarjetas de vocabulario que se ven a la vez; las demás, con las flechas. */
private const val TARJETAS_POR_PAGINA = 3

/**
 * Una sección de teoría (introducción, vocabulario, gramática o diálogo) — CU-06 paso 3,
 * con el diseño de la Ilustración 39. [onEscuchar] lee en voz alta un texto en inglés.
 */
@Composable
fun SeccionTeoria(
    seccion: SeccionDto,
    nivel: String,
    @DrawableRes portada: Int,
    canDo: List<String>,
    onEscuchar: (String) -> Unit
) {
    when (seccion.tipo) {
        TipoSeccion.INTRODUCCION -> Introduccion(seccion, nivel, portada, canDo)
        TipoSeccion.VOCABULARIO -> Vocabulario(seccion, onEscuchar)
        TipoSeccion.DIALOGO -> Dialogo(seccion, onEscuchar)
        else -> Explicacion(seccion)
    }
}

@Composable
private fun Introduccion(seccion: SeccionDto, nivel: String, @DrawableRes portada: Int, canDo: List<String>) {
    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(
                painterResource(portada),
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.weight(1f)) {
                TituloTarjeta("Introducción")
                seccion.titulo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
            DistintivoNivel(nivel)
        }
        seccion.cuerpo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        canDo.forEach { Text("✓ $it", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario) }
    }
}

/** Tarjetas de tres en tres con flechas; al tocar una se escucha la palabra en inglés. */
@Composable
private fun Vocabulario(seccion: SeccionDto, onEscuchar: (String) -> Unit) {
    val paginas = seccion.items.orEmpty().filter { it.en != null }.chunked(TARJETAS_POR_PAGINA)
    var pagina by rememberSaveable(seccion.titulo) { mutableIntStateOf(0) }
    val actual = paginas.getOrNull(pagina).orEmpty()

    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                TituloTarjeta("Tarjetas")
                seccion.titulo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario) }
            }
            if (paginas.size > 1) {
                BotonFlecha(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Tarjetas anteriores", habilitado = pagina > 0) { pagina-- }
                BotonFlecha(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Tarjetas siguientes", habilitado = pagina < paginas.lastIndex) { pagina++ }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            actual.forEach { item ->
                val en = item.en.orEmpty()
                Surface(
                    color = XPBlanco,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(MaterialTheme.shapes.medium)
                        .clickable(onClickLabel = "Escuchar") { onEscuchar(en) }
                ) {
                    Column(
                        modifier = Modifier.heightIn(min = 96.dp).padding(horizontal = 6.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                    ) {
                        Text(en, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        item.es?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario, textAlign = TextAlign.Center) }
                    }
                }
            }
            // Huecos para que la última página no estire sus tarjetas.
            repeat(TARJETAS_POR_PAGINA - actual.size) { Box(modifier = Modifier.weight(1f)) }
        }
        actual.mapNotNull { it.nota?.let { nota -> "${it.en}: $nota" } }.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        }
    }
}

@Composable
private fun Dialogo(seccion: SeccionDto, onEscuchar: (String) -> Unit) {
    val lineas = seccion.lineas.orEmpty()
    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                color = XPBlanco,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .size(36.dp)
                    .clip(MaterialTheme.shapes.small)
                    .clickable { onEscuchar(lineas.joinToString(" ") { it.en }) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar el diálogo", tint = XPMarino)
                }
            }
            Column {
                TituloTarjeta("Diálogo de ejemplo")
                seccion.titulo?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario) }
            }
        }
        Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                lineas.forEach { linea ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${linea.hablante}:", fontWeight = FontWeight.Bold, color = XPMarino)
                        Column {
                            Text(linea.en, style = MaterialTheme.typography.bodyMedium)
                            Text(linea.es, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Explicacion(seccion: SeccionDto) {
    TarjetaXP {
        TituloTarjeta(seccion.titulo ?: "Gramática")
        seccion.cuerpo?.let { Text(it) }
        seccion.explicacion?.let { Text(it) }
        seccion.ejemplos?.forEach {
            Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                Text(it, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun TituloTarjeta(texto: String) {
    Text(
        texto,
        fontFamily = AdlamDisplay,
        style = MaterialTheme.typography.titleLarge,
        color = XPMarino,
        modifier = Modifier.semantics { heading() }
    )
}

/** Flecha blanca cuadrada del carrusel de tarjetas. */
@Composable
private fun BotonFlecha(icono: ImageVector, descripcion: String, habilitado: Boolean, onClick: () -> Unit) {
    Surface(
        color = XPBlanco,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = habilitado, onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icono, contentDescription = descripcion, tint = if (habilitado) XPMarino else XPGris)
        }
    }
}
