package com.xpspeak.app.feature.progress.data

import com.google.gson.Gson
import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import okhttp3.Headers.Companion.headersOf
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException

/** ProgresoRepository con fakes: caché, ETag y la regla de sincronización de la racha. */
class ProgresoRepositoryTest {

    private val gson = Gson()

    private val dto = ProgresoDto(
        nivel = "A1",
        dominio = DominioDto(1, 12, 8),
        xp = XpDto(136, 2, 36, 200),
        racha = RachaDto(4, 6, "2026-10-11"),
        habilidades = emptyList(),
        insignias = emptyList(),
        sinActividad = false
    )

    private class FakeSesion(override val uid: String? = "u") : SesionActual {
        override suspend fun autorizacion() = "Bearer token"
    }

    private class FakeUsuarioDao(var usuario: UsuarioEntity?) : UsuarioDao {
        override suspend fun guardar(usuario: UsuarioEntity) { this.usuario = usuario }
        override suspend fun buscarPorUid(uid: String) = usuario
        override suspend fun sumarXp(uid: String, xp: Int) { usuario = usuario?.copy(xp = usuario!!.xp + xp) }
        override suspend fun actualizarRacha(uid: String, racha: Int, dia: String) {
            usuario = usuario?.copy(racha = racha, ultimoDiaActivo = dia)
        }
        override suspend fun actualizarProgreso(uid: String, xp: Int, racha: Int, ultimoDia: String?) {
            usuario = usuario?.copy(xp = xp, racha = racha, ultimoDiaActivo = ultimoDia)
        }
        override suspend fun actualizarXp(uid: String, xp: Int) { usuario = usuario?.copy(xp = xp) }
        override fun observarUsuario(uid: String): Flow<UsuarioEntity?> = flowOf(usuario)
    }

    private class FakeProgresoDao(var pendientes: Int = 0) : ProgresoDao {
        val caches = MutableStateFlow<Map<String, ProgresoCacheEntity>>(emptyMap())
        override fun observar(uid: String) = caches.map { it[uid] }
        override suspend fun buscar(uid: String) = caches.value[uid]
        override suspend fun guardar(cache: ProgresoCacheEntity) { caches.value = caches.value + (cache.uid to cache) }
        override suspend fun marcarFresca(uid: String, ahora: Long) {
            caches.value[uid]?.let { guardar(it.copy(actualizadoEn = ahora)) }
        }
        override suspend fun contarPendientes(uid: String) = pendientes
    }

    private class FakeApi(var responder: () -> Response<ProgresoDto>) : ProgresoApi {
        val etagsRecibidos = mutableListOf<String?>()
        var chatFalla = false
        override suspend fun progreso(auth: String, etag: String?, nivel: String): Response<ProgresoDto> {
            etagsRecibidos += etag
            return responder()
        }
        override suspend fun registrarChat(auth: String, request: ChatProgresoRequest): Response<ChatProgresoResponse> {
            if (chatFalla) throw IOException("sin red")
            val insignia = InsigniaDto("habla-con-ia", "Habla con IA", "…", null, "chat")
            return Response.success(ChatProgresoResponse(listOf(insignia), null, repetido = false))
        }
    }

    private val usuarioLocal = UsuarioEntity(uid = "u", correo = "a@b.c", nivel = "A1", xp = 100, racha = 9, ultimoDiaActivo = "2026-10-11")

    private fun ok(cuerpo: ProgresoDto = dto, etag: String = "\"abc\"") = Response.success(cuerpo, headersOf("ETag", etag))

    private fun noModificado(): Response<ProgresoDto> = Response.error(
        "".toResponseBody(null),
        okhttp3.Response.Builder()
            .code(304)
            .message("Not Modified")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/api/progress").build())
            .build()
    )

    private fun repositorio(api: ProgresoApi, dao: ProgresoDao, usuarioDao: UsuarioDao, sesion: SesionActual = FakeSesion()) =
        ProgresoRepository(api, dao, usuarioDao, CatalogoInsignias { listOf(InsigniaDto("primer-logro", "Primer Logro", "…", null, "estrella")) }, sesion)

