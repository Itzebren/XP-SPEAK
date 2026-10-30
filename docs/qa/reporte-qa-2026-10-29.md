# Reporte de pruebas QA · XP-SPEAK · 29 de octubre de 2026

Pruebas funcionales de la app Android completa, hechas a mano sobre el emulador y guiadas
con `adb` (toques, texto y volcados de la jerarquía de la pantalla). Las capturas están en
`capturas-2026-10-29/` y se citan por su número.

## 1. Entorno

| Pieza | Valor |
|---|---|
| App | `develop` en `6bac815` + correcciones de este reporte, `app-debug.apk` (59 MB) |
| Dispositivo | Emulador Pixel_10a, Android 17, 1080×2424 |
| Backend | `npm run dev:emulador` (servidor local en :3000 con emuladores de Firestore y Auth de Firebase) |
| Red | `adb reverse tcp:3000` y `tcp:9099`; para el modo sin conexión se quitó el reverse y se apagaron wifi y datos |
| Cuenta | `qa.tester@xpspeak.test` creada durante la prueba en el emulador de Auth (correo verificado con el enlace que expone el emulador) |
| Pruebas automáticas | Android 126/126 (`testDebugUnitTest`), backend 163/163 (`npm test`) |

Para poder registrar y verificar cuentas sin tocar el proyecto real de Firebase se agregó la
bandera de compilación `-PfirebaseAuthEmulador=true` (solo debug; en release queda apagada).
Sin ella la app usa el Firebase real, como hasta ahora.

Limitaciones del entorno (no son defectos de la app):

- El backend local no tiene `AZURE_OPENAI_*` ni `AZURE_SPEECH_*`: el chat responde 503 y la
  voz cae al reconocedor del teléfono. El micrófono del emulador no capta audio, así que
  toda evaluación de voz termina en "no te entendimos" (CU-05 A1/A3), que es el camino esperado.
- El backend de producción en Vercel responde **403 Forbidden** a los tokens del emulador de
  Auth (son JWT con `alg: none` y el firewall de Vercel los rechaza). Con un token real
  responde normal (401 sin token, 200 con token). No afecta a usuarios reales.

## 2. Resultado por apartado

| # | Apartado | Qué se probó | Resultado | Capturas |
|---|---|---|---|---|
| 1 | Arranque y sesión guardada | Abrir la app con sesión previa sin verificar → aviso de verificación; relanzar con sesión verificada → entra directo a Inicio | ✅ | 01, 76 |
| 2 | Crear cuenta | Validación de contraseña en vivo (débil y válida), confirmación distinta, registro, pantalla "Verifica tu correo", verificación por enlace y "Ya verifiqué" | ✅ | 03–06 |
| 3 | Personalización | Botón bloqueado hasta elegir nivel, elección de A1 y avatar, entrada a Inicio | ✅ | 09–12 |
| 4 | Iniciar sesión | Contraseña incorrecta ("Correo o contraseña incorrectos"), inicio correcto | ✅ | 74, 75 |
| 5 | Recuperar contraseña | Pantalla y validación de correo inválido | ✅ (sin enviar correo real) | 72, 73 |
| 6 | Catálogo de lecciones | Estados Disponible/Bloqueada/En progreso/Completada, "mejor: %", desbloqueo de la siguiente al aprobar, botón Actualizar | ✅ | 13, 14, 27, 32 |
| 7 | Lección (teoría) | Introducción, tarjetas con audio, gramática con ejemplos, diálogo; micrófono de práctica en tarjetas, ejemplos y líneas | ✅ | 15, 19, 20 |
| 8 | Práctica de pronunciación desde la lección | Hoja con la frase, Escuchar/Más lento, Hablar, respaldo del teléfono y mensaje de audio no legible | ✅ | 16–18 |
| 9 | Evaluación de lección | Opción múltiple, completar, emparejar; reprobar (16 %, 50 %, 66 %) con nota por ítem; "Intentar de nuevo"; aprobar (vía API) con 50 XP e insignia | ⚠️ ver H1 | 21–26 |
| 10 | Retomar lección a medias | Salir de la evaluación deja la lección "En progreso" en el catálogo | ✅ (la restauración de respuestas no se pudo confirmar en pantalla; está cubierta por pruebas unitarias) | 32 |
| 11 | Progreso | Nivel, dominio, XP, nivel de XP, racha, habilidades, insignias (9, 1 obtenida), detalle de insignia con fecha | ✅ | 28–30 |
| 12 | Repaso SRS y RN-07 | Con conceptos vencidos el catálogo marca "Repasa primero" en el tema nuevo y lo manda al repaso; 9 ejercicios, envío, resultado con "vuelve mañana / en 6 días"; al terminar, el tema queda Disponible | ✅ | 80–84 |
| 13 | Minijuegos · catálogo | Cuatro juegos, "Continuar" cuando hay partida guardada | ✅ | 34 |
| 14 | Orden Maestro | Acierto, error con fichas en rojo y segundo intento, "Pronunciar la oración", salir y retomar la partida (diálogo) | ✅ | 35–41b |
| 15 | Misión Situacional | Opción incorrecta con explicación, avance por pasos, micrófono en tus líneas, resultado con XP, tiempo, racha y "Para repasar" | ✅ | 42–48 |
| 16 | Ráfaga de Palabras | Juego contra reloj con botones, resultado (10/12, +10 XP), micrófono sin Azure avisa y sigue con botones | ⚠️ ver H2 | 49–52 |
| 17 | Eco Vocal | Frase con audio, micrófono, tarjeta "Pronunciación incorrecta / índice de confianza bajo" (Il. 40), saltar frase | ✅ | 53–55 |
| 18 | Chat con IA | Envío de texto; sin Azure OpenAI muestra error; modo en vivo abre el micrófono y queda escuchando; enlace a Práctica | ⚠️ ver H3 | 56–60 |
| 19 | Práctica de pronunciación | Frase por nivel, Escuchar/Lento/Otra frase, Hablar con respaldo del teléfono | ✅ | 61–63 |
| 20 | Ajustes de perfil | Nombre, correo, avatar, cambio de nivel A1↔A2 (se refleja en catálogo y Progreso), accesos a Notificaciones, Cerrar sesión y Eliminar | ✅ ver H5 | 64–66 |
| 21 | Notificaciones | Estado, recordatorio diario, alertas de racha, frecuencia, hora (selector), permisos | ✅ | 67–69 |
| 22 | Eliminar cuenta | Pantalla de advertencia con lo que se pierde; "Cancelar y volver" | ✅ (no se confirmó la eliminación) | 70 |
| 23 | Cerrar sesión | Vuelve al acceso y limpia la sesión | ✅ | 71 |
| 24 | Sin conexión | Catálogo con aviso, lección A2 abierta desde el APK, Progreso con "datos de hace 1 min" | ✅ | 77–79 |

