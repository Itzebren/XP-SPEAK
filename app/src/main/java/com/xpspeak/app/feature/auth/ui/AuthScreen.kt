package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
 * Pantalla de ejemplo (Fase 1): solo demuestra que Compose -> ViewModel ->
 * Repository -> Room funcionan de punta a punta. El flujo real de
 * registro/login/recuperación (CU-01, CU-02, CU-03) se construye en la
 * Fase 2 sobre esta misma base.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.registroExitoso) {
        if (uiState.registroExitoso) onAuthSuccess()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("XP-SPEAK — Demo de arquitectura (Fase 1)")

        OutlinedTextField(
            value = uiState.correo,
            onValueChange = viewModel::onCorreoChange,
            label = { Text("Correo") },
            modifier = Modifier.padding(top = 16.dp)
        )

        OutlinedTextField(
            value = uiState.nivel,
            onValueChange = viewModel::onNivelChange,
            label = { Text("Nivel (A1/A2)") },
            modifier = Modifier.padding(top = 8.dp)
        )

        Button(
            onClick = viewModel::registrarDemo,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text("Registrar (demo)")
        }

        if (uiState.cargando) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