    @Test
    fun `sin pendientes guarda la cache y sincroniza XP y racha con el servidor`() = runTest {
        val dao = FakeProgresoDao(pendientes = 0)
        val usuarios = FakeUsuarioDao(usuarioLocal)
        val repo = repositorio(FakeApi { ok() }, dao, usuarios)

        assertTrue(repo.refrescar().isSuccess)

        val cache = dao.caches.value.getValue("u")
        assertEquals("\"abc\"", cache.etag)
        assertEquals(dto, gson.fromJson(cache.json, ProgresoDto::class.java))
        assertEquals(dto, repo.observar().first()!!.datos)
        assertEquals(136, usuarios.usuario!!.xp)
        assertEquals(4, usuarios.usuario!!.racha)
        assertEquals("2026-10-11", usuarios.usuario!!.ultimoDiaActivo)
    }

    @Test
    fun `con partidas o intentos sin enviar la racha local se conserva y la XP si se sincroniza`() = runTest {
        val usuarios = FakeUsuarioDao(usuarioLocal)
        val repo = repositorio(FakeApi { ok() }, FakeProgresoDao(pendientes = 1), usuarios)

        assertTrue(repo.refrescar().isSuccess)

        assertEquals(136, usuarios.usuario!!.xp)
        assertEquals(9, usuarios.usuario!!.racha)
    }

    @Test
    fun `revalida con If-None-Match y un 304 solo marca la cache como fresca`() = runTest {
        val dao = FakeProgresoDao()
        val api = FakeApi { ok() }
        val usuarios = FakeUsuarioDao(usuarioLocal)
        val repo = repositorio(api, dao, usuarios)
        repo.refrescar()
        val antes = dao.caches.value.getValue("u")

        api.responder = { noModificado() }
        dao.guardar(antes.copy(actualizadoEn = 0))
        assertTrue(repo.refrescar().isSuccess)

        assertEquals(listOf(null, "\"abc\""), api.etagsRecibidos)
        val despues = dao.caches.value.getValue("u")
        assertEquals(antes.json, despues.json)
        assertTrue(despues.actualizadoEn > 0)
        assertEquals(136, usuarios.usuario!!.xp)
    }

    @Test
    fun `si cambio el nivel del perfil no manda el ETag del otro nivel`() = runTest {
        val dao = FakeProgresoDao()
        dao.guardar(ProgresoCacheEntity("u", "A2", gson.toJson(dto), "\"viejo\"", 1))
        val api = FakeApi { ok() }

        repositorio(api, dao, FakeUsuarioDao(usuarioLocal)).refrescar()

        assertEquals(listOf<String?>(null), api.etagsRecibidos)
    }

    @Test
    fun `sin conexion falla con un mensaje claro y no toca el perfil`() = runTest {
        val usuarios = FakeUsuarioDao(usuarioLocal)
        val repo = repositorio(FakeApi { throw IOException("timeout") }, FakeProgresoDao(), usuarios)

        val resultado = repo.refrescar()

        assertEquals("Sin conexión.", resultado.exceptionOrNull()?.message)
        assertEquals(usuarioLocal, usuarios.usuario)
    }

    @Test
    fun `estado minimo con el perfil local y el catalogo empaquetado`() = runTest {
        val repo = repositorio(FakeApi { ok() }, FakeProgresoDao(), FakeUsuarioDao(usuarioLocal))
        val minimo = repo.estadoMinimo()!!

        assertEquals(100, minimo.xp.total)
        assertEquals(listOf("primer-logro"), minimo.insignias.map { it.id })
        assertNull(repositorio(FakeApi { ok() }, FakeProgresoDao(), FakeUsuarioDao(null), FakeSesion(uid = null)).estadoMinimo())
    }

    @Test
    fun `registrar el chat devuelve las insignias nuevas y si falla no rompe nada`() = runTest {
        val api = FakeApi { ok() }
        val repo = repositorio(api, FakeProgresoDao(), FakeUsuarioDao(usuarioLocal))
        assertEquals(listOf("habla-con-ia"), repo.registrarChat("sesion-1").map { it.id })

        api.chatFalla = true
        assertEquals(emptyList<InsigniaDto>(), repo.registrarChat("sesion-2"))
    }
}
