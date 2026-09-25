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
fun RecuperarAccesoScreen(
    onCompletado: () -> Unit,
    viewModel: RecuperarAccesoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.completado) {
        if (uiState.completado) onCompletado()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (uiState.paso) {
            PasoRecuperacion.CORREO -> {
                Text("Recuperar acceso")
                Text("Te enviaremos un código de verificación a tu correo.")

                OutlinedTextField(
                    value = uiState.correo,
                    onValueChange = viewModel::onCorreoChange,
                    label = { Text("Correo") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                Button(
                    onClick = viewModel::solicitarCodigo,
                    modifier = Modifier.padding(top = 16.dp)
                ) { Text("Enviar código") }
            }

            PasoRecuperacion.CODIGO -> {
                Text("Revisa tu correo")
                Text("Ingresa el código de 6 caracteres y tu nueva contraseña.")

                OutlinedTextField(
                    value = uiState.codigo,
                    onValueChange = viewModel::onCodigoChange,
                    label = { Text("Código") },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )

                OutlinedTextField(
                    value = uiState.nuevaPassword,
                    onValueChange = viewModel::onNuevaPasswordChange,
                    label = { Text("Nueva contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                OutlinedTextField(
                    value = uiState.confirmarPassword,
                    onValueChange = viewModel::onConfirmarPasswordChange,
                    label = { Text("Confirmar contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Button(
                    onClick = viewModel::confirmarNuevaPassword,
                    modifier = Modifier.padding(top = 16.dp)
                ) { Text("Cambiar contraseña") }
            }
        }

        uiState.error?.let { mensaje ->
            Text(mensaje, modifier = Modifier.padding(top = 8.dp))
        }

        if (uiState.cargando) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
