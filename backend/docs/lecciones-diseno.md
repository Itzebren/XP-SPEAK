# Diseño y alcance del módulo de **Lecciones** — XP-SPEAK

> Backend de contenido (Node/Vercel) · Rama `feature/lecciones`
> Trabajo Terminal 2026-B162 · Documento de diseño previo a la implementación
> Estado: **v4 — temario completo A1/A2, revisado frente al MCER** (actualizado 2026-09-29)

Este documento define la lógica, el alcance y la mejor forma de implementar el
apartado de **Lecciones** de XP-SPEAK. Se escribió antes del código y se
actualizó después para reflejar lo implementado. Se apoya en el Documento
Técnico del proyecto (RF/RN/CU) y en fuentes autorizadas del MCER (ver §9).

> **Fuente única del contenido: el MCER.** La revisión de todo lo planteado
> frente al MCER está en [`revision-mcer.md`](revision-mcer.md).
>
> **¿Vas a escribir lecciones?** Usa la
> [Guía de autoría](guia-autoria-lecciones.md): plan curricular A1/A2, reglas
> de tamaño y XP, estilo, validaciones y plantilla. Este documento explica el
> diseño; la guía explica cómo producir el contenido.

## 0. Estado actual de la implementación

| Pieza | Estado | Dónde |
|---|---|---|
| Esquema de contenido + linter | ✅ | `content/schema/`, `scripts/lint-content.js` |
| 3 lecciones A1 (corte vertical, §7) | ✅ | `content/lessons/` |
| Temario completo: 12 lecciones A1 + 12 A2 | ✅ | `content/lessons/`, plan en la guía de autoría §2 |
| Revisión frente al MCER (RN-03 por nivel) + regla del linter | ✅ | `docs/revision-mcer.md`, regla `fuentesMcer` |
| Endpoints (§6) con token, errores, ETag, rate limiting | ✅ | `api/lessons/`, `api/srs/` |
| Calificación en servidor, XP atómico e idempotente, desbloqueo | ✅ | `lib/lecciones/` |
| Registro por concepto y conceptos débiles (base SRS) | ✅ | `srs_conceptos`, `GET /api/srs/review` |
| Pruebas: unitarias, API y emuladores | ✅ | `test/`, `scripts/probar-lecciones.sh` |
| UI Android (catálogo, teoría, evaluación, resultado) | ✅ | `XP-SPEAK/app/.../feature/lessons/` |
| **Caché offline en Room** (§3, RN-11) | ❌ pendiente | Android pide todo al servidor en cada pantalla |
| **Calificación local + outbox** (§6.2) | ❌ pendiente | Hoy solo califica el servidor |
| **Algoritmo SM-2** (intervalos) | ❌ pendiente (fuera de alcance, §2.2) | — |
| **CI** que corra el linter antes de desplegar (§4.5) | ❌ pendiente | Hoy se corre a mano |

---

## 1. Decisiones que fijan este diseño

| # | Decisión | Elección |
|---|----------|----------|
| 1 | ¿Dónde vive el trabajo? | **Backend de contenido** (este repo Node/Vercel) que sirve catálogo + contenido y sincroniza progreso; Android lo consume y cachea. |
| 2 | Alcance de esta iteración | **Contenido + evaluación corta (umbral 70%) + registrar desempeño para alimentar el SRS.** No se implementa aún el algoritmo SRS completo. |
| 3 | Origen del contenido | **Autoría propia acotada por el MCER**, fuente única: temas del MCER §4.2 y can-do respaldados por descriptores del nivel (ver [`revision-mcer.md`](revision-mcer.md)). |
| 4 | Cobertura | **Corte vertical de 2–3 temas A1** atravesando todo el flujo (contenido → evaluación → XP → engancha SRS), para validar la arquitectura antes de escalar. |

---

## 2. Alcance de esta iteración

### 2.1 Qué SÍ entra
- Modelo de **contenido curricular** (esquema JSON de una lección) alineado al MCER.
- **Backend de contenido**: endpoints para catálogo, contenido de lección,
  manifiesto de versiones (para caché offline) y envío/calificación de evaluación.
- **Calificación en servidor** de la evaluación corta, con la regla de **70%**
  (RN-06) y cálculo de **XP atómico** (RN-09).
- **Registro de desempeño por concepto** que deja lista la base para el SRS
  (RF-13/RN-07), sin ejecutar todavía el algoritmo de intervalos.
- **2–3 lecciones A1 reales** curadas y listas para consumir.
- Colección de pruebas de los endpoints.

### 2.2 Qué NO entra (fuera de alcance de esta iteración)
- El **algoritmo SRS** completo (cálculo de intervalos SM-2). Solo dejamos el
  registro de aciertos/fallos y el endpoint que expone "conceptos débiles".
