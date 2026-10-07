package com.xpspeak.app.feature.games.data

import android.content.Context
import com.google.gson.Gson
import com.xpspeak.app.feature.games.domain.BancoMinijuegos
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CU-07 paso 4: el contenido de los juegos se carga del APK (RNF-08 / RN-11),
 * no de la red. Se lee una vez y se queda en memoria.
 */
@Singleton
class BancoLocal @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson = Gson()
    private val leyendo = Mutex()
    private var banco: BancoMinijuegos? = null

    suspend fun banco(): BancoMinijuegos = leyendo.withLock {
        banco ?: withContext(Dispatchers.IO) {
            context.assets.open(RUTA).reader().use { gson.fromJson(it, BancoMinijuegos::class.java) }
        }.also { banco = it }
    }

    /** Texto para mostrar un concepto ("kitchen = cocina" o el tema de gramática). */
    suspend fun descripcionConcepto(conceptoId: String): String? {
        val lecciones = banco().lecciones
        lecciones.firstNotNullOfOrNull { l -> l.vocabulario.firstOrNull { it.conceptoId == conceptoId } }
            ?.let { return "${it.en} = ${it.es}" }
        return lecciones.firstNotNullOfOrNull { l -> l.oraciones.firstOrNull { it.conceptoId == conceptoId } }?.tema
    }

    private companion object {
        const val RUTA = "minijuegos/banco.json"
    }
}
