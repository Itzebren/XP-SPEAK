# Diseño y plan del módulo de **Progreso y logros** (CU-08, RF-05, RF-14)

> Rama `feature/progreso` (desde `develop` `bd20c01`). Estado: **implementado y probado en
> emulador** (2026-10-11). El plan se aprobó el 2026-10-07. Es el documento de contexto del
> módulo, igual que `docs/minijuegos-diseno.md`: se actualiza conforme avance el desarrollo.

## 0. Estado actual

| Pieza | Estado | Dónde |
|---|---|---|
| Fase 1: reglas puras (niveles, racha, insignias, resumen) | ✅ | `backend/lib/progreso/{niveles,racha,insignias,resumen}.js` |
| Fase 2: resumen en las transacciones, `GET /api/progress` (ETag), `POST /api/progress/chat` | ✅ | `backend/lib/progreso/{repositorio,vista}.js`, `backend/api/progress/` |
| Scripts `migrar:resumen` y `exportar:insignias` | ✅ | `backend/scripts/`, `app/src/main/assets/progreso/insignias.json` |
| Fase 3: datos en Android (API, Room v5, repositorio, catálogo local) | ✅ | `feature/progress/data/`, `core/sesion/SesionActual.kt` |
| Fase 4: pantalla Progreso, detalle de insignia, festejo `InsigniaNueva` | ✅ | `feature/progress/ui/` (también en lección, minijuego, repaso y chat) |
| Fase 5: racha unificada (`RegistroActividad`) y chat | ✅ | `core/actividad/`, `LeccionesRepository`, `ChatViewModel` |
| Carga del arte final de insignias y mascota | ✅ | `recursoDrawable` en `feature/progress/ui/IconosInsignia.kt`: usa `res/drawable/insignia_<id>` y `mascota` si existen |
| Archivos del arte final | ✅ | VectorDrawable `insignia_<id>` y `mascota` generados en `feature/diseno` (ver `docs/diseno.md`) |
| Despliegue a producción y `npm run migrar:resumen` | ❌ | Seguir "Orden de despliegue" (abajo y en `backend/README.md`) |

### 0.1 Contexto para retomar el desarrollo

Última sesión: **2026-10-11**. Pruebas automáticas:
- Backend: **119/119** con `npm test`, de las cuales 28 son nuevas en `test/progreso.test.js`.
- Emulador de Firestore: **3/3** con `npm run test:emulador`. Incluye la reconstrucción
  dentro de la transacción con Firestore real, el ETag, dos registros de chat simultáneos y la
  migración.
- Android: **63/63** con `testDebugUnitTest`. Las nuevas son `ProgresoReglasTest`,
  `ProgresoCacheTest`, `ProgresoContratoTest`, `ProgresoRepositoryTest` y
  `ProgressViewModelTest`.

Verificación en el emulador (Pixel_10a, `npm run dev:app`, cuenta de prueba con un historial
de 136 XP sembrado sin resumen):

| Caso | Resultado |
|---|---|
| Progreso reconstruye desde eventos | 136 XP, nivel 2 (36/200), racha de 2 días, dominio 1/12 |
| Insignias que ya merecía | "Primer Logro" a color, sin diálogo |
| Orden Maestro al 100 % | Festejo "Estrella del Minijuego", +10 XP; en Progreso: 146 XP y racha de 3 días |
| Primera respuesta del chat | Festejo "Habla con IA" (`POST /api/progress/chat` → 200) |
| Sin conexión con caché | Aviso "Sin conexión · datos de hace un momento" |
| Sin conexión y sin caché | 9 insignias en gris desde el catálogo empaquetado, más la XP y la racha del perfil local |
| Usuario nuevo (A1) | Mensaje motivacional; "Haz tu primera lección" lleva a Lecciones |
| 360 dp con fuente al 130 % | Nada se corta en Progreso |

**Cambios respecto al plan** (las secciones de abajo siguen siendo el diseño de referencia):
1. **Resumen faltante dentro de una actividad.** La transacción lo reconstruye desde el
   historial (`leerResumenEnTx`, con lecturas consistentes vía `tx.get(consulta)`) y luego
   aplica la actividad en vivo. Se mantiene la regla de nunca crearlo desde `resumenVacio`,
   pero un usuario nuevo sí ve el festejo de "Primer Logro". Con el plan original, ese festejo
   se perdía (habría llegado como retroactiva).
