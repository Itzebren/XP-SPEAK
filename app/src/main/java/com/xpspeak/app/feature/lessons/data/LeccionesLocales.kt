package com.xpspeak.app.feature.lessons.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Lecciones empaquetadas en el APK (RNF-08 / RN-11, CU-06 paso 2): `assets/lecciones/lecciones.json`,
 * generado con `npm run exportar:lecciones` desde backend/content/lessons. Con esto las 24 lecciones
 * se abren y se califican sin haberse conectado nunca; el manifiesto del servidor solo sirve
 * para bajar las que cambien después de instalar la app.
 */
@Singleton
class LeccionesLocales @Inject constructor(@ApplicationContext private val context: Context) {

    private val paquete: PaqueteLecciones by lazy {
        context.assets.open(RUTA).bufferedReader().use { Gson().fromJson(it, PaqueteLecciones::class.java) }
    }

    private val porId: Map<String, JsonObject> by lazy {
        paquete.lecciones.associateBy { it.get("id").asString }
    }

    /** Lecciones del nivel en orden, con lo necesario para armar el catálogo sin servidor. */
    fun entradas(nivel: String): List<EntradaPaqueteDto> = paquete.niveles[nivel].orEmpty()

    fun version(id: String): Int? = paquete.niveles.values.flatten().firstOrNull { it.id == id }?.version

    /** El JSON de la lección tal como lo manda GET /api/lessons/:id. */
    fun json(id: String): String? = porId[id]?.toString()

    private companion object {
        const val RUTA = "lecciones/lecciones.json"
    }
}

data class PaqueteLecciones(
    @SerializedName("version_contenido") val versionContenido: String,
    val niveles: Map<String, List<EntradaPaqueteDto>>,
    val lecciones: List<JsonObject>
)

/** Una entrada del manifiesto (backend/lib/lecciones/contenido.js → construirManifiesto). */
data class EntradaPaqueteDto(
    val id: String,
    val version: Int,
    val orden: Int,
    @SerializedName("prerequisito_id") val prerequisitoId: String?,
    val titulo: String,
    @SerializedName("xp_recompensa") val xpRecompensa: Int
)
