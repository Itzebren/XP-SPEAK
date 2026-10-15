package com.xpspeak.app.feature.lessons.data

/**
 * Catálogo sin servidor (RN-11, RN-12). Mismas reglas que backend/lib/lecciones/estado.js:
 * una lección sin prerrequisito arranca disponible y las demás se desbloquean al aprobar
 * (≥ 70 %, RN-06) la anterior. El servidor sigue siendo la fuente de verdad: en cuanto hay
 * conexión, su catálogo reemplaza a este.
 */
object CatalogoLocal {

    /** Primer catálogo, cuando nunca se ha podido hablar con el servidor. */
    fun inicial(nivel: String, entradas: List<EntradaPaqueteDto>): CatalogoDto = CatalogoDto(
        nivel = nivel,
        lecciones = entradas.map { entrada ->
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

    /**
     * Refleja en el catálogo guardado un intento calificado en el teléfono: así, sin conexión,
     * aprobar una lección ya desbloquea la siguiente en vez de esperar a sincronizar.
     */
    fun conIntento(
        catalogo: CatalogoDto,
        entradas: List<EntradaPaqueteDto>,
        leccionId: String,
        aprobada: Boolean,
        puntaje: Double
    ): CatalogoDto {
        val siguientes = if (aprobada) entradas.filter { it.prerequisitoId == leccionId }.map { it.id }.toSet() else emptySet()
        return catalogo.copy(
            lecciones = catalogo.lecciones.map { leccion ->
                when {
                    leccion.id == leccionId -> leccion.copy(
                        estado = if (aprobada || leccion.estado == EstadoLeccion.COMPLETADA) EstadoLeccion.COMPLETADA else EstadoLeccion.REPROBADA,
                        intentos = leccion.intentos + 1,
                        mejorPuntaje = maxOf(leccion.mejorPuntaje ?: 0.0, puntaje)
                    )
                    leccion.id in siguientes && leccion.estado == EstadoLeccion.BLOQUEADA ->
                        leccion.copy(estado = EstadoLeccion.DISPONIBLE)
                    else -> leccion
                }
            }
        )
    }
}
