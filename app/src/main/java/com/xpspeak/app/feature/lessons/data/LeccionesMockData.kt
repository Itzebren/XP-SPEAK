package com.xpspeak.app.feature.lessons.data

import com.google.gson.Gson

object LeccionesMockData {
    private val gson = Gson()

    fun catalogoMock(nivel: String = "A1"): CatalogoDto {
        return CatalogoDto(
            nivel = nivel.ifEmpty { "A1" },
            lecciones = listOf(
                LeccionResumenDto(
                    id = "a1-saludos",
                    orden = 1,
                    titulo = "Saludos y Presentaciones",
                    estado = EstadoLeccion.DISPONIBLE,
                    intentos = 0,
                    xpRecompensa = 50,
                    mejorPuntaje = null
                ),
                LeccionResumenDto(
                    id = "a1-personal",
                    orden = 2,
                    titulo = "Información Personal y Origen",
                    estado = EstadoLeccion.DISPONIBLE,
                    intentos = 0,
                    xpRecompensa = 60,
                    mejorPuntaje = null
                ),
                LeccionResumenDto(
                    id = "a1-rutinas",
                    orden = 3,
                    titulo = "Rutinas Diarias y Horarios",
                    estado = EstadoLeccion.EN_PROGRESO,
                    intentos = 1,
                    xpRecompensa = 70,
                    mejorPuntaje = 0.6
                ),
                LeccionResumenDto(
                    id = "a1-comida",
                    orden = 4,
                    titulo = "Comida y Restaurantes",
                    estado = EstadoLeccion.BLOQUEADA,
                    intentos = 0,
                    xpRecompensa = 80,
                    mejorPuntaje = null
                )
            )
        )
    }

    fun leccionMock(id: String): LeccionDto {
        return when (id) {
            "a1-personal" -> leccionPersonal()
            else -> leccionSaludos()
        }
    }

    private fun leccionSaludos(): LeccionDto {
        return LeccionDto(
            id = "a1-saludos",
            moduloTematico = "Saludos y Presentaciones",
            nivelMcer = "A1",
            canDo = listOf("Saludar a personas en inglés", "Presentarse y preguntar el nombre de alguien"),
            secciones = listOf(
                SeccionDto(
                    tipo = TipoSeccion.INTRODUCCION,
                    titulo = "Bienvenido a Saludos",
                    cuerpo = "En esta lección aprenderás las formas más comunes de saludar en inglés y cómo presentarte por primera vez.",
                    ejemplos = listOf("Hello! / ¡Hola!", "Good morning! / ¡Buenos días!")
                ),
                SeccionDto(
                    tipo = TipoSeccion.VOCABULARIO,
                    titulo = "Vocabulario Clave",
                    cuerpo = "Repasa estas expresiones fundamentales:",
                    items = listOf(
                        ItemDto(en = "Hello", es = "Hola", nota = "Formal e informal"),
                        ItemDto(en = "Good morning", es = "Buenos días", nota = "Hasta el mediodía"),
                        ItemDto(en = "Nice to meet you", es = "Gusto en conocerte", nota = "Al conocer a alguien")
                    )
                ),
                SeccionDto(
                    tipo = TipoSeccion.DIALOGO,
                    titulo = "Diálogo Corto",
                    cuerpo = "Escucha/Lee la conversación entre dos personas que se conocen:",
                    lineas = listOf(
                        LineaDialogoDto(hablante = "Anna", en = "Hi! I'm Anna. What's your name?", es = "¡Hola! Soy Anna. ¿Cuál es tu nombre?"),
                        LineaDialogoDto(hablante = "Ben", en = "Hello Anna! My name is Ben. Nice to meet you.", es = "¡Hola Anna! Mi nombre es Ben. Gusto en conocerte.")
                    )
                ),
                SeccionDto(
                    tipo = TipoSeccion.EVALUACION,
                    titulo = "Evaluación de la Lección",
                    cuerpo = "Demuestra lo aprendido respondiendo estas preguntas:",
                    items = listOf(
                        ItemDto(
                            id = "q1",
                            tipo = TipoEjercicio.OPCION_MULTIPLE,
                            enunciado = "¿Cómo dices 'Buenos días' en inglés?",
                            opciones = listOf("Good morning", "Good night", "Goodbye", "Hello"),
                            conceptoId = "voc.saludos.morning",
                            respuestaCorrecta = gson.toJsonTree(0),

                            feedbackError = "'Good morning' es la forma correcta para decir Buenos Días."
                        ),
                        ItemDto(
                            id = "q2",
                            tipo = TipoEjercicio.COMPLETAR,
                            enunciado = "Nice to ___ you.",
                            conceptoId = "voc.saludos.meet",
                            respuestaCorrecta = gson.toJsonTree("meet"),
                            acepta = listOf("meet"),
                            feedbackError = "La palabra correcta es 'meet' (Nice to meet you)."
                        ),
                        ItemDto(
                            id = "q3",
                            tipo = TipoEjercicio.EMPAREJAR,
                            enunciado = "Empareja cada expresión en inglés con su traducción en español:",
                            pares = listOf(
                                ParDto(izq = "Hi!", der = "¡Hola!", conceptoId = "voc.saludos.hi"),
                                ParDto(izq = "Goodbye", der = "Adiós", conceptoId = "voc.saludos.bye"),
                                ParDto(izq = "Thank you", der = "Gracias", conceptoId = "voc.saludos.thanks")
                            )
                        )
                    ),
                    umbralAprobacion = 0.7
                )
            )
        )
    }

    private fun leccionPersonal(): LeccionDto {
        return LeccionDto(
            id = "a1-personal",
            moduloTematico = "Información Personal",
            nivelMcer = "A1",
            canDo = listOf("Decir de dónde eres", "Dar tu edad y ocupación"),
            secciones = listOf(
                SeccionDto(
                    tipo = TipoSeccion.INTRODUCCION,
                    titulo = "Información Personal",
                    cuerpo = "Aprende a responder cuando te pregunten sobre tu origen y trabajo.",
                    ejemplos = listOf("Where are you from? / ¿De dónde eres?", "I am from Mexico. / Soy de México.")
                ),
                SeccionDto(
                    tipo = TipoSeccion.EVALUACION,
                    titulo = "Evaluación",
                    items = listOf(
                        ItemDto(
                            id = "q1",
                            tipo = TipoEjercicio.OPCION_MULTIPLE,
                            enunciado = "¿Cómo preguntas '¿De dónde eres?'?",
                            opciones = listOf("Where are you from?", "How old are you?", "What is your name?"),
                            conceptoId = "voc.personal.origin",
                            respuestaCorrecta = gson.toJsonTree(0),
                            feedbackError = "'Where are you from?' significa '¿De dónde eres?'"
                        )
                    ),
                    umbralAprobacion = 0.7
                )
            )
        )
    }
}
