package com.xpspeak.app.core.ui.componentes

import androidx.annotation.DrawableRes
import com.xpspeak.app.R

/**
 * Avatares de la pantalla de Personalización (Ilustración 31). El id es lo que se
 * guarda en `UsuarioEntity.avatar`; sin avatar, o con uno que ya no existe, se
 * muestra el cohete. Los dibujos salen de docs/diseno-arte/avatares.js.
 */
enum class Avatar(val id: String, val descripcion: String, @DrawableRes val dibujo: Int) {
    COHETE("cohete", "Cohete", R.drawable.avatar_cohete),
    ZORRO("zorro", "Zorro con audífonos", R.drawable.avatar_zorro),
    ASTRONAUTA("astronauta", "Astronauta", R.drawable.avatar_astronauta),
    MONSTRUO("monstruo", "Monstruo gamer", R.drawable.avatar_monstruo),
    PINGUINO("pinguino", "Pingüino con gorro", R.drawable.avatar_pinguino),
    CHICA("chica", "Chica con audífonos", R.drawable.avatar_chica),
    CHICO("chico", "Chico con gorra", R.drawable.avatar_chico),
    ESTRELLA("estrella", "Estrella", R.drawable.avatar_estrella);

    companion object {
        fun porId(id: String?): Avatar = entries.firstOrNull { it.id == id } ?: COHETE
    }
}