2. **`racha.ultimo_dia` en `GET /api/progress`.** Así la app sigue contando la racha sin red
   después de sincronizar. Por eso `UsuarioDao.actualizarProgreso` recibe también `ultimoDia`.
   Con actividades pendientes se usa `actualizarXp`.
3. **Habilidades:** se muestra "Sin datos" con menos de 5 respuestas
   (`MIN_RESPUESTAS_HABILIDAD`). Cada habilidad trae una `sugerencia` ("Juega Eco Vocal").
4. **Jugador Experto:** sin haber jugado los 4 juegos, su progreso se queda en 24/25.
5. **Festejo también en el repaso y en el chat**, no solo en lección y minijuego.
6. **Interfaz para pruebas.** `SesionActual`, `CatalogoInsignias` y `FuenteProgreso` son
   interfaces (enlazadas en `core/di/ProgresoModule.kt`) para probar con fakes, sin Firebase.
7. **Barra inferior:**
   - Juegos cambió a `SportsEsports`, porque `EmojiEvents` ahora es de Progreso.
   - Las etiquetas van en una sola línea, porque con fuente grande se partían.
8. **`diaLocal`** se movió a `backend/lib/progreso/racha.js`. `minijuegos/repositorio.js` lo
   reexporta.

**Pendiente:**
- Los archivos del arte final. Basta con agregar `res/drawable/insignia_<id con _>.png`
  (por ejemplo `insignia_primer_logro.png`) y `mascota.png`; la app los usa sin cambiar código
  y pone en gris las bloqueadas. Se verificó en el emulador con un drawable temporal.
- Desplegar. El cliente de chat no tiene un id de conversación propio:
`ChatViewModel` genera un UUID por pantalla de chat.

**Cómo levantar el entorno de prueba:** igual que en minijuegos (§0.1 de
`docs/minijuegos-diseno.md`). `npm run dev:app` y `npm run test:emulador` necesitan Java en
el `PATH`:
```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"; export PATH="$JAVA_HOME/bin:$PATH"
```

## Contexto

La pestaña Progreso es un placeholder (`feature/progress/ui/ProgressScreen.kt`) y no existen
insignias en ningún lado. El PDF lo exige:

- **CU-08 (Tabla 32, Ilustr. 21):** ver nivel MCER con % de dominio, XP total, racha, gráfica de
  habilidades (radar o barras, RF-12) e insignias (obtenidas a color, pendientes en gris).
  Flujo alterno A1: usuario nuevo → indicadores en cero + mensaje motivacional.
- **RF-05:** perfil con nivel, avatar, puntos y rachas. **RF-14:** insignias, *niveles de
  experiencia* y puntos. **RN-08:** la racha sube con al menos una actividad válida (lección,
  chat o minijuego) por día. **RN-09:** insignias y XP solo al terminar una actividad.
  **RN-14:** el usuario puede ver sus métricas acumuladas. **RNF-10:** funciona sin red.
- **Diseño visual:** Ilustraciones 33 y 34 (encabezado azul con mascota, tarjetas de nivel,
  XP y racha, cuadrícula de 9 insignias). **Modelo de datos** (Ilustr. 25): tablas `insignias`
  (`id, nombre, descripcion, criterio_desbloqueo, icono_url`) y `usuarios_insignias`
  (`id_usuario, id_insignia, fecha_desbloqueo`).

**Lo que hay hoy:**
- Backend: no guarda XP total, racha ni perfil. Solo existen eventos `eventos_actividad`
  (`leccion_completada`, `repaso_completado`, `minijuego_completado`, cada uno con `xp` y
  `fecha`), `progreso_lecciones` (`aprobada`, `mejor_puntaje`) y `srs_conceptos` (`tipo`
  vocab/gramatica/funcion, `aciertos`, `fallos`).
- Android: `UsuarioEntity` tiene `xp`, `racha`, `ultimoDiaActivo` y `avatar` (este nunca se
  escribe). La XP se suma solo cuando el servidor la confirma (lecciones y juegos). La racha
  solo la actualizan los minijuegos. No hay librería de gráficas ni ilustraciones.

## Decisiones

1. **El servidor es la fuente de verdad** de XP, racha e insignias (varios dispositivos,
   anti-trampa). Android guarda en Room la última respuesta para mostrarla sin red.
