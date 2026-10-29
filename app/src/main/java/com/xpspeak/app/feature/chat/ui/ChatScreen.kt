package com.xpspeak.app.feature.chat.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.res.painterResource
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPTexto
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.ResultadoPronunciacion
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import com.xpspeak.app.feature.voice.ui.DetallePalabra
import com.xpspeak.app.feature.voice.ui.colorPorNivel
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Stop
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.alpha
import com.xpspeak.app.feature.progress.ui.InsigniaNueva
import com.xpspeak.app.feature.chat.data.CorreccionDto
import com.xpspeak.app.core.ui.theme.XPFondo
import com.xpspeak.app.core.ui.theme.XPNaranjaTexto
import com.xpspeak.app.core.ui.theme.XPRojoError
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel(),
    onNavigateToPronunciacion: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Progreso: "Habla con IA" se festeja con la primera respuesta de la conversación.
    InsigniaNueva(uiState.insigniasNuevas, nivelXpNuevo = null, onCerrar = viewModel::cerrarFestejo)

    val context = LocalContext.current
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.alternarEnVivo() else viewModel.onPermisoMicrofonoDenegado()
    }
    // RF-08: las respuestas de la IA se leen en voz alta para practicar la comprensión auditiva.
    val locutor = rememberLocutor()
    val onMicClick = {
        val tienePermiso = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        when {
            // Detener también corta a la IA si está hablando.
            uiState.enVivo -> { viewModel.detenerEnVivo(); locutor.callar() }
            tienePermiso -> viewModel.alternarEnVivo()
            else -> permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    // Al salir del chat no se deja el micrófono abierto.
    DisposableEffect(Unit) { onDispose { viewModel.detenerEnVivo() } }
    // Cuenta solo mensajes completos: con streaming (RNF-02) la respuesta se lee en voz alta
    // hasta que termina de llegar. Arranca en el total actual para no repetirla al regresar.
    val completos = uiState.mensajes.count { it.completo }
    var mensajesLeidos by rememberSaveable { mutableIntStateOf(completos) }

    LaunchedEffect(completos) {
        if (completos > mensajesLeidos) {
            uiState.mensajes.last().takeIf { !it.esUsuario && it.completo }?.let {
                // En vivo, al terminar de hablar la IA se vuelve a abrir el micrófono.
                locutor.decirConIdiomas(it.texto, alTerminar = viewModel::onRespuestaLeida)
            }
        }
        mensajesLeidos = completos
    }

    // También sigue a la burbuja mientras crece, y a la del usuario mientras habla.
    LaunchedEffect(uiState.mensajes.size, uiState.mensajes.lastOrNull()?.texto?.length, uiState.parcial) {
        val total = uiState.mensajes.size + if (uiState.parcial.isNotBlank()) 1 else 0
        if (total > 0) {
            scope.launch { listState.animateScrollToItem(total - 1) }
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        EncabezadoXP("Practica con la IA") {
            Text("Conversa en inglés con el asistente de XP-SPEAK.", style = MaterialTheme.typography.bodyMedium)
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.mensajes) { mensaje ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BurbujaMensaje(mensaje, onEscuchar = { locutor.decirConIdiomas(mensaje.texto) })
                    mensaje.correccion?.let { CorreccionMensaje(it, locutor) }
                    mensaje.pronunciacion?.let { PronunciacionMensaje(it, locutor) }
                }
            }
            // Lo que el usuario va diciendo, palabra por palabra, antes de enviarse.
            if (uiState.escuchando && uiState.parcial.isNotBlank()) {
                item {
                    Box(modifier = Modifier.alpha(0.6f)) {
                        BurbujaMensaje(MensajeUi(uiState.parcial, esUsuario = true, completo = false), onEscuchar = {})
                    }
                }
            }
            // El indicador solo mientras no llega el primer pedazo; después, la burbuja ya crece sola.
            if (uiState.cargando && uiState.mensajes.lastOrNull()?.esUsuario == true) {
                item {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        uiState.error?.let { mensajeError ->
            Text(
                mensajeError,
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }

        if (uiState.enVivo) {
            Text(
                when {
                    uiState.escuchando -> "🎙 Te escucho… habla en inglés y haz una pausa al terminar."
                    uiState.cargando -> "💭 La IA está pensando…"
                    else -> "🔊 La IA está hablando…"
                },
                style = MaterialTheme.typography.labelLarge,
                color = XPMarino,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        TextButton(
            onClick = onNavigateToPronunciacion,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text("🎙 Practicar pronunciación")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.entrada,
                onValueChange = viewModel::onEntradaChange,
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = XPBlanco, unfocusedContainerColor = XPBlanco),
                placeholder = {
                    Text(if (uiState.enVivo) "O escribe en inglés..." else "Escribe o habla en inglés...")
                }
            )
            // Un toque inicia la conversación en vivo; otro la detiene.
            FilledIconButton(
                onClick = onMicClick,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (uiState.enVivo) MaterialTheme.colorScheme.error else XPAzulClaro,
                    contentColor = if (uiState.enVivo) XPBlanco else XPMarino
                ),
                modifier = Modifier.padding(start = 8.dp).size(52.dp)
            ) {
                Icon(
                    if (uiState.enVivo) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (uiState.enVivo) "Detener conversación en vivo" else "Conversar en vivo"
                )
            }
            FilledIconButton(
                onClick = viewModel::enviar,
                enabled = !uiState.cargando,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = XPNaranja, contentColor = XPMarino),
                modifier = Modifier.padding(start = 8.dp).size(52.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}

/**
 * RF-09 en el chat: los errores de gramática del mensaje, aparte de la respuesta de la IA.
 * Cada uno muestra lo que se escribió tachado, la forma correcta y por qué.
 */
@Composable
private fun CorreccionMensaje(correccion: CorreccionDto, locutor: Locutor) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            color = XPFondo,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, XPNaranja),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "✏️ Corrección",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = XPNaranjaTexto
                )
                correccion.corregido?.let { corregido ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(corregido, color = XPTexto, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                        IconButton(onClick = { locutor.decir(corregido) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar la oración corregida", tint = XPMarino)
                        }
                    }
                }
                correccion.errores.forEach { error ->
                    Column {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(color = XPRojoError, textDecoration = TextDecoration.LineThrough)) { append(error.original) }
                                append("  →  ")
                                withStyle(SpanStyle(color = XPVerdeExito, fontWeight = FontWeight.Bold)) { append(error.correccion) }
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(error.explicacion, style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
                    }
                }
            }
        }
    }
}

/** RF-09 en el chat: cómo pronunció el usuario lo que dijo; toca una palabra para ver sus sonidos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PronunciacionMensaje(resultado: ResultadoPronunciacion, locutor: Locutor) {
    var seleccionada by remember(resultado) { mutableStateOf<Int?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val general = resultado.puntuacionGeneral.roundToInt()
        Text(
            "Pronunciación $general/100 · Fluidez ${resultado.puntuacionFluidez.roundToInt()}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = colorPorNivel(Pronunciacion.nivelDe(general))
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            resultado.palabras.forEachIndexed { i, palabra ->
                Text(
                    palabra.palabra,
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier
                        .background(colorPorNivel(Retroalimentacion.nivelDe(palabra)), RoundedCornerShape(6.dp))
                        .then(
                            if (seleccionada == i) Modifier.border(BorderStroke(2.dp, XPMarino), RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable { seleccionada = if (seleccionada == i) null else i }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        seleccionada?.let { resultado.palabras.getOrNull(it) }?.let { palabra ->
            Column(modifier = Modifier.widthIn(max = 300.dp)) {
                DetallePalabra(palabra, onEscuchar = { locutor.decir(palabra.palabra, lento = true) })
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(mensaje: MensajeUi, onEscuchar: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mensaje.esUsuario) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!mensaje.esUsuario) {
            Image(
                painterResource(R.drawable.avatar_robot),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp).size(36.dp)
            )
        }
        Surface(
            color = if (mensaje.esUsuario) XPMarino else XPBlanco,
            shape = if (mensaje.esUsuario) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                    else RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            border = if (mensaje.esUsuario) null else BorderStroke(1.dp, XPAzulClaro),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                mensaje.texto,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = if (mensaje.esUsuario) XPBlanco else XPTexto
            )
        }
        if (!mensaje.esUsuario && mensaje.completo) {
            IconButton(onClick = onEscuchar) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Escuchar respuesta", tint = XPMarino)
            }
        }
    }
}
