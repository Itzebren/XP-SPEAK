# Calendario semanal del TT 2026-B162 · agosto–diciembre 2026

> Aplicación móvil con inteligencia artificial y gamificación para reforzar conocimientos del idioma inglés en niveles A1 y A2.
>
> Integrantes: **Brenda Itzel Parrales Ramírez** (Scrum Master / Lead Developer) y **José Juan Arellano Juárez** (Developer).
> Product Owner: M. en C. Nidia Asunción Cortez Duarte.

Desglose por semana del **Cronograma final** de agosto a diciembre (periodo 2026-2). Las actividades y sus semanas son las del Cronograma; aquí se detalla qué hace cada integrante cada semana. Semanas: S1 = días 1–7, S2 = 8–14, S3 = 15–21, S4 = 22 a fin de mes.

La versión para entregar es `Calendario semanal - TT 2026-B162 .xlsx` (misma información, con estado automático según la fecha). Ambos archivos se generan con `generar.py`.

Estado al **29-10-2026**: ✅ concluida · ▶️ en curso · (vacío) pendiente.

## Plan contra lo desarrollado

La planeación se mantiene apegada al Cronograma. Lo que ya está desarrollado antes de su semana se marca en *Avance real*; esas semanas se usan para pruebas, ajustes y para documentar el módulo en el capítulo 5 del reporte.

| Actividad del Cronograma | Semanas planeadas | Avance real |
|---|---|---|
| Interfaz y estructura básica | Ago S4 – Sep S2 | Arquitectura 14-sep, Auth 20-sep, recuperación 25-sep (≈2 semanas tarde) |
| Integración de chatbot con IA | Sep S3 – Oct S1 | Chat en texto 29-sep (a tiempo) |
| Módulo de reconocimiento de voz | Oct S2 – Oct S3 | Azure + evaluación de pronunciación 06-oct; voz en el chat 18-oct y chat en vivo con pronunciación 28-oct (a tiempo) |
| Módulos de gramática y vocabulario | Oct S4 – Nov S1 | Hecho 26-sep – 01-oct; 24 lecciones (adelantado) |
| Ejercicios de gramática y vocabulario | Nov S2 – Nov S3 | Evaluaciones y SRS 29-sep (adelantado) |
| Desarrollo de mini-juegos | Nov S4 – Dic S1 | Los 4 juegos 06-oct (adelantado) |
| Integración (progreso, notificaciones) | Nov S4 – Dic S2 | Progreso, insignias, notificaciones y 14 pantallas 11-oct (adelantado) |

Diferencia entre los dos renglones del Cronograma final: Brenda tiene la marca de product backlog de noviembre en la S2 y José Juan en la S1. El calendario respeta esas semanas.

## Sprint 3 — Desarrollo (2026-2, agosto–noviembre)

### Semana 1 · Agosto S1 (01/08 – 07/08) ✅

**Brenda** — Creación del product backlog · Reporte técnico TT-II

- Organizar el product backlog por épicas: Acceso y cuenta, Chatbot con IA, Voz
- Ligar Historias de Usuario a RF/CU y escribir criterios de aceptación
- Abrir el capítulo 5 Desarrollo e Implementación en el reporte

**José Juan** — Creación del product backlog · Reporte técnico TT-II

- Organizar las épicas Lecciones, Minijuegos, Progreso y logros, Notificaciones
- Ligar Historias de Usuario a RF/CU y escribir criterios de aceptación

**Entregable:** Product backlog por épicas

### Semana 2 · Agosto S2 (08/08 – 14/08) ✅

**Brenda** — Creación del product backlog · Creación del sprint backlog · Diseño de la interfaz · Reporte técnico TT-II

- Priorizar el backlog con la Product Owner
- Seleccionar las HU del Sprint 3 y crear el tablero Kanban (Sin empezar / En desarrollo / Finalizado)
- Diseño: paleta de colores y logotipo (Ilustraciones 26–27)

**José Juan** — Creación del product backlog · Creación del sprint backlog · Diseño de la interfaz · Reporte técnico TT-II

- Estimar las HU y repartir el trabajo del sprint
- Diseño: sistema visual (tipografía, botones, tarjetas, íconos)

**Entregable:** Backlog priorizado · Tablero Kanban · Paleta y logotipo

### Semana 3 · Agosto S3 (15/08 – 21/08) ✅

**Brenda** — Creación del sprint backlog · Diseño de la interfaz · Reporte técnico TT-II

