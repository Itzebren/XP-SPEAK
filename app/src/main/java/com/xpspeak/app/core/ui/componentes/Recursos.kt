package com.xpspeak.app.core.ui.componentes

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Id de un drawable por nombre, o 0 si no existe. Así el arte que depende de un id
 * del servidor (`insignia_<id>`, `leccion_<id>`, `juego_<id>`) se agrega sin cambiar código.
 */
@SuppressLint("DiscouragedApi")
@Composable
fun recursoDrawable(nombre: String): Int {
    val context = LocalContext.current
    return remember(nombre) { context.resources.getIdentifier(nombre, "drawable", context.packageName) }
}
