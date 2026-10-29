package com.xpspeak.app.feature.progress.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Última respuesta de GET /api/progress, para mostrar Progreso sin red
 * (RNF-10). Mismo patrón que `catalogos_cache` de Lecciones.
 */
@Entity(tableName = "progreso_cache")
data class ProgresoCacheEntity(
    @PrimaryKey val uid: String,
    val nivel: String,
    /** ProgresoDto serializado. */
    val json: String,
    /** ETag de esa respuesta, para revalidar con If-None-Match. */
    val etag: String?,
    /** Última vez que el servidor la confirmó (200 o 304). */
    val actualizadoEn: Long
)

@Dao
interface ProgresoDao {

    @Query("SELECT * FROM progreso_cache WHERE uid = :uid")
    fun observar(uid: String): Flow<ProgresoCacheEntity?>

    @Query("SELECT * FROM progreso_cache WHERE uid = :uid")
    suspend fun buscar(uid: String): ProgresoCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(cache: ProgresoCacheEntity)

    /** 304: la copia sigue vigente. */
    @Query("UPDATE progreso_cache SET actualizadoEn = :ahora WHERE uid = :uid")
    suspend fun marcarFresca(uid: String, ahora: Long)

    /** Partidas e intentos hechos sin conexión que el servidor aún no conoce. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM resultados_minijuego WHERE uid = :uid AND sincronizado = 0) + " +
            "(SELECT COUNT(*) FROM intentos_pendientes WHERE uid = :uid)"
    )
    suspend fun contarPendientes(uid: String): Int
}
