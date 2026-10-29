package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.CampoTexto
import com.xpspeak.app.core.ui.componentes.Logotipo

/** Recuperar contraseña (Ilustración 30), en dos pasos: correo y código. */
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
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Logotipo(tamano = 150.dp)
        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)

        when (uiState.paso) {
            PasoRecuperacion.CORREO -> {
                Text(
                    "Ingresa tu correo electrónico para recibir un código",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                CampoTexto(
                    valor = uiState.correo,
                    onCambio = viewModel::onCorreoChange,
                    etiqueta = "Correo electrónico",
                    icono = Icons.Filled.Email,
                    tipoTeclado = KeyboardType.Email
                )
                Error(uiState.error)
                BotonPrincipal("Enviar código", onClick = viewModel::solicitarCodigo, cargando = uiState.cargando)
            }

            PasoRecuperacion.CODIGO -> {
                Text(
                    "Revisa tu correo: escribe el código de 6 caracteres y tu nueva contraseña.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                CampoTexto(
                    valor = uiState.codigo,
                    onCambio = viewModel::onCodigoChange,
                    etiqueta = "Código de verificación",
                    icono = Icons.Filled.Pin
                )
                CampoTexto(
                    valor = uiState.nuevaPassword,
                    onCambio = viewModel::onNuevaPasswordChange,
                    etiqueta = "Nueva contraseña",
                    icono = Icons.Filled.Lock,
                    esContrasena = true
                )
                CampoTexto(
                    valor = uiState.confirmarPassword,
                    onCambio = viewModel::onConfirmarPasswordChange,
                    etiqueta = "Confirmar contraseña",
                    icono = Icons.Filled.Lock,
                    esContrasena = true
                )
                Error(uiState.error)
                BotonPrincipal("Restablecer contraseña", onClick = viewModel::confirmarNuevaPassword, cargando = uiState.cargando)
            }
        }
    }
}

@Composable
private fun Error(mensaje: String?) {
    mensaje?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
}
