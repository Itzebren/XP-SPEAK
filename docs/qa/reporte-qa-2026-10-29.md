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
| Pruebas automáticas | Android 131/131 (`testDebugUnitTest`), backend 166/166 (`npm test`), lint 0 errores, después de las correcciones |
| Producción | `https://xp-speak-auth-backend.vercel.app` con una cuenta real del proyecto `xp-speak` (sección 4) |

Para poder registrar y verificar cuentas sin tocar el proyecto real de Firebase se agregó la
bandera de compilación `-PfirebaseAuthEmulador=true` (solo debug; en release queda apagada).
Sin ella la app usa el Firebase real, como hasta ahora.

Limitaciones del entorno local (no son defectos de la app):

- El backend local no tiene `AZURE_OPENAI_*` ni `AZURE_SPEECH_*`: el chat responde 503 y la
  voz cae al reconocedor del teléfono. El micrófono del emulador no capta audio, así que
  toda evaluación de voz termina en "no te entendimos" (CU-05 A1/A3), que es el camino esperado.
  Lo que depende de Azure se verificó contra producción (sección 4).
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
| 9 | Evaluación de lección | Opción múltiple, completar, emparejar; reprobar (16 %, 50 %, 66 %) con nota por ítem; "Intentar de nuevo"; aprobar (vía API) con 50 XP e insignia | ✅ (H1 y H4 corregidos) | 21–26 |
| 10 | Retomar lección a medias | Salir de la evaluación deja la lección "En progreso" en el catálogo | ✅ (la restauración de respuestas no se pudo confirmar en pantalla; está cubierta por pruebas unitarias) | 32 |
| 11 | Progreso | Nivel, dominio, XP, nivel de XP, racha, habilidades, insignias (9, 1 obtenida), detalle de insignia con fecha | ✅ | 28–30 |
| 12 | Repaso SRS y RN-07 | Con conceptos vencidos el catálogo marca "Repasa primero" en el tema nuevo y lo manda al repaso; 9 ejercicios, envío, resultado con "vuelve mañana / en 6 días"; al terminar, el tema queda Disponible | ✅ | 80–84 |
| 13 | Minijuegos · catálogo | Cuatro juegos, "Continuar" cuando hay partida guardada | ✅ | 34 |
| 14 | Orden Maestro | Acierto, error con fichas en rojo y segundo intento, "Pronunciar la oración", salir y retomar la partida (diálogo) | ✅ | 35–41b |
| 15 | Misión Situacional | Opción incorrecta con explicación, avance por pasos, micrófono en tus líneas, resultado con XP, tiempo, racha y "Para repasar" | ✅ | 42–48 |
| 16 | Ráfaga de Palabras | Juego contra reloj con botones, resultado (10/12, +10 XP), micrófono sin Azure avisa y sigue con botones | ✅ (H2 corregido) | 49–52 |
| 17 | Eco Vocal | Frase con audio, micrófono, tarjeta "Pronunciación incorrecta / índice de confianza bajo" (Il. 40), saltar frase | ✅ | 53–55 |
| 18 | Chat con IA | Envío de texto; sin Azure OpenAI muestra error; modo en vivo abre el micrófono y queda escuchando; enlace a Práctica | ✅ (H3 corregido) | 56–60 |
| 19 | Práctica de pronunciación | Frase por nivel, Escuchar/Lento/Otra frase, Hablar con respaldo del teléfono | ✅ | 61–63 |
| 20 | Ajustes de perfil | Nombre, correo, avatar, cambio de nivel A1↔A2 (se refleja en catálogo y Progreso), accesos a Notificaciones, Cerrar sesión y Eliminar | ✅ (H5 corregido) | 64–66 |
| 21 | Notificaciones | Estado, recordatorio diario, alertas de racha, frecuencia, hora (selector), permisos | ✅ | 67–69 |
| 22 | Eliminar cuenta | Pantalla de advertencia con lo que se pierde; "Cancelar y volver" | ✅ (no se confirmó la eliminación) | 70 |
| 23 | Cerrar sesión | Vuelve al acceso y limpia la sesión | ✅ | 71 |
| 24 | Sin conexión | Catálogo con aviso, lección A2 abierta desde el APK, Progreso con "datos de hace 1 min" | ✅ | 77–79 |

## 3. Hallazgos

