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
