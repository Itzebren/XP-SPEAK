package com.xpspeak.app.feature.games.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.recursoDrawable
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.temaDe

/**
 * RF-11 / CU-07 (pasos 1–3): catálogo de minijuegos en cuadrícula 2x2 con su
 * portada (Ilustración 37, res/drawable/juego_<id>), cada uno con su color.
 */
@Composable
fun GamesScreen(onAbrirJuego: (Minijuego) -> Unit, viewModel: GamesViewModel = hiltViewModel()) {
    val nivel by viewModel.nivel.collectAsState()
    val enCurso by viewModel.enCurso.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoXP("Catálogo de minijuegos") {
            Text(
                nivel?.let { "Practica lo que ya viste · Nivel $it" } ?: "Practica lo que ya viste en tus lecciones",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(Minijuego.entries, key = { it.id }) { juego ->
                TarjetaMinijuego(juego, enCurso = juego in enCurso, onClick = { onAbrirJuego(juego) })
            }
        }
    }
}

@Composable
private fun TarjetaMinijuego(juego: Minijuego, enCurso: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tema = temaDe(juego)
    Surface(
        onClick = onClick,
        color = tema.suave,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(2.dp, tema.acento),
        shadowElevation = 2.dp,
        modifier = modifier.semantics { role = Role.Button }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val portada = recursoDrawable("juego_${juego.id.replace('-', '_')}")
            if (portada != 0) {
                Image(
                    painter = painterResource(portada),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(18.dp))
                )
            }
            Text(
                juego.titulo,
                fontFamily = AdlamDisplay,
                style = MaterialTheme.typography.titleMedium,
                color = XPMarino,
                textAlign = TextAlign.Center,
                minLines = 2
            )
            Text(
                juego.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = XPTextoSecundario,
                textAlign = TextAlign.Center,
                minLines = 2
            )
            Surface(color = tema.acento, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = tema.sobreAcento, modifier = Modifier.size(20.dp))
                    Text(
                        if (enCurso) "Continuar" else "Jugar",
                        color = tema.sobreAcento,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
