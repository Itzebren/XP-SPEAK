package com.xpspeak.app.feature.lessons.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** assets/lecciones/contenido.json, generado con `npm run exportar:lecciones`. */
data class ContenidoEmpaquetado(
    @SerializedName("version_contenido") val versionContenido: String,
    val niveles: Map<String, List<EntradaEmpaquetada>>,
    val lecciones: List<LeccionDto>
)

data class EntradaEmpaquetada(
    val id: String,
    val version: Int,
    val orden: Int,
    @SerializedName("prerequisito_id") val prerequisitoId: String?,
    val titulo: String,
    @SerializedName("xp_recompensa") val xpRecompensa: Int
)

/**
 * RNF-08 / RN-11: las lecciones vienen en el APK, así se abren sin conexión
 * desde la instalación. El servidor sigue siendo el que manda: si tiene una
 * versión más nueva de una lección, el repositorio la descarga a Room.
 */
@Singleton
class LeccionesLocales @Inject constructor(@ApplicationContext private val context: Context) {
    private val gson = Gson()
    private val leyendo = Mutex()
    private var contenido: ContenidoEmpaquetado? = null

    suspend fun contenido(): ContenidoEmpaquetado = leyendo.withLock {
        contenido ?: withContext(Dispatchers.IO) {
            context.assets.open(RUTA).reader().use { gson.fromJson(it, ContenidoEmpaquetado::class.java) }
        }.also { contenido = it }
    }

    suspend fun leccion(id: String): LeccionDto? = contenido().lecciones.firstOrNull { it.id == id }

    /** Versión de cada lección empaquetada, para no descargar lo que ya trae el APK. */
    suspend fun versiones(): Map<String, Int> =
        contenido().niveles.values.flatten().associate { it.id to it.version }

    /** Catálogo sin conexión ni caché: sin progreso conocido, igual que un usuario nuevo. */
    suspend fun catalogo(nivel: String): CatalogoDto = catalogoInicial(contenido(), nivel)

    companion object {
        private const val RUTA = "lecciones/contenido.json"

        /** Como el servidor sin progreso: disponible la lección sin prerrequisito, las demás bloqueadas (RN-06). */
        fun catalogoInicial(contenido: ContenidoEmpaquetado, nivel: String): CatalogoDto = CatalogoDto(
            nivel = nivel,
            lecciones = contenido.niveles[nivel].orEmpty().map { entrada ->
                LeccionResumenDto(
                    id = entrada.id,
                    orden = entrada.orden,
                    titulo = entrada.titulo,
                    estado = if (entrada.prerequisitoId == null) EstadoLeccion.DISPONIBLE else EstadoLeccion.BLOQUEADA,
                    intentos = 0,
                    xpRecompensa = entrada.xpRecompensa,
                    mejorPuntaje = null
                )
            }
        )
    }
}
