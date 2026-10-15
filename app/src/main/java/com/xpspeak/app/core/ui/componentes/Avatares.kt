package com.xpspeak.app.core.ui.componentes

import androidx.annotation.DrawableRes
import com.xpspeak.app.R

/**
 * Avatares de la pantalla de Personalización (Ilustración 31). El id es lo que se
 * guarda en `UsuarioEntity.avatar`; sin avatar se muestra el robot.
 */
enum class Avatar(val id: String, val descripcion: String, @DrawableRes val dibujo: Int) {
    ROBOT("robot", "Robot", R.drawable.avatar_robot),
    LECTORA("lectora", "Lectora con lentes", R.drawable.avatar_lectora),
    CHONGOS("chongos", "Chica con chongos", R.drawable.avatar_chongos),
    ZORRO("zorro", "Zorro", R.drawable.avatar_zorro),
    RIZOS("rizos", "Chico de rizos", R.drawable.avatar_rizos),
    ABUELO("abuelo", "Abuelo con gorro", R.drawable.avatar_abuelo),
    PANUELO("panuelo", "Chica con pañuelo", R.drawable.avatar_panuelo),
    GUITARRA("guitarra", "Guitarrista", R.drawable.avatar_guitarra);

    companion object {
        fun porId(id: String?): Avatar = entries.firstOrNull { it.id == id } ?: ROBOT
    }
}
