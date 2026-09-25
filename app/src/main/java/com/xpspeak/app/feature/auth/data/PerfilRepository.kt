package com.xpspeak.app.feature.auth.data

import javax.inject.Inject

class PerfilRepository @Inject constructor(
    private val usuarioDao: UsuarioDao
) {
    suspend fun existePerfil(uid: String): Boolean =
        usuarioDao.buscarPorUid(uid) != null

    suspend fun crearPerfil(uid: String, correo: String, nivel: String) {
        usuarioDao.guardar(UsuarioEntity(uid = uid, correo = correo, nivel = nivel))
    }
}
