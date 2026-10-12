package com.xpspeak.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta del Trabajo Terminal, Cap. 4.5 (Ilustración 26). Ver docs/diseno.md.
// Azules = confianza/tecnología (base y estructura)
// Naranja/Amarillo = energía/recompensa/logro (botones, XP, rachas)
// Neutros = contenido y tipografía

// --- Tonos azules (base y estructura) ---
val XPAzulClaro = Color(0xFFC0D6EB)
val XPAzul = Color(0xFF4B7BBE)
val XPMarino = Color(0xFF232074)

// --- Acentos cálidos (recompensa, XP, rachas) ---
val XPNaranja = Color(0xFFFF8C00)
val XPAmarillo = Color(0xFFFFC107)

/** Naranja para TEXTO sobre fondos claros (4.9:1); el #FF8C00 solo da 2.3:1 (RNF-01). */
val XPNaranjaTexto = Color(0xFFB85400)

// --- Neutros (contenido y tipografía) ---
val XPBlanco = Color(0xFFFFFFFF)
val XPGris = Color(0xFF9E9E9E)

// --- Derivados de la paleta (no aparecen como muestra en el PDF, sí en las pantallas) ---
/** Fondo de las pantallas (Ilustraciones 28–41). */
val XPFondo = Color(0xFFF5F7FA)
/** Tarjetas sobre el fondo: progreso, ajustes, lección. */
val XPTarjeta = Color(0xFFDCE7F3)
/** Texto de cuerpo; los títulos van en [XPMarino]. */
val XPTexto = Color(0xFF1B1B2F)
/** Texto secundario con contraste suficiente sobre [XPFondo] (el #9E9E9E del PDF no llega a 4.5:1). */
val XPTextoSecundario = Color(0xFF5B6070)

// --- Retroalimentación (acierto/error en lecciones y minijuegos) ---
val XPVerdeExito = Color(0xFF2E7D32)
val XPRojoError = Color(0xFFC62828)
