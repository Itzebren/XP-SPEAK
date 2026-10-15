package com.xpspeak.app.feature.chat.ui

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.feature.progress.ui.InsigniaNueva
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(viewModel: ChatViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // Progreso: "Habla con IA" se festeja con la primera respuesta de la conversación.
    InsigniaNueva(uiState.insigniasNuevas, nivelXpNuevo = null, onCerrar = viewModel::cerrarFestejo)

    LaunchedEffect(uiState.mensajes.size) {
        if (uiState.mensajes.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(uiState.mensajes.size - 1) }
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
            items(uiState.mensajes) { mensaje -> BurbujaMensaje(mensaje) }
            if (uiState.cargando) {
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
                placeholder = { Text("Escribe en inglés...") }
            )
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

@Composable
private fun BurbujaMensaje(mensaje: MensajeUi) {
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
    }
}
