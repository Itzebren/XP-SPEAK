package com.xpspeak.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Formas de las Ilustraciones 28–41: campos y botones con esquinas suaves (no píldora),
 * tarjetas más redondeadas y portadas de minijuegos/lecciones a 24 dp.
 */
val XPSpeakShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Botones principales (naranja) y secundarios: rectángulo suave, como en el PDF. */
val FormaBoton = RoundedCornerShape(10.dp)

/** Portadas ilustradas de minijuegos y lecciones. */
val FormaPortada = RoundedCornerShape(24.dp)
