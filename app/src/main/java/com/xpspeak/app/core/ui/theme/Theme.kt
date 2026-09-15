package com.xpspeak.app.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val XPSpeakLightColors = lightColorScheme(
    primary = XPBluePrimary,
    onPrimary = XPNeutralWhite,
    primaryContainer = XPBlueLight,
    secondary = XPOrangeAccent,
    tertiary = XPYellowAccent,
    background = XPNeutralGrayLight,
    surface = XPNeutralWhite,
    onSurface = XPNeutralGrayDark
)

private val XPSpeakDarkColors = darkColorScheme(
    primary = XPBlueLight,
    onPrimary = XPNeutralGrayDark,
    primaryContainer = XPBlueDark,
    secondary = XPOrangeAccent,
    tertiary = XPYellowAccent,
    background = XPNeutralGrayDark,
    surface = XPBlueDark,
    onSurface = XPNeutralWhite
)

@Composable
fun XPSpeakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) XPSpeakDarkColors else XPSpeakLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = XPSpeakTypography,
        content = content
    )
}
