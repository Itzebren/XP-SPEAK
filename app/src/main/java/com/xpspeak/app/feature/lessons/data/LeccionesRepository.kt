package com.xpspeak.app.feature.lessons.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.xpspeak.app.core.actividad.RegistroActividad
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/** Catálogo del nivel; `sinConexion` indica que viene de la caché (último visto). */
data class Catalogo(val dto: CatalogoDto, val sinConexion: Boolean, val intentosPendientes: Int)

sealed interface EnvioIntento {
    val resultado: IntentoResponse

    /** El servidor calificó: XP y desbloqueo son oficiales. */
    data class Calificado(override val resultado: IntentoResponse) : EnvioIntento

    /** Sin conexión: se calificó en el teléfono y el intento quedó en cola (RN-12). */
    data class Pendiente(override val resultado: IntentoResponse) : EnvioIntento
}

/** El backend respondió con error HTTP (a diferencia de no poder conectarse). */
class ErrorDelServidor(val codigo: Int, mensaje: String) : Exception(mensaje) {
    /** RN-07: el servidor pide repasar antes de empezar un tema nuevo. */
    val esRepasoPendiente get() = codigo == 409
}

/** Lección a medias guardada en el teléfono (RN-12, CU-06 A2). */
data class LeccionEnCurso(val attemptId: String, val respuestas: Map<String, RespuestaUsuario>, val posicion: Int)

/**
 * RF-10/RF-12/RF-13: consume el backend de Lecciones. El servidor saca el uid
 * del ID token de Firebase, así que cada llamada manda "Bearer <token>".
 *
 * Offline-first (backend/docs/lecciones-diseno.md §3 y §6.2): las lecciones vienen en el APK
 * (LeccionesLocales) y las que cambien después se bajan a Room; el último catálogo también se
 * guarda en Room. Sin conexión la evaluación se califica en el teléfono y el intento se
 * reenvía después con el mismo attempt_id, para que el servidor no duplique el XP.
 */