2. **Documento resumen por usuario** `resumen_usuario/{uid}` que se actualiza dentro de las
   transacciones que ya existen (lección, repaso, minijuego). Así `GET /api/progress` lee
   pocos documentos en vez de sumar todo el historial. Si no existe (usuarios que ya jugaron
   antes de este módulo), se reconstruye una vez a partir de `eventos_actividad`.
   **Regla para no perder historial:** las transacciones de actividad solo *actualizan* un
   resumen que ya existe; si no existe, no lo crean. El resumen solo lo crea la
   reconstrucción, que ya incluye el evento nuevo. Si una transacción lo creara desde
   `resumenVacio`, un usuario con historial (por ejemplo 136 XP) que juega antes de abrir
   Progreso se quedaría con la XP total equivocada.
3. **Insignias evaluadas en el servidor al terminar cada actividad** (RN-09). Nunca se
   revocan. Las respuestas de lección y minijuego devuelven `insignias_nuevas` para festejarlas.
   Las insignias que se otorgan **al reconstruir** el resumen se guardan sin festejo
   (`retroactiva: true`), para no mostrar cinco diálogos de golpe. Solo se festejan las que
   se ganan en vivo.
4. **Habilidades en barras, no radar.** Son 4 ejes, son más legibles y no requieren librería
   (el PDF permite "radar o barras").
5. **Catálogo de insignias empaquetado en la app** (RN-11, RNF-08): se exporta a
   `app/src/main/assets/progreso/insignias.json` con `npm run exportar:insignias`, igual que
   `banco.json` de minijuegos. Así la cuadrícula se dibuja aunque no haya red ni caché.
   El servidor sigue siendo quien decide qué insignias se obtienen.
6. **Las ilustraciones son provisionales:** iconos de Material dentro de círculos de color,
   hasta tener el arte final (`res/drawable/insignia_<id>.png`). Mascota: placeholder en el
   encabezado.

## Reglas del módulo

**Niveles de experiencia (RF-14):** el nivel *n* requiere `50·n·(n−1)` XP acumulada, así que
cada nivel pide 100 XP más que el anterior: N1=0, N2=100, N3=300, N4=600, N5=1000… Se muestra
en la tarjeta de XP como "Nivel 4 · 120/400 para el siguiente". Es independiente del nivel MCER.

**Racha (RN-08):** días distintos con actividad, en hora de México (se reutiliza `diaLocal` de
`backend/lib/minijuegos/repositorio.js`). Si el último día activo fue antes de ayer, la racha
mostrada es 0. El resumen guarda `racha_actual`, `racha_maxima`, `ultimo_dia` y los últimos
14 `dias_activos`.

**Dominio del nivel:** lecciones aprobadas del nivel actual ÷ total de lecciones de ese nivel
(12 en A1 y 12 en A2, con `listarLeccionesDeNivel`).

**Habilidades (barras de 0 a 100 %, o "Sin datos" si no hay suficientes):**

| Habilidad | Fuente |
|---|---|
| Vocabulario | `srs_conceptos` tipo `vocab` + precisión de Ráfaga |
| Gramática | `srs_conceptos` tipo `gramatica` + precisión de Orden Maestro |
| Pronunciación | precisión de Eco Vocal |
| Comunicación | `srs_conceptos` tipo `funcion` + precisión de Misión Situacional |

Cada valor es la precisión acumulada `aciertos / (aciertos + fallos)`, sumando ambas fuentes.

**Insignias (las 9 de la Ilustración 34):**

| id | Nombre | Criterio |
|---|---|---|
| `primer-logro` | Primer Logro | Terminar la primera actividad válida |
| `habla-con-ia` | Habla con IA | Primera conversación con el chatbot (ver Fase 2) |
| `estrella-minijuego` | Estrella del Minijuego | 100 % de precisión en una partida |
| `racha-7` | Racha de 7 Días | `racha_actual` ≥ 7 |
| `explorador-a1` | Explorador A1 | Aprobar las 12 lecciones A1 |
| `graduado-a2` | Graduado A2 | Aprobar las 12 lecciones A2 |
| `maestro-speaking` | Maestro Speaking | 10 partidas de Eco Vocal con precisión ≥ 80 % |
| `jugador-experto` | Jugador Experto | Jugar los 4 minijuegos y 25 partidas en total |
| `desafio-semanal` | Desafío Semanal | Actividad en 5 días distintos de una misma semana (lun–dom) |

Cada insignia expone `progreso` (por ejemplo 3/7) para que la tarjeta bloqueada diga cuánto falta.

## Fase 1 — Backend: reglas puras (`backend/lib/progreso/`)

- `niveles.js`: `nivelPorXp(xp)` devuelve `{nivel, xpEnNivel, xpParaSiguiente}`, y
  `subioDeNivel(xpAntes, xpDespues)` sirve para festejar el cambio de nivel.
