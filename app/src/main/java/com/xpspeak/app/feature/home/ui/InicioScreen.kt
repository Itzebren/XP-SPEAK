package com.xpspeak.app.feature.home.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.xpspeak.app.R
import com.xpspeak.app.core.actividad.Racha
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPGris
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTexto

private val DIAS = listOf("L" to "Lunes", "M" to "Martes", "M" to "Miércoles", "J" to "Jueves", "V" to "Viernes", "S" to "Sábado", "D" to "Domingo")

/** Pantalla de Inicio (Ilustración 32): saludo, racha de la semana y los cuatro accesos. */
@Composable
fun InicioScreen(
    onLecciones: () -> Unit,
    onMinijuegos: () -> Unit,
    onChat: () -> Unit,
    onProgreso: () -> Unit,
    onAjustes: () -> Unit,
    onNotificaciones: () -> Unit,
    viewModel: InicioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(XPAzulClaro)
                .statusBarsPadding()
                .padding(start = 16.dp, end = 8.dp, bottom = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onNotificaciones) {
                    Icon(Icons.Filled.Notifications, contentDescription = "Notificaciones", tint = XPMarino)
                }
                IconButton(onClick = onAjustes) {
                    Image(
                        painter = painterResource(uiState.avatar.dibujo),
                        contentDescription = "Ajustes de perfil",
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.mascota),
                    contentDescription = null,
                    modifier = Modifier.size(width = 96.dp, height = 106.dp)
                )
                Text(
                    "¡Bienvenido a XP SPEAK!",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                )
            }
            TarjetaRacha(uiState.racha, uiState.semana, modifier = Modifier.padding(top = 10.dp, end = 8.dp))
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Acceso("Lecciones", R.drawable.inicio_lecciones, onLecciones, Modifier.weight(1f))
                Acceso("Minijuegos", R.drawable.inicio_minijuegos, onMinijuegos, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Acceso("Practica con la IA", R.drawable.inicio_ia, onChat, Modifier.weight(1f))
                Acceso("Sigue tu progreso", R.drawable.inicio_progreso, onProgreso, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TarjetaRacha(racha: Int, semana: List<Boolean>, modifier: Modifier = Modifier) {
    Surface(color = XPBlanco, shape = RoundedCornerShape(18.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(painterResource(R.drawable.icono_racha), contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    if (racha > 0) "Racha de ${Racha.texto(racha)}" else "Empieza tu racha hoy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                DIAS.forEachIndexed { i, (letra, nombre) ->
                    val activo = semana.getOrElse(i) { false }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.semantics(mergeDescendants = true) {
                            contentDescription = "$nombre: " + if (activo) "practicaste" else "sin práctica"
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (activo) XPMarino else XPBlanco)
                                .border(BorderStroke(2.dp, if (activo) XPMarino else XPGris), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activo) Icon(Icons.Filled.Check, contentDescription = null, tint = XPBlanco, modifier = Modifier.size(18.dp))
                        }
                        Text(letra, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun Acceso(titulo: String, @DrawableRes dibujo: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            titulo,
            fontFamily = AdlamDisplay,
            style = MaterialTheme.typography.titleLarge,
            color = XPTexto,
            textAlign = TextAlign.Center,
            minLines = 2
        )
        Image(painterResource(dibujo), contentDescription = null, modifier = Modifier.size(124.dp))
    }
}
