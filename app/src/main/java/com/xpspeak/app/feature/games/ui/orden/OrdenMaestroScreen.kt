package com.xpspeak.app.feature.games.ui.orden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.XPGreenSuccess
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@Composable
fun OrdenMaestroScreen(onVolver: () -> Unit, viewModel: OrdenMaestroViewModel = hiltViewModel()) {
    val fase by viewModel.fase.collectAsState()
    val locutor = rememberLocutor()

    PantallaPartida(
        juego = Minijuego.ORDEN_MAESTRO,
        viewModel = viewModel,
        fase = fase,
        onVolver = onVolver,
        progreso = { (it.numeroRonda - 1).toFloat() / GeneradorRondas.RONDAS_ORDEN }
    ) { estado ->
        val ronda = estado.ronda
        val revision = estado.revision
        val oracion = ronda.solucion.joinToString(" ").replaceFirstChar { it.uppercase() } + ronda.puntuacion

        // Al acertar se escucha la oración completa (§5.4 paso 3).
        LaunchedEffect(revision) { if (revision?.correcta == true) locutor.decir(oracion) }

        Text("Oración ${estado.numeroRonda} de ${GeneradorRondas.RONDAS_ORDEN}", style = MaterialTheme.typography.labelLarge)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ordena la oración", style = MaterialTheme.typography.labelMedium)
                Text(ronda.pista, style = MaterialTheme.typography.titleMedium)
            }
        }

        // Oración en construcción: tocar una ficha la regresa.
        OutlinedCard(modifier = Modifier.fillMaxWidth().heightIn(min = 96.dp)) {
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                estado.colocadas.forEachIndexed { posicion, indice ->
                    val color = when {
                        revision == null -> null
                        revision.correcta -> XPGreenSuccess
                        posicion in revision.posicionesMal -> MaterialTheme.colorScheme.error
                        else -> null
                    }
                    Ficha(ronda.fichas[indice], color = color, onClick = { viewModel.quitar(posicion) })
                }
                if (estado.colocadas.size == ronda.fichas.size) {
                    Text(ronda.puntuacion, style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ronda.fichas.forEachIndexed { indice, ficha ->
                if (indice !in estado.colocadas) Ficha(ficha, onClick = { viewModel.colocar(indice) })
            }
        }

        Spacer(Modifier.weight(1f))
        if (revision != null) {
            when {
                revision.correcta -> TarjetaFeedback(Veredicto.ACIERTO, "¡Correcto!", oracion)
                !revision.definitiva -> TarjetaFeedback(
                    Veredicto.ERROR,
                    "Casi. Las fichas en rojo están fuera de lugar.",
                    "Tienes un intento más."
                )
                else -> TarjetaFeedback(Veredicto.ERROR, "La oración correcta es:", oracion)
            }
            Button(onClick = viewModel::continuar, modifier = Modifier.fillMaxWidth()) {
                Text(if (revision.definitiva) "Continuar" else "Intentar de nuevo")
            }
        }
    }
}

@Composable
private fun Ficha(texto: String, color: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        border = BorderStroke(2.dp, color ?: MaterialTheme.colorScheme.outlineVariant),
        color = color?.copy(alpha = 0.12f) ?: MaterialTheme.colorScheme.surface
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
