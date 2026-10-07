package com.xpspeak.app.feature.games.data

import android.content.Context
import androidx.room.withTransaction
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.xpspeak.app.core.data.local.AppDatabase
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.games.domain.CalculadorXp
import com.xpspeak.app.feature.games.domain.ContenidoJuegos
import com.xpspeak.app.feature.games.domain.ConteoConcepto
import com.xpspeak.app.feature.games.domain.Marcador
import com.xpspeak.app.feature.games.domain.Minijuego
import com.xpspeak.app.feature.games.domain.Racha
import com.xpspeak.app.feature.games.domain.ResultadoPartida
import com.xpspeak.app.feature.lessons.data.CatalogoDto
import com.xpspeak.app.feature.lessons.data.ErrorDelServidor
import com.xpspeak.app.feature.lessons.data.ErrorDto
import com.xpspeak.app.feature.lessons.data.EstadoLeccion
import com.xpspeak.app.feature.lessons.data.LeccionesDao
import com.xpspeak.app.feature.lessons.data.LeccionesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import retrofit2.Response
import java.time.LocalDate
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Motor común de los minijuegos (docs/minijuegos-diseno.md §4): qué contenido
 * puede jugar el usuario, la partida en curso en Room, el cierre de partida
 * (XP, racha, outbox) y la sincronización con el servidor.
 *
 * Offline-first como Lecciones: los juegos se califican en el teléfono
 * (CU-07 "opera de forma local"); el servidor confirma la XP al recibir el
 * resultado, y la outbox se reenvía con el mismo partida_id hasta lograrlo.
 */