- La **UI Android** (Kotlin/Room). Aquí solo definimos el contrato que consumirá.
- **A2 completo** y el resto de temas A1 (se replican tras validar el corte vertical).
- Chatbot IA, minijuegos, notificaciones, TTS de audio (ver §8, decisiones abiertas).

### 2.3 Trazabilidad con el Documento Técnico
| Requisito/Regla | Cómo lo cubre este diseño |
|---|---|
| **RF-10** Ofrecer lecciones de refuerzo | Catálogo + contenido de lección servido por API. |
| **RF-12** Evaluaciones cortas | Endpoint de intento con calificación en servidor. |
| **RF-13** Algoritmo SRS | Se registra desempeño por concepto (base del SRS). Algoritmo, iteración siguiente. |
| **RF-14** Recompensas | XP calculado al completar (atómico). |
| **RN-02** Nivelación obligatoria | Catálogo filtrado por nivel del perfil (A1/A2). |
| **RN-03** Filtro MCER | Contenido escrito por el equipo (no generado) y **por nivel**: cada lección cita en `autoria.fuentes` los descriptores del MCER de su nivel que respaldan sus can-do; el linter lo exige. |
| **RN-06** Validación de avance | Umbral **≥70%** para aprobar y desbloquear (`prerequisito_id`). |
| **RN-07** Lógica SRS | Conceptos con alta tasa de error se marcan como "débiles" y se priorizan. |
| **RN-08** Criterio de racha | El intento aprobado emite un evento "actividad válida" (interfaz con módulo racha). |
| **RN-09** Recompensas atómicas | XP solo al **completar** el intento; sin créditos parciales. |
| **RN-11** Disponibilidad de recursos | Backend = fuente de verdad; Android **cachea** el contenido localmente (offline). |
| **RN-12** Tolerancia/persistencia | Progreso parcial persistible (endpoint opcional de avance). |
| **CU-06** Realizar lección de refuerzo | Flujo completo modelado (§3–§6). |

---

## 3. Arquitectura: backend de contenido + caché local (offline-first)

El PDF exige que los recursos residan **localmente** (RN-11/RNF-08). Elegir un
"backend de contenido" **no contradice** esto si se usa el patrón estándar:

```
  Autoría (equipo)                Distribución                 Consumo
  ────────────────         ─────────────────────         ───────────────────
  JSON curado en git  ──►   Backend Node/Vercel     ──►   App Android (Kotlin)
  (fuente de verdad)        GET /lessons, /manifest       Room/SQLite (caché)
                            POST /attempt (califica)       ▲  offline-first
                                     │                     │
                                     └── Firestore ────────┘
                                         (progreso + estado SRS por usuario)
```

> **Estado:** el backend (manifiesto con versiones, ETag/304) está listo para
> esto, pero Android **todavía no cachea** en Room (§0).

- **El backend es la fuente de verdad y el punto de autoría/actualización.**
- **Android descarga el contenido una vez y lo cachea en Room.** A partir de ahí
  las lecciones funcionan **sin conexión** (satisface RN-11/RNF-08). El envío de
  la evaluación puede diferirse/sincronizarse cuando haya red (RN-12).
- Un **manifiesto de versiones** permite a Android saber si su caché quedó vieja
  y volver a descargar solo lo cambiado.

### 3.1 Dónde se almacena cada cosa (recomendación)

| Dato | Almacén | Motivo |
|---|---|---|
| **Contenido de lecciones** (inmutable, versionado) | **Archivos JSON en el repo**, servidos por funciones serverless | Curado + autoría, versionable con git, barato, cacheable, offline-friendly. Evita costos/consultas de BD para datos que cambian poco. |
| **Progreso por usuario** (intentos, completado, mejor puntaje) | **Firestore** (ya disponible vía `firebase-admin`) | Estado mutable por usuario, requiere persistencia y consulta. |
| **Estado SRS por concepto** (aciertos/fallos, próxima revisión) | **Firestore** | Igual que arriba; base para RF-13. |
| **Eventos XP / racha** | **Firestore** + evento hacia el módulo de gamificación | Recompensas atómicas (RN-09), racha (RN-08). |

> **Alternativa considerada:** guardar el contenido también en Firestore. Se
> descarta para esta iteración: el contenido es casi estático y curado, y el JSON
> versionado en git da mejor trazabilidad académica y menor costo (mitiga R10 del
> análisis de riesgos: "dependencia y exceso de cuotas en la nube").

Este esquema **reutiliza el stack actual** (`firebase-admin`, funciones en
`api/`, despliegue Vercel) sin introducir dependencias nuevas.

---

## 4. Modelo de contenido curricular

### 4.1 Jerarquía

