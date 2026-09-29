package com.xpspeak.app.feature.lessons.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

/*
 * Caché offline de Lecciones (RN-11/RNF-08, docs/lecciones-diseno.md §3 y §6.2).
 * El contenido se guarda como el JSON que manda el backend: así un campo nuevo
 * en el esquema de la lección no obliga a migrar la base.
 */

/** Contenido de una lección (incluye la clave de respuestas para calificar offline). */
@Entity(tableName = "lecciones_cache")
data class LeccionCacheEntity(
    @PrimaryKey val id: String,
    /** Versión del manifiesto; 0 si se descargó sin pasar por el manifiesto. */
    val version: Int,
    val json: String
)

/** Último catálogo visto por usuario y nivel: los estados de desbloqueo son de cada usuario. */
@Entity(tableName = "catalogos_cache", primaryKeys = ["uid", "nivel"])
data class CatalogoCacheEntity(
    val uid: String,
    val nivel: String,
    val json: String
)

/** Outbox de intentos calificados sin conexión (RN-12); se reenvían con el mismo attempt_id. */
@Entity(tableName = "intentos_pendientes")
data class IntentoPendienteEntity(
    @PrimaryKey val attemptId: String,
    val uid: String,
    val leccionId: String,
    /** IntentoRequest serializado, listo para el POST. */
    val json: String,
    val creadoEn: Long
)

@Dao
interface LeccionesDao {

    @Query("SELECT * FROM lecciones_cache WHERE id = :id")
    suspend fun leccion(id: String): LeccionCacheEntity?

    @Query("SELECT id, version FROM lecciones_cache")
    suspend fun versiones(): List<VersionLeccion>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarLeccion(leccion: LeccionCacheEntity)

    @Query("DELETE FROM lecciones_cache WHERE id NOT IN (:vigentes)")
    suspend fun borrarLeccionesExcepto(vigentes: List<String>)

    @Query("SELECT * FROM catalogos_cache WHERE uid = :uid AND nivel = :nivel")
    suspend fun catalogo(uid: String, nivel: String): CatalogoCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarCatalogo(catalogo: CatalogoCacheEntity)

    @Query("SELECT * FROM intentos_pendientes WHERE uid = :uid ORDER BY creadoEn")
    suspend fun intentosPendientes(uid: String): List<IntentoPendienteEntity>

    @Query("SELECT COUNT(*) FROM intentos_pendientes WHERE uid = :uid")
    suspend fun contarPendientes(uid: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun encolarIntento(intento: IntentoPendienteEntity)

    @Query("DELETE FROM intentos_pendientes WHERE attemptId = :attemptId")
    suspend fun borrarIntento(attemptId: String)
}

data class VersionLeccion(val id: String, val version: Int)
