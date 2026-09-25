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

    @Query("SELECT * FROM usuarios WHERE uid = :uid")
    fun observarUsuario(uid: String): Flow<UsuarioEntity?>
}
