package com.xpspeak.app.feature.auth.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuarios WHERE uid = :uid LIMIT 1")
    suspend fun buscarPorUid(uid: String): UsuarioEntity?

    /** RF-14: XP que confirma el servidor al aprobar una lección. */
    @Query("UPDATE usuarios SET xp = xp + :xp WHERE uid = :uid")
    suspend fun sumarXp(uid: String, xp: Int)

    /** RN-08: racha calculada con Racha.siguiente al terminar una actividad. */
    @Query("UPDATE usuarios SET racha = :racha, ultimoDiaActivo = :dia WHERE uid = :uid")
    suspend fun actualizarRacha(uid: String, racha: Int, dia: String)

    /**
     * Progreso: XP y racha que confirma GET /api/progress (corrige la deriva
     * local). Solo se usa cuando no hay actividades sin enviar.
     */
    @Query("UPDATE usuarios SET xp = :xp, racha = :racha, ultimoDiaActivo = :ultimoDia WHERE uid = :uid")
    suspend fun actualizarProgreso(uid: String, xp: Int, racha: Int, ultimoDia: String?)

    /** Progreso con actividades sin enviar: la XP se sincroniza, la racha local se conserva. */
    @Query("UPDATE usuarios SET xp = :xp WHERE uid = :uid")
    suspend fun actualizarXp(uid: String, xp: Int)

    /** RF-05: avatar elegido en Personalización o en Ajustes de perfil (id de `Avatar`). */
    @Query("UPDATE usuarios SET avatar = :avatar WHERE uid = :uid")
    suspend fun actualizarAvatar(uid: String, avatar: String)

    /** Ajustes de perfil: cambiar de nivel MCER. El avance de cada nivel se guarda aparte. */
    @Query("UPDATE usuarios SET nivel = :nivel WHERE uid = :uid")
    suspend fun actualizarNivel(uid: String, nivel: String)

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    fun observarUsuario(uid: String): Flow<UsuarioEntity?>
}
