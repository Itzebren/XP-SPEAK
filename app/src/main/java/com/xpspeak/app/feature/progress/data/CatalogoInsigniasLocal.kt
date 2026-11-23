package com.xpspeak.app.feature.progress.data

import android.content.Context
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Reader
import javax.inject.Inject
import javax.inject.Singleton

/** Las 9 insignias del catálogo, aunque nunca se haya abierto Progreso con red. */
fun interface CatalogoInsignias {
    suspend fun insignias(): List<InsigniaDto>
}

/**
 * Catálogo de insignias empaquetado en el APK (docs/progreso-diseno.md
 * Decisión 5, RN-11 / RNF-08), igual que BancoLocal de minijuegos. Se lee
 * una vez y se queda en memoria; quién obtiene cada una lo decide el servidor.
 */
@Singleton
class CatalogoInsigniasLocal @Inject constructor(@ApplicationContext private val context: Context) : CatalogoInsignias {
    private val leyendo = Mutex()
    private var insignias: List<InsigniaDto>? = null

    override suspend fun insignias(): List<InsigniaDto> = leyendo.withLock {
        insignias ?: withContext(Dispatchers.IO) {
            context.assets.open(RUTA).reader().use(::leer)
        }.also { insignias = it }
    }

    companion object {
        const val RUTA = "progreso/insignias.json"

        fun leer(reader: Reader): List<InsigniaDto> =
            Gson().fromJson(reader, CatalogoInsigniasDto::class.java).insignias
    }
}
