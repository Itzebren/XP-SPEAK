package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.CampoTexto
import com.xpspeak.app.core.ui.componentes.Logotipo
import com.xpspeak.app.core.ui.theme.XPMarino

/** Iniciar sesión y Crear cuenta (Ilustraciones 28 y 29). */
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onNecesitaNivel: () -> Unit,
    onOlvideContrasena: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val esLogin = uiState.modo == AuthMode.LOGIN

    LaunchedEffect(uiState.destino) {
        when (uiState.destino) {
            DestinoPostAuth.SELECCIONAR_NIVEL -> onNecesitaNivel()
            DestinoPostAuth.HOME -> onAuthSuccess()
            DestinoPostAuth.NINGUNO -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Logotipo(tamano = if (esLogin) 190.dp else 160.dp)
        Text(
            if (esLogin) "¡Bienvenido!" else "Crear cuenta",
            style = MaterialTheme.typography.displaySmall,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        CampoTexto(
            valor = uiState.correo,
            onCambio = viewModel::onCorreoChange,
            etiqueta = "Correo electrónico",
            icono = Icons.Filled.Email,
            tipoTeclado = KeyboardType.Email
        )
        if (!esLogin) {
            CampoTexto(
                valor = uiState.nombre,
                onCambio = viewModel::onNombreChange,
                etiqueta = "Nombre de usuario",
                icono = Icons.Filled.Person
            )
        }
        CampoTexto(
            valor = uiState.password,
            onCambio = viewModel::onPasswordChange,
            etiqueta = "Contraseña",
            icono = Icons.Filled.Lock,
            esContrasena = true
        )
        if (!esLogin) {
            CampoTexto(
                valor = uiState.confirmarPassword,
                onCambio = viewModel::onConfirmarPasswordChange,
                etiqueta = "Confirmar contraseña",
                icono = Icons.Filled.Lock,
                esContrasena = true
            )
        }

        if (esLogin) {
            TextButton(onClick = onOlvideContrasena, modifier = Modifier.align(Alignment.End)) {
                Text("¿Olvidaste tu contraseña?", color = XPMarino, fontWeight = FontWeight.SemiBold)
            }
        }

        uiState.error?.let { mensaje ->
            Text(mensaje, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        BotonPrincipal(
            texto = if (esLogin) "Iniciar sesión" else "Comenzar",
            onClick = viewModel::enviar,
            cargando = uiState.cargando,
            modifier = Modifier.padding(top = 4.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (esLogin) "¿No tienes cuenta?" else "¿Ya tienes cuenta?",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { viewModel.cambiarModo(if (esLogin) AuthMode.REGISTRO else AuthMode.LOGIN) }) {
                Text(if (esLogin) "Crear cuenta" else "Iniciar sesión", color = XPMarino, fontWeight = FontWeight.Bold)
            }
        }
    }
}