```
Nivel (A1 | A2)
 └── Lección  = 1 módulo temático   (p. ej. "Saludos y presentaciones")
      ├── Sección: introducción     (objetivo + can-do MCER)
      ├── Sección: vocabulario       (flashcards ES↔EN + audio)
      ├── Sección: gramática         (mini-explicación + ejemplos)
      ├── Sección: diálogo           (ejemplo en contexto, con audio)
      └── Sección: evaluación        (ítems calificables, umbral 70%)
```

Cada **ítem de vocabulario y cada ejercicio** se etiqueta con un `concepto_id`.
El concepto es la unidad que el SRS rastrea (RN-07): un lexema, una expresión o
una regla gramatical. Así, cuando el usuario falla el ejercicio "plural de nouns",
el SRS sabe qué **concepto** está débil, no solo qué lección.

### 4.2 Esquema JSON de una lección (contrato de contenido)

```jsonc
{
  "id": "a1-saludos-presentaciones",
  "version": 1,                          // sube al editar → invalida caché
  "nivel_mcer": "A1",
  "modulo_tematico": "Saludos y presentaciones",
  "orden": 1,                            // orden dentro del nivel
  "prerequisito_id": null,               // null = desbloqueada de inicio (RN-06)
  "xp_recompensa": 50,                   // XP al aprobar (atómico, RN-09)
  "can_do": [                            // descriptores MCER que cubre (RN-03)
    "Puedo saludar y despedirme de forma sencilla.",
    "Puedo presentarme y presentar a alguien."
  ],
  "autoria": {                           // interno (§8.5): no se envía a la app
    "fuentes": ["MCER (Consejo de Europa, 2001; trad. Instituto Cervantes, 2002), §4.2 Temas de comunicación (pp. 55–56): Relaciones con otras personas.",
                "MCER, Conversación, A1 (p. 77): «Se presenta y utiliza saludos y expresiones de despedida básicos…»"],
    "notas": "Ejemplos y ejercicios originales."
  },
  "secciones": [
    {
      "tipo": "introduccion",
      "titulo": "¡Aprende a saludar en inglés!",
      "cuerpo": "En esta lección aprenderás saludos y cómo presentarte."
    },
    {
      "tipo": "vocabulario",
      "titulo": "Saludos",
      "items": [
        { "concepto_id": "voc.saludos.hello", "en": "Hello!", "es": "¡Hola!", "audio": "tts:Hello", "nota": "neutro" },
        { "concepto_id": "voc.saludos.hi", "en": "Hi!", "es": "¡Hola! (informal)", "audio": "tts:Hi" },
        { "concepto_id": "voc.saludos.good_morning", "en": "Good morning!", "es": "¡Buenos días!", "audio": "tts:Good morning" },
        { "concepto_id": "voc.saludos.my_name_is", "en": "My name is…", "es": "Me llamo…", "audio": "tts:My name is" }
      ]
    },
    {
      "tipo": "gramatica",
      "concepto_id": "gram.verb_to_be_present",
      "titulo": "Verbo to be (am/is/are)",
      "explicacion": "Usamos 'am/is/are' para presentarnos: I am Ana. He is Tom.",
      "ejemplos": ["I am a student.", "She is my friend.", "They are teachers."]
    },
    {
      "tipo": "dialogo",
      "audio": "tts:dialogue",
      "lineas": [
        { "hablante": "A", "en": "Hello, how are you?", "es": "Hola, ¿cómo estás?" },
        { "hablante": "B", "en": "I'm fine, thank you!", "es": "¡Bien, gracias!" }
      ]
    },
    {
      "tipo": "evaluacion",
      "umbral_aprobacion": 0.70,          // RN-06
      "items": [
        {
          "id": "q1",
          "tipo": "opcion_multiple",
          "concepto_id": "voc.saludos.good_morning",
          "enunciado": "¿Cómo dices '¡Buenos días!' en inglés?",
          "opciones": ["Good night!", "Good morning!", "Goodbye!"],
          "respuesta_correcta": 1,
          "feedback_error": "'Good morning!' se usa en la mañana."
        },
        {
          "id": "q2",
          "tipo": "completar",
          "concepto_id": "gram.verb_to_be_present",
          "enunciado": "My name ___ Ana.",
          "respuesta_correcta": "is",
          "acepta": ["is"],
          "feedback_error": "Con 'name' (3ª persona) usamos 'is'."
        },
        {
          "id": "q3",
          "tipo": "emparejar",
          "enunciado": "Une cada saludo con su significado.",
          "feedback_error": "Repasa la tabla de saludos.",
          "pares": [
            { "concepto_id": "voc.saludos.hello", "izq": "Hello!", "der": "¡Hola!" },
            { "concepto_id": "voc.saludos.good_morning", "izq": "Good morning!", "der": "¡Buenos días!" }
          ]
        }
      ]
    }
  ]
}
```

