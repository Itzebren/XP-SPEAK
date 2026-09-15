package com.xpspeak.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity

/**
 * Base de datos local (Room sobre SQLite — decisión del Cap. 3.4.6).
 *
 * Cada módulo (Lecciones, Progreso/SRS, Gamificación) agregará aquí su
 * propia entidad y DAO conforme lo vayamos construyendo en las siguientes
 * fases. Por ahora solo contiene Usuario, como ejemplo de arquitectura.
 */
@Database(
    entities = [UsuarioEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
}
