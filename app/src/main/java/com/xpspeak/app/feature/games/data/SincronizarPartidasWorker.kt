package com.xpspeak.app.feature.games.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * CU-07 paso 8: sincroniza en segundo plano los resultados que se terminaron
 * sin conexión, igual que SincronizarIntentosWorker en Lecciones.
 */
class SincronizarPartidasWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun minijuegosRepository(): MinijuegosRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, Dependencias::class.java)
            .minijuegosRepository()
        return if (repository.sincronizarPendientes()) Result.success() else Result.retry()
    }
}