> **`audio`: `"tts:…"`** — marcamos el texto a sintetizar. Android tiene TTS
> nativo, así que el backend **no** necesita servir archivos de audio en esta
> iteración (ver §8). Si más adelante se quiere audio pre-grabado, `audio` puede
> apuntar a una URL/archivo.

### 4.3 Tipos de ítem de evaluación (los 3 del CU-06)
- **`opcion_multiple`** — 1 correcta entre N opciones.
- **`completar`** (fill-in) — respuesta de texto normalizada (minúsculas, trim);
  `acepta` lista variantes válidas.
- **`emparejar`** (match) — pares izquierda/derecha; correcto = todos emparejados.

Cada ítem correcto/incorrecto genera una señal por `concepto_id` para el SRS.

### 4.4 Convención de identificadores de concepto (`concepto_id`)

El `concepto_id` es la unidad que rastrea el SRS, así que debe ser **estable y
predecible**. Convención:

- `voc.<tema>.<lema>` — vocabulario. Ej.: `voc.saludos.good_morning`.
- `gram.<estructura>` — regla gramatical. Ej.: `gram.verb_to_be_present`.
- `func.<funcion>` — función comunicativa (opcional). Ej.: `func.presentarse`.

Reglas:
- **Los IDs no cambian entre versiones** de una lección. Si el vocablo se
  reescribe, se mantiene el mismo `concepto_id` para no perder el historial SRS
  del usuario (§5.1). Si un concepto se elimina, se marca como *deprecado*, no se
  reasigna el ID.
- Un mismo concepto puede aparecer en varias lecciones (repaso en espiral): el
  SRS lo trata como **uno solo**, acumulando aciertos/fallos de todas partes.

### 4.5 Validación de contenido (JSON Schema + linter)

El contenido es curado a mano, así que necesita una **red de seguridad
automática** antes de servirse (mitiga R03/R08 del análisis de riesgos):

- **JSON Schema** (`content/schema/leccion.schema.json`) que valida la estructura
  del §4.2.
