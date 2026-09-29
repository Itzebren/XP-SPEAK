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
class ErrorDelServidor(val codigo: Int, mensaje: String) : Exception(mensaje)

/**
 * RF-10/RF-12/RF-13: consume el backend de Lecciones. El servidor saca el uid
 * del ID token de Firebase, así que cada llamada manda "Bearer <token>".
 *
 * Offline-first (docs/lecciones-diseno.md §3 y §6.2): las lecciones y el último
 * catálogo se guardan en Room; sin conexión la evaluación se califica en el
 * teléfono y el intento se reenvía después con el mismo attempt_id, para que
 * el servidor no duplique el XP.
 */
class LeccionesRepository @Inject constructor(
    private val api: LeccionesApi,
    private val firebaseAuth: FirebaseAuth,
    private val usuarioDao: UsuarioDao,
    private val dao: LeccionesDao,
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
        sincronizarPendientes()

        llamar { api.catalogo(it, nivel) }.fold(
            onSuccess = { dto ->
                dao.guardarCatalogo(CatalogoCacheEntity(uid, nivel, gson.toJson(dto)))
                Catalogo(dto, sinConexion = false, intentosPendientes = dao.contarPendientes(uid))
            },
            onFailure = { error ->
                if (!esSinConexion(error)) throw error
                val cache = dao.catalogo(uid, nivel)
                    ?: throw Exception("Sin conexión. Conéctate una vez para descargar tus lecciones.")
                Catalogo(
                    gson.fromJson(cache.json, CatalogoDto::class.java),
                    sinConexion = true,
                    intentosPendientes = dao.contarPendientes(uid)
                )
            }
        )
    }

    /**
     * Descarga las lecciones del nivel que falten o cambiaron según el manifiesto,
     * para que después se puedan abrir sin conexión (RN-11).
     */
    suspend fun sincronizarContenido(): Result<Unit> = runCatching {
        val manifiesto = cuerpo(api.manifiesto())
        val vigentes = manifiesto.niveles.values.flatten().map { it.id }
        if (vigentes.isNotEmpty()) dao.borrarLeccionesExcepto(vigentes)

        val locales = dao.versiones().associate { it.id to it.version }
        manifiesto.niveles[nivelDelUsuario()].orEmpty()
            .filter { locales[it.id] != it.version }
            .forEach { entrada ->
                val dto = pedir { api.leccion(it, entrada.id) }
                dao.guardarLeccion(LeccionCacheEntity(entrada.id, entrada.version, gson.toJson(dto)))
            }
    }

    /** Usa la copia local si existe; la sincronización del manifiesto la mantiene al día. */
    suspend fun leccion(id: String): Result<LeccionDto> = runCatching {
        dao.leccion(id)?.let { return@runCatching gson.fromJson(it.json, LeccionDto::class.java) }
        val dto = pedir { api.leccion(it, id) }
        dao.guardarLeccion(LeccionCacheEntity(id, version = 0, json = gson.toJson(dto)))
        dto
    }

    suspend fun guardarAvance(id: String, seccion: Int): Result<AvanceResponse> =
        llamar { api.guardarAvance(it, id, AvanceRequest(seccion)) }

    suspend fun conceptosDebiles(): Result<ConceptosDebilesDto> =
        llamar { api.conceptosDebiles(it, nivelDelUsuario()) }

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
            respuestas = respuestas.map { (itemId, respuesta) -> respuesta.aDto(itemId) }
        )
        llamar { api.enviarIntento(it, leccionId, request) }.fold(
            onSuccess = { resultado ->
                acreditarXp(resultado)
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
                EnvioIntento.Pendiente(CalificadorLocal.calificar(evaluacion, respuestas))
            }
        )
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
