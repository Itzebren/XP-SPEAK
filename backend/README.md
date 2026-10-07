# XP-SPEAK — Backend serverless (Vercel)

Incluye tres módulos:
- **Recuperación de acceso (RF-03)** — esta sección.
- **Lecciones (RF-10, RF-12, RF-13, RF-14)** — ver [Módulo de Lecciones](#módulo-de-lecciones) más abajo.
- **Minijuegos (RF-11)** — ver [Módulo de Minijuegos](#módulo-de-minijuegos) al final.

## Recuperación de acceso (RF-03)

Dos endpoints serverless para el flujo de "olvidé mi contraseña":
- `POST /api/send-reset-code` — genera un código de 6 caracteres y lo envía por correo
- `POST /api/verify-reset-code` — valida el código y actualiza la contraseña en Firebase Auth

No necesitas Next.js ni ningún framework: Vercel detecta automáticamente
cualquier archivo dentro de `api/` como un endpoint.

## 1. Dónde vive

Este backend vive en la carpeta `backend/` del repositorio principal
**XP-SPEAK**, junto a la app de Android (`app/`). Ya no existe un repositorio
aparte: todo cambio del backend se hace y se sube aquí, en XP-SPEAK.

## 2. Proyecto en Vercel

1. Entra a vercel.com → "Add New..." → "Project" (o, si ya tienes el proyecto
   `xp-speak-auth-backend`, ve a Settings → Git y conéctalo al repositorio
   `XP-SPEAK`)
2. Selecciona el repositorio `XP-SPEAK`
3. En **Root Directory** escribe `backend` (Settings → Build and Deployment).
   Es lo único que cambia respecto a un repo independiente: Vercel solo mira
   esta carpeta y detecta los endpoints de `api/` automáticamente. Deja el
   build en blanco/default
4. **No le des "Deploy" todavía** — primero hay que agregar las variables
   de entorno (siguiente paso), o el primer deploy va a fallar porque le
   faltan las credenciales

## 3. Variables de entorno en Vercel

En la pantalla de configuración del proyecto (o después, en Settings →
Environment Variables), agrega dos:

**`RESEND_API_KEY`** — pega la API key que copiaste de Resend.

**`FIREBASE_SERVICE_ACCOUNT_BASE64`** — es el archivo .json completo que
descargaste de Firebase, pero convertido a una sola línea en Base64
(las variables de entorno no aceptan bien archivos JSON multilínea tal cual).

Para convertirlo, abre PowerShell donde tengas el archivo descargado y corre:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("ruta\al\archivo-service-account.json")) | Set-Clipboard
```

Esto lo copia directo a tu portapapeles — solo pégalo como valor de la
variable en Vercel.

## 4. Deploy

Con las dos variables agregadas, dale "Deploy". Cuando termine, Vercel te
da una URL tipo `https://xp-speak-auth-backend.vercel.app` — esa es la
que vamos a usar desde la app de Android en el siguiente paso.

## 5. Probar que funciona (sin la app todavía)

Puedes probar el primer endpoint directo desde PowerShell:

```powershell
Invoke-RestMethod -Uri "https://TU-PROYECTO.vercel.app/api/send-reset-code" `
  -Method Post -ContentType "application/json" `
  -Body '{"correo":"tu-correo-de-prueba@gmail.com"}'
```

Si todo está bien configurado, deberías recibir el correo con el código
en unos segundos, y la respuesta en PowerShell debería ser el mensaje
genérico de éxito.

## Nota de seguridad

El archivo `.json` de Service Account que descargaste de Firebase le da
acceso administrativo TOTAL a tu proyecto (puede leer/escribir cualquier
dato, crear/borrar usuarios, etc.). Nunca lo subas a GitHub, ni siquiera
a un repositorio privado — solo debe vivir como variable de entorno en
Vercel. Este repo ya lo protege por si acaso: revisa que el `.gitignore`
no lo esté ignorando por error si algún día lo copias localmente aquí.

---

# Módulo de Lecciones

Implementa el corte vertical de `docs/lecciones-diseno.md`: contenido curado
en JSON + evaluación corta calificada en servidor (70%, RN-06) + XP atómico e
idempotente (RN-09) + registro de desempeño por concepto para el SRS (RF-13).

Contenido: el temario completo de la guía de autoría §2, **12 lecciones A1 y
12 lecciones A2**, cada nivel como una cadena propia de prerequisitos (RN-02).

## Estructura

```
content/
  lessons/<id>.json          ← una lección (esquema §4.2); única fuente de verdad del contenido
  minijuegos/misiones/<id>.json ← un guion de Misión Situacional
  schema/leccion.schema.json ← JSON Schema que valida cada lección
  schema/mision.schema.json  ← JSON Schema de las misiones
api/                         ← un archivo por endpoint; solo su lógica propia
  lessons/index.js           GET  /api/lessons?level=A1
  lessons/manifest.js        GET  /api/lessons/manifest        (público)
  lessons/[id].js            GET  /api/lessons/:id
  lessons/[id]/attempt.js    POST /api/lessons/:id/attempt
  lessons/[id]/progress.js   POST /api/lessons/:id/progress
  srs/review.js              GET  /api/srs/review[?level=A1&limit=20]   conceptos débiles
  srs/session.js             GET  /api/srs/session[?level=A1]         sesión de repaso de hoy
                             POST /api/srs/session                    respuestas del repaso
  games/result.js            POST /api/games/result                   partida terminada de un minijuego
  speech/token.js            GET  /api/speech/token                   token temporal de Azure AI Speech
lib/
  endpoint.js     envoltura común: método HTTP, token y errores
  errores.js      ErrorApi + respuesta estándar { error, codigo }
  http.js         ETag / 304
  lecciones/
    contenido.js    carga de JSON, manifiesto e índice de conceptos (en memoria)
    calificador.js  calificación pura (misma lógica que replicará Android offline)
    estado.js       máquina de estados, desbloqueo y XP atómico
    srs.js          acumulación por concepto, conceptos débiles y selección del repaso
    sm2.js          algoritmo SM-2 (intervalo, facilidad, próxima revisión)
    repaso.js       ejercicios de repaso sacados de las evaluaciones, por concepto
    limite.js       rate limiting de intentos
    validacion.js   validación de entrada (body, query, :id)
    repositorio.js  Firestore: solo lectura/escritura, en transacciones
  minijuegos/
    contenido.js    banco de contenido de los juegos (sale de las lecciones + misiones)
    xp.js           XP por partida (misma fórmula que CalculadorXp.kt)
    validacion.js   validación del resultado de una partida
    repositorio.js  Firestore: partida idempotente, tope diario, SRS y evento de actividad
  speech/azure.js   token de Azure AI Speech (la llave solo vive aquí)
scripts/
  lint-content.js   linter de contenido (una función por regla; lecciones y misiones)
  exportar-banco-minijuegos.js  genera app/src/main/assets/minijuegos/banco.json
  servidor-local.js API local que imita el ruteo de Vercel (desarrollo)
```

Las reglas de negocio (estado, srs, sm2, repaso, limite, calificador) son funciones puras
sin Firestore, así se prueban sin emuladores. Un endpoint nuevo se escribe así:

```js
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'mi-endpoint' }, async ({ req, res, uid, db }) => {
  // …solo la lógica del endpoint; errores con `throw new ErrorApi(status, codigo, mensaje)`
});
```

## Autenticación y errores

Todos los endpoints (salvo el manifiesto) exigen el ID token de Firebase del
usuario: `Authorization: Bearer <idToken>` (en Android:
`FirebaseAuth.getInstance().currentUser.getIdToken(false)`). El `uid` sale del
token verificado; nunca del body.

Errores: `{ "error": "mensaje legible", "codigo": "LECCION_BLOQUEADA" }`

| HTTP | `codigo` |
|---|---|
| 400 | `ENTRADA_INVALIDA` |
| 401 | `TOKEN_AUSENTE`, `TOKEN_INVALIDO` |
| 403 | `LECCION_BLOQUEADA` |
| 404 | `LECCION_NO_ENCONTRADA` |
| 405 | `METODO_NO_PERMITIDO` |
| 409 | `ATTEMPT_ID_REUTILIZADO` (el mismo `attempt_id` en otra lección), `PARTIDA_ID_REUTILIZADO` |
| 429 | `DEMASIADOS_INTENTOS` (más de 10 intentos/min en una lección), `DEMASIADAS_PARTIDAS`, `DEMASIADOS_TOKENS` |
| 500 | `ERROR_INTERNO` |
| 502 | `SPEECH_NO_DISPONIBLE` (Azure no respondió) |
| 503 | `SPEECH_NO_CONFIGURADO` (faltan `AZURE_SPEECH_KEY` / `AZURE_SPEECH_REGION`) |

## Contrato del intento

```jsonc
// POST /api/lessons/a1-saludos-presentaciones/attempt
{ "attempt_id": "<UUID generado en Android>",
  "respuestas": [ { "id": "q1", "valor": 1 },                       // opcion_multiple: índice
                  { "id": "q2", "valor": "is" },                    // completar: texto
                  { "id": "q6", "valor": [["Hello!", "¡Hola!"]] } ] } // emparejar: pares [izq, der]

// 200
{ "attempt_id": "…", "leccion_id": "…", "puntaje": 0.8333, "correctas": 5, "total": 6,
  "umbral_aprobacion": 0.7, "aprobada": true, "estado": "completada", "mejor_puntaje": 0.8333,
  "xp_ganado": 50, "desbloqueada_siguiente": "a1-informacion-personal",
  "feedback": [ { "id": "q2", "correcta": false, "mensaje": "…", "respuesta_correcta": "is" } ],
  "conceptos_debiles": ["gram.verb_to_be_present"], "sugerencia": null, "repetido": false }
```

- Reenviar el mismo `attempt_id` (reintento de red, outbox offline) devuelve la
  respuesta original con `"repetido": true` y **no** vuelve a escribir nada.
- `xp_ganado` solo es > 0 la primera vez que se aprueba la lección.
- `desbloqueada_siguiente` solo viene cuando esta aprobación desbloqueó algo nuevo.
- `sugerencia: "repetir_leccion"` cuando no se alcanza el 70% (CU-06 A3).
- `GET /api/lessons/:id` y el manifiesto mandan `ETag`; Android puede revalidar
  su caché con `If-None-Match` y recibir `304`.

## Colecciones de Firestore

| Colección | Doc id | Para qué |
|---|---|---|
| `progreso_lecciones` | `<uid>_<leccion_id>` | estado, intentos, mejor/último puntaje, `xp_otorgado`, avance de teoría |
| `srs_conceptos` | `<uid>_<concepto_id>` | aciertos, fallos, `tasa_error`, `ultima_vista` y SM-2: `intervalo`, `ease`, `repeticiones`, `proxima_revision` |
| `intentos_lecciones` | `<uid>_<attempt_id>` | idempotencia: respuesta ya calculada de cada intento |
| `intentos_repaso` | `<uid>_<attempt_id>` | idempotencia de las sesiones de repaso |
| `eventos_actividad` | automático | `leccion_completada` (con XP), `repaso_completado` (sin XP) y `minijuego_completado`, para racha (RN-08) y gamificación |
| `partidas_minijuego` | `<uid>_<partida_id>` | idempotencia: respuesta ya calculada de cada partida |
| `minijuegos_diario` | `<uid>_<AAAA-MM-DD>` | partidas del día por juego (hora de México), para el tope de XP |
| `limite_speech` | `<uid>` | ventana de rate limiting de tokens de voz |

Las fechas se guardan en milisegundos epoch (igual que `password_reset_codes`).
Solo se usan consultas `where('uid', '==', …)`, así que **no hace falta crear
índices compuestos**.

## Agregar o editar una lección

> Guía completa (plan curricular, tamaños, XP, estilo, fuentes y plantilla):
> [`docs/guia-autoria-lecciones.md`](docs/guia-autoria-lecciones.md).

1. Crea/edita `content/lessons/<id>.json` siguiendo el esquema (copia una
   existente como plantilla). Si editas una existente, **sube `version`** (así
   Android sabe que su caché quedó vieja) y **no cambies los `concepto_id`**
   (perderías el historial SRS de los usuarios).
2. `npm run lint:content` — valida el schema y las reglas del §4.5 (clave de
   respuesta coherente, ids únicos, prerequisitos sin ciclos, convención de
   `concepto_id`, y que no se evalúe nada que no se haya enseñado en la lección
   o en sus prerequisitos).

No hay más pasos: el manifiesto, los desbloqueos y el índice de conceptos se
calculan solos a partir de los JSON.

## Pruebas

```bash
cd backend            # todos los comandos npm se corren dentro de backend/
npm install
npm test               # unitarias + API completa con Firestore en memoria + contenido (clave de cada lección, cadenas A1/A2)
npm run lint:content
npm run test:emulador  # integración con firebase-admin real contra emuladores (requiere Java 11+)
```

Para probar a mano con curl sin tocar producción:

```bash
npm run dev:emulador   # emuladores + API local en http://localhost:3000
# en otra terminal: crea un usuario en el emulador y corre la colección
TOKEN=$(curl -s -X POST 'http://127.0.0.1:9099/identitytoolkit.googleapis.com/v1/accounts:signUp?key=x' \
  -H 'Content-Type: application/json' \
  -d '{"email":"prueba@xpspeak.com","password":"secreta123","returnSecureToken":true}' | node -pe 'JSON.parse(require("fs").readFileSync(0)).idToken')
TOKEN=$TOKEN ./scripts/probar-lecciones.sh
```

Contra el despliegue real (usa un usuario de prueba, escribe en Firestore):

```bash
TOKEN=$(FIREBASE_WEB_API_KEY=... node scripts/obtener-token.js prueba@correo.com contraseña)
BASE_URL=https://xp-speak-auth-backend.vercel.app TOKEN=$TOKEN ./scripts/probar-lecciones.sh
```

## Decisiones tomadas sobre lo que el diseño dejaba abierto

- **Nivel del catálogo:** el perfil (A1/A2) hoy vive solo en Room, así que la
  app manda `?level=` y el servidor lo valida (RN-02).
- **Manifiesto público**, sin token: no tiene datos de usuario y permite revisar
  la caché antes de iniciar sesión. El resto exige token.
- **`emparejar`:** para el puntaje es un solo ítem (correcto solo si todos los
  pares lo están, §4.3); para el SRS la señal es **por par**, porque cada par es
  un concepto distinto. Por eso el ejemplo "5/6" del §6.1 no aplica literal.
- **Ítems sin responder:** cuentan como incorrectos para el puntaje, pero no
  generan señal SRS (no sabemos si el concepto es débil).
- **Repetir una lección completada** registra el intento y el evento de
  actividad (sirve para racha), mejora `mejor_puntaje`, pero da 0 XP; reprobar un
  repaso no la regresa a "reprobada".
- **`concepto_id`** sigue la convención del §4.4 (`voc.<tema>.<lema>`); el
  ejemplo del §4.2 (`voc.hello`) no la cumple y el linter lo rechazaría.
- **Conceptos débiles:** al menos un fallo y `tasa_error ≥ 0.3`, ordenados por
  tasa de error. Se muestran en el catálogo; la sesión de repaso usa SM-2.
- **SM-2 sin botones de dificultad:** las respuestas se califican solas, así que
  la calidad sale del resultado: acierto = 4 ("bueno"), fallo = 1. Aplica a las
  lecciones y al repaso. Acertar antes de la fecha (repetir la lección el mismo
  día) no alarga el intervalo; fallar siempre lo reinicia a 1 día.
- **Sesión de repaso:** hasta 10 conceptos vencidos, primero los de mayor tasa
  de error (RN-07). Cada uno usa un ítem real de alguna evaluación; un par de
  "emparejar" se pregunta como opción múltiple. No da XP (no se puede farmear),
  pero cuenta como actividad para la racha.
- **XP total del usuario:** este módulo no lo acumula; emite `eventos_actividad`
  con el XP de cada aprobación para que Gamificación lo consuma (y la respuesta
  del intento trae `xp_ganado` para que Android actualice su perfil local).

---

# Módulo de Minijuegos

Diseño completo: [`../docs/minijuegos-diseno.md`](../docs/minijuegos-diseno.md).
Los cuatro juegos (Eco Vocal, Misión Situacional, Ráfaga de Palabras y Orden
Maestro) se juegan y califican **en el teléfono** (CU-07 "opera de forma
local"); el backend solo registra el resultado y confirma la XP.

## Contenido de los juegos

El contenido sale de las lecciones (ya revisadas contra el MCER, RN-03) más
los guiones de `content/minijuegos/misiones/`. Se empaqueta dentro del APK
(RNF-08 / RN-11):

```bash
npm run lint:content         # valida lecciones y misiones
npm run exportar:minijuegos  # regenera app/src/main/assets/minijuegos/banco.json
```

Después de cambiar una lección o una misión hay que volver a exportar; `npm test`
falla si el banco del APK quedó viejo. Reglas de una misión (linter): una sola
opción correcta por paso, feedback en cada incorrecta, cada objetivo se cumple
exactamente una vez, solo conceptos ya enseñados en su lección o antes, frases de
máximo 14 palabras y fuentes MCER como en las lecciones.

## Contrato del resultado

```jsonc
// POST /api/games/result
{ "partida_id": "<UUID generado en Android>", "juego": "orden-maestro",
  "aciertos": 7, "total": 8, "duracion_ms": 94000,
  "conceptos": [ { "concepto_id": "gram.some_any", "aciertos": 2, "errores": 1 } ] }

// 200
{ "partida_id": "…", "juego": "orden-maestro", "aciertos": 7, "total": 8, "precision": 0.875,
  "xp_ganado": 13, "xp_reducida": false, "partidas_hoy": 1, "repetido": false }
```

- La XP la calcula el servidor (`lib/minijuegos/xp.js`), no se confía en la del
  cliente: `round(xpBase · precisión) + bono de rapidez (0–5, solo con ≥ 70%)`.
  `xpBase` es 10 en Ráfaga y Orden, 15 en Misión y Eco Vocal.
- Desde la 6.ª partida del mismo juego en el día la XP va a la mitad; con 60
  partidas en el día se responde 429.
- Reenviar el mismo `partida_id` devuelve la respuesta original con `"repetido": true`.
- Los aciertos/errores por concepto actualizan `srs_conceptos` (RF-13) igual que
  una lección; los conceptos que ya no existen se ignoran.

## Eco Vocal y Azure AI Speech

`GET /api/speech/token` devuelve `{ token, region, expira_en }`: un token de
Azure de 10 minutos para que la app evalúe la pronunciación en streaming (el
audio no pasa por este backend ni se guarda, RN-04). Variables de entorno en
Vercel:

- **`AZURE_SPEECH_KEY`** — llave del recurso *Speech* de Azure (la capa F0 gratuita sirve para pruebas).
- **`AZURE_SPEECH_REGION`** — región del recurso, p. ej. `southcentralus`.

Sin esas variables el endpoint responde 503 y la app usa el reconocimiento de
voz del teléfono como respaldo (sin puntaje fonético). Límite: 10 tokens por
usuario por hora.

