package com.xpspeak.app.feature.auth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * RF-04 (nivel inicial) y RF-05 (perfil: nivel, avatar, puntos, rachas).
 * La clave primaria es el UID de Firebase, no un id autogenerado local:
 * así el perfil queda ligado directamente a la cuenta de Firebase Auth.
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val uid: String,
    val correo: String,
    val nivel: String,       // "A1" o "A2" — RF-04
    val avatar: String? = null,
    val xp: Int = 0,
    val racha: Int = 0
)
