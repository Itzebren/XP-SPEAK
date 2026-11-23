package com.xpspeak.app.feature.lessons.domain

/**
 * Lo que el usuario contestó en un ítem de la evaluación, según su tipo
 * (CU-06 paso 6). Reemplaza el uso de `Any` para que cada ejercicio tenga
 * un valor bien tipado.
 */
sealed interface RespuestaUsuario {
    /** opcion_multiple: índice de la opción elegida. */
    data class Opcion(val indice: Int) : RespuestaUsuario

    /** completar: texto libre (el servidor lo normaliza). */
    data class Texto(val texto: String) : RespuestaUsuario

    /** emparejar: izquierda → derecha elegida por el usuario. */
    data class Pares(val elegidos: Map<String, String>) : RespuestaUsuario
}
