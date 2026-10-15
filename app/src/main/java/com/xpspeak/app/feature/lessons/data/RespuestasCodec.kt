package com.xpspeak.app.feature.lessons.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario

/**
 * Guarda en texto las respuestas de una lección a medias (LeccionEnCursoEntity). Cada una
 * lleva su tipo para volver a la clase correcta de RespuestaUsuario al retomar.
 */
object RespuestasCodec {

    fun codificar(respuestas: Map<String, RespuestaUsuario>): String = JsonArray().apply {
        respuestas.forEach { (id, respuesta) ->
            add(JsonObject().apply {
                addProperty("id", id)
                when (respuesta) {
                    is RespuestaUsuario.Opcion -> { addProperty("tipo", "opcion"); addProperty("indice", respuesta.indice) }
                    is RespuestaUsuario.Texto -> { addProperty("tipo", "texto"); addProperty("texto", respuesta.texto) }
                    is RespuestaUsuario.Pares -> {
                        addProperty("tipo", "pares")
                        add("pares", JsonObject().apply { respuesta.elegidos.forEach { (izq, der) -> addProperty(izq, der) } })
                    }
                }
            })
        }
    }.toString()

    /** Lo que no se pueda leer (p. ej. guardado por otra versión de la app) se descarta. */
    fun decodificar(json: String): Map<String, RespuestaUsuario> = runCatching {
        JsonParser.parseString(json).asJsonArray.mapNotNull { elemento ->
            val obj = elemento.asJsonObject
            val respuesta = when (obj.get("tipo")?.asString) {
                "opcion" -> RespuestaUsuario.Opcion(obj.get("indice").asInt)
                "texto" -> RespuestaUsuario.Texto(obj.get("texto").asString)
                "pares" -> RespuestaUsuario.Pares(obj.getAsJsonObject("pares").entrySet().associate { it.key to it.value.asString })
                else -> return@mapNotNull null
            }
            obj.get("id").asString to respuesta
        }.toMap()
    }.getOrDefault(emptyMap())
}
