package com.xpspeak.app.feature.lessons.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.componentes.DistintivoNivel
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.lessons.data.SeccionDto
import com.xpspeak.app.feature.lessons.data.TipoSeccion

/**
 * Una sección de teoría (introducción, vocabulario, gramática o diálogo) — CU-06 paso 3,
 * con el diseño de la Ilustración 39. [onEscuchar] lee en voz alta un texto en inglés.
 */
@Composable
fun SeccionTeoria(
    seccion: SeccionDto,
    nivel: String,
    @DrawableRes portada: Int,
    onEscuchar: (String) -> Unit
) {
    when (seccion.tipo) {
        TipoSeccion.INTRODUCCION -> Introduccion(seccion, nivel, portada)
        TipoSeccion.VOCABULARIO -> Vocabulario(seccion, onEscuchar)
        TipoSeccion.DIALOGO -> Dialogo(seccion, onEscuchar)
        else -> Explicacion(seccion)
    }
}

@Composable
private fun Introduccion(seccion: SeccionDto, nivel: String, @DrawableRes portada: Int) {
    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource(portada), contentDescription = null, modifier = Modifier.size(64.dp))
            Column(modifier = Modifier.weight(1f)) {
                TituloTarjeta("Introducción")
                seccion.titulo?.let { Text(it, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold) }
            }
            DistintivoNivel(nivel)
        }
        seccion.cuerpo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Tarjetas en carrusel: la palabra en inglés, su traducción y un botón para escucharla. */
@Composable
private fun Vocabulario(seccion: SeccionDto, onEscuchar: (String) -> Unit) {
    TarjetaXP {
        TituloTarjeta(seccion.titulo ?: "Vocabulario")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(seccion.items.orEmpty()) { item ->
                val en = item.en ?: return@items
                Surface(color = XPBlanco, shape = MaterialTheme.shapes.medium, modifier = Modifier.width(132.dp)) {
                    Column(
                        modifier = Modifier.heightIn(min = 128.dp).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(en, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        item.es?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario, textAlign = TextAlign.Center) }
                        BotonEscuchar("Escuchar $en") { onEscuchar(en) }
                    }
                }
            }
        }
        seccion.items.orEmpty().mapNotNull { it.nota?.let { nota -> "${it.en}: $nota" } }.forEach {
            Text(it, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        }
    }
}

@Composable
private fun Dialogo(seccion: SeccionDto, onEscuchar: (String) -> Unit) {
    val lineas = seccion.lineas.orEmpty()
    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BotonEscuchar("Escuchar el diálogo") { onEscuchar(lineas.joinToString(" ") { it.en }) }
            TituloTarjeta(seccion.titulo ?: "Diálogo de ejemplo")
        }
        Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lineas.forEach { linea ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${linea.hablante}:", fontWeight = FontWeight.Bold, color = XPMarino)
                        Column {
                            Text(linea.en, style = MaterialTheme.typography.bodyLarge)
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
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun BotonEscuchar(descripcion: String, onClick: () -> Unit) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = XPAzulClaro, contentColor = XPMarino)
    ) {
        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = descripcion)
    }
}