- `racha.js`: `registrarDia(resumen, dia)` y `rachaVigente(resumen, hoy)`.
- `insignias.js`: `CATALOGO` (id, nombre, descripción, criterio en texto, icono) y
  `evaluar(resumen)`, que devuelve los ids nuevos y el progreso de cada insignia.
- `resumen.js`:
  - `resumenVacio(uid)`.
  - `aplicarActividad(resumen, actividad, ahora)`, función pura. `actividad` es
    `{tipo, xp, juego?, aciertos?, total?, leccionId?, nivel?, aprobada?}`. Suma XP, día,
    conteos por juego y lecciones aprobadas por nivel, evalúa insignias y devuelve
    `{resumen, insigniasNuevas, nivelNuevo?}`.
  - `reconstruirResumen(uid, eventos, progresos)`: marca las insignias que otorga como
    `retroactiva: true`.
- Pruebas: `backend/test/progreso.test.js` (reglas puras, casos de borde de racha con cambio
  de día en hora de México, cada criterio de insignia).

## Fase 2 — Backend: integración y endpoint

- **Colección nueva** `resumen_usuario/{uid}`:
  `{uid, xp_total, racha_actual, racha_maxima, ultimo_dia, dias_activos[], lecciones_aprobadas:{A1:[],A2:[]}, juegos:{[juego]:{partidas, aciertos, total, eco_80?}}, conteos:{repasos, chats}, insignias:{[id]: fecha}, version}`.
- **Engancharlo en las tres transacciones.** Agregar la ref del resumen a su `getAll` (en
  Firestore las lecturas van antes que las escrituras), llamar `aplicarActividad` y hacer
  `tx.set` **solo si el resumen ya existe** (Decisión 2). Agregar a la respuesta
  `insignias_nuevas` y `nivel_xp_nuevo` (opcionales):
  - `registrarIntento` en `backend/lib/lecciones/repositorio.js` (solo cuando el intento
    aprueba).
  - `registrarRepaso` en el mismo archivo.
  - `registrarPartida` en `backend/lib/minijuegos/repositorio.js`.
  - Las respuestas idempotentes (repetidas) no vuelven a aplicar nada.
- **`GET /api/progress`** (`backend/api/progress/index.js` con `crearEndpoint`; el servidor
  local lo descubre solo):
  - Lee el resumen, o lo reconstruye y guarda si no existe. Esto va en una transacción,
    para no pisar una actividad que se registre al mismo tiempo.
  - Lee `progreso_lecciones` y `srs_conceptos` del uid y recibe `?level=` con `validarNivel`.
  - Devuelve
    `{nivel, dominio:{aprobadas,total,porcentaje}, xp:{total,nivel,xp_en_nivel,xp_para_siguiente}, racha:{actual,maxima}, habilidades:[{id,nombre,porcentaje|null}], insignias:[{id,nombre,descripcion,criterio,icono,obtenida,fecha?,retroactiva?,progreso:{actual,meta}}], sin_actividad}`.
  - Responde con **ETag** usando `responderConEtag` de `backend/lib/http.js`, para no
    descargar el progreso completo cada vez que se entra a la pestaña.
- **`POST /api/progress/chat`** con body `{sesion_id}`, idempotente: la app lo llama al
  recibir la primera respuesta de la IA en una conversación. Registra el evento
  `chat_completado` (0 XP), cuenta para la racha (RN-08) y desbloquea "Habla con IA". Va aquí
  porque el backend del chat vive en otro proyecto. Límite: 20 por día.
- Pruebas con el Firestore falso:
  - `GET` con y sin datos, reconstrucción e insignias nuevas.
  - Que las transacciones existentes escriban el resumen (extender
    `backend/test/minijuegos.test.js` y las pruebas de lecciones).
- **Script `npm run migrar:resumen`** (`backend/scripts/migrar-resumen.js`): genera el
  resumen de todos los usuarios con actividad (con `reconstruirResumen`), es idempotente y
  se corre antes de desplegar.
- **Script `npm run exportar:insignias`** (`backend/scripts/exportar-insignias.js`): escribe
  `app/src/main/assets/progreso/insignias.json` a partir de `CATALOGO` (id, nombre,
  descripción, criterio e icono).
- `backend/README.md`: sección "Módulo de Progreso", que incluye el orden de despliegue.

## Fase 3 — Android: datos (`feature/progress/data/`)

