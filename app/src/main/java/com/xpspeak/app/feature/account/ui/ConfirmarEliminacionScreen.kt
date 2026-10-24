package com.xpspeak.app.feature.account.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.BotonSecundario
import com.xpspeak.app.core.ui.componentes.DistintivoNivel
import com.xpspeak.app.core.ui.theme.XPBlanco

/** Confirmar eliminación (Ilustración 36, RN-14): advierte qué se pierde antes de borrar. */
@Composable
fun ConfirmarEliminacionScreen(
    onCancelar: () -> Unit,
    onEliminada: () -> Unit,
    viewModel: ConfirmarEliminacionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.eliminada) {
        if (uiState.eliminada) onEliminada()
    }

    Column(
        modifier = Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painterResource(R.drawable.mascota_preocupada),
            contentDescription = null,
            modifier = Modifier.padding(top = 16.dp).size(width = 140.dp, height = 154.dp)
        )
        Surface(color = XPBlanco, shape = MaterialTheme.shapes.extraLarge, shadowElevation = 6.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "¡Advertencia!",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Esta acción es irreversible",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Estás a punto de eliminar permanentemente tu cuenta y todo tu progreso. No podrás recuperar:",
                    style = MaterialTheme.typography.bodyLarge
                )
                Perdida(R.drawable.moneda_xp, "Puntos de experiencia (XP)")
                Perdida(R.drawable.insignia_primer_logro, "Insignias desbloqueadas")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DistintivoNivel("A1", tamano = 30.dp)
                    Text("Progreso MCER (A1/A2)", fontWeight = FontWeight.Medium)
                }
                Perdida(R.drawable.icono_racha, "Racha de días consecutivos")

                uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

                BotonPrincipal(
                    "Confirmar y eliminar",
                    onClick = viewModel::eliminar,
                    cargando = uiState.eliminando,
                    modifier = Modifier.padding(top = 6.dp)
                )
                BotonSecundario("Cancelar y volver", onClick = onCancelar)
            }
        }
        Box(modifier = Modifier.padding(bottom = 24.dp))
    }
}

@Composable
private fun Perdida(@DrawableRes icono: Int, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(painterResource(icono), contentDescription = null, modifier = Modifier.size(30.dp))
        Text(texto, fontWeight = FontWeight.Medium)
    }
}
