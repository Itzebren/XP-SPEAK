package com.xpspeak.app.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor

/** RF-09: lee una frase de tu nivel y recibe la retroalimentación de frase, palabra y sonido. */
@Composable
fun PronunciacionScreen(
    viewModel: PronunciacionViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locutor = rememberLocutor()

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) viewModel.iniciarEvaluacion()
    }

    val onHablarClick = {
        locutor.callar()
        val tienePermiso = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (tienePermiso) viewModel.iniciarEvaluacion()
        else permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        EncabezadoXP("Práctica de pronunciación", onVolver = onBack) {
            Text("Lee la frase en voz alta y toca cada palabra para ver cómo te fue.", style = MaterialTheme.typography.bodyMedium)
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TarjetaXP {
                Text("Lee en voz alta:", style = MaterialTheme.typography.labelLarge)
                Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        uiState.textoReferencia,
                        style = MaterialTheme.typography.headlineSmall,
                        color = XPTexto,
                        fontFamily = FontFamily.Default,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { locutor.decir(uiState.textoReferencia) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Text("  Escuchar")
                    }
                    OutlinedButton(onClick = { locutor.decir(uiState.textoReferencia, lento = true) }) {
                        Icon(Icons.Filled.SlowMotionVideo, contentDescription = null)
                        Text("  Lento")
                    }
                    TextButton(onClick = viewModel::siguienteFrase, enabled = !uiState.evaluando) {
                        Text("Otra frase")
                    }
                }
            }

            BotonPrincipal(
                texto = if (uiState.resultado == null) "🎙 Hablar" else "🎙 Intentar de nuevo",
                onClick = onHablarClick,
                cargando = uiState.evaluando
            )
            if (uiState.evaluando) {
                Text("Escuchando... lee la frase completa.", style = MaterialTheme.typography.bodyMedium, color = XPTextoSecundario)
            }

            uiState.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            uiState.resultado?.let { PanelPronunciacion(it, locutor) }
        }
    }
}
