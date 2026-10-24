package com.xpspeak.app.core.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Esquema de color de XP-SPEAK (docs/diseno.md, Decisiones 1 y 2).
 *
 * - `primary` es el marino: Material lo usa como color de TEXTO en botones de texto,
 *   enlaces, radios y campos, y el naranja ahí no se lee (2.3:1, RNF-01). En el PDF
 *   esos enlaces también son marinos.
 * - El naranja (`secondary`) va en los botones de acción ([coloresBotonPrincipal]) y en
 *   las barras de avance, con el texto encima en marino.
 * - Solo hay tema claro: el diseño del TT no define uno oscuro y el arte está pensado
 *   para fondos claros.
 */
private val XPSpeakColors = lightColorScheme(
    primary = XPMarino,
    onPrimary = XPBlanco,
    primaryContainer = XPAzulClaro,
    onPrimaryContainer = XPMarino,
    secondary = XPNaranja,
    onSecondary = XPMarino,
    secondaryContainer = XPTarjeta,
    onSecondaryContainer = XPMarino,
    tertiary = XPAmarillo,
    onTertiary = XPMarino,
    tertiaryContainer = XPAmarillo.copy(alpha = 0.25f),
    onTertiaryContainer = XPMarino,
    background = XPFondo,
    onBackground = XPTexto,
    surface = XPFondo,
    onSurface = XPTexto,
    surfaceVariant = XPTarjeta,
    onSurfaceVariant = XPTextoSecundario,
    surfaceContainerLowest = XPBlanco,
    surfaceContainerLow = XPBlanco,
    surfaceContainer = XPBlanco,
    surfaceContainerHigh = XPTarjeta,
    surfaceContainerHighest = XPTarjeta,
    outline = XPGris,
    outlineVariant = XPAzulClaro,
    error = XPRojoError,
)

@Composable
fun XPSpeakTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XPSpeakColors,
        typography = XPSpeakTypography,
        shapes = XPSpeakShapes,
        content = content
    )
}

/** Botón naranja de acción con texto marino (Ilustraciones 28–41). */
@Composable
fun coloresBotonPrincipal(): ButtonColors =
    ButtonDefaults.buttonColors(containerColor = XPNaranja, contentColor = XPMarino)
