package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.exito) {
        if (uiState.exito) onAuthSuccess()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(if (uiState.modo == AuthMode.LOGIN) "Iniciar sesión" else "Crear cuenta")

        OutlinedTextField(
            value = uiState.correo,
            onValueChange = viewModel::onCorreoChange,
            label = { Text("Correo") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        OutlinedTextField(
            value = uiState.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        if (uiState.modo == AuthMode.REGISTRO) {
            OutlinedTextField(
                value = uiState.confirmarPassword,
                onValueChange = viewModel::onConfirmarPasswordChange,
                label = { Text("Confirmar contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }

        uiState.error?.let { mensaje ->
            Text(mensaje, modifier = Modifier.padding(top = 8.dp))
        }

        Button(onClick = viewModel::enviar, modifier = Modifier.padding(top = 16.dp)) {
            Text(if (uiState.modo == AuthMode.LOGIN) "Entrar" else "Registrarme")
        }

        TextButton(
            onClick = {
                viewModel.cambiarModo(if (uiState.modo == AuthMode.LOGIN) AuthMode.REGISTRO else AuthMode.LOGIN)
            },
            modifier = Modifier.padding(top = 8.dp)
        ) {
            Text(
                if (uiState.modo == AuthMode.LOGIN) "¿No tienes cuenta? Regístrate"
                else "¿Ya tienes cuenta? Inicia sesión"
            )
        }

        if (uiState.cargando) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
