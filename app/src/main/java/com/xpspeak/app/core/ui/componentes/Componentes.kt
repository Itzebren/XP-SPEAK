package com.xpspeak.app.core.ui.componentes

import androidx.compose.foundation.layout.statusBarsPadding
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.ButtonDefaults
import com.xpspeak.app.R
import com.xpspeak.app.core.ui.theme.FormaBoton
import com.xpspeak.app.core.ui.theme.coloresBotonPrincipal
import com.xpspeak.app.core.ui.theme.XPAzulClaro
import com.xpspeak.app.core.ui.theme.XPBlanco
import com.xpspeak.app.core.ui.theme.XPMarino
import com.xpspeak.app.core.ui.theme.XPTarjeta

/*
 * Piezas comunes de las Ilustraciones 28–41 (docs/diseno.md, sección 3).
 */

/**
 * Encabezado azul claro con la mascota, como en Progreso, Minijuegos y Lecciones.
 * [contenido] va debajo del título (p. ej. el distintivo de nivel o la racha).
 */
@Composable
fun EncabezadoXP(
    titulo: String,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    @DrawableRes mascota: Int? = R.drawable.mascota,
    acciones: @Composable RowScope.() -> Unit = {},
    contenido: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(XPAzulClaro)
            .statusBarsPadding()
            .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onVolver != null) {
                IconButton(onClick = onVolver) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = XPMarino)
                }
            } else {
                Box(modifier = Modifier.size(8.dp))
            }
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.End, content = acciones)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                contenido()
            }
            if (mascota != null) {
                Image(
                    painter = painterResource(mascota),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(width = 84.dp, height = 92.dp)
                )
            }
        }
    }
}

/** Botón naranja de las acciones principales ("Iniciar sesión", "Comenzar"). */
@Composable
fun BotonPrincipal(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cargando: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled && !cargando,
        shape = FormaBoton,
        colors = coloresBotonPrincipal(),
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = XPMarino)
        } else {
            Text(texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Botón azul claro con borde marino ("Cancelar y volver"). */
@Composable
fun BotonSecundario(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        shape = FormaBoton,
        border = BorderStroke(1.5.dp, XPMarino),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = XPAzulClaro, contentColor = XPMarino),
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp)
    ) {
        Text(texto, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

/** Tarjeta azul de contenido (nivel, XP, racha, secciones de ajustes y lección). */
@Composable
fun TarjetaXP(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(color = XPTarjeta, shape = MaterialTheme.shapes.large, modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Título de sección en mayúsculas dentro de una [TarjetaXP] ("DATOS Y CUENTA"). */
@Composable
fun TituloSeccion(texto: String) {
    Text(
        texto.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        color = XPMarino,
        letterSpacing = 0.5.sp
    )
}

/** Cuadro marino con el nivel MCER ("A1"). */
@Composable
fun DistintivoNivel(nivel: String, modifier: Modifier = Modifier, tamano: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano / 4))
            .background(XPMarino),
        contentAlignment = Alignment.Center
    ) {
        Text(nivel, color = XPBlanco, fontWeight = FontWeight.Bold, fontSize = (tamano.value * 0.4f).sp)
    }
}

/** Campo de texto con ícono al inicio; si [esContrasena], con botón para mostrarla. */
@Composable
fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    etiqueta: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    esContrasena: Boolean = false,
    tipoTeclado: KeyboardType = KeyboardType.Text
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        label = { Text(etiqueta) },
        singleLine = true,
        leadingIcon = { Icon(icono, contentDescription = null) },
        trailingIcon = if (esContrasena) {
            {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña"
                    )
                }
            }
        } else null,
        visualTransformation = if (esContrasena && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (esContrasena) KeyboardType.Password else tipoTeclado),
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = XPMarino,
            focusedLabelColor = XPMarino,
            focusedContainerColor = XPBlanco,
            unfocusedContainerColor = XPBlanco
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/** Logotipo de XP-SPEAK (Ilustración 27) para las pantallas de acceso. */
@Composable
fun Logotipo(modifier: Modifier = Modifier, tamano: Dp = 180.dp) {
    Image(
        painter = painterResource(R.drawable.logo_xpspeak),
        contentDescription = "Logotipo de XP-SPEAK",
        modifier = modifier.size(tamano)
    )
}
