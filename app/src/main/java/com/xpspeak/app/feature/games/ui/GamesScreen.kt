package com.xpspeak.app.feature.games.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.games.domain.Minijuego

/**
 * RF-11 / CU-07 (pasos 1–3): catálogo de minijuegos en cuadrícula 2x2.
 * Versión sencilla, sin imágenes.
 */
@Composable
fun GamesScreen(onAbrirJuego: (Minijuego) -> Unit, viewModel: GamesViewModel = hiltViewModel()) {
    val nivel by viewModel.nivel.collectAsState()
    val enCurso by viewModel.enCurso.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text(
            "Catálogo de minijuegos",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            nivel?.let { "Practica lo que ya viste en tus lecciones · Nivel $it" } ?: "Practica lo que ya viste en tus lecciones",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Minijuego.entries, key = { it.id }) { juego ->
                TarjetaMinijuego(juego, enCurso = juego in enCurso, onClick = { onAbrirJuego(juego) })
            }
        }
    }
}

@Composable
private fun TarjetaMinijuego(juego: Minijuego, enCurso: Boolean, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    juego.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    juego.descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (enCurso) {
                    AssistChip(onClick = onClick, label = { Text("Continuar") }, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}