- Desglosar las HU del sprint en tareas técnicas
- Diseño de pantallas: Iniciar sesión, Crear cuenta, Recuperar contraseña, Personalización (Ilustraciones 28–31)

**José Juan** — Creación del sprint backlog · Diseño de la interfaz · Reporte técnico TT-II

- Desglosar las HU del sprint en tareas técnicas
- Diseño de pantallas: Inicio, Progreso y logros, Minijuegos, Catálogo de lecciones, Lección (Ilustraciones 32–34, 37–39)

**Entregable:** Sprint backlog · Mockups Il. 28–34, 37–39

### Semana 4 · Agosto S4 (22/08 – 31/08) ✅

**Brenda** — Diseño de la interfaz · Interfaz y estructura básica · Reporte técnico TT-II

- Diseño de pantallas: Ajustes de perfil, Confirmar eliminación, Error de pronunciación, Notificaciones (Ilustraciones 35–36, 40–41)
- Crear el proyecto Android (Kotlin, MVVM, Hilt, Navigation Compose, Room)

**José Juan** — Diseño de la interfaz · Interfaz y estructura básica · Reporte técnico TT-II

- Prototipo navegable con las 14 pantallas y revisión con la directora
- Crear el servidor de servicios (Vercel) y el proyecto de Firebase (Auth y Firestore)

**Entregable:** Sección 4.5 · Prototipo navegable · Esqueleto de la app y del servidor

**Avance real:** Arquitectura base de la app: 14-sep (Sep S2), 2 semanas después de lo planeado.

### Semana 5 · Septiembre S1 (01/09 – 07/09) ✅

**Brenda** — Creación del product backlog · Interfaz y estructura básica · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Registro de cuenta e inicio de sesión con Firebase Auth (CU-01, CU-02, RN-01)
- Validación de contraseña y mensajes de error

**José Juan** — Creación del product backlog · Interfaz y estructura básica · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Tema de la app (colores, tipografía, formas) y componentes comunes
- Barra de navegación inferior y pantalla de Inicio

**Entregable:** Registro e inicio de sesión funcionando

**Avance real:** Firebase Auth (registro, login, validación): 20-sep (Sep S3).

### Semana 6 · Septiembre S2 (08/09 – 14/09) ✅

**Brenda** — Interfaz y estructura básica · Reporte técnico TT-II

- Recuperación de acceso por código de verificación (CU-03): envío por correo con Resend, vigencia e intentos
- Nivel inicial A1/A2 al registrarse (CU-04, RN-02)
- Documentar 5.1 Arquitectura base y módulos de registro y recuperación

**José Juan** — Interfaz y estructura básica · Reporte técnico TT-II

- Personalización: nombre de usuario y avatar (Ilustración 31)
- Pantalla de cuenta: datos del perfil, cerrar sesión, eliminar cuenta (CU-10, RN-14)

**Entregable:** Incremento: acceso, perfil y cuenta

**Avance real:** Recuperación de acceso: 25-sep (Sep S4). Cuenta: 29-sep. Eliminación de cuenta en servidor: 11-oct.

### Semana 7 · Septiembre S3 (15/09 – 21/09) ✅

**Brenda** — Integración de chatbot con IA · Reporte técnico TT-II

- Conectar el motor de IA generativa desde el servidor (RF-06)
- System prompt restringido a A1/A2 según el MCER (RN-03, riesgo R03)

**José Juan** — Integración de chatbot con IA · Reporte técnico TT-II

- Armar un banco de conversaciones de prueba por nivel para validar el prompt
- Revisar que el vocabulario de las respuestas no exceda A1/A2

**Entregable:** Chatbot respondiendo desde el servidor

### Semana 8 · Septiembre S4 (22/09 – 30/09) ✅

**Brenda** — Integración de chatbot con IA · Reporte técnico TT-II

- Pantalla de chat (CU-05) con ChatViewModel e historial de la conversación
- Correcciones gramaticales inmediatas y efímeras (RF-09, RN-05)

**José Juan** — Integración de chatbot con IA · Reporte técnico TT-II

- Manejo de errores y sin conexión en el chat (RNF-10)
- Pruebas del chat contra el banco de conversaciones

**Entregable:** Incremento: chat con IA en texto

**Avance real:** Módulo de chat con IA (interfaz y lógica): 29-sep (Sep S4), a tiempo.

### Semana 9 · Octubre S1 (01/10 – 07/10) ✅

**Brenda** — Creación del product backlog · Integración de chatbot con IA · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Pruebas unitarias del chat y ajustes del prompt
- Documentar el módulo de chat en el capítulo 5

