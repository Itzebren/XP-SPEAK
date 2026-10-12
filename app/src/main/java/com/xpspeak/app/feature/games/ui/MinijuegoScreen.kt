package com.xpspeak.app.feature.games.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.ecovocal.EcoVocalScreen
import com.xpspeak.app.feature.games.ui.mision.MisionScreen
import com.xpspeak.app.feature.games.ui.orden.OrdenMaestroScreen
import com.xpspeak.app.feature.games.ui.rafaga.RafagaScreen

/** CU-07 paso 4: abre el juego elegido en el catálogo (ruta games/{id}). */
@Composable
fun MinijuegoScreen(juego: Minijuego?, onVolver: () -> Unit) {
    when (juego) {
        Minijuego.ECO_VOCAL -> EcoVocalScreen(onVolver)
        Minijuego.MISION_SITUACIONAL -> MisionScreen(onVolver)
        Minijuego.RAFAGA_PALABRAS -> RafagaScreen(onVolver)
        Minijuego.ORDEN_MAESTRO -> OrdenMaestroScreen(onVolver)
        null -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
        ) {
            Text("Ese minijuego no existe.")
            Button(onClick = onVolver) { Text("Volver") }
        }
    }
}
