package com.xpspeak.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xpspeak.app.R

// TODO (Fase 1, pendiente de tu parte): descarga "ADLaM Display" desde
// https://fonts.google.com/specimen/ADLaM+Display, coloca el archivo .ttf
// en app/src/main/res/font/adlam_display.ttf y descomenta la línea de abajo.
// Mientras tanto se usa la tipografía por defecto del sistema para que el
// proyecto compile sin errores.

private val AdlamDisplay = FontFamily.Default
// private val AdlamDisplay = FontFamily(Font(R.font.adlam_display))

val XPSpeakTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = AdlamDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AdlamDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)
