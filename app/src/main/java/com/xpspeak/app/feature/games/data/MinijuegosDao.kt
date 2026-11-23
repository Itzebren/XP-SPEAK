package com.xpspeak.app.feature.games.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/*
 * Persistencia local de los minijuegos (docs/minijuegos-diseno.md §4.2).
 * El estado de cada juego se guarda como JSON: así un cambio en la mecánica
 * no obliga a migrar la base.
 */

/** Partida en curso, una por usuario y juego (RNF-10 / RN-12, CU-07 A1). */
@Entity(tableName = "partidas_minijuego", primaryKeys = ["uid", "juego"])
data class PartidaGuardadaEntity(
    val uid: String,
    val juego: String,
    val partidaId: String,
    val estadoJson: String,
    val iniciadaEn: Long,
    val actualizadaEn: Long
)

/**
 * Partida terminada. Es la outbox del servidor (`sincronizado = false`) y,
 * ya sincronizada, el historial que cuenta las partidas del día y los
 * conceptos fallados sin conexión.
 */
@Entity(tableName = "resultados_minijuego")
data class ResultadoMinijuegoEntity(
    @PrimaryKey val partidaId: String,
    val uid: String,
    val juego: String,
    val aciertos: Int,
    val total: Int,
    val duracionMs: Long,
    /** XP calculada en el teléfono; la oficial llega en `xpServidor`. */
    val xpLocal: Int,
    val xpServidor: Int? = null,
    /** Map<conceptoId, ConteoConcepto> serializado. */
    val conceptosJson: String,
    /** Día local (ISO) en que terminó: para el tope diario de XP. */
    val dia: String,
    val terminadaEn: Long,
    val sincronizado: Boolean = false
)

@Dao
interface MinijuegosDao {

    @Query("SELECT * FROM partidas_minijuego WHERE uid = :uid AND juego = :juego")
    suspend fun partida(uid: String, juego: String): PartidaGuardadaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarPartida(partida: PartidaGuardadaEntity)

    @Query("DELETE FROM partidas_minijuego WHERE uid = :uid AND juego = :juego")
    suspend fun borrarPartida(uid: String, juego: String)

    /** Juegos con una partida a medias, para marcarlos en el catálogo. */
    @Query("SELECT juego FROM partidas_minijuego WHERE uid = :uid AND actualizadaEn >= :desde")
    fun juegosEnCurso(uid: String, desde: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertarResultado(resultado: ResultadoMinijuegoEntity)

    @Query("SELECT * FROM resultados_minijuego WHERE uid = :uid AND sincronizado = 0 ORDER BY terminadaEn")
    suspend fun pendientes(uid: String): List<ResultadoMinijuegoEntity>

    @Query("UPDATE resultados_minijuego SET sincronizado = 1, xpServidor = :xp WHERE partidaId = :partidaId")
    suspend fun marcarSincronizado(partidaId: String, xp: Int)

    @Query("SELECT COUNT(*) FROM resultados_minijuego WHERE uid = :uid AND juego = :juego AND dia = :dia")
    suspend fun contarDelDia(uid: String, juego: String, dia: String): Int

    @Query("SELECT conceptosJson FROM resultados_minijuego WHERE uid = :uid AND terminadaEn >= :desde")
    suspend fun conceptosDesde(uid: String, desde: Long): List<String>

    /** El historial solo hace falta unos días (tope diario y conceptos recientes). */
    @Query("DELETE FROM resultados_minijuego WHERE sincronizado = 1 AND terminadaEn < :antes")
    suspend fun borrarHistorialAntes(antes: Long)
}
