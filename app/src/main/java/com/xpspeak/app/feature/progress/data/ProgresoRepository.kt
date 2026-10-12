package com.xpspeak.app.feature.progress.data

import com.google.gson.Gson
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import com.xpspeak.app.feature.lessons.data.ErrorDelServidor
import com.xpspeak.app.feature.lessons.data.ErrorDto
import com.xpspeak.app.feature.lessons.data.LeccionesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/** Último progreso confirmado por el servidor y cuándo (para "datos de hace X"). */
data class ProgresoGuardado(val datos: ProgresoDto, val actualizadoEn: Long)

/** Lo que necesita la pestaña Progreso; es interfaz para probar el ViewModel con fakes. */
interface FuenteProgreso {
    /** La caché de Room del usuario actual (null si nunca se descargó). */
    fun observar(): Flow<ProgresoGuardado?>

    /** Descarga GET /api/progress, actualiza la caché y sincroniza el perfil local. */
    suspend fun refrescar(): Result<Unit>

    /** Sin red ni caché: perfil local + catálogo empaquetado. null si no hay sesión. */
    suspend fun estadoMinimo(): ProgresoDto?
}

/**
 * Módulo de Progreso (CU-08, docs/progreso-diseno.md Fase 3). El servidor es
 * la fuente de verdad de XP, racha e insignias; Room guarda la última
 * respuesta para mostrarla sin red (RNF-10) y revalidarla con ETag.
 */
class ProgresoRepository @Inject constructor(
    private val api: ProgresoApi,
    private val dao: ProgresoDao,
    private val usuarioDao: UsuarioDao,
    private val catalogo: CatalogoInsignias,
    private val sesion: SesionActual
) : FuenteProgreso {
    private val gson = Gson()

    override fun observar(): Flow<ProgresoGuardado?> {
        val uid = sesion.uid ?: return flowOf(null)
        return dao.observar(uid).map { cache ->
            cache?.let { leer(it)?.let { datos -> ProgresoGuardado(datos, it.actualizadoEn) } }
        }
    }

    override suspend fun refrescar(): Result<Unit> = runCatching {
        val uid = sesion.uid ?: throw Exception("Inicia sesión para ver tu progreso.")
        val usuario = usuarioDao.buscarPorUid(uid)
        val nivel = usuario?.nivel ?: "A1"
        val cache = dao.buscar(uid)?.takeIf { it.nivel == nivel }
        val respuesta = api.progreso(sesion.autorizacion(), cache?.etag, nivel)
        val ahora = System.currentTimeMillis()

        val datos = when {
            respuesta.code() == HTTP_NO_MODIFICADO && cache != null -> {
                dao.marcarFresca(uid, ahora)
                leer(cache) ?: throw Exception("No se pudo leer tu progreso guardado.")
            }
            respuesta.isSuccessful -> {
                val dto = respuesta.body() ?: throw Exception("Respuesta vacía del servidor")
                dao.guardar(ProgresoCacheEntity(uid, nivel, gson.toJson(dto), respuesta.headers()["ETag"], ahora))
                dto
            }
            else -> {
                val mensaje = respuesta.errorBody()?.string()?.let {
                    runCatching { gson.fromJson(it, ErrorDto::class.java).error }.getOrNull()
                }
                throw ErrorDelServidor(respuesta.code(), mensaje ?: "Ocurrió un error (${respuesta.code()}).")
            }
        }
        sincronizarPerfil(uid, usuario, datos)
    }.recoverCatching { error ->
        throw if (LeccionesRepository.esSinConexion(error)) Exception("Sin conexión.") else error
    }

    /**
     * Corrige la deriva del perfil local (Cuenta y los resultados lo leen). La
     * XP se sobrescribe siempre: la local solo suma XP que el servidor ya confirmó.
     */
    private suspend fun sincronizarPerfil(uid: String, usuario: UsuarioEntity?, datos: ProgresoDto) {
        if (usuario == null) return
        if (ProgresoReglas.debeSobrescribirRacha(dao.contarPendientes(uid))) {
            usuarioDao.actualizarProgreso(uid, datos.xp.total, datos.racha.actual, datos.racha.ultimoDia ?: usuario.ultimoDiaActivo)
        } else {
            usuarioDao.actualizarXp(uid, datos.xp.total)
        }
    }

    override suspend fun estadoMinimo(): ProgresoDto? {
        val uid = sesion.uid ?: return null
        val insignias = runCatching { catalogo.insignias() }.getOrDefault(emptyList())
        return ProgresoReglas.estadoMinimo(usuarioDao.buscarPorUid(uid), insignias, LocalDate.now())
    }

    /**
     * La primera respuesta de la IA en una conversación cuenta como actividad
     * (RN-08) y desbloquea "Habla con IA". Se intenta una vez; si falla, se
     * ignora para no estorbar al chat.
     * @return las insignias ganadas en vivo (para festejarlas)
     */
    suspend fun registrarChat(sesionId: String): List<InsigniaDto> = runCatching {
        val respuesta = api.registrarChat(sesion.autorizacion(), ChatProgresoRequest(sesionId))
        respuesta.body()?.takeIf { respuesta.isSuccessful && !it.repetido }?.insigniasNuevas.orEmpty()
    }.getOrDefault(emptyList())

    private fun leer(cache: ProgresoCacheEntity): ProgresoDto? =
        runCatching { gson.fromJson(cache.json, ProgresoDto::class.java) }.getOrNull()

    private companion object {
        const val HTTP_NO_MODIFICADO = 304
    }
}