**José Juan** — Creación del product backlog · Integración de chatbot con IA · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Revisión del chat con la directora (reunión quincenal)

**Entregable:** Chat con IA cerrado · Backlog refinado

### Semana 10 · Octubre S2 (08/10 – 14/10) ✅

**Brenda** — Módulo de reconocimiento de voz · Reporte técnico TT-II

- Voz a texto en el chat (RF-07): botón de micrófono, permiso de micrófono y envío del texto reconocido
- Manejo de audio sin almacenarlo (RN-04, RNF-05)

**José Juan** — Módulo de reconocimiento de voz · Reporte técnico TT-II

- Endpoint de token de Azure AI Speech en el servidor (la llave no vive en la app)
- Evaluación de pronunciación con Azure y respaldo con el reconocedor del teléfono

**Entregable:** Voz a texto en el chat

**Avance real:** Adelantado: token de Azure y evaluación de pronunciación hechos el 06-oct para Eco Vocal. Voz en el chat (RF-07, RF-08): 18-oct; locución bilingüe 22-oct y chat en vivo con pronunciación por mensaje 28-oct.

### Semana 11 · Octubre S3 (15/10 – 21/10) ✅

**Brenda** — Módulo de reconocimiento de voz · Reporte técnico TT-II

- Texto a voz de las respuestas del chatbot (RF-08)
- Respaldo en modo texto si el servicio de voz no responde a tiempo (riesgo R01)

**José Juan** — Módulo de reconocimiento de voz · Reporte técnico TT-II

- Pantalla de error de pronunciación (Ilustración 40, RF-09)
- Medir la latencia de voz (RNF-03) y configurar Azure en producción

**Entregable:** Incremento: conversación por voz

**Avance real:** Pantalla de error de pronunciación (RF-09) 18-oct; respuestas en streaming (RNF-02) 19-oct; medición de tiempos (R01, RNF-03) 28-oct. Azure Speech verificado en producción el 29-oct (token y evaluación REST).

### Semana 12 · Octubre S4 (22/10 – 31/10) ▶️ **en curso**

**Brenda** — Desarrollo de módulos de gramática y vocabulario · Reporte técnico TT-II

- Plan curricular A1 y A2 alineado al MCER (temas por nivel)
- Revisar los temas contra los descriptores del MCER

**José Juan** — Desarrollo de módulos de gramática y vocabulario · Reporte técnico TT-II

- Estructura de una lección (introducción, vocabulario, gramática, diálogos, evaluación) y su esquema JSON
- API de lecciones en el servidor y pantallas de catálogo y lección (Ilustraciones 38–39)

**Entregable:** Plan curricular · Estructura de lección · Catálogo

**Avance real:** Adelantado: cliente, catálogo y pantalla de lección 26-sep; servidor de lecciones 30-sep.

### Semana 13 · Noviembre S1 (01/11 – 07/11)

**Brenda** — Creación del product backlog · Desarrollo de módulos de gramática y vocabulario · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Redactar el contenido de las lecciones A1
- Revisión humana del contenido (guía de autoría)

**José Juan** — Creación del product backlog · Desarrollo de módulos de gramática y vocabulario · Reporte técnico TT-II

- Refinar el product backlog (inicio de mes)
- Redactar el contenido de las lecciones A2
- Validación automática del contenido y caché fuera de línea (RN-11)

**Entregable:** Lecciones A1 y A2 publicadas

**Avance real:** Adelantado: 24 lecciones (12 A1 y 12 A2) y caché offline (29-sep).

### Semana 14 · Noviembre S2 (08/11 – 14/11)

**Brenda** — Creación del product backlog · Ejercicios de gramática y vocabulario · Reporte técnico TT-II

- Refinar el product backlog
- Evaluaciones cortas al final de cada lección (RF-12): tipos de ejercicio y calificación
- Regla de aprobación con 70 % y desbloqueo (RN-06)

**José Juan** — Ejercicios de gramática y vocabulario · Reporte técnico TT-II

- XP solo en la primera aprobación y registro de errores por concepto
- Envío de intentos al servidor con reintento si no hay conexión

**Entregable:** Evaluaciones por lección

**Avance real:** Adelantado: calificación local y outbox de intentos (29-sep).

### Semana 15 · Noviembre S3 (15/11 – 21/11)

**Brenda** — Ejercicios de gramática y vocabulario · Reporte técnico TT-II

- Pruebas del flujo completo de lección: aprobar, reprobar, desbloquear y XP
- Documentar el módulo de lecciones en el capítulo 5

