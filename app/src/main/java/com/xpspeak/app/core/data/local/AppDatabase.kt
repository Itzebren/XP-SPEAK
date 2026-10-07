package com.xpspeak.app.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import com.xpspeak.app.feature.games.data.MinijuegosDao
import com.xpspeak.app.feature.games.data.PartidaGuardadaEntity
import com.xpspeak.app.feature.games.data.ResultadoMinijuegoEntity
import com.xpspeak.app.feature.lessons.data.CatalogoCacheEntity
import com.xpspeak.app.feature.lessons.data.IntentoPendienteEntity
import com.xpspeak.app.feature.lessons.data.LeccionCacheEntity
import com.xpspeak.app.feature.lessons.data.LeccionesDao

/**
 * Base de datos local (Room sobre SQLite — decisión del Cap. 3.4.6).
 *
 * Cada módulo (Lecciones, Progreso/SRS, Gamificación) agrega aquí su
 * propia entidad y DAO conforme lo vamos construyendo.
 */
@Database(
    entities = [
        UsuarioEntity::class,
        LeccionCacheEntity::class,
        CatalogoCacheEntity::class,
        IntentoPendienteEntity::class,
        PartidaGuardadaEntity::class,
        ResultadoMinijuegoEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun leccionesDao(): LeccionesDao
    abstract fun minijuegosDao(): MinijuegosDao

    companion object {
        /**
         * v3: caché offline de Lecciones. Se migra en vez de recrear la base
         * para no perder el perfil (nivel y XP solo viven en Room).
         */
        val MIGRACION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lecciones_cache` (`id` TEXT NOT NULL, " +
                        "`version` INTEGER NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `catalogos_cache` (`uid` TEXT NOT NULL, " +
                        "`nivel` TEXT NOT NULL, `json` TEXT NOT NULL, PRIMARY KEY(`uid`, `nivel`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `intentos_pendientes` (`attemptId` TEXT NOT NULL, " +
                        "`uid` TEXT NOT NULL, `leccionId` TEXT NOT NULL, `json` TEXT NOT NULL, " +
                        "`creadoEn` INTEGER NOT NULL, PRIMARY KEY(`attemptId`))"
                )
            }
        }

        /**
         * v4: Minijuegos (docs/minijuegos-diseno.md §4.2) — partida en curso,
         * outbox de resultados y el último día activo para la racha (RN-08).
         */
        val MIGRACION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `usuarios` ADD COLUMN `ultimoDiaActivo` TEXT")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `partidas_minijuego` (`uid` TEXT NOT NULL, `juego` TEXT NOT NULL, " +
                        "`partidaId` TEXT NOT NULL, `estadoJson` TEXT NOT NULL, `iniciadaEn` INTEGER NOT NULL, " +
                        "`actualizadaEn` INTEGER NOT NULL, PRIMARY KEY(`uid`, `juego`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `resultados_minijuego` (`partidaId` TEXT NOT NULL, `uid` TEXT NOT NULL, " +
                        "`juego` TEXT NOT NULL, `aciertos` INTEGER NOT NULL, `total` INTEGER NOT NULL, " +
                        "`duracionMs` INTEGER NOT NULL, `xpLocal` INTEGER NOT NULL, `xpServidor` INTEGER, " +
                        "`conceptosJson` TEXT NOT NULL, `dia` TEXT NOT NULL, `terminadaEn` INTEGER NOT NULL, " +
                        "`sincronizado` INTEGER NOT NULL, PRIMARY KEY(`partidaId`))"
                )
            }
        }
    }
}
