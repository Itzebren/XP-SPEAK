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

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    fun observarUsuario(uid: String): Flow<UsuarioEntity?>
}
