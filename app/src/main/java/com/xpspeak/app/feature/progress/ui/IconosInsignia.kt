package com.xpspeak.app.feature.progress.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.componentes.recursoDrawable
import com.xpspeak.app.core.ui.theme.XPAzul
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.feature.progress.data.InsigniaDto

/**
 * Ilustraciones provisionales de las insignias (docs/progreso-diseno.md
 * Decisión 6): un ícono de Material dentro de un círculo de color, hasta
 * tener el arte final. `icono` es la clave que manda el servidor.
 */
private data class Estilo(val icono: ImageVector, val color: Color)

private val ESTILOS = mapOf(
    "estrella" to Estilo(Icons.Filled.Star, Color(0xFFF9A825)),
    "chat" to Estilo(Icons.AutoMirrored.Filled.Chat, XPAzul),
    "juego" to Estilo(Icons.Filled.SportsEsports, Color(0xFF8E24AA)),
    "fuego" to Estilo(Icons.Filled.LocalFireDepartment, Color(0xFFE64A19)),
    "brujula" to Estilo(Icons.Filled.Explore, Color(0xFF00897B)),
    "birrete" to Estilo(Icons.Filled.School, Color(0xFF3949AB)),
    "microfono" to Estilo(Icons.Filled.Mic, Color(0xFFD81B60)),
    "trofeo" to Estilo(Icons.Filled.EmojiEvents, XPNaranja),
    "calendario" to Estilo(Icons.Filled.CalendarMonth, XPVerdeExito)
)
private val ESTILO_DESCONOCIDO = Estilo(Icons.Filled.MilitaryTech, XPAzul)

/** Bloqueada: gris y con candado, nunca solo el color (RNF-01). */
@Composable
fun IconoInsignia(insignia: InsigniaDto, obtenida: Boolean, tamano: Dp = 64.dp, modifier: Modifier = Modifier) {
    val estilo = ESTILOS[insignia.icono] ?: ESTILO_DESCONOCIDO
    // Bloqueada: gris que contrasta con la tarjeta (surfaceVariant se confundía con el fondo).
    val fondo = if (obtenida) estilo.color else MaterialTheme.colorScheme.outlineVariant
    val tinte = if (obtenida) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    Box(modifier = modifier.size(tamano)) {
        val arte = recursoDrawable("insignia_${insignia.id.replace('-', '_')}")
        if (arte != 0) {
            // Arte final (res/drawable/insignia_<id>): bloqueada en escala de grises.
            Image(
                painter = painterResource(arte),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                colorFilter = if (obtenida) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
                modifier = Modifier.size(tamano).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(tamano).clip(CircleShape).background(fondo),
                contentAlignment = Alignment.Center
            ) {
                Icon(estilo.icono, contentDescription = null, tint = tinte, modifier = Modifier.size(tamano * 0.55f))
            }
        }
        if (!obtenida) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(3.dp)
            ) {
                Icon(
                    Icons.Filled.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(tamano * 0.25f)
                )
            }
        }
    }
}
