package com.xpspeak.app.core.actividad

import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.auth.data.UsuarioDao
import java.time.LocalDate
import javax.inject.Inject

/**
 * RN-08 en el perfil local: una actividad válida (lección aprobada, repaso,
 * minijuego o conversación con la IA) sube la racha una vez por día. Así la
 * racha se ve al momento, aunque no haya red; al refrescar Progreso se
 * sincroniza con la del servidor (docs/progreso-diseno.md, Fase 5).
 */
class RegistroActividad @Inject constructor(
    private val usuarioDao: UsuarioDao,
    private val sesion: SesionActual
) {
    /** @return la racha nueva, o 0 si el usuario no tiene perfil local */
    suspend fun registrar(uid: String, hoy: LocalDate = LocalDate.now()): Int {
        val usuario = usuarioDao.buscarPorUid(uid) ?: return 0
        val racha = Racha.siguiente(usuario.racha, usuario.ultimoDiaActivo, hoy)
        usuarioDao.actualizarRacha(uid, racha, hoy.toString())
        return racha
    }

    suspend fun registrarUsuarioActual(hoy: LocalDate = LocalDate.now()): Int =
        sesion.uid?.let { registrar(it, hoy) } ?: 0
}