- **Linter** (`npm run lint:content`) que además verifica reglas semánticas:
  1. `id` de lección e `id` de ítem **únicos**.
  2. Cada ítem de evaluación tiene **clave de respuesta** válida y coherente con
     su `tipo`.
  3. Todo `concepto_id` sigue la convención (§4.4).
  4. `prerequisito_id` apunta a una lección existente y **no forma ciclos**.
  5. `xp_recompensa`, `nivel_mcer`, `umbral_aprobacion` presentes y en rango.
  6. Todo `voc`/`gram` de la evaluación fue **introducido antes** en la lección
     o en su cadena de prerequisitos (no se evalúa lo que no se enseñó).

  Implementadas además: el archivo se llama `<id>.json`; la introducción va
  primero y hay exactamente una evaluación, al final; `orden` único por nivel;
  opciones, `izq` y `der` sin repetir; `acepta` incluye la respuesta correcta.
  La lista completa y actualizada está en la
  [Guía de autoría §7](guia-autoria-lecciones.md#7-validaciones).

Se corre con `npm run lint:content` y como parte de `npm test`. **Pendiente:**
correrlo en CI antes de desplegar (hoy no hay CI; ver §0).

---

## 5. Modelo de datos de persistencia (refinamiento del ER del PDF)

El diagrama ER del Documento Técnico tiene `lecciones` y `progreso_lecciones`,
pero **le faltan** (a) los ítems de contenido y (b) el estado por concepto del
SRS. Sin esos, RF-13/RN-07 no son implementables. Refinamiento propuesto:

### 5.1 Contenido (archivos JSON versionados, no BD)
- `content/lessons/<id>.json` — una lección (esquema §4.2).
- **Manifiesto:** índice por nivel con `{id, version, orden, prerequisito_id,
  titulo, xp_recompensa}` y un hash `version_contenido`. No es un archivo: el
  servidor lo **calcula** al cargar las lecciones (`lib/lecciones/contenido.js`)
  y lo sirve en `GET /api/lessons/manifest`. Es lo que Android consultará para
  saber qué cachear/refrescar.

### 5.2 Colecciones Firestore (estado por usuario)

**`progreso_lecciones`** (doc id: `<uid>_<leccion_id>`)
```
uid, leccion_id, estado ('en_progreso'|'reprobada'|'completada'),
mejor_puntaje (0..1), ultimo_puntaje, intentos, aprobada (bool),
xp_otorgado (bool), fecha_actualizacion
```

**`srs_conceptos`** (doc id: `<uid>_<concepto_id>`) — *base para RF-13*
```
uid, concepto_id, nivel_mcer, tipo ('vocab'|'gramatica'),
aciertos, fallos, tasa_error, ultima_vista,
proxima_revision (null en esta iteración), intervalo, ease
```
> En esta iteración solo escribimos `aciertos/fallos/tasa_error/ultima_vista`.
> Los campos `proxima_revision/intervalo/ease` quedan definidos pero se calculan
> cuando implementemos el algoritmo SM-2.

**`eventos_actividad`** (opcional, para racha RN-08 y auditoría)
```
uid, tipo ('leccion_completada'), leccion_id, fecha, xp
```

### 5.3 Regla de desbloqueo (RN-06)
Una lección está `disponible` si `prerequisito_id == null` **o** si la lección
prerequisito tiene `aprobada == true` para ese usuario. Al aprobar (≥70%), se
marca `completada`, se desbloquea la siguiente y se otorga XP **una sola vez**
(`xp_otorgado` evita duplicar — RN-09).

### 5.4 Máquina de estados de una lección

```
  bloqueada ──(prerequisito aprobado)──► disponible
                                            │
                        (inicia teoría)     ▼
                                        en_progreso ──(intento < 70%)──► reprobada
                                            │                               │
                             (intento ≥ 70%)▼           (reintenta)─────────┘
                                        completada  ◄── se puede repetir para subir mejor_puntaje
```

- **`reprobada`** no bloquea: el usuario puede reintentar cuantas veces quiera
  (CU-06 A3 sugiere repetir). No otorga XP hasta aprobar.
- Una lección `completada` **sigue siendo reabrible** para repasar; un reintento
  posterior actualiza `mejor_puntaje` pero **no vuelve a dar XP** (RN-09).
- El campo `estado` en `progreso_lecciones` refleja este autómata. `bloqueada`
  y `disponible` **no se guardan**: se derivan del progreso del prerequisito,
  así nunca quedan desincronizados (`lib/lecciones/estado.js`).

---

## 6. Diseño de API (backend de contenido)

Convención: funciones serverless en `api/`, JSON, mismo estilo que los endpoints
existentes de RF-03. Autenticación por token de Firebase (el `uid` sale del token
verificado en servidor; **no** se confía en un `uid` enviado por el cliente).

| Método | Ruta | Propósito |
|---|---|---|
| `GET` | `/api/lessons?level=A1` | Catálogo del nivel + estado de desbloqueo por usuario. |
| `GET` | `/api/lessons/manifest` | Manifiesto de versiones para sync/caché offline. |
| `GET` | `/api/lessons/:id` | Contenido completo de una lección (esquema §4.2). |
| `POST` | `/api/lessons/:id/attempt` | Enviar respuestas → **califica en servidor**, aplica 70%, calcula XP, guarda progreso, actualiza SRS, devuelve feedback por ítem. |
| `GET` | `/api/srs/review` | Conceptos "débiles" del usuario (base RF-13). |
| `POST` | `/api/lessons/:id/progress` | (Opcional) Guardar avance parcial de teoría (RN-12). |

### 6.1 `POST /api/lessons/:id/attempt` — el endpoint central

**Request**
```jsonc
{
  "respuestas": [
    { "id": "q1", "valor": 1 },
    { "id": "q2", "valor": "is" },
    { "id": "q3", "valor": [["Hello!","¡Hola!"], ["Good morning!","¡Buenos días!"]] }
  ]
}
```

**Response**
```jsonc
{
  "puntaje": 0.83,                 // 5/6 correctas
  "aprobada": true,               // >= 0.70 (RN-06)
  "xp_ganado": 50,                // solo si aprueba y es la 1ª vez (RN-09)
  "desbloqueada_siguiente": "a1-numeros-hora",
  "feedback": [
    { "id": "q1", "correcta": true },
    { "id": "q2", "correcta": false, "mensaje": "Con 'name' usamos 'is'." }
  ],
  "conceptos_debiles": ["gram.verb_to_be_present"]  // los que falló
}
```

**Lógica del servidor (pseudocódigo)**
```
1. Verificar token → uid.
2. Cargar lección :id (JSON). Validar que esté 'disponible' para uid (RN-06).
3. Calificar cada respuesta contra la clave del JSON.
4. puntaje = correctas / total; aprobada = puntaje >= umbral (0.70).
5. Por cada ítem: actualizar srs_conceptos[uid, concepto_id] (acierto/fallo).
6. Si aprobada:
      - marcar progreso 'completada', aprobada=true
      - si !xp_otorgado → sumar xp_recompensa, xp_otorgado=true (RN-09)
      - desbloquear lección con prerequisito_id == :id
      - emitir evento 'actividad válida' (racha, RN-08)
   Si no aprobada:
      - guardar ultimo_puntaje/mejor_puntaje; sugerir repetir (CU-06 A3)
7. Responder puntaje + feedback + conceptos_debiles.
```

> **Nota:** la calificación autoritativa (la que otorga XP y desbloquea) ocurre
> en el servidor, pero por el requisito offline el cliente también califica
> localmente — ver §6.2.

### 6.2 Calificación offline + sincronización idempotente

> **Estado:** la parte de servidor (recalificación, `attempt_id` idempotente)
> está implementada; la calificación local y el outbox en Android están
> pendientes (§0).

Tensión a resolver: el PDF exige **funcionar sin conexión** (RN-11, RN-12,
RNF-08), pero "backend de contenido" sugiere calificar en servidor. Se resuelve
con un modelo de **doble calificación**:

1. **Cliente (offline, feedback inmediato):** como la clave de respuestas viaja
   dentro del JSON de la lección (necesario para el offline), Android **califica
   localmente** y muestra el resultado al instante (RN-05: feedback < 2.5 s).
   El puntaje y la racha se reflejan de inmediato en la UI.
2. **Servidor (autoritativo, al sincronizar):** cuando hay red, el cliente envía
   el intento a `POST /lessons/:id/attempt`. El servidor **recalifica**, y es la
   fuente de verdad para **XP, desbloqueo y estado SRS**. Si el dispositivo está
   offline, el intento se **encola** (outbox) y se envía después (RN-12).

**Idempotencia (clave para RN-09):** cada intento lleva un `attempt_id` (UUID
generado en el cliente). El servidor ignora un `attempt_id` ya procesado, de modo
que reintentos de red **no duplican XP**. El request de §6.1 incluye entonces:

```jsonc
{ "attempt_id": "b1f2…uuid", "respuestas": [ … ] }
```

**Resolución de conflictos:** ante discrepancia cliente/servidor, **gana el
servidor** para XP y desbloqueo (evita manipulación). El feedback local ya
mostrado no se revierte visualmente; solo se ajustan los contadores oficiales.

> **Modelo de amenaza (contexto de bajo riesgo):** al ser una app educativa de
> refuerzo A1/A2 (no un examen con validez oficial), aceptamos que un usuario
> técnico podría inspeccionar el JSON y ver las respuestas. El costo de blindar
> eso (calificación 100% servidor) rompería el offline, que sí es requisito. Se
> prioriza el offline; el anti-trampa se limita a la idempotencia y a que el
> servidor sea autoritativo.

### 6.3 Contrato de errores y seguridad de la API

- **Autenticación:** todo endpoint (salvo, si acaso, el catálogo público) exige
  el **ID token de Firebase** en `Authorization: Bearer <token>`. El servidor lo
  verifica con `firebase-admin` y deriva el `uid`; **nunca** confía en un `uid`
  del body (coherente con RF-03 y RNF-06).
- **Forma de error estándar** (igual estilo que los endpoints actuales):
  ```jsonc
  { "error": "mensaje legible", "codigo": "LECCION_BLOQUEADA" }
  ```
  Códigos HTTP: `400` entrada inválida · `401` token ausente/ inválido ·
  `403` lección bloqueada (prerequisito no cumplido) · `404` lección inexistente ·
  `429` demasiados intentos · `500` error interno.
- **Rate limiting** en `POST /attempt` (p. ej. por `uid`+lección) para evitar
  fuerza bruta de respuestas y abuso de cuota (mitiga R10).
- **Validación de entrada** estricta del body (tipos, longitudes) antes de tocar
  Firestore.
- **Sin PII en el contenido**: los JSON de lecciones no contienen datos
  personales; el `uid` solo aparece en las colecciones de progreso.

---

## 7. Corte vertical propuesto (2–3 temas A1)

Se eligen los **primeros can-do de A1**, que son la base de todo lo demás y
forman una secuencia natural con prerequisitos:

| Orden | Lección | Can-do MCER | Vocab (curado) | Gramática | Prerequisito |
|---|---|---|---|---|---|
| 1 | **Saludos y presentaciones** | Saludar/despedirse; presentarse | hello, hi, good morning/afternoon, goodbye, my name is, nice to meet you | verbo *to be* (am/is/are), pronombres | — |
| 2 | **Información personal** | Dar datos personales (nombre, país, edad) | country, city, age, old, from, live | *to be* + *have got*; *wh-questions* (what/where/how old) | Lección 1 |
| 3 | **Números y la hora** | Contar; decir la hora | numbers 0–20, o'clock, time, hour | *there is/are*; preguntas *how many* | Lección 2 |

**Entregables del corte vertical:**
1. `content/lessons/` con las 3 lecciones en JSON (§4.2); el manifiesto se
   calcula a partir de ellas (§5.1).
2. Endpoints `GET /lessons`, `GET /lessons/:id`, `GET /manifest`,
   `POST /lessons/:id/attempt`, `GET /srs/review`.
3. Colecciones Firestore `progreso_lecciones` y `srs_conceptos` funcionando.
4. Colección de pruebas (curl/Postman) que recorra: catálogo → contenido →
   intento aprobado → desbloqueo → intento reprobado → conceptos débiles.

Validado el flujo, escalar a los temas A1 restantes y luego A2 fue
**replicar contenido**, no rediseñar arquitectura: las 21 lecciones restantes
se agregaron solo como JSON, sin tocar código. El plan completo (12
lecciones A1 + 12 A2, con tema, can-do, gramática y prerequisitos) está en la
[Guía de autoría §2](guia-autoria-lecciones.md#2-plan-curricular-temario-a1-y-a2).

---

## 8. Fundamento MCER del contenido

Fuente única: Consejo de Europa (2001), *Marco común europeo de referencia
para las lenguas*, trad. Instituto Cervantes (2002). El detalle, con citas
textuales y páginas, está en [`revision-mcer.md`](revision-mcer.md); aquí va
el resumen que rige el diseño.

### 8.1 Qué toma el contenido del MCER

- **Temas:** los *temas de comunicación* de §4.2 (pp. 55–56): identificación
  personal; vivienda, hogar y entorno; vida cotidiana; tiempo libre y ocio;
  viajes; relaciones con otras personas; salud y cuidado corporal; educación;
  compras; comidas y bebidas; servicios públicos; lugares; lengua extranjera;
  condiciones atmosféricas.
- **Can-do:** cada uno se respalda con un descriptor del **nivel de la
  lección** (Cuadro 1, p. 26; Cuadro 2, p. 30; escalas de los capítulos 4 y
  5). La RN-03 se aplica por nivel: nada en A1 que el MCER ubique en A2.
- **Alcance del vocabulario** (Riqueza de vocabulario, p. 109). A1:
  *"repertorio básico de palabras y frases aisladas relativas a situaciones
  concretas"*. A2: *"suficiente vocabulario para desenvolverse en
  actividades habituales y en transacciones cotidianas que comprenden
  situaciones y temas conocidos"*.

### 8.2 Qué **no** toma del MCER (decisiones del equipo)

- **Listas de palabras.** El MCER no las da (p. 28). Cada lección tiene
  10–12 palabras o expresiones elegidas por el equipo para el tema y las
  funciones de la lección. Eso da ~130–150 por nivel: un subconjunto de
  **refuerzo** (RF-10), no un vocabulario completo del nivel.
- **Gramática por nivel.** El MCER no la define (*"No se considera posible
  elaborar una escala de la progresión relativa a la estructura gramatical
  que sea aplicable a todas las lenguas"*, p. 111). La distribución de
  estructuras de la Guía de autoría §2 es del equipo. Cada estructura se usa
  solo para funciones que tienen descriptor en el nivel (por ejemplo, el
  primer condicional de A2 sirve para *planes*, no para *hipótesis*, que es
  B2).
- **Umbral del 70%**, tamaños de sección y XP: decisiones de diseño
  (Documento Técnico y §2.3).

### 8.3 Proceso de autoría (cómo se produce cada lección)
1. Elegir el **tema** de §4.2 y los **can-do** con sus descriptores del nivel.
2. Seleccionar el **vocabulario** para esas funciones dentro del tema.
3. Elegir la **estructura gramatical** que sirve a esas funciones.
4. Redactar **diálogo** y **ejercicios** que practiquen solo esas funciones.
5. Etiquetar cada ítem con `concepto_id` (para el SRS).
6. Citar en `autoria.fuentes` el tema y los descriptores, textuales y con
   página. El linter revisa que estén.

### 8.4 Propiedad intelectual del contenido

Conecta con la factibilidad legal del proyecto (3.6.4 del Documento Técnico):

- Los descriptores del MCER se **citan** textualmente, con fuente y página,
  solo en el campo interno `autoria`, que no se envía a la app.
- Los ejemplos, diálogos y ejercicios se **redactan originales** para
  XP-SPEAK. No se copian de libros ni de exámenes.

---

## 9. Fuentes (investigación)

Enlaces revisados el 2026-09-29.

**Contenido y niveles: fuente única**
- Consejo de Europa (2001), *Marco común europeo de referencia para las
  lenguas: aprendizaje, enseñanza, evaluación*. Madrid: Ministerio de
  Educación, Cultura y Deporte / Anaya, 2002. Traducción del Instituto
  Cervantes: https://cvc.cervantes.es/ensenanza/biblioteca_ele/marco/cvc_mer.pdf
- Consejo de Europa (2020), *Common European Framework of Reference for
  Languages: Companion Volume*. Actualiza los descriptores; está **pendiente**
  cotejar con él las citas de la revisión (`revision-mcer.md` §6).

**Repaso espaciado (base de RF-13 / RN-07)**
- N. J. Cepeda, H. Pashler, E. Vul, J. T. Wixted y D. Rohrer, "Distributed
  practice in verbal recall tasks: A review and quantitative synthesis,"
  *Psychological Bulletin*, vol. 132, no. 3, pp. 354–380, 2006.
  doi:10.1037/0033-2909.132.3.354
- P. A. Woźniak, *Optimization of Learning*, tesis de maestría, Universidad
  Tecnológica de Poznań, 1990 (origen del algoritmo SM-2):
  https://super-memory.com/english/ol.htm
- H. Ebbinghaus, *Über das Gedächtnis*, 1885 (trad. al inglés: *Memory: A
  Contribution to Experimental Psychology*, 1913) — curva del olvido.

**Umbral de aprobación (RN-06)**
- B. S. Bloom, "Learning for Mastery," *Evaluation Comment*, vol. 1, no. 2,
  pp. 1–12, 1968. Respalda el **mecanismo** (evaluación corta al final de cada
  unidad, retroalimentación y nuevo intento antes de avanzar). Ojo: el
  *mastery learning* suele fijar el criterio en 80–90%; el **70%** de XP-SPEAK
  sale del Documento Técnico ("70% recomendado") y se eligió más bajo a
  propósito para una app de refuerzo sin carácter de examen. Si se cita, hay
  que presentarlo así, no como un número tomado de Bloom.

> **Nota para la tesis:** en el Documento Técnico (§2.6.1–2.6.2), Ebbinghaus
> y SM-2 están citados con [32] y [33], que son artículos sobre chatbots. Las
> referencias correctas son las de arriba.

---

## 10. Decisiones tomadas

Las decisiones que quedaron abiertas en la v1 ya se resolvieron:

| # | Decisión | Resultado |
|---|---|---|
| 1 | Almacén del contenido | **JSON versionado en git** (§3.1). |
| 2 | Audio | **TTS nativo de Android** (`"tts:…"`); sin archivos de audio. |
| 3 | Autenticación | **ID token de Firebase** en `Authorization: Bearer`; el `uid` sale del token. |
| 4 | Temas del corte vertical | Saludos, información personal, números y hora (§7). |
| 5 | Rutas | Subcarpetas en `api/` (`/api/lessons/...`, `/api/srs/...`). |
| 6 | Idioma L1 | Español; campos `es` planos. Si algún día hay otra L1, `es` pasa a mapa por idioma. |
| 7 | Fuente del contenido | **Solo el MCER** (2026-09-29): temas de §4.2 y can-do con descriptores del nivel; sin listas de vocabulario externas. |
| 8 | Alcance de la RN-03 | **Por nivel** (2026-09-29): una lección A1 no practica nada que el MCER ubique en A2. |

Siguen abiertas:

1. **Sesión de repaso SRS:** `GET /srs/review` ya devuelve conceptos débiles;
   la sesión de repaso se diseña junto con el algoritmo SM-2.
2. **Plan curricular y XP por nivel:** propuestos en la Guía de autoría (§2 y
   §4), pendientes de validar por el equipo.
3. **Caché offline y calificación local en Android** (§0).

---

## 11. Definition of Done (corte vertical)

Cumplido. Cada punto lo verifica una prueba automática:

- [x] 3 lecciones A1 en JSON pasan el linter de contenido (§4.5).
      → `test/lint-content.test.js`
- [x] `GET /lessons`, `/lessons/:id`, `/lessons/manifest` responden con el
      contrato de §6 y filtran por nivel/desbloqueo (RN-02, RN-06).
      → `test/api-lecciones.test.js` (catálogo, contenido, manifiesto)
- [x] `POST /lessons/:id/attempt` califica, aplica 70%, otorga XP atómico e
      idempotente, desbloquea la siguiente y registra `srs_conceptos` (RN-06/09, §6.2).
      → flujo completo, idempotencia y concurrencia en `test/api-lecciones.test.js`
- [x] `GET /srs/review` devuelve los conceptos débiles del usuario.
- [x] Colección de pruebas recorre el flujo feliz y los casos borde (bloqueada,
      reprobada, reintento idempotente). → `scripts/probar-lecciones.sh`
- [x] Endpoints verifican token de Firebase y devuelven errores según §6.3.
      → 401/405/409/429 en `test/api-lecciones.test.js`; token real en
      `test/integracion/emulador.test.js`
