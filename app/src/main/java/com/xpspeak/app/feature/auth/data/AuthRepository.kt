package com.xpspeak.app.feature.auth.data

import javax.inject.Inject

/**
 * Repositorio de ejemplo: aísla al ViewModel de los detalles de Room.
 * En la Fase 2 esto crecerá para incluir validación de credenciales,
 * hash de contraseña y, eventualmente, sincronización remota si el
 * diseño final lo requiere.
 */
class AuthRepository @Inject constructor(
    private val usuarioDao: UsuarioDao
) {
    suspend fun registrarUsuarioDemo(correo: String, nivel: String): Long {
        return usuarioDao.insertar(UsuarioEntity(correo = correo, nivel = nivel))
    }
}