**José Juan** — Ejercicios de gramática y vocabulario · Reporte técnico TT-II

- Repaso con repetición espaciada SM-2 (RF-13, RN-07): sesión de repaso en el servidor y pantalla de repaso

**Entregable:** Incremento: lecciones con evaluación y repaso SRS

**Avance real:** Adelantado: repaso SRS con SM-2 (29-sep).

### Semana 16 · Noviembre S4 (22/11 – 30/11)

**Brenda** — Desarrollo de mini-juegos · Pruebas y mejoras · Integración de todos los módulos · Reporte técnico TT-II

- Minijuegos Misión Situacional y Eco Vocal (RF-11, CU-07)
- Pruebas y mejoras de chat y voz
- Integración: notificaciones de recordatorio configurables (RF-15, CU-09, RN-15)

**José Juan** — Desarrollo de mini-juegos · Pruebas y mejoras · Integración de todos los módulos · Reporte técnico TT-II

- Catálogo de minijuegos (Ilustración 37) y motor común (rondas, XP, racha)
- Minijuegos Orden Maestro y Ráfaga de Palabras
- Integración: progreso y logros (CU-08, RF-05, RF-14, RN-08, RN-09)

**Entregable:** Cuatro minijuegos · Progreso · Notificaciones

**Avance real:** Adelantado: los 4 minijuegos (06-oct), progreso e insignias (11-oct), notificaciones y las 14 pantallas del diseño (11-oct).

### Semana 17 · Diciembre S1 (01/12 – 07/12)

**Brenda** — Creación del product backlog · Desarrollo de mini-juegos · Pruebas y mejoras · Integración de todos los módulos · Reporte técnico TT-II

- Refinar el product backlog (último del proyecto)
- Pruebas de los minijuegos en dispositivo real
- Integración: flujo completo registro → nivel → chat → lección

**José Juan** — Desarrollo de mini-juegos · Pruebas y mejoras · Integración de todos los módulos · Reporte técnico TT-II

- Sincronizar resultados de partidas con el servidor (XP, racha y SRS)
- Racha unificada entre lecciones, minijuegos y chat
- Documentar minijuegos y progreso en el capítulo 5

**Entregable:** Incremento: minijuegos y gamificación · Cierre del Sprint 3

## Sprint 4 — Integración, pruebas y evaluación (2026-2, diciembre)

### Semana 18 · Diciembre S2 (08/12 – 14/12)

**Brenda** — Integración de todos los módulos · Realización de pruebas del sistema · Corrección de pruebas · Segunda evaluación de TT-II · Reporte técnico TT-II

- Cerrar la integración y desplegar el servidor en producción
- Pruebas del sistema por caso de uso (CU-01 a CU-05, CU-10)
- Preparar la presentación del TT-II

**José Juan** — Integración de todos los módulos · Realización de pruebas del sistema · Corrección de pruebas · Segunda evaluación de TT-II · Reporte técnico TT-II

- Pruebas del sistema por caso de uso (CU-06 a CU-09) y de RNF (latencia, offline, Android 8.0+)
- Corregir los defectos encontrados

**Entregable:** Sistema integrado · Reporte de pruebas

### Semana 19 · Diciembre S3 (15/12 – 21/12)

**Brenda** — Realización de pruebas del sistema · Corrección de pruebas · Segunda evaluación de TT-II · Retroalimentación del jurado · Reporte técnico TT-II

- Corregir los defectos pendientes y repetir las pruebas
- Presentar el TT-II ante el jurado y registrar observaciones

**José Juan** — Realización de pruebas del sistema · Corrección de pruebas · Segunda evaluación de TT-II · Retroalimentación del jurado · Reporte técnico TT-II

- Corregir los defectos pendientes y repetir las pruebas
- Presentar el TT-II ante el jurado y registrar observaciones

**Entregable:** Presentación TT-II · Versión estable de la app

### Semana 20 · Diciembre S4 (22/12 – 31/12)

**Brenda** — Pruebas piloto con usuarios reales · Segunda evaluación de TT-II · Retroalimentación del jurado · Reporte técnico TT-II

- Prueba piloto con estudiantes (A1/A2): instalar la app y aplicar el cuestionario de uso
- Atender las observaciones del jurado

**José Juan** — Pruebas piloto con usuarios reales · Segunda evaluación de TT-II · Retroalimentación del jurado · Reporte técnico TT-II

- Analizar los resultados de la prueba piloto
- Cerrar el reporte técnico TT-II con resultados y conclusiones

**Entregable:** Resultados de la prueba piloto · Reporte TT-II final
