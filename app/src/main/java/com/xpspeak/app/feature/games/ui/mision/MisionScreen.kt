package com.xpspeak.app.feature.games.ui.mision

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.XPGreenSuccess
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@Composable
fun MisionScreen(onVolver: () -> Unit, viewModel: MisionViewModel = hiltViewModel()) {
    val fase by viewModel.fase.collectAsState()
    val locutor = rememberLocutor()

    PantallaPartida(
        juego = Minijuego.MISION_SITUACIONAL,
        viewModel = viewModel,
        fase = fase,
        onVolver = onVolver,
        progreso = { it.paso.toFloat() / it.mision.pasos.size }
    ) { estado ->
        val mision = estado.mision
        // El personaje "habla" cada línea nueva (§5.2 paso 2).
        LaunchedEffect(estado.paso) { locutor.decir(estado.pasoActual.npc) }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(mision.titulo, style = MaterialTheme.typography.titleMedium)
                Text(mision.escenario, style = MaterialTheme.typography.bodySmall)
                mision.objetivos.forEach { objetivo ->
                    val hecho = objetivo.id in estado.cumplidos
                    Text(
                        "${if (hecho) "✅" else "⬜"} ${objetivo.texto}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hecho) XPGreenSuccess else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        val lista = rememberLazyListState()
        LaunchedEffect(estado.historial.size) { lista.animateScrollToItem(estado.historial.lastIndex.coerceAtLeast(0)) }
        LazyColumn(
            state = lista,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(estado.historial) { linea ->
                Burbuja(linea, mision.personaje, onEscuchar = { locutor.decir(linea.en) })
            }
        }

        estado.feedback?.let { TarjetaFeedback(Veredicto.ERROR, "Inténtalo de nuevo", it) }

        if (estado.escribiendo) {
            RespuestaEscrita(
                paso = estado.paso,
                onEnviar = viewModel::escribir,
                onVerOpciones = viewModel::verOpciones
            )
        } else {
            estado.opciones.forEach { (indice, opcion) ->
                OutlinedButton(
                    onClick = { viewModel.elegir(indice) },
                    enabled = indice !in estado.falladas,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(opcion.en) }
            }
        }
    }
}

@Composable
private fun Burbuja(linea: LineaChat, personaje: String, onEscuchar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (linea.deNpc) Arrangement.Start else Arrangement.End
    ) {
        Card(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (linea.deNpc) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(if (linea.deNpc) personaje else "Tú", style = MaterialTheme.typography.labelSmall)
                    Text(linea.en, style = MaterialTheme.typography.bodyLarge)
                    linea.es?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
                IconButton(onClick = onEscuchar) { Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar") }
            }
        }
    }
}

@Composable
private fun RespuestaEscrita(paso: Int, onEnviar: (String) -> Unit, onVerOpciones: () -> Unit) {
    var texto by rememberSaveable(paso) { mutableStateOf("") }
    Text("¡Vas muy bien! Ahora escribe tu respuesta en inglés.", style = MaterialTheme.typography.labelLarge)
    OutlinedTextField(
        value = texto,
        onValueChange = { texto = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("Tu respuesta…") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onEnviar(texto) })
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onVerOpciones, modifier = Modifier.weight(1f)) { Text("Ver opciones") }
        Button(onClick = { onEnviar(texto) }, enabled = texto.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Responder") }
    }
}