class LeccionesRepository @Inject constructor(
    private val api: LeccionesApi,
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao,
    private val dao: LeccionesDao,
    private val registroActividad: RegistroActividad,
    private val locales: LeccionesLocales,
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    /** RN-02: el catálogo depende del nivel elegido al registrarse (guardado en Room). */
    suspend fun nivelDelUsuario(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return "A1"
        return usuarioDao.buscarPorUid(uid)?.nivel ?: "A1"
    }

    suspend fun catalogo(): Result<Catalogo> = runCatching {
        val uid = uidActual()
        val nivel = nivelDelUsuario()
        // Primero los intentos en cola, para que el catálogo ya refleje sus desbloqueos.
        runCatching { sincronizarPendientes() }

        llamar { api.catalogo(it, nivel) }.fold(
            onSuccess = { dto ->
                dao.guardarCatalogo(CatalogoCacheEntity(uid, nivel, gson.toJson(dto)))
                Catalogo(dto, sinConexion = false, intentosPendientes = dao.contarPendientes(uid))
            },
            onFailure = { error ->
                if (!esSinConexion(error)) throw error
                Catalogo(catalogoGuardado(uid, nivel), sinConexion = true, intentosPendientes = dao.contarPendientes(uid))
            }
        )
    }

    /** El último catálogo del servidor o, si nunca hubo conexión, el que se arma con el APK (RN-11). */
    private suspend fun catalogoGuardado(uid: String, nivel: String): CatalogoDto =
        dao.catalogo(uid, nivel)?.let { gson.fromJson(it.json, CatalogoDto::class.java) }
            ?: CatalogoLocal.inicial(nivel, locales.entradas(nivel))

    /**
     * Descarga las lecciones del nivel que cambiaron en el servidor después de instalar la app
     * (las demás ya vienen en el APK), para abrirlas sin conexión (RN-11).
     */
    suspend fun sincronizarContenido(): Result<Unit> = runCatching {
        val manifiesto = cuerpo(api.manifiesto())
        val vigentes = manifiesto.niveles.values.flatten().map { it.id }
        if (vigentes.isNotEmpty()) dao.borrarLeccionesExcepto(vigentes)

        val descargadas = dao.versiones().associate { it.id to it.version }
        manifiesto.niveles[nivelDelUsuario()].orEmpty()
            .filter { (descargadas[it.id] ?: locales.version(it.id)) != it.version }
            .forEach { entrada ->
                val dto = pedir { api.leccion(it, entrada.id) }
                dao.guardarLeccion(LeccionCacheEntity(entrada.id, entrada.version, gson.toJson(dto)))
            }
    }

    /**
     * La copia más nueva entre la descargada (Room) y la del APK; el servidor solo si la
     * lección no existe en ninguna de las dos (una lección agregada después de instalar).
     */
    suspend fun leccion(id: String): Result<LeccionDto> = runCatching {
        val descargada = dao.leccion(id)
        val versionApk = locales.version(id)
        val json = when {
            descargada != null && (versionApk == null || descargada.version >= versionApk) -> descargada.json
            else -> locales.json(id)
        }
        json?.let { return@runCatching gson.fromJson(it, LeccionDto::class.java) }
        val dto = pedir { api.leccion(it, id) }
        dao.guardarLeccion(LeccionCacheEntity(id, version = 0, json = gson.toJson(dto)))
        dto
    }

    suspend fun guardarAvance(id: String, seccion: Int): Result<AvanceResponse> =
        llamar { api.guardarAvance(it, id, AvanceRequest(seccion)) }

    /** La lección a medias de este usuario, si la dejó hace menos de DIAS_EN_CURSO días. */
    suspend fun leccionEnCurso(id: String): LeccionEnCurso? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        val guardada = dao.leccionEnCurso(uid, id) ?: return null
        if (System.currentTimeMillis() - guardada.actualizadaEn > TimeUnit.DAYS.toMillis(DIAS_EN_CURSO)) {
            dao.borrarLeccionEnCurso(uid, id)
            return null
        }
        return LeccionEnCurso(guardada.attemptId, RespuestasCodec.decodificar(guardada.respuestasJson), guardada.posicion)
    }

    suspend fun guardarEnCurso(id: String, enCurso: LeccionEnCurso) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        dao.guardarLeccionEnCurso(
            LeccionEnCursoEntity(uid, id, enCurso.attemptId, RespuestasCodec.codificar(enCurso.respuestas), enCurso.posicion, System.currentTimeMillis())
        )
    }

    suspend fun descartarEnCurso(id: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        dao.borrarLeccionEnCurso(uid, id)
    }

    suspend fun conceptosDebiles(): Result<ConceptosDebilesDto> =
        llamar { api.conceptosDebiles(it, nivelDelUsuario()) }

    /** RF-13: ejercicios de los conceptos que SM-2 marca para hoy. Requiere conexión. */
    suspend fun sesionRepaso(): Result<SesionRepasoDto> =
        llamar { api.sesionRepaso(it, nivelDelUsuario()) }.recoverCatching { error ->
            throw if (esSinConexion(error)) Exception("Necesitas conexión para repasar.") else error
        }

    /** Igual que en las lecciones, el attempt_id evita aplicar dos veces el mismo repaso. */
    suspend fun enviarRepaso(attemptId: String, respuestas: Map<String, RespuestaUsuario>): Result<RepasoResponse> =
        llamar {
            api.enviarRepaso(it, RepasoRequest(attemptId, respuestas.map { (id, respuesta) -> respuesta.aDto(id) }))
        }.onSuccess {
            // El repaso cuenta como actividad para la racha (RN-08), igual que en el servidor.
            registroActividad.registrar(uidActual())
        }.recoverCatching { error ->
            throw if (esSinConexion(error)) Exception("Sin conexión. Intenta enviar de nuevo cuando tengas red.") else error
        }

    /**
     * `attemptId` lo genera la pantalla una vez por intento: si el usuario
     * reintenta el envío tras un error, el servidor lo reconoce y no duplica XP (§6.2).
     */
    suspend fun enviarIntento(
        leccionId: String,
        attemptId: String,
        respuestas: Map<String, RespuestaUsuario>
    ): Result<EnvioIntento> = runCatching {
        val request = IntentoRequest(
            attemptId = attemptId,
            respuestas = respuestas.map { (itemId, respuesta) -> respuesta.aDto(itemId) },
            completadoEn = System.currentTimeMillis()
        )
        llamar { api.enviarIntento(it, leccionId, request) }.fold(
            onSuccess = { resultado ->
                acreditarXp(resultado)
                if (resultado.aprobada) registroActividad.registrar(uidActual())
                EnvioIntento.Calificado(resultado)
            },
            onFailure = { error ->
                if (!esSinConexion(error)) throw error
                val evaluacion = leccion(leccionId).getOrThrow().secciones
                    .first { it.tipo == TipoSeccion.EVALUACION }
                dao.encolarIntento(
                    IntentoPendienteEntity(attemptId, uidActual(), leccionId, gson.toJson(request), System.currentTimeMillis())
                )
                programarSincronizacion(context)
                val calificado = CalificadorLocal.calificar(evaluacion, respuestas)
                // La lección se aprobó hoy aunque el servidor se entere después: la racha sube ya.
                if (calificado.aprobada) registroActividad.registrar(uidActual())
                reflejarEnCatalogo(leccionId, calificado)
                EnvioIntento.Pendiente(calificado)
            }
        )
    }

    /** Sin conexión, el catálogo guardado ya muestra la lección aprobada y la siguiente desbloqueada. */
    private suspend fun reflejarEnCatalogo(leccionId: String, resultado: IntentoResponse) {
        val uid = uidActual()
        val nivel = nivelDelUsuario()
        val entradas = locales.entradas(nivel)
        if (entradas.none { it.id == leccionId }) return
        val catalogo = CatalogoLocal.conIntento(catalogoGuardado(uid, nivel), entradas, leccionId, resultado.aprobada, resultado.puntaje)
        dao.guardarCatalogo(CatalogoCacheEntity(uid, nivel, gson.toJson(catalogo)))
    }

    /**
     * Reenvía los intentos en cola del usuario actual. Devuelve false si alguno
     * no se pudo enviar por un problema temporal y hay que reintentar más tarde.
     */
    suspend fun sincronizarPendientes(): Boolean = sincronizando.withLock {
        val uid = firebaseAuth.currentUser?.uid ?: return@withLock true
        for (pendiente in dao.intentosPendientes(uid)) {
            val request = gson.fromJson(pendiente.json, IntentoRequest::class.java)
            try {
                val resultado = pedir { api.enviarIntento(it, pendiente.leccionId, request) }
                // Primero se saca de la cola: si algo falla después, se pierde el XP local
                // (el servidor ya lo tiene) en vez de sumarlo dos veces.
                dao.borrarIntento(pendiente.attemptId)
                acreditarXp(resultado)
            } catch (error: ErrorDelServidor) {
                // Un 4xx no se arregla reenviando (p. ej. lección bloqueada): se descarta.
                if (error.codigo in 400..499 && error.codigo !in listOf(401, 408, 429)) {
                    dao.borrarIntento(pendiente.attemptId)
                } else {
                    return@withLock false
                }
            } catch (error: Exception) {
                return@withLock false
            }
        }
        true
    }

    /** RF-14: el perfil local suma el XP que otorgó el servidor (Cuenta lo muestra). */
    private suspend fun acreditarXp(resultado: IntentoResponse) {
        if (resultado.xpGanado > 0) usuarioDao.sumarXp(uidActual(), resultado.xpGanado)
    }

    private fun uidActual(): String =
        firebaseAuth.currentUser?.uid ?: throw Exception("Inicia sesión para ver tus lecciones.")

    private suspend fun <T> llamar(peticion: suspend (String) -> Response<T>): Result<T> =
        runCatching { pedir(peticion) }

    private suspend fun <T> pedir(peticion: suspend (String) -> Response<T>): T {
        val usuario = firebaseAuth.currentUser ?: throw Exception("Inicia sesión para ver tus lecciones.")
        val token = usuario.getIdToken(false).await().token
            ?: throw Exception("No se pudo obtener la sesión. Vuelve a iniciar sesión.")
        return cuerpo(peticion("Bearer $token"))
    }

    private fun <T> cuerpo(respuesta: Response<T>): T {
        if (respuesta.isSuccessful) return respuesta.body() ?: throw Exception("Respuesta vacía del servidor")
        val mensajeError = respuesta.errorBody()?.string()?.let {
            runCatching { gson.fromJson(it, ErrorDto::class.java).error }.getOrNull()
        }
        throw ErrorDelServidor(
            respuesta.code(),
            mensajeError ?: "Ocurrió un error (${respuesta.code()}). Intenta de nuevo."
        )
    }

    companion object {
        // Evita que la pantalla y el Worker reenvíen el mismo intento a la vez.
        private val sincronizando = Mutex()

        /** Una lección a medias se ofrece para retomar durante una semana. */
        private const val DIAS_EN_CURSO = 7L

        /** Sin red: OkHttp no conecta, o Firebase no puede renovar el token. */
        fun esSinConexion(error: Throwable) = error is IOException || error is FirebaseNetworkException

        /** Envía la cola en cuanto haya red, aunque la app esté cerrada (RN-12). */
        fun programarSincronizacion(context: Context) {
            val trabajo = OneTimeWorkRequestBuilder<SincronizarIntentosWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork("sincronizar-intentos", ExistingWorkPolicy.APPEND_OR_REPLACE, trabajo)
        }
    }
}
