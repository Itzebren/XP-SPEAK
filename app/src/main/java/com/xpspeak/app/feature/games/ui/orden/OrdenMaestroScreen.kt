package com.xpspeak.app.feature.games.ui.orden

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reorder
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.feature.games.domain.GeneradorRondas
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.ChipDato
import com.xpspeak.app.feature.games.ui.comun.EtiquetaJuego
import com.xpspeak.app.feature.games.ui.comun.LocalTemaJuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TONOS_FICHA
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.TarjetaJuego
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
        val tema = LocalTemaJuego.current
        val ronda = estado.ronda
        val revision = estado.revision
        val oracion = ronda.solucion.joinToString(" ").replaceFirstChar { it.uppercase() } + ronda.puntuacion

        // Al acertar se escucha la oración completa (§5.4 paso 3).
        LaunchedEffect(revision) { if (revision?.correcta == true) locutor.decir(oracion) }

        ChipDato(Icons.Filled.Reorder, "Oración ${estado.numeroRonda} de ${GeneradorRondas.RONDAS_ORDEN}", XPBlanco, tema.texto)
        TarjetaJuego {
            EtiquetaJuego("Ordena la oración")
            Text(ronda.pista, style = MaterialTheme.typography.titleMedium, color = XPMarino)
        }

        // Oración en construcción: tocar una ficha la regresa.
        Surface(
            color = XPBlanco,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(2.dp, tema.acento.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp)
        ) {
            FlowRow(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                if (estado.colocadas.isEmpty()) {
                    Text("Toca las fichas en orden para armar la oración", style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario)
                }
                estado.colocadas.forEachIndexed { posicion, indice ->
                    val revisada = when {
                        revision == null -> null
                        revision.correcta -> XPVerdeExito
                        posicion in revision.posicionesMal -> MaterialTheme.colorScheme.error
                        else -> null
                    }
                    Ficha(ronda.fichas[indice], tono = tonoDe(indice), revisada = revisada, onClick = { viewModel.quitar(posicion) })
                }
                if (estado.colocadas.size == ronda.fichas.size) {
                    Text(ronda.puntuacion, style = MaterialTheme.typography.titleLarge, color = XPMarino)
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ronda.fichas.forEachIndexed { indice, ficha ->
                if (indice !in estado.colocadas) Ficha(ficha, tono = tonoDe(indice), onClick = { viewModel.colocar(indice) })
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
            Button(
                colors = coloresBotonPrincipal(),
                shape = FormaBoton,
                onClick = viewModel::continuar,
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp)
            ) {
                Text(if (revision.definitiva) "Continuar" else "Intentar de nuevo", fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Cada ficha conserva su color al moverse, para seguirla con la vista. */
private fun tonoDe(indice: Int) = TONOS_FICHA[indice % TONOS_FICHA.size]

@Composable
private fun Ficha(texto: String, tono: Color, revisada: Color? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        border = BorderStroke(if (revisada != null) 3.dp else 1.5.dp, revisada ?: XPMarino),
        color = revisada?.copy(alpha = 0.15f) ?: tono
    ) {
        Text(
            texto,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = XPMarino
        )
    }
}