## 3. Hallazgos

| ID | Severidad | Descripción | Estado |
|---|---|---|---|
| H1 | **Media** | En las evaluaciones (lección y repaso) la opción solo se marcaba al tocar el círculo; tocar el texto no hacía nada. En pantalla se ve como "no me toma la respuesta" | **Corregido**: toda la fila es seleccionable (`EjercicioEvaluacion.kt`), verificado en la lección 2 (captura 31) |
| H2 | Baja | Ráfaga: el aviso del micrófono salía con doble punto: "…reconocimiento de voz.. Sigue con los botones." | **Corregido** (`RafagaViewModel.kt`) |
| H3 | Baja | Chat: cuando el servidor responde 503 con mensaje ("El chat no está disponible por ahora") la app muestra el genérico "Ocurrió un error. Intenta de nuevo." | Pendiente: revisar el mapeo del cuerpo de error en `ChatRepository` |
| H4 | Baja | Resultado de lección reprobada: "Conceptos a reforzar" muestra el identificador interno (`gram.verb_to_be_present`) en vez del nombre del concepto | Pendiente: usar el `titulo` del concepto (el repaso sí lo muestra bien) |
| H5 | Baja | Inconsistencias de datos locales vs servidor: Inicio decía "Empieza tu racha hoy" mientras Progreso mostraba "1 día" (la racha de Inicio es local y solo la actualiza la actividad hecha desde el teléfono); la pantalla de eliminar cuenta mostraba "A1" después de cambiar a A2 | Pendiente: tomar ambos datos del resumen del servidor cuando esté disponible |
| H6 | Info | `gradle.properties` trae `leccionesBaseUrl` apuntando a producción, así que el `debug` por defecto ya no usa `localhost:3000` como dice el comentario de `build.gradle.kts` | Documentado; para probar en local hay que pasar `-PleccionesBaseUrl=http://localhost:3000/` |
| H7 | Info | Producción (Vercel) rechaza con 403 los JWT `alg: none` del emulador de Auth. Es el firewall de Vercel, no la API | Sin acción; las pruebas contra producción deben usar una cuenta real |

No se encontraron cierres inesperados (crashes) en ninguna pantalla durante la sesión.

## 4. Lo que no se pudo probar aquí

- Evaluación real de pronunciación con Azure Speech (sin llaves y sin micrófono en el emulador).
- Respuestas del chat con IA y correcciones gramaticales (sin llaves de Azure OpenAI).
- Envío real del correo de recuperación (Resend) y eliminación definitiva de la cuenta.
- Notificación programada a la hora elegida (ya se verificó el 11 de octubre forzando el job).
- Teléfono físico y Android 8.0 (`minSdk 26`): solo se probó en Android 17.

## 5. Cómo repetir

```sh
# backend con emuladores de Firebase
cd backend && npm run dev:emulador
# app apuntando al backend local y al emulador de Auth
sh ./gradlew :app:assembleDebug -PfirebaseAuthEmulador=true -PleccionesBaseUrl=http://localhost:3000/
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb reverse tcp:3000 tcp:3000 && adb reverse tcp:9099 tcp:9099
# enlace de verificación de la cuenta recién creada
curl -s http://localhost:9099/emulator/v1/projects/demo-xpspeak/oobCodes
```
