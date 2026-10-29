package com.xpspeak.app.feature.voice.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTexto
import com.xpspeak.app.core.ui.theme.XPTextoSecundario
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.games.domain.Pronunciacion
import com.xpspeak.app.feature.games.ui.comun.Locutor
import com.xpspeak.app.feature.games.ui.comun.rememberLocutor
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.PalabraEvaluada
import com.xpspeak.app.feature.voice.data.EvaluadorPronunciacion.ResultadoPronunciacion
import com.xpspeak.app.feature.voice.domain.Retroalimentacion
import kotlin.math.roundToInt

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
                    OutlinedButton(onClick = { locutor.decir(uiState.textoReferencia, lento = true) }) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                        Text("  Escuchar")
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
                Text("Escuchando... lee la frase completa.", style = MaterialTheme.typography.bodyMedium)
            }

            uiState.error?.let { error ->
                Text(text = error, color = MaterialTheme.colorScheme.error)
            }

            uiState.resultado?.let { resultado ->
                Resultado(
                    resultado = resultado,
                    seleccionada = uiState.palabraSeleccionada,
                    onSeleccionar = viewModel::seleccionarPalabra,
                    locutor = locutor
                )
            }
        }
    }
}

/** RF-09: puntajes, palabras coloreadas y detalle de la palabra tocada. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Resultado(
    resultado: ResultadoPronunciacion,
    seleccionada: Int?,
    onSeleccionar: (Int) -> Unit,
    locutor: Locutor
) {
    TarjetaXP {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val general = resultado.puntuacionGeneral.roundToInt()
            Text(
                "$general",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = colorPorNivel(Pronunciacion.nivelDe(general))
            )
            Text(" / 100", style = MaterialTheme.typography.titleMedium)
        }
        Text(Retroalimentacion.resumen(resultado), style = MaterialTheme.typography.bodyLarge)

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            resultado.palabras.forEachIndexed { i, palabra ->
                val omitida = palabra.errorType == "Omission"
                Text(
                    palabra.palabra,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (omitida) TextDecoration.LineThrough else null,
                    modifier = Modifier
                        .background(colorPorNivel(Retroalimentacion.nivelDe(palabra)), RoundedCornerShape(6.dp))
                        .then(
                            if (seleccionada == i) Modifier.border(BorderStroke(2.dp, XPMarino), RoundedCornerShape(6.dp))
                            else Modifier
                        )
                        .clickable { onSeleccionar(i) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        seleccionada?.let { resultado.palabras.getOrNull(it) }?.let { palabra ->
            DetallePalabra(palabra, onEscuchar = { locutor.decir(palabra.palabra, lento = true) })
        }

        Metrica("Precisión", resultado.puntuacionPrecision)
        Metrica("Fluidez", resultado.puntuacionFluidez)
        Metrica("Completitud", resultado.puntuacionCompletitud)
        Text("Oímos: “${resultado.textoReconocido}”", style = MaterialTheme.typography.bodySmall, color = XPTextoSecundario)
        Text(
            "Tu audio no se guarda: esta evaluación desaparece al salir o cambiar de frase.",
            style = MaterialTheme.typography.bodySmall,
            color = XPTextoSecundario
        )
    }
}

@Composable
internal fun DetallePalabra(palabra: PalabraEvaluada, onEscuchar: () -> Unit) {
    Surface(color = XPBlanco, shape = MaterialTheme.shapes.small, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "«${palabra.palabra}»  ${palabra.puntuacionPrecision.roundToInt()}/100",
                    style = MaterialTheme.typography.titleMedium,
                    color = XPTexto,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onEscuchar) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Text(" Escuchar")
                }
            }
            Text(Retroalimentacion.descripcion(palabra), color = XPTexto)
            val fonemas = Retroalimentacion.fonemasAPracticar(palabra)
            if (fonemas.isNotEmpty()) {
                Text("Sonidos a mejorar:", style = MaterialTheme.typography.labelLarge, color = XPTexto)
                Text(
                    fonemas.joinToString("   ") { "/${it.fonema}/ ${it.puntuacionPrecision.roundToInt()}" },
                    fontFamily = FontFamily.Default,
                    color = XPTexto
                )
            }
        }
    }
}

@Composable
private fun Metrica(nombre: String, valor: Double) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(nombre, modifier = Modifier.weight(1f))
        Text("${valor.roundToInt()}/100", fontWeight = FontWeight.Bold)
    }
}

/** Mismos colores que Eco Vocal (verde/amarillo/rojo). */
@Composable
internal fun colorPorNivel(nivel: Pronunciacion.Nivel): Color = when (nivel) {
    Pronunciacion.Nivel.BUENO -> XPVerdeExito
    Pronunciacion.Nivel.REGULAR -> Color(0xFFF9A825)
    Pronunciacion.Nivel.MALO -> MaterialTheme.colorScheme.error
}
