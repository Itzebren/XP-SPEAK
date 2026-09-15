package com.xpspeak.app.feature.auth.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que respalda RF-01 (registro), RF-04 (nivel inicial) y
 * RF-05 (perfil y progreso). Se mantiene mínima a propósito: en la Fase 2
 * (Autenticación) la ampliaremos con lo que realmente necesite el flujo de
 * registro/login (por ejemplo, hash seguro de contraseña, no texto plano).
 *
 * Este archivo es solo el ejemplo de arquitectura de la Fase 1 — todavía
 * no implementa la lógica real de autenticación.
 */
@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val correo: String,
    val nivel: String,   // "A1" o "A2" — RF-04
    val xp: Int = 0,
    val racha: Int = 0
)
