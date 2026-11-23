package com.xpspeak.app.feature.notifications.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.xpspeak.app.core.ui.componentes.EncabezadoXP
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.componentes.TituloSeccion
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPGris
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.notifications.domain.Frecuencia
import com.xpspeak.app.feature.notifications.domain.Recordatorio
import java.time.LocalTime

/** Notificaciones (Ilustración 41): estado general, recordatorios y permiso del sistema. */
@Composable
fun NotificacionesScreen(onVolver: () -> Unit, viewModel: NotificacionesViewModel = hiltViewModel()) {
    val ajustes by viewModel.ajustes.collectAsState()
    val permiso by viewModel.permiso.collectAsState()
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.revisarPermiso() }

    // El permiso puede cambiar en los ajustes del sistema mientras la app está en segundo plano.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.revisarPermiso() }
    LaunchedEffect(Unit) {
        if (!permiso && ajustes.activadas && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        EncabezadoXP("Notificaciones", onVolver = onVolver) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Estado", style = MaterialTheme.typography.titleLarge)
                Interruptor(ajustes.activadas, "Notificaciones activadas") { viewModel.cambiar(ajustes.copy(activadas = it)) }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TarjetaXP {
                TituloSeccion("Recordatorios")
                FilaInterruptor("Recordatorio diario", ajustes.recordatorioDiario, habilitada = ajustes.activadas) {
                    viewModel.cambiar(ajustes.copy(recordatorioDiario = it))
                }
                FilaInterruptor("Alertas de racha", ajustes.alertasRacha, habilitada = ajustes.activadas) {
                    viewModel.cambiar(ajustes.copy(alertasRacha = it))
                }
                FilaSelector(
                    etiqueta = "Frecuencia",
                    valor = ajustes.frecuencia.etiqueta,
                    opciones = Frecuencia.entries.map { it.etiqueta },
                    habilitada = ajustes.activadas
                ) { i -> viewModel.cambiar(ajustes.copy(frecuencia = Frecuencia.entries[i])) }
                FilaSelector(
                    etiqueta = "Hora",
                    valor = ajustes.hora.formato(),
                    opciones = Recordatorio.HORAS.map { it.formato() },
                    habilitada = ajustes.activadas
                ) { i -> viewModel.cambiar(ajustes.copy(hora = Recordatorio.HORAS[i])) }
            }

            TarjetaXP {
                TituloSeccion("Permisos del sistema")
                Surface(color = XPBlanco, shape = FormaBoton, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (permiso) "El dispositivo tiene permisos otorgados" else "XP-SPEAK no puede mostrar notificaciones",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        if (permiso) {
                            Box(
                                modifier = Modifier.size(40.dp).clip(FormaBoton).background(XPVerdeExito),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = XPBlanco)
                            }
                        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { pedirPermiso.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                                Text("Permitir", color = XPMarino)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Interruptor(activo: Boolean, descripcion: String, onCambio: (Boolean) -> Unit) {
    Switch(
        checked = activo,
        onCheckedChange = onCambio,
        colors = coloresInterruptor(),
        modifier = Modifier.semantics { contentDescription = descripcion }
    )
}

@Composable
private fun coloresInterruptor() = SwitchDefaults.colors(
    checkedTrackColor = XPNaranja,
    checkedThumbColor = XPMarino,
    checkedBorderColor = XPMarino,
    uncheckedBorderColor = XPGris
)

@Composable
private fun FilaInterruptor(etiqueta: String, activo: Boolean, habilitada: Boolean, onCambio: (Boolean) -> Unit) {
    Surface(color = XPBlanco, shape = FormaBoton, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .toggleable(value = activo, enabled = habilitada, role = Role.Switch, onValueChange = onCambio)
                .heightIn(min = 56.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(etiqueta, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Switch(
                checked = activo,
                onCheckedChange = null,
                enabled = habilitada,
                colors = coloresInterruptor()
            )
        }
    }
}

@Composable
private fun FilaSelector(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    habilitada: Boolean,
    onElegir: (Int) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Box {
            OutlinedButton(
                onClick = { abierto = true },
                enabled = habilitada,
                shape = FormaBoton,
                modifier = Modifier.width(168.dp)
            ) {
                Text(valor, color = XPMarino, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Cambiar $etiqueta", tint = XPMarino)
            }
            DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
                opciones.forEachIndexed { i, opcion ->
                    DropdownMenuItem(text = { Text(opcion) }, onClick = { abierto = false; onElegir(i) })
                }
            }
        }
    }
}

private fun LocalTime.formato(): String = "%02d:%02d".format(hour, minute)