- `ProgresoApi` (`GET api/progress`, `POST api/progress/chat`) en el retrofit
  `@Named("lecciones")` de `core/di/NetworkModule.kt`, más DTOs con `@SerializedName`.
- Room v5 con `MIGRACION_4_5`: tabla `progreso_cache(uid PK, nivel, json, actualizadoEn)`,
  con el mismo patrón que `catalogos_cache`. Registrarla en `core/di/DatabaseModule.kt`.
- `ProgresoRepository`:
  - `observar(uid): Flow<ProgresoGuardado?>` desde la caché.
  - `refrescar()`: red, luego caché, y sincroniza `UsuarioEntity` (`xp`, `racha`) con el
    servidor mediante el nuevo `UsuarioDao.actualizarProgreso(uid, xp, racha)`. Corrige la
    deriva local. Hay que actualizar `FakeUsuarioDao` en `AccountViewModelTest`.
  - **Regla de sincronización de la racha:** la racha del servidor solo sobrescribe la local
    si no hay partidas ni intentos pendientes en los outbox (`resultados_minijuego` sin
    sincronizar, `intentos_pendientes`). Si los hay, se queda la local, para no borrar lo
    hecho sin conexión. La XP sí se sobrescribe siempre, porque la local solo suma XP ya
    confirmada.
  - Manda `If-None-Match` con el ETag guardado; si recibe 304, solo marca la caché como
    fresca.
  - `registrarChat(sesionId)`: se intenta una vez y, si falla, se ignora (no bloquea el chat).
- Sin red y sin caché: se arma el estado mínimo con `UsuarioEntity` (nivel, XP, racha) y las
  insignias en gris, tomadas del catálogo empaquetado `assets/progreso/insignias.json`
  (lo lee `CatalogoInsigniasLocal`, con el mismo patrón que `BancoLocal` de minijuegos).
- La tabla `progreso_cache` guarda también el `etag`.

## Fase 4 — Android: UI (`feature/progress/ui/`)

- `ProgressViewModel` (patrón de `AccountViewModel`): estados `Cargando`,
  `Listo(datos, desactualizado)`, `SinActividad` y `Error`. Refresca al entrar y con
  *pull-to-refresh*.
- `ProgressScreen`, en columna con scroll, siguiendo las Ilustraciones 33 y 34:
  - Encabezado `XPBlueLight` con mascota provisional y el título "Tu perfil: Progreso y logros".
  - **TarjetaNivel:** chip "A1", "Nivel actual: A1 (Principiante)", barra naranja
    (`XPOrangeAccent`) y "Dominio general: X %".
  - **TarjetaXp:** moneda, "2500 XP", "Puntos XP totales" y "Nivel N · a/b para el siguiente".
  - **TarjetaRacha:** 🔥, "N días", "Racha de días consecutivos" y la racha máxima.
  - **Habilidades:** 4 `LinearProgressIndicator` con su porcentaje, o "Sin datos — juega X".
  - **Insignias obtenidas:** cuadrícula de 3 columnas (filas con `chunked(3)`, no un lazy
    grid dentro del scroll). Las bloqueadas van en escala de grises (`ColorFilter` con
    saturación 0) y con un candado. Al tocar una se abre un diálogo con descripción, criterio
    y progreso.
  - **A1 Sin actividad:** todo en cero, mensaje motivacional y botón "Haz tu primera lección".
  - **Datos viejos:** aviso "Sin conexión · datos de hace X".
  - **Accesibilidad y compatibilidad (RNF-01, RNF-07):**
    - Cada insignia lleva un `contentDescription` como "Racha de 7 días, bloqueada, 3 de 7".
    - El estado bloqueado se marca con gris *y* candado, nunca solo con el color.
    - Los números grandes respetan el tamaño de fuente del sistema.
    - Se prueba en una pantalla chica (360 dp de ancho) y con fuente al 130 %.
- Barra inferior: cambiar el ícono de Progreso a `Icons.Filled.EmojiEvents` en
  `core/navigation/XPSpeakNavHost.kt`.
- Componente público `InsigniaNueva` (diálogo de festejo) para usar en el resultado de lección
  (`lessons/ui/components/ResultadoIntento.kt`) y de minijuego
  (`games/ui/comun/PantallaPartida.kt`) cuando la respuesta trae `insignias_nuevas`. Las
  `retroactiva` no se festejan.
- **Subida de nivel de XP:** si la respuesta trae `nivel_xp_nuevo`, el mismo resultado
  muestra "¡Subiste a nivel N!" junto con las insignias nuevas.

