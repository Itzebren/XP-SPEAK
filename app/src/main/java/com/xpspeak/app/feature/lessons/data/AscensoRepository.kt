package com.xpspeak.app.feature.lessons.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.lessons.domain.AscensoNivel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Aplica [AscensoNivel]: cambia el nivel del perfil local (el mismo que se cambia
 * desde Cuenta) y recuerda por cuenta que ya ascendió, para no volver a subirlo
 * si regresa a A1 a repasar.
 */
class AscensoRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao,
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("ascenso_nivel", Context.MODE_PRIVATE)

    /** El nivel nuevo si el usuario acaba de ascender, o null. */
    suspend fun ascenderSiCorresponde(catalogo: Catalogo): String? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        val nuevo = AscensoNivel.siguiente(catalogo.dto, catalogo.sinConexion, yaAscendio = prefs.getBoolean(clave(uid, catalogo.dto.nivel), false))
            ?: return null
        usuarioDao.actualizarNivel(uid, nuevo)
        prefs.edit().putBoolean(clave(uid, catalogo.dto.nivel), true).apply()
        return nuevo
    }

    private fun clave(uid: String, desde: String) = "$uid:$desde"
}
