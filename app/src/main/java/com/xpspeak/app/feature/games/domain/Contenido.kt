package com.xpspeak.app.feature.games.domain

import com.google.gson.annotations.SerializedName

/*
 * Banco de contenido de los minijuegos (docs/minijuegos-diseno.md §3): viene
 * dentro del APK en assets/minijuegos/banco.json (RNF-08 / RN-11) y lo genera
 * backend/scripts/exportar-banco-minijuegos.js a partir de las lecciones.
 */

data class BancoMinijuegos(
    val version: Int,
    @SerializedName("version_contenido") val versionContenido: String,
    val lecciones: List<LeccionJuego>,
    val misiones: List<Mision>
)

data class LeccionJuego(
    val id: String,
    val nivel: String,
    val orden: Int,
    val titulo: String,
    val vocabulario: List<Palabra>,
    val oraciones: List<Oracion>,
    val dialogo: List<LineaDialogo>
)

data class Palabra(@SerializedName("concepto_id") val conceptoId: String, val en: String, val es: String)

/** Ejemplo de gramática; no trae traducción, así que la pista es el tema. */
data class Oracion(@SerializedName("concepto_id") val conceptoId: String, val tema: String, val en: String)

data class LineaDialogo(val hablante: String, val en: String, val es: String)

data class Mision(
    val id: String,
    val nivel: String,
    @SerializedName("leccion_id") val leccionId: String,
    val titulo: String,
    val escenario: String,
    val personaje: String,
    val objetivos: List<ObjetivoMision>,
    val pasos: List<PasoMision>
)

data class ObjetivoMision(val id: String, val texto: String)

data class PasoMision(
    val npc: String,
    @SerializedName("npc_es") val npcEs: String,
    val opciones: List<OpcionMision>
)

data class OpcionMision(
    val en: String,
    val correcta: Boolean,
    val cumple: String? = null,
    @SerializedName("concepto_id") val conceptoId: String? = null,
    val feedback: String? = null
)

/**
 * Lo que el usuario puede jugar ahora: solo lecciones no bloqueadas de su
 * nivel (CU-07 A2), más todo A1 como repaso si es A2.
 *
 * @param prioritarios conceptos débiles según el SRS (RF-13): salen más seguido.
 */
data class ContenidoJuegos(
    val nivel: String,
    val lecciones: List<LeccionJuego>,
    val misiones: List<Mision>,
    val prioritarios: Set<String> = emptySet()
)
