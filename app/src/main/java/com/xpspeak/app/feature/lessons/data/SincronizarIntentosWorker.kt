package com.xpspeak.app.feature.lessons.data

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * RN-12: envía los intentos que se calificaron sin conexión. WorkManager lo
 * corre cuando hay red y lo reintenta con espera creciente si el servidor
 * todavía no responde.
 */
class SincronizarIntentosWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // WorkManager crea el Worker por su cuenta, así que el repositorio se pide a Hilt.
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencias {
        fun leccionesRepository(): LeccionesRepository
    }

    override suspend fun doWork(): Result {
        val repository = EntryPointAccessors
            .fromApplication(applicationContext, Dependencias::class.java)
            .leccionesRepository()
        return if (repository.sincronizarPendientes()) Result.success() else Result.retry()
    }
}
