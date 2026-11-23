package com.xpspeak.app.feature.progress.ui

import com.xpspeak.app.feature.progress.data.DominioDto
import com.xpspeak.app.feature.progress.data.FuenteProgreso
import com.xpspeak.app.feature.progress.data.InsigniaDto
import com.xpspeak.app.feature.progress.data.ProgresoDto
import com.xpspeak.app.feature.progress.data.ProgresoGuardado
import com.xpspeak.app.feature.progress.data.RachaDto
import com.xpspeak.app.feature.progress.data.XpDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private fun progreso(xp: Int = 136, sinActividad: Boolean = false) = ProgresoDto(
        nivel = "A1",
        dominio = DominioDto(1, 12, 8),
        xp = XpDto(xp, 2, 36, 200),
        racha = RachaDto(4, 6, "2026-10-11"),
        habilidades = emptyList(),
        insignias = listOf(InsigniaDto("primer-logro", "Primer Logro", "…", null, "estrella", obtenida = !sinActividad)),
        sinActividad = sinActividad
    )

    /** Fuente falsa: `refrescar` escribe en la "caché" igual que el repositorio real. */
    private class FakeFuente(
        var resultado: () -> Result<ProgresoDto>,
        var minimo: ProgresoDto? = null,
        inicial: ProgresoGuardado? = null
    ) : FuenteProgreso {
        val cache = MutableStateFlow(inicial)
        var refrescos = 0
        override fun observar() = cache
        override suspend fun refrescar(): Result<Unit> {
            refrescos++
            return resultado().map { cache.value = ProgresoGuardado(it, actualizadoEn = 1_000L) }
        }
        override suspend fun estadoMinimo() = minimo
    }

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `al entrar refresca y muestra los datos del servidor`() {
        val fuente = FakeFuente({ Result.success(progreso()) })
        val vm = ProgressViewModel(fuente)

        val estado = vm.uiState.value.estado as EstadoProgreso.Listo
        assertEquals(136, estado.datos.xp.total)
        assertFalse(estado.desactualizado)
        assertEquals(1_000L, estado.actualizadoEn)
        assertFalse(vm.uiState.value.refrescando)
        assertEquals(1, fuente.refrescos)
    }

    @Test
    fun `sin red muestra la cache con el aviso de datos viejos`() {
        val fuente = FakeFuente(
            { Result.failure(Exception("Sin conexión.")) },
            inicial = ProgresoGuardado(progreso(xp = 90), actualizadoEn = 500L)
        )
        val estado = ProgressViewModel(fuente).uiState.value.estado as EstadoProgreso.Listo

        assertEquals(90, estado.datos.xp.total)
        assertTrue(estado.desactualizado)
        assertEquals(500L, estado.actualizadoEn)
    }

    @Test
    fun `sin red ni cache arma el estado minimo con el perfil local`() {
        val fuente = FakeFuente({ Result.failure(Exception("Sin conexión.")) }, minimo = progreso(xp = 40))
        val estado = ProgressViewModel(fuente).uiState.value.estado as EstadoProgreso.Listo

        assertEquals(40, estado.datos.xp.total)
        assertTrue(estado.desactualizado)
        assertNull(estado.actualizadoEn)
    }

    @Test
    fun `usuario nuevo ve el estado sin actividad`() {
        val vm = ProgressViewModel(FakeFuente({ Result.success(progreso(xp = 0, sinActividad = true)) }))
        assertTrue(vm.uiState.value.estado is EstadoProgreso.SinActividad)
    }

    @Test
    fun `sin sesion ni datos muestra el error`() {
        val vm = ProgressViewModel(FakeFuente({ Result.failure(Exception("Inicia sesión para ver tu progreso.")) }))
        assertEquals(EstadoProgreso.Error("Inicia sesión para ver tu progreso."), vm.uiState.value.estado)
    }

    @Test
    fun `pull to refresh vuelve a pedir y se recupera al volver la red`() {
        val fuente = FakeFuente({ Result.failure(Exception("Sin conexión.")) }, minimo = progreso(xp = 40))
        val vm = ProgressViewModel(fuente)
        assertTrue((vm.uiState.value.estado as EstadoProgreso.Listo).desactualizado)

        fuente.resultado = { Result.success(progreso(xp = 150)) }
        vm.refrescar()

        val estado = vm.uiState.value.estado as EstadoProgreso.Listo
        assertEquals(150, estado.datos.xp.total)
        assertFalse(estado.desactualizado)
        assertEquals(2, fuente.refrescos)
    }

    @Test
    fun `tocar una insignia abre su detalle y se puede cerrar`() {
        val vm = ProgressViewModel(FakeFuente({ Result.success(progreso()) }))
        val insignia = (vm.uiState.value.estado as EstadoProgreso.Listo).datos.insignias.first()

        vm.seleccionarInsignia(insignia)
        assertEquals(insignia, vm.uiState.value.insigniaSeleccionada)
        vm.seleccionarInsignia(null)
        assertNull(vm.uiState.value.insigniaSeleccionada)
    }

    @Test
    fun `hace cuanto se actualizaron los datos`() {
        val ahora = 10 * 24 * 60 * 60_000L
        assertEquals("hace un momento", haceCuanto(ahora - 30_000, ahora))
        assertEquals("hace 5 min", haceCuanto(ahora - 5 * 60_000, ahora))
        assertEquals("hace 3 h", haceCuanto(ahora - 3 * 60 * 60_000, ahora))
        assertEquals("ayer", haceCuanto(ahora - 30 * 60 * 60_000, ahora))
        assertEquals("hace 4 días", haceCuanto(ahora - 4 * 24 * 60 * 60_000, ahora))
    }
}
