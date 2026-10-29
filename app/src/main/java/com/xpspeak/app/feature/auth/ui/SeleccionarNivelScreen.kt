package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.Avatar
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.theme.AdlamDisplay
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPNaranja

/**
 * RF-04: selección manual de nivel A1/A2 en el primer ingreso, con avatar
 * (Ilustración 31). No es un examen de colocación — el usuario elige directamente.
 */
@Composable
fun SeleccionarNivelScreen(
    onNivelSeleccionado: () -> Unit,
    viewModel: NivelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var nivel by rememberSaveable { mutableStateOf<String?>(null) }
    var avatar by rememberSaveable { mutableStateOf(Avatar.COHETE.id) }

    LaunchedEffect(uiState.listo) {
        if (uiState.listo) onNivelSeleccionado()
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(XPAzulClaro)
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("¡Personaliza tu experiencia!", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Image(
                painter = painterResource(R.drawable.mascota),
                contentDescription = null,
                modifier = Modifier.padding(top = 4.dp).size(width = 110.dp, height = 121.dp)
            )
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text("¿Cuál es tu nivel de inglés?", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                TarjetaNivel(
                    "A1", "Saludos, presentaciones, frases sencillas, vocabulario cotidiano.",
                    elegido = nivel == "A1", onClick = { nivel = "A1" }, modifier = Modifier.weight(1f)
                )
                TarjetaNivel(
                    "A2", "Expresiones frecuentes, descripciones breves, historias sencillas.",
                    elegido = nivel == "A2", onClick = { nivel = "A2" }, modifier = Modifier.weight(1f)
                )
            }

            Text("Selecciona tu avatar", style = MaterialTheme.typography.titleLarge)
            Avatar.entries.chunked(4).forEach { fila ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    fila.forEach { opcion ->
                        val elegido = opcion.id == avatar
                        Image(
                            painter = painterResource(opcion.dibujo),
                            contentDescription = opcion.descripcion,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(BorderStroke(3.dp, if (elegido) XPNaranja else Color.Transparent), CircleShape)
                                .selectable(selected = elegido, role = Role.RadioButton) { avatar = opcion.id }
                                .padding(4.dp)
                        )
                    }
                }
            }

            BotonPrincipal(
                texto = if (nivel == null) "Elige tu nivel" else "Confirmar y comenzar",
                onClick = { nivel?.let { viewModel.seleccionarNivel(it, avatar) } },
                enabled = nivel != null,
                cargando = uiState.guardando
            )
        }
    }
}

@Composable
private fun TarjetaNivel(
    nivel: String,
    descripcion: String,
    elegido: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = XPBlanco,
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(3.dp, if (elegido) XPNaranja else Color.Transparent),
        modifier = modifier.selectable(selected = elegido, role = Role.RadioButton, onClick = onClick)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(XPMarino).padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                Text(nivel, fontFamily = AdlamDisplay, style = MaterialTheme.typography.headlineLarge, color = XPBlanco)
            }
            Text(
                descripcion,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(10.dp)
            )
        }
    }
}
