package com.xpspeak.app.core.navigation

object Routes {
    const val AUTH = "auth"
    const val SELECCIONAR_NIVEL = "seleccionar_nivel"
    const val RECUPERAR_ACCESO = "recuperar_acceso"
    const val INICIO = "inicio"
    const val CHAT = "chat"
    const val LESSONS = "lessons"
    const val LECCION = "lessons/{id}"
    const val REPASO = "repaso"
    const val GAMES = "games"
    const val MINIJUEGO = "games/{id}"
    const val PROGRESS = "progress"
    const val ACCOUNT = "account"
    const val NOTIFICACIONES = "notificaciones"
    const val ELIMINAR_CUENTA = "account/eliminar"
    const val PRONUNCIACION = "pronunciacion"

    fun leccion(id: String) = "lessons/$id"
    fun minijuego(id: String) = "games/$id"
}
