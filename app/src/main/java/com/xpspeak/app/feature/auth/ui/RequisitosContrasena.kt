package com.xpspeak.app.feature.auth.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xpspeak.app.core.ui.theme.XPVerdeExito
import com.xpspeak.app.feature.auth.domain.PasswordValidator

/**
 * Debajo de la contraseña nueva (crear cuenta y recuperar acceso): mientras se
 * escribe dice qué le falta, "Tu contraseña debe contener al menos: …" (RNF-06).
 */
@Composable
fun RequisitosContrasena(password: String) {
    if (password.isEmpty()) return
    val falta = PasswordValidator.mensajeError(password)
    val color = if (falta == null) XPVerdeExito else MaterialTheme.colorScheme.error
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            if (falta == null) Icons.Filled.CheckCircle else Icons.Filled.Info,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
        Text(falta ?: "Tu contraseña cumple los requisitos.", color = color, style = MaterialTheme.typography.bodyMedium)
    }
}
