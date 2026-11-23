package com.xpspeak.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xpspeak.app.R

/** Tipografía principal del TT (Cap. 4.5): ADLaM Display, solo para títulos y cifras. */
val AdlamDisplay = FontFamily(Font(R.font.adlam_display))

private val base = Typography()

// Títulos en ADLaM Display y en marino, como en las Ilustraciones 28–41;
// el cuerpo se queda en la fuente del sistema para leer cómodo en las lecciones.
private fun TextStyle.titulo() = copy(fontFamily = AdlamDisplay, fontWeight = FontWeight.Normal, color = XPMarino)

val XPSpeakTypography = Typography(
    displayLarge = base.displayLarge.titulo(),
    displayMedium = base.displayMedium.titulo(),
    displaySmall = base.displaySmall.titulo(),
    headlineLarge = base.headlineLarge.titulo().copy(fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = base.headlineMedium.titulo().copy(fontSize = 28.sp, lineHeight = 34.sp),
    headlineSmall = base.headlineSmall.titulo().copy(fontSize = 24.sp, lineHeight = 30.sp),
    titleLarge = base.titleLarge.titulo().copy(fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge,
    bodyMedium = base.bodyMedium,
    bodySmall = base.bodySmall,
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    labelMedium = base.labelMedium,
    labelSmall = base.labelSmall,
)
