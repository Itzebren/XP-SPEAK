package com.xpspeak.app.core.di

import com.xpspeak.app.core.sesion.SesionActual
import com.xpspeak.app.core.sesion.SesionFirebase
import com.xpspeak.app.feature.progress.data.CatalogoInsignias
import com.xpspeak.app.feature.progress.data.CatalogoInsigniasLocal
import com.xpspeak.app.feature.progress.data.FuenteProgreso
import com.xpspeak.app.feature.progress.data.ProgresoRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Interfaces del módulo de Progreso (docs/progreso-diseno.md) y su implementación real. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProgresoModule {

    @Binds
    abstract fun bindSesionActual(impl: SesionFirebase): SesionActual

    @Binds
    abstract fun bindCatalogoInsignias(impl: CatalogoInsigniasLocal): CatalogoInsignias

    @Binds
    abstract fun bindFuenteProgreso(impl: ProgresoRepository): FuenteProgreso
}
