package com.xpspeak.app.feature.account.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.CampoTexto
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.Avatar
import com.xpspeak.app.core.ui.componentes.TarjetaXP
import com.xpspeak.app.core.ui.componentes.TituloSeccion
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja
import com.xpspeak.app.core.ui.theme.XPTexto

/**
 * Ajustes de perfil (Ilustración 35, RF-15 / RN-14): información personal, avatar,
 * nivel, notificaciones, cerrar sesión y eliminar la cuenta (Ilustración 36).
 */
@Composable
fun AccountScreen(
    onVolver: () -> Unit = {},
    onNotificaciones: () -> Unit = {},
    onEliminarCuenta: () -> Unit = {},
    onCerrarSesion: () -> Unit = {},
    viewModel: AccountViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val correo = uiState.usuario?.correo ?: uiState.correo
    val avatar = Avatar.porId(uiState.usuario?.avatar)
    var nombre by rememberSaveable(uiState.nombre) { mutableStateOf(uiState.nombre) }
    var nuevoCorreo by rememberSaveable(correo) { mutableStateOf(correo) }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(XPAzulClaro)
                .statusBarsPadding()
                .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = XPMarino)
                }
                Text("Ajustes de perfil", style = MaterialTheme.typography.headlineSmall)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 12.dp, top = 4.dp)
            ) {
                Image(painterResource(avatar.dibujo), contentDescription = "Tu avatar: ${avatar.descripcion}", modifier = Modifier.size(72.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(uiState.nombre.ifBlank { nombreVisible(correo) }, fontFamily = AdlamDisplay, style = MaterialTheme.typography.titleLarge, color = XPTexto)
                    Text(correo, style = MaterialTheme.typography.bodyMedium)
                }
                Image(painterResource(R.drawable.mascota), contentDescription = null, modifier = Modifier.size(width = 64.dp, height = 70.dp))
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TarjetaXP {
                TituloSeccion("Información personal")
                CampoTexto(nombre, { nombre = it }, "Nombre de usuario", Icons.Filled.Person)
                if (nombre.trim() != uiState.nombre) {
                    BotonPrincipal("Guardar nombre", onClick = { viewModel.guardarNombre(nombre) }, cargando = uiState.guardando)
                }
                CampoTexto(nuevoCorreo, { nuevoCorreo = it.filterNot(Char::isWhitespace) }, "Correo electrónico", Icons.Filled.Email, tipoTeclado = KeyboardType.Email)
                if (!nuevoCorreo.equals(correo, ignoreCase = true)) {
                    BotonPrincipal("Cambiar correo", onClick = { viewModel.cambiarCorreo(nuevoCorreo) }, cargando = uiState.guardando)
                }
                uiState.mensaje?.let {
                    Text(it, color = if (uiState.esError) MaterialTheme.colorScheme.error else XPVerdeExito, style = MaterialTheme.typography.bodyMedium)
                }
            }

            TarjetaXP {
                TituloSeccion("Tu avatar")
                Avatar.entries.chunked(4).forEach { fila ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        fila.forEach { opcion ->
                            val elegido = opcion == avatar
                            Image(
                                painter = painterResource(opcion.dibujo),
                                contentDescription = opcion.descripcion,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(BorderStroke(3.dp, if (elegido) XPNaranja else Color.Transparent), CircleShape)
                                    .semantics { selected = elegido }
                                    .clickable(role = Role.RadioButton) { viewModel.cambiarAvatar(opcion.id) }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }

            uiState.usuario?.let { usuario ->
                TarjetaXP {
                    TituloSeccion("Nivel de inglés")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("A1" to "Principiante", "A2" to "Elemental").forEach { (nivel, nombreNivel) ->
                            val elegido = usuario.nivel == nivel
                            Surface(
                                color = if (elegido) XPNaranja else XPBlanco,
                                shape = MaterialTheme.shapes.medium,
                                border = BorderStroke(2.dp, if (elegido) XPMarino else XPAzulClaro),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .selectable(selected = elegido, role = Role.RadioButton) { viewModel.cambiarNivel(nivel) }
                            ) {
                                Column(modifier = Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(nivel, fontFamily = AdlamDisplay, style = MaterialTheme.typography.headlineLarge, color = XPMarino)
                                    Text(nombreNivel, style = MaterialTheme.typography.bodySmall, color = XPMarino)
                                }
                            }
                        }
                    }
                    Text(
                        "Tu avance de cada nivel se guarda: puedes cambiar y volver cuando quieras.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            TarjetaXP {
                TituloSeccion("Datos y cuenta")
                FilaCuenta("Notificaciones", Icons.Filled.Notifications, onClick = onNotificaciones)
                FilaCuenta("Cerrar sesión", Icons.AutoMirrored.Filled.ExitToApp) {
                    viewModel.cerrarSesion()
                    onCerrarSesion()
                }
                FilaCuenta("Eliminar cuenta e historial", Icons.Filled.Delete, destacada = true, onClick = onEliminarCuenta)
            }
        }
    }
}

/** "itzel.p@gmail.com" → "Itzel.p": no guardamos nombre, así que se usa el del correo. */
private fun nombreVisible(correo: String): String =
    correo.substringBefore('@').replaceFirstChar { it.uppercase() }.ifBlank { "Tu perfil" }

@Composable
private fun FilaCuenta(texto: String, icono: ImageVector, destacada: Boolean = false, onClick: () -> Unit) {
    Surface(
        color = if (destacada) XPNaranja else XPBlanco,
        shape = FormaBoton,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clip(FormaBoton)
            .clickable(role = Role.Button, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                texto,
                fontWeight = if (destacada) FontWeight.Bold else FontWeight.SemiBold,
                color = if (destacada) XPMarino else XPTexto,
                modifier = Modifier.weight(1f)
            )
            Icon(icono, contentDescription = null, tint = XPMarino)
        }
    }
}