| ID | Severidad | Descripción | Estado |
|---|---|---|---|
| H1 | **Media** | En las evaluaciones (lección y repaso) la opción solo se marcaba al tocar el círculo; tocar el texto no hacía nada. En pantalla se ve como "no me toma la respuesta" | **Corregido**: toda la fila es seleccionable (`EjercicioEvaluacion.kt`), verificado en la lección 2 (captura 31) |
| H2 | Baja | Ráfaga: el aviso del micrófono salía con doble punto: "…reconocimiento de voz.. Sigue con los botones." | **Corregido** (`RafagaViewModel.kt`) |
| H3 | Baja | Chat: cuando el servidor responde 503 con mensaje ("El chat no está disponible por ahora") la app mostraba el genérico "Ocurrió un error. Intenta de nuevo." | **Corregido**: `ChatRepository.mensajeDeError` lee el `error` del servidor y, si no viene, da un mensaje según el código HTTP (prueba unitaria en `ChatContratoTest`) |
| H4 | Baja | Resultado de lección: "Conceptos a reforzar" mostraba el identificador interno (`gram.verb_to_be_present`) y "Desbloqueaste" el id de la lección | **Corregido**: el backend manda `conceptos_debiles_detalle` y `desbloqueada_titulo` (prueba en `requisitos.test.js`) y la app los muestra; sin servidor arma un nombre legible a partir del id (prueba en `LeccionesContratoTest`) |
| H5 | Baja | Inconsistencias de datos locales vs servidor: Inicio decía "Empieza tu racha hoy" mientras Progreso mostraba "1 día"; la pantalla de eliminar cuenta mostraba "A1" después de cambiar a A2 | **Corregido**: Inicio combina la racha local con la del resumen del servidor guardado por Progreso y usa la mayor; la pantalla de eliminar cuenta lee el nivel real del perfil |
| H6 | Info | `gradle.properties` trae `leccionesBaseUrl` apuntando a producción, así que el `debug` por defecto ya no usa `localhost:3000` como dice el comentario de `build.gradle.kts` | Documentado; para probar en local hay que pasar `-PleccionesBaseUrl=http://localhost:3000/` |
| H7 | Info | Producción (Vercel) rechaza con 403 los JWT `alg: none` del emulador de Auth. Es el firewall de Vercel, no la API | Sin acción; las pruebas contra producción deben usar una cuenta real |
| H8 | **Alta** | Producción: `POST /api/send-reset-code` (recuperar contraseña) respondía **500 "Ocurrió un error"** al pedir un código para `diseno.prueba@xpspeak.com`. Causa más probable: el remitente de pruebas `onboarding@resend.dev` solo puede entregar al correo del dueño de la cuenta de Resend (o falta `RESEND_API_KEY` en Vercel) | **Corregido en la app y el backend**: el backend responde 503 con `CORREO_NO_CONFIGURADO` / `CORREO_NO_DISPONIBLE` (pruebas en `recuperacion.test.js`) y la app, al recibirlo, manda el **enlace de restablecimiento del propio Firebase** (`RecuperacionRepository.enviarEnlaceFirebase`, paso `ENLACE` en la pantalla), que no depende de Resend; se verificó que Firebase lo envía para el proyecto real (`accounts:sendOobCode` → 200). Opcional para volver al código de 6 dígitos: verificar un dominio en Resend |

No se encontraron cierres inesperados (crashes) en ninguna pantalla durante la sesión. Los cinco
hallazgos funcionales de la app (H1–H5) quedaron corregidos en el mismo día; H6 y H7 son notas de
entorno. H8 era de producción y quedó cubierto con el respaldo de Firebase, así la recuperación de
contraseña funciona aunque Resend no entregue. H1 se verificó de nuevo en el emulador; H2–H5 y H8
quedaron cubiertos por pruebas unitarias y del backend.

## 4. Verificación contra producción (Vercel + Firebase real)

Con una cuenta real del proyecto (`diseno.prueba@xpspeak.com`) y una cuenta desechable creada y
borrada durante la prueba:

| Qué | Resultado |
|---|---|
| Chat con IA (`POST /api/chat`, Azure OpenAI) | ✅ 200. A "I go to the park yesterday and I see my friends" respondió en inglés A1 y devolvió la corrección (RF-09): *I went… I saw…* con explicación en español |
| Token de voz (`GET /api/speech-token`) | ✅ 200. Token del recurso Azure Speech F0 en `eastus2` (las llaves sí están en Vercel; los docs decían lo contrario y se actualizaron) |
| Evaluación de pronunciación real (REST de Azure con ese token, audio sintetizado "Good morning! My name is Laura.") | ✅ Precisión 98, fluidez 97, completitud 100, confianza 99, seis palabras con fonemas IPA y candidatos. La respuesta se guardó como `app/src/test/resources/azure-pronunciation-real.json` y la lee el analizador de la app en una prueba unitaria |
| Progreso (`GET /api/progress`) | ✅ 200 con nivel, XP, racha, habilidades e insignias |
| Eliminar cuenta (`POST /api/account/delete`) | ✅ 200 `{eliminada: true, documentos: 1}`; la cuenta desechable dejó de existir en Firebase Auth (las credenciales de administrador sí están en Vercel) |
| Recuperar contraseña (`POST /api/send-reset-code`) | ❌ 500 genérico → hallazgo **H8**, resuelto con el enlace de Firebase (`accounts:sendOobCode` responde 200 para el proyecto real). La validación de correo inválido (400) y de código incompleto (400) sí funcionan |
| Firewall de Vercel | Rechaza tokens del emulador (403), acepta los reales |

Compatibilidad con Android 8.0 (`minSdk 26`): `lintDebug` termina con 0 errores y ninguna
advertencia `NewApi`/`InlinedApi`, es decir, no hay llamadas a APIs posteriores al mínimo sin
proteger. Solo se ejecutó en Android 17 porque no hay imagen de sistema de API 26 instalada.

## 5. Lo que sigue requiriendo algo externo

- Una pasada en teléfono físico con micrófono real (Eco Vocal, chat en vivo y práctica con
  Azure): el emulador no capta audio. Todo lo demás de la voz ya quedó verificado por API.
- Ejecutar la app en una imagen de Android 8.0 (API 26): no está instalada y la instalación de las
  herramientas del SDK quedó bloqueada por permisos en esta sesión; se instala desde Android Studio
  (SDK Manager → `system-images;android-26;google_apis;arm64-v8a`). El análisis estático ya pasó.
- Si se quiere volver al código de 6 dígitos por correo propio: verificar un dominio en Resend.

## 6. Cómo repetir

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
