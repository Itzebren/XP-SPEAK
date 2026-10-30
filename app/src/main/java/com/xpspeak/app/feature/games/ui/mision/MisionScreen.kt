package com.xpspeak.app.feature.games.ui.mision

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPGris
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.ui.comun.EtiquetaJuego
import com.xpspeak.app.feature.games.ui.comun.LocalTemaJuego
import com.xpspeak.app.feature.games.ui.comun.PantallaPartida
import com.xpspeak.app.feature.games.ui.comun.TarjetaFeedback
import com.xpspeak.app.feature.games.ui.comun.TarjetaJuego
import com.xpspeak.app.feature.games.ui.comun.Veredicto
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

@OptIn(ExperimentalLayoutApi::class)
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
        val tema = LocalTemaJuego.current
        val mision = estado.mision
        // El personaje "habla" cada línea nueva (§5.2 paso 2).
        LaunchedEffect(estado.paso) { locutor.decir(estado.pasoActual.npc) }

        // Con el teclado abierto la misión se resume en una línea para que se vea la conversación.
        val tecladoAbierto = WindowInsets.isImeVisible
        TarjetaJuego {
            if (tecladoAbierto) {
                Text(
                    "${mision.titulo} · ${estado.cumplidos.size} de ${mision.objetivos.size} objetivos",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = XPMarino
                )
                return@TarjetaJuego
            }
            EtiquetaJuego("Tu misión")
            Text(mision.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = XPMarino)
            Text(mision.escenario, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
            mision.objetivos.forEach { objetivo ->
                val hecho = objetivo.id in estado.cumplidos
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        if (hecho) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = if (hecho) "Cumplido" else "Pendiente",
                        tint = if (hecho) XPVerdeExito else XPGris,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        objetivo.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (hecho) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (hecho) XPVerdeExito else XPMarino
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
                val disponible = indice !in estado.falladas
                OutlinedButton(
                    onClick = { viewModel.elegir(indice) },
                    enabled = disponible,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(2.dp, if (disponible) tema.acento else XPGris.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) {
                    Text(opcion.en, color = if (disponible) XPMarino else XPGris, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** El personaje en burbuja blanca con borde de color; tus respuestas, en el color del juego. */
@Composable
private fun Burbuja(linea: LineaChat, personaje: String, onEscuchar: () -> Unit) {
    val tema = LocalTemaJuego.current
    val deNpc = linea.deNpc
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (deNpc) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 300.dp),
            shape = if (deNpc) RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp) else RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            color = if (deNpc) XPBlanco else tema.acento,
            border = if (deNpc) BorderStroke(2.dp, tema.acento) else null,
            shadowElevation = 1.dp
        ) {
            Row(modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val texto = if (deNpc) XPMarino else tema.sobreAcento
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        if (deNpc) personaje else "Tú",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (deNpc) tema.texto else tema.sobreAcento
                    )
                    Text(linea.en, style = MaterialTheme.typography.bodyLarge, color = texto)
                    linea.es?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = if (deNpc) XPTextoSecundario else tema.sobreAcento) }
                }
                IconButton(onClick = onEscuchar) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar", tint = texto)
                }
            }
        }
    }
}

@Composable
private fun RespuestaEscrita(paso: Int, onEnviar: (String) -> Unit, onVerOpciones: () -> Unit) {
    val tema = LocalTemaJuego.current
    var texto by rememberSaveable(paso) { mutableStateOf("") }
    Text("¡Vas muy bien! Ahora escribe tu respuesta en inglés.", style = MaterialTheme.typography.labelLarge, color = tema.texto)
    OutlinedTextField(
        value = texto,
        onValueChange = { texto = it },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        placeholder = { Text("Tu respuesta…") },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = XPBlanco,
            unfocusedContainerColor = XPBlanco,
            focusedBorderColor = tema.acento,
            unfocusedBorderColor = tema.acento.copy(alpha = 0.5f)
        ),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onEnviar(texto) })
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        TextButton(onClick = onVerOpciones, modifier = Modifier.weight(1f)) { Text("Ver opciones", color = tema.texto, fontWeight = FontWeight.Bold) }
        Button(
            colors = coloresBotonPrincipal(),
            shape = FormaBoton,
            onClick = { onEnviar(texto) },
            enabled = texto.isNotBlank(),
            modifier = Modifier.weight(1f)
        ) { Text("Responder", fontWeight = FontWeight.Bold) }
    }
}
