package com.xpspeak.app.feature.games.ui.comun

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.theme.XPAmarillo
import com.xpspeak.app.core.ui.theme.XPAzul
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.feature.games.domain.Minijuego

/**
 * Color propio de cada juego (mismo tono que su portada), sacado de la paleta del
 * Trabajo Terminal: encabezado, fondo, botones y detalles de la partida.
 *
 * @param acento color fuerte (encabezado, botones)
 * @param sobreAcento texto e íconos encima de [acento], con contraste suficiente
 * @param suave fondo claro de la pantalla
 * @param texto texto de color sobre [suave] o blanco (≥ 4.5:1)
 */
data class TemaJuego(val acento: Color, val sobreAcento: Color, val suave: Color, val texto: Color)

fun temaDe(juego: Minijuego): TemaJuego = when (juego) {
    Minijuego.ECO_VOCAL -> TemaJuego(XPNaranja, XPMarino, Color(0xFFFFF1DE), XPNaranjaTexto)
    Minijuego.MISION_SITUACIONAL -> TemaJuego(XPAzul, XPBlanco, Color(0xFFE3EDF8), Color(0xFF2F5C9E))
    Minijuego.RAFAGA_PALABRAS -> TemaJuego(XPAmarillo, XPMarino, Color(0xFFFFF6D6), Color(0xFF7A5600))
    Minijuego.ORDEN_MAESTRO -> TemaJuego(XPMarino, XPBlanco, Color(0xFFE7E7F6), XPMarino)
}

val LocalTemaJuego = staticCompositionLocalOf { temaDe(Minijuego.ECO_VOCAL) }

/** Tonos claros para las fichas y etiquetas (se alternan para dar variedad). */
val TONOS_FICHA = listOf(
    Color(0xFFFFE9A6), Color(0xFFC0D6EB), Color(0xFFFFE3BF), Color(0xFFD9CCF5), Color(0xFFBFE3D0)
)

/** Tarjeta blanca de las partidas, con borde opcional del color del juego. */
@Composable
fun TarjetaJuego(
    modifier: Modifier = Modifier,
    borde: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        color = XPBlanco,
        shape = RoundedCornerShape(20.dp),
        border = borde?.let { BorderStroke(2.dp, it) },
        shadowElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Pastilla con ícono y texto: tiempo, puntos, ronda… */
@Composable
fun ChipDato(icono: ImageVector, texto: String, fondo: Color, contenido: Color, modifier: Modifier = Modifier) {
    Surface(color = fondo, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(icono, contentDescription = null, tint = contenido, modifier = Modifier.size(18.dp))
            Text(texto, color = contenido, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }
    }
}

/** Etiqueta pequeña del color del juego ("Repite en voz alta", "Ordena la oración"). */
@Composable
fun EtiquetaJuego(texto: String) {
    val tema = LocalTemaJuego.current
    Surface(color = tema.acento, shape = RoundedCornerShape(50)) {
        Text(
            texto,
            color = tema.sobreAcento,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
