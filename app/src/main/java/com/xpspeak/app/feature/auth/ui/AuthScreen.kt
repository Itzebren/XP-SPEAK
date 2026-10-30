package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xpspeak.app.core.ui.componentes.BotonPrincipal
import com.xpspeak.app.core.ui.componentes.BotonSecundario
import com.xpspeak.app.core.ui.componentes.CampoTexto
import com.xpspeak.app.core.ui.componentes.Logotipo
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.feature.auth.domain.PasswordValidator

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
            .imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Sesión guardada: solo el logo mientras se decide a dónde ir.
        if (uiState.retomandoSesion) {
            Logotipo(tamano = 190.dp)
            CircularProgressIndicator(color = XPMarino)
            return@Column
        }
        if (uiState.pendienteVerificacion) {
            AvisoVerificacion(uiState, viewModel)
            return@Column
        }
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
        if (!esLogin) RequisitosContrasena(uiState.password)
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

        // El aviso de requisitos ya está bajo el campo: no se repite aquí.
        uiState.error?.takeIf { esLogin || uiState.password.isEmpty() || it != PasswordValidator.mensajeError(uiState.password) }?.let { mensaje ->
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

/** RN-01: la cuenta no entra hasta confirmar el correo con el enlace que manda Firebase. */
@Composable
private fun ColumnScope.AvisoVerificacion(uiState: AuthUiState, viewModel: AuthViewModel) {
    Logotipo(tamano = 160.dp)
    Text("Verifica tu correo", style = MaterialTheme.typography.displaySmall)
    Text(
        "Te enviamos un enlace a ${uiState.correo}. Ábrelo para confirmar tu cuenta y después toca «Ya verifiqué». " +
            "Si no lo ves, revisa la carpeta de spam.",
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    uiState.aviso?.let { Text(it, color = XPMarino, style = MaterialTheme.typography.bodyMedium) }
    uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
    BotonPrincipal(texto = "Ya verifiqué", onClick = viewModel::comprobarVerificacion, cargando = uiState.cargando)
    BotonSecundario(texto = "Reenviar correo", onClick = viewModel::reenviarVerificacion)
    TextButton(onClick = viewModel::usarOtraCuenta, modifier = Modifier.align(Alignment.CenterHorizontally)) {
        Text("Usar otra cuenta", color = XPMarino, fontWeight = FontWeight.SemiBold)
    }
}
