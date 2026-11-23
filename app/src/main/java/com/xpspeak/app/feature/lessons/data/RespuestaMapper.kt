package com.xpspeak.app.feature.lessons.data

import com.xpspeak.app.feature.lessons.domain.RespuestaUsuario

/** Formato de `valor` que espera POST /api/lessons/:id/attempt para cada tipo de ejercicio. */
internal fun RespuestaUsuario.aDto(itemId: String): RespuestaDto = RespuestaDto(
    id = itemId,
    valor = when (this) {
        is RespuestaUsuario.Opcion -> indice
        is RespuestaUsuario.Texto -> texto
        is RespuestaUsuario.Pares -> elegidos.map { (izq, der) -> listOf(izq, der) }
    }
)

/** Forma plana de una respuesta para guardarla en el borrador local (Gson no maneja la interfaz sellada). */
internal data class BorradorRespuesta(
    val id: String,
    val indice: Int? = null,
    val texto: String? = null,
    val pares: Map<String, String>? = null
)

internal fun RespuestaUsuario.aBorrador(itemId: String): BorradorRespuesta = when (this) {
    is RespuestaUsuario.Opcion -> BorradorRespuesta(itemId, indice = indice)
    is RespuestaUsuario.Texto -> BorradorRespuesta(itemId, texto = texto)
    is RespuestaUsuario.Pares -> BorradorRespuesta(itemId, pares = elegidos)
}

internal fun BorradorRespuesta.aRespuesta(): RespuestaUsuario? = when {
    indice != null -> RespuestaUsuario.Opcion(indice)
    texto != null -> RespuestaUsuario.Texto(texto)
    pares != null -> RespuestaUsuario.Pares(pares)
    else -> null
}
