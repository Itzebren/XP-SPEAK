package com.xpspeak.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class ArranqueTest {

    @Test
    fun `sin sesion abre el acceso`() {
        assertEquals(Routes.AUTH, ArranqueViewModel.destinoInicial(haySesion = false, tienePerfil = false))
    }

    @Test
    fun `con sesion y perfil entra directo al inicio, tambien sin conexion`() {
        assertEquals(Routes.INICIO, ArranqueViewModel.destinoInicial(haySesion = true, tienePerfil = true))
    }

    @Test
    fun `con sesion pero sin nivel elegido pide la nivelacion`() {
        assertEquals(Routes.SELECCIONAR_NIVEL, ArranqueViewModel.destinoInicial(haySesion = true, tienePerfil = false))
    }
}