class MinijuegosRepository @Inject constructor(
    private val bancoLocal: BancoLocal,
    private val api: MinijuegosApi,
    private val dao: MinijuegosDao,
    private val usuarioDao: UsuarioDao,
    private val leccionesDao: LeccionesDao,
    private val leccionesRepository: LeccionesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val db: AppDatabase,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    suspend fun nivelDelUsuario(): String = leccionesRepository.nivelDelUsuario()

    /**
     * CU-07 pasos 2 y 4 + flujo A2: lecciones no bloqueadas del nivel del usuario
     * (un usuario A2 también repasa todo A1) y los conceptos que el SRS prioriza.
     */
    suspend fun contenido(): ContenidoJuegos {
        val uid = uidActual()
        val nivel = nivelDelUsuario()
        val banco = bancoLocal.banco()
        val desbloqueadas = leccionesDesbloqueadas(uid, nivel)
        val lecciones = banco.lecciones.filter { leccion ->
            (nivel == "A2" && leccion.nivel == "A1") ||
                (leccion.nivel == nivel && (desbloqueadas == null || leccion.id in desbloqueadas))
        }
        val ids = lecciones.map { it.id }.toSet()
        return ContenidoJuegos(
            nivel = nivel,
            lecciones = lecciones,
            misiones = banco.misiones.filter { it.leccionId in ids },
            prioritarios = conceptosPrioritarios(uid)
        )
    }

    /**
     * Del último catálogo de Lecciones (caché de Room). Si nunca se abrió, se
     * intenta descargar; sin conexión ni caché se usa todo el nivel (§3.3).
     */
    private suspend fun leccionesDesbloqueadas(uid: String, nivel: String): Set<String>? {
        val catalogo = leccionesDao.catalogo(uid, nivel)?.let { gson.fromJson(it.json, CatalogoDto::class.java) }
            ?: withTimeoutOrNull(TIEMPO_RED_MS) { leccionesRepository.catalogo().getOrNull()?.dto }
            ?: return null
        // Un estado desconocido (app vieja) cuenta como disponible.
        return catalogo.lecciones.filter { it.estado != EstadoLeccion.BLOQUEADA }.map { it.id }.toSet()
    }

    /** RF-13: los débiles del servidor más los fallados en partidas recientes (sirve sin red). */
    private suspend fun conceptosPrioritarios(uid: String): Set<String> {
        val delServidor = withTimeoutOrNull(TIEMPO_RED_MS) { leccionesRepository.conceptosDebiles().getOrNull() }
            ?.conceptos?.map { it.conceptoId }.orEmpty()
        val locales = dao.conceptosDesde(uid, System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DIAS_HISTORIAL))
            .flatMap { json -> leerConceptos(json).filterValues { it.errores > 0 }.keys }
        return (delServidor + locales).toSet()
    }

    // ── Partida en curso (RNF-10, RN-12) ───────────────────────────────────

    /** La partida a medias de este juego, si no venció (24 h, §4.1). */
    suspend fun <E> partidaGuardada(juego: Minijuego, clase: Class<E>): E? {
        val uid = uidActual()
        val guardada = dao.partida(uid, juego.id) ?: return null
        if (guardada.actualizadaEn < limiteVigencia()) {
            dao.borrarPartida(uid, juego.id)
            return null
        }
        // Si el formato del estado cambió con una actualización, se empieza de nuevo.
        return runCatching { gson.fromJson(guardada.estadoJson, clase) }.getOrNull()
            ?: run { dao.borrarPartida(uid, juego.id); null }
    }

    suspend fun guardarPartida(juego: Minijuego, partidaId: String, estado: Any) {
        val uid = uidActual()
        val ahora = System.currentTimeMillis()
        val iniciada = dao.partida(uid, juego.id)?.takeIf { it.partidaId == partidaId }?.iniciadaEn ?: ahora
        dao.guardarPartida(PartidaGuardadaEntity(uid, juego.id, partidaId, gson.toJson(estado), iniciada, ahora))
    }

    suspend fun descartarPartida(juego: Minijuego) = dao.borrarPartida(uidActual(), juego.id)

    fun juegosEnCurso(): Flow<Set<Minijuego>> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyFlow()
        return dao.juegosEnCurso(uid, limiteVigencia()).map { ids -> ids.mapNotNull(Minijuego::porId).toSet() }
    }

    // ── Fin de partida (CU-07 pasos 7 y 8) ─────────────────────────────────

    /**
     * RN-09: solo una partida terminada da XP. Se guarda en la outbox, se
     * actualiza la racha (RN-08) y se intenta enviar de inmediato; sin
     * conexión, WorkManager la envía después.
     */
    suspend fun terminarPartida(juego: Minijuego, partidaId: String, marcador: Marcador): ResultadoPartida {
        val uid = uidActual()
        val hoy = LocalDate.now()
        val previas = dao.contarDelDia(uid, juego.id, hoy.toString())
        val xpLocal = CalculadorXp.calcular(juego, marcador.aciertos, marcador.total, marcador.tiempoJugadoMs, previas)
        val resultado = ResultadoMinijuegoEntity(
            partidaId = partidaId,
            uid = uid,
            juego = juego.id,
            aciertos = marcador.aciertos,
            total = marcador.total,
            duracionMs = marcador.tiempoJugadoMs,
            xpLocal = xpLocal.xp,
            conceptosJson = gson.toJson(marcador.conceptos),
            dia = hoy.toString(),
            terminadaEn = System.currentTimeMillis()
        )
        val racha = db.withTransaction {
            dao.borrarPartida(uid, juego.id)
            dao.insertarResultado(resultado)
            registrarActividad(uid, hoy)
        }

        val confirmado = sincronizando.withLock { enviar(resultado) }
        if (confirmado == null) programarSincronizacion(context)
        return ResultadoPartida(
            juego = juego,
            aciertos = marcador.aciertos,
            total = marcador.total,
            duracionMs = marcador.tiempoJugadoMs,
            xp = confirmado?.xpGanado ?: xpLocal.xp,
            xpReducida = confirmado?.xpReducida ?: xpLocal.reducida,
            confirmada = confirmado != null,
            racha = racha,
            paraRepasar = marcador.conceptos.filterValues { it.errores > 0 }.keys
                .mapNotNull { bancoLocal.descripcionConcepto(it) }.distinct()
        )
    }

    /** RN-08 en el perfil local: sube una vez por día con actividad. */
    private suspend fun registrarActividad(uid: String, hoy: LocalDate): Int {
        val usuario = usuarioDao.buscarPorUid(uid) ?: return 0
        val racha = Racha.siguiente(usuario.racha, usuario.ultimoDiaActivo, hoy)
        usuarioDao.actualizarRacha(uid, racha, hoy.toString())
        return racha
    }

    /** Reenvía la outbox. Devuelve false si hay que reintentar más tarde. */
    suspend fun sincronizarPendientes(): Boolean = sincronizando.withLock {
        val uid = firebaseAuth.currentUser?.uid ?: return@withLock true
        dao.borrarHistorialAntes(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(DIAS_HISTORIAL))
        dao.pendientes(uid).all { enviar(it) != null }
    }

    /**
     * Envía un resultado y, si el servidor lo registró, acredita su XP (RF-14).
     * null = no se pudo por un problema temporal (sin red, 5xx, 429).
     */
    private suspend fun enviar(resultado: ResultadoMinijuegoEntity): ResultadoResponse? {
        val request = ResultadoRequest(
            partidaId = resultado.partidaId,
            juego = resultado.juego,
            aciertos = resultado.aciertos,
            total = resultado.total,
            duracionMs = resultado.duracionMs,
            conceptos = leerConceptos(resultado.conceptosJson).map { (id, c) -> ConceptoResultadoDto(id, c.aciertos, c.errores) }
        )
        return try {
            val respuesta = pedir { api.enviarResultado(it, request) }
            // Marcar y acreditar juntos: si la app muere a la mitad, el reenvío es idempotente.
            db.withTransaction {
                dao.marcarSincronizado(resultado.partidaId, respuesta.xpGanado)
                if (respuesta.xpGanado > 0) usuarioDao.sumarXp(resultado.uid, respuesta.xpGanado)
            }
            respuesta
        } catch (error: ErrorDelServidor) {
            // Un 4xx no se arregla reenviando: se descarta sin XP, igual que en Lecciones.
            if (error.codigo in 400..499 && error.codigo !in listOf(401, 408, 429)) {
                dao.marcarSincronizado(resultado.partidaId, 0)
                ResultadoResponse(xpGanado = 0, xpReducida = false, partidasHoy = 0, repetido = false)
            } else {
                null
            }
        } catch (error: Exception) {
            null
        }
    }

    // ── Eco Vocal ──────────────────────────────────────────────────────────

    /** Token de Azure AI Speech (§5.1). Falla con ErrorDelServidor 503 si el backend no tiene Azure. */
    suspend fun tokenVoz(): TokenVozDto = pedir { api.tokenVoz(it) }

    // ── Utilidades ─────────────────────────────────────────────────────────

    private fun leerConceptos(json: String): Map<String, ConteoConcepto> =
        runCatching { gson.fromJson<Map<String, ConteoConcepto>>(json, TIPO_CONCEPTOS) }.getOrNull().orEmpty()

    private fun limiteVigencia() = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(HORAS_VIGENCIA_PARTIDA)

    private fun uidActual(): String =
        firebaseAuth.currentUser?.uid ?: throw Exception("Inicia sesión para jugar.")

    private suspend fun <T> pedir(peticion: suspend (String) -> Response<T>): T {
        val usuario = firebaseAuth.currentUser ?: throw Exception("Inicia sesión para jugar.")
        val token = usuario.getIdToken(false).await().token
            ?: throw Exception("No se pudo obtener la sesión. Vuelve a iniciar sesión.")
        val respuesta = peticion("Bearer $token")
        if (respuesta.isSuccessful) return respuesta.body() ?: throw Exception("Respuesta vacía del servidor")
        val mensaje = respuesta.errorBody()?.string()?.let {
            runCatching { gson.fromJson(it, ErrorDto::class.java).error }.getOrNull()
        }
        throw ErrorDelServidor(respuesta.code(), mensaje ?: "Ocurrió un error (${respuesta.code()}).")
    }

    companion object {
        private const val TIEMPO_RED_MS = 3_000L
        private const val DIAS_HISTORIAL = 7L
        private const val HORAS_VIGENCIA_PARTIDA = 24L
        private val TIPO_CONCEPTOS = object : TypeToken<Map<String, ConteoConcepto>>() {}.type

        // Evita que la pantalla y el Worker envíen el mismo resultado a la vez.
        private val sincronizando = Mutex()

        fun programarSincronizacion(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SincronizarPartidasWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("sincronizar-partidas", ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
        }
    }
}
