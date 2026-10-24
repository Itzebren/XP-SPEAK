package com.xpspeak.app.feature.games.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.FormaPortada
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.core.ui.componentes.recursoDrawable

/**
 * RF-11 / CU-07 (pasos 1–3): catálogo de minijuegos en cuadrícula 2x2 con su
 * portada (Ilustración 37, res/drawable/juego_<id>).
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
            contentPadding = PaddingValues(18.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            items(Minijuego.entries, key = { it.id }) { juego ->
                TarjetaMinijuego(juego, enCurso = juego in enCurso, onClick = { onAbrirJuego(juego) })
            }
        }
    }
}

@Composable
private fun TarjetaMinijuego(juego: Minijuego, enCurso: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            juego.titulo,
            fontFamily = AdlamDisplay,
            style = MaterialTheme.typography.titleLarge,
            color = XPTexto,
            textAlign = TextAlign.Center,
            minLines = 2
        )
        Box(contentAlignment = Alignment.BottomCenter) {
            val portada = recursoDrawable("juego_${juego.id.replace('-', '_')}")
            if (portada != 0) {
                Image(
                    painter = painterResource(portada),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(FormaPortada)
                )
            }
            if (enCurso) {
                Surface(color = XPMarino, shape = MaterialTheme.shapes.small, modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        "Continuar",
                        color = XPBlanco,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Text(
            juego.descripcion,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
