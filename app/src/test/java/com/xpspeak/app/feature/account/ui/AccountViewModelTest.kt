package com.xpspeak.app.feature.account.ui

import com.xpspeak.app.feature.auth.data.AuthRepository
import com.xpspeak.app.feature.auth.data.UsuarioDao
import com.xpspeak.app.feature.auth.data.UsuarioEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountViewModelTest {

    private class FakeUsuarioDao : UsuarioDao {
        override suspend fun guardar(usuario: UsuarioEntity) {}
        override suspend fun buscarPorUid(uid: String): UsuarioEntity? = null
        override fun observarUsuario(uid: String): Flow<UsuarioEntity?> = flowOf(null)
    }

    private class FakeAuthRepository : AuthRepository() {
        var sesionCerrada = false
        override val usuarioActual get() = null
        override fun cerrarSesion() {
            sesionCerrada = true
        }
    }

    @Test
    fun `cerrarSesion invoca el cierre de sesion en el repositorio`() {
        val fakeRepo = FakeAuthRepository()
        val fakeDao = FakeUsuarioDao()
        val viewModel = AccountViewModel(fakeRepo, fakeDao)

        viewModel.cerrarSesion()

        assertTrue(fakeRepo.sesionCerrada)
    }

    @Test
    fun `estado inicial de uiState no es nulo`() {
        val fakeRepo = FakeAuthRepository()
        val fakeDao = FakeUsuarioDao()
        val viewModel = AccountViewModel(fakeRepo, fakeDao)

        assertEquals("", viewModel.uiState.value.correo)
    }
}