## Fase 5 — Racha unificada y chat

- Mover `games/domain/Racha.kt` y `MinijuegosRepository.registrarActividad` a un
  `core/actividad/RegistroActividad` compartido. Llamarlo también al aprobar una lección
  (`LeccionesRepository`) y al recibir la primera respuesta del chat (`ChatViewModel`, que
  además llama a `registrarChat`). De paso se corrige "Racha: 1 días" en `AccountScreen`.

## Orden de despliegue y compatibilidad

1. Desplegar primero el backend: el endpoint nuevo y las transacciones con resumen.
2. Correr `npm run migrar:resumen` contra producción.
3. Publicar la app. Los campos nuevos de las respuestas (`insignias_nuevas`,
   `nivel_xp_nuevo`) son opcionales, así que una app vieja sigue funcionando contra el
   backend nuevo. Una app nueva contra un backend viejo ve "Sin conexión" en Progreso y nada
   más.

## Fuera de alcance (otros módulos)

- Avatar (RF-05): se edita en Ajustes de perfil (Ilustración 35, CU-04/CU-10). Aquí solo se
  muestra si existe.
- Notificaciones de racha (CU-09).
- Eliminación de datos (CU-10). Cuando se haga, debe borrar también `resumen_usuario`.

## Archivos clave

- **Backend nuevos:** `backend/lib/progreso/{niveles,racha,insignias,resumen,repositorio}.js`,
  `backend/api/progress/index.js`, `backend/api/progress/chat.js`,
  `backend/scripts/{migrar-resumen,exportar-insignias}.js`, `backend/test/progreso.test.js`.
- **Backend modificados:** `backend/lib/lecciones/repositorio.js`,
  `backend/lib/minijuegos/repositorio.js`, `backend/README.md`.
- **Android nuevos:** `feature/progress/data/*` (incluye `CatalogoInsigniasLocal`),
  `feature/progress/ui/*`, `core/actividad/RegistroActividad.kt` y
  `app/src/main/assets/progreso/insignias.json`.
- **Android modificados:** `core/data/local/AppDatabase.kt`, `core/di/DatabaseModule.kt`,
  `core/di/NetworkModule.kt`, `feature/auth/data/UsuarioDao.kt`,
  `core/navigation/XPSpeakNavHost.kt`, `feature/games/data/MinijuegosRepository.kt`,
  `feature/lessons/data/LeccionesRepository.kt`, `feature/chat/ui/ChatViewModel.kt`.

## Verificación

1. `cd backend && npm test`: todas las pruebas verdes, incluidas las nuevas de progreso.
   Casos obligatorios:
   - Una partida de un usuario con historial y sin resumen no crea un resumen vacío, y el
     `GET` posterior reconstruye la XP correcta.
   - La migración es idempotente.
   - El `GET` responde 304 con el ETag correcto.
2. Android:
   - `sh gradlew testDebugUnitTest` (con `JAVA_HOME` del JBR de Android Studio).
   - Pruebas nuevas: `ProgresoReglasTest` (nivel por XP, mapeo de DTOs), `ProgresoCacheTest`
     (ida y vuelta con Gson) y `ProgressViewModelTest` (con fakes, incluida la regla de
     sincronización de la racha con outbox pendiente).
   - **`ProgresoContratoTest`**, con el patrón de `LeccionesContratoTest`: deserializa un
     JSON real de `GET /api/progress` y del resultado con `insignias_nuevas`, para que
     Android y el backend no se desfasen. También valida que `assets/progreso/insignias.json`
     tenga las 9 insignias.
3. Emulador (`npm run dev:app` + `adb reverse tcp:3000 tcp:3000`) con la cuenta de prueba:
   - La pestaña Progreso muestra la XP y la racha que ya tiene (la reconstrucción desde
     eventos funciona).
   - Jugar una partida al 100 % debe festejar "Estrella del Minijuego" y mostrarla a color en
     Progreso.
   - Modo avión: Progreso sigue mostrando los datos en caché con el aviso. Con la caché
     borrada, se ven las 9 insignias en gris desde el catálogo empaquetado.
   - Las insignias que la cuenta ya merecía aparecen a color, sin diálogos de festejo.
   - Pantalla chica y fuente grande: nada se corta.
   - Cuenta nueva: indicadores en cero y mensaje motivacional (A1).
4. Al terminar: actualizar `docs/progreso-diseno.md` con el estado (como en el §0 de
   minijuegos), commit y push de `feature/progreso`.
