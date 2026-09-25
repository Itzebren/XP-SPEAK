package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * RF-04: selección manual de nivel A1/A2 en el primer ingreso.
 * No es un examen de colocación — el usuario elige directamente.
 */
@Composable
fun SeleccionarNivelScreen(
    onNivelSeleccionado: () -> Unit,
    viewModel: NivelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.listo) {
        if (uiState.listo) onNivelSeleccionado()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¿Cuál es tu nivel de inglés?")

        Button(
            onClick = { viewModel.seleccionarNivel("A1") },
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) { Text("A1 — Principiante") }

        Button(
            onClick = { viewModel.seleccionarNivel("A2") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) { Text("A2 — Elemental") }

        if (uiState.guardando) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
