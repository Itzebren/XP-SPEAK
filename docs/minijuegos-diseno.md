# Diseño y alcance del módulo de **Minijuegos** — XP-SPEAK

> App Android (Kotlin/Compose) + backend Node/Vercel · Rama `feature/minijuegos`
> Trabajo Terminal 2026-B162 · Documento de diseño
> Estado: **v2 — implementado** (2026-10-06). v1 (propuesta) del 2026-10-01; las
> decisiones abiertas se resolvieron en §9 y lo que cambió respecto a la propuesta
> está en §10.

Este documento reúne el contexto del Documento Técnico (PDF) y la propuesta de
implementación de los cuatro minijuegos del catálogo: **Eco Vocal**,
**Misión Situacional**, **Ráfaga de Palabras** y **Orden Maestro**. Lo que
dice el PDF está marcado como requisito; lo demás es propuesta y está abierto
a cambio (ver §9).

---

## 0. Estado actual

| Pieza | Estado | Dónde |
|---|---|---|
| Catálogo 2x2 sin imágenes (CU-07 pasos 1–3), nivel y "Continuar" | ✅ | `feature/games/ui/GamesScreen.kt`, `GamesViewModel.kt` |
| Enum del catálogo (con `xpBase` y tiempo objetivo) | ✅ | `feature/games/domain/Minijuego.kt` |
| Navegación catálogo → juego (`games/{id}`) | ✅ | `feature/games/ui/MinijuegoScreen.kt`, `core/navigation/` |
| Banco de contenido local (6 misiones + lecciones) | ✅ | §3 · `backend/scripts/exportar-banco-minijuegos.js` → `app/src/main/assets/minijuegos/banco.json` |
| Motor común (rondas, Room v4, XP, racha, retomar) | ✅ | §4 · `feature/games/ui/comun/`, `feature/games/data/` |
| Orden Maestro | ✅ | `feature/games/ui/orden/` |
| Ráfaga de Palabras | ✅ | `feature/games/ui/rafaga/` |
| Misión Situacional | ✅ | `feature/games/ui/mision/` |
| Eco Vocal (Azure + respaldo del teléfono) | ✅ | `feature/games/ui/ecovocal/`, `feature/games/data/voz/` |
| `POST /api/games/result` + worker + SRS + racha | ✅ | §6 · `backend/api/games/result.js`, `SincronizarPartidasWorker.kt` |
| `GET /api/speech/token` | ✅ | `backend/api/speech/token.js` |
| Imágenes del catálogo y sonidos (fase 8) | ❌ | Pendiente de assets |
| Recurso de Azure Speech en producción | ❌ | Configurar `AZURE_SPEECH_KEY` / `AZURE_SPEECH_REGION` en Vercel (ver `backend/README.md`) |

---

## 1. Lo que exige el Documento Técnico

### 1.1 Requisitos y reglas que aplican

| ID | Qué dice | Qué implica para los minijuegos |
|---|---|---|
| **RF-11** Ejecutar minijuegos síncronos | Retos lúdicos **individuales (en solitario)** para reforzar **vocabulario y estructuras gramaticales básicas**. | Nada multijugador. El contenido debe ser vocabulario y gramática A1/A2. |
| **RF-13** Algoritmo SRS | Identificar temas de bajo desempeño y priorizarlos. | Los errores en juegos alimentan `srs_conceptos`; los conceptos débiles salen más seguido en los juegos. |
| **RF-14** Recompensas | Insignias, niveles y puntos por actividad. | Cada partida terminada da XP. |
| **RNF-08** Eficiencia | Recursos estáticos y multimedia **integrados en el paquete de instalación**. | El banco de contenido de los juegos va en `assets/` del APK, no se descarga. |
| **RNF-10** Tolerancia a fallos | Recuperar el estado de una lección **o minijuego** tras cierre o falla de red. | Guardar la partida en curso en Room. |
| **RN-03** Filtro MCER | Nada por encima de A1/A2. | Reutilizar el contenido ya revisado de las lecciones; si hay IA, restringirla igual que el chat. |
| **RN-04** Privacidad de voz | Audio efímero, sin grabaciones permanentes en servidores. | Eco Vocal no guarda audio; Azure se usa en modo streaming sin almacenamiento. |
| **RN-05** Inmediatez | Feedback fonético/gramatical en ≤ 2.5 s. | Validación local inmediata; la evaluación de voz debe responder en ese tiempo. |
| **RN-08** Racha | La racha sube con una actividad válida (**lección, chat o minijuego**) en 24 h. | Terminar una partida cuenta para la racha. |
| **RN-09** Recompensas atómicas | XP solo al concluir; nada por sesiones abandonadas. | Si se sale a mitad, 0 XP (pero se guarda para retomar). |
| **RN-11** Disponibilidad | Textos, gramática y **multimedia de minijuegos** deben residir localmente. | Igual que RNF-08. |
| **RN-12** Persistencia | Guardar localmente el estado de cualquier actividad en curso. | Igual que RNF-10. |

### 1.2 CU-07 Participar en minijuegos (Tabla 31 + Ilustración 20)

Flujo principal:
1. El usuario abre "Minijuegos". → ✅ hecho
2. El sistema despliega el catálogo (consulta catálogo + nivel A1/A2 en Room). → ✅ catálogo; falta leer nivel
3. El usuario selecciona un juego. → ✅ hecho
4. Se cargan lógica y activos **desde almacenamiento local** (RNF-08).
5. El usuario interactúa con la mecánica (loop de sesión).
6. El sistema valida cada acción en tiempo real y da feedback visual/auditivo de acierto o error.
7. Al terminar, calcula el desempeño con **precisión y tiempo**.
8. Muestra la XP obtenida y actualiza la racha; registra el hito en Room y lo sincroniza al servidor en segundo plano.

Flujos alternos:
- **A1 Cierre inesperado:** se guarda el estado parcial en Room para no perder el progreso.
- **A2 Nivel bloqueado:** si un usuario A1 intenta entrar a contenido A2, mensaje "Completa más lecciones para desbloquear".

Precondiciones: sesión activa (CU-02) y recursos locales disponibles.
Postcondición: XP actualizada y el hito registrado en BD local para sincronizar.

### 1.3 Marco de diseño del PDF (§2.4)

- **MDA** (Mecánicas → Dinámicas → Estética): cada juego se describe abajo con esas tres capas.
- **Flujo (Csikszentmihalyi):** equilibrio reto/habilidad. Si es muy difícil genera ansiedad, si es muy fácil aburre. → Dificultad adaptativa simple en cada juego (§4.4).
- **Filtro afectivo:** práctica privada, sin presión pública. → Sin rankings contra otros; los errores se explican, no se castigan con mensajes duros.

---

## 2. Decisiones que fijan este diseño (propuesta)

| # | Decisión | Propuesta | Motivo |
|---|---|---|---|
| 1 | ¿De dónde sale el contenido? | **De las lecciones ya escritas** (`backend/content/lessons/*.json`), exportado a un banco JSON dentro del APK. | Ya está revisado contra el MCER (RN-03), tiene `concepto_id` para el SRS y cumple RNF-08 sin trabajo extra de autoría. |
| 2 | ¿Qué contenido ve el usuario? | Conceptos de su nivel **de lecciones no bloqueadas** (disponibles, en progreso o completadas). | Refuerza lo que ya vio y cubre el flujo A2 de CU-07 sin un candado aparte. |
| 3 | ¿Quién califica? | **El dispositivo** (excepto la pronunciación, que la evalúa Azure). | CU-07 dice que los juegos "operan de forma local"; la validación debe ser inmediata (RN-05). |
| 4 | ¿Quién otorga la XP? | El dispositivo la calcula y la muestra; el **servidor la confirma** al sincronizar con un tope por partida. | Funciona offline (outbox) y evita XP inflada desde un cliente alterado. |
| 5 | Orden de implementación | Orden Maestro → Ráfaga → Misión Situacional → Eco Vocal. | De menor a mayor dependencia: los dos primeros son 100% locales; Eco Vocal necesita Azure. |

---

## 3. Banco de contenido local

### 3.1 Origen
Cada lección ya trae lo que los juegos necesitan:

| Sección de la lección | Campos útiles | Juego que lo usa |
|---|---|---|
| `vocabulario.items[]` | `concepto_id`, `en`, `es`, `audio` (`tts:...`) | Ráfaga, Eco Vocal |
| `gramatica.ejemplos[]` + `concepto_id` | oraciones completas en inglés | Orden Maestro, Eco Vocal |
| `dialogo.lineas[]` | `hablante`, `en`, `es` | Misión Situacional, Orden Maestro, Eco Vocal |
| `evaluacion.items[].feedback_error` | explicación del error | Feedback en todos |

### 3.2 Exportación
Script nuevo en el backend: `backend/scripts/exportar-banco-minijuegos.js`.

- Lee `content/lessons/*.json` y genera `app/src/main/assets/minijuegos/banco.json`.
- Agrupa por `nivel_mcer` y por `leccion_id` para poder filtrar por lecciones desbloqueadas.
- Añade al banco lo que solo existe para los juegos (guiones de Misión Situacional, §5.2), escrito en `backend/content/minijuegos/` y validado por el mismo linter (fuentes MCER, longitud de oraciones, etc.).
- Se corre a mano antes de compilar (y en la CI cuando exista).

Forma sugerida:

```json
{
  "version": 1,
  "lecciones": {
    "a1-casa-hogar": {
      "nivel": "A1",
      "vocabulario": [{ "concepto_id": "voc.casa.kitchen", "en": "kitchen", "es": "cocina" }],
      "oraciones":   [{ "concepto_id": "gram.prepositions_of_place", "en": "My shoes are under the bed.", "es": "Mis zapatos están debajo de la cama." }],
      "dialogos":    [{ "titulo": "¿Dónde está mi celular?", "lineas": [{ "hablante": "Diego", "en": "...", "es": "..." }] }]
    }
  },
  "misiones": [ /* §5.2 */ ]
}
```

> **Pendiente de contenido:** los `ejemplos` de gramática hoy no traen traducción al
> español. Orden Maestro la necesita como pista: hay que añadir `es` a los ejemplos
> (o usar solo las líneas de diálogo, que sí la tienen).

### 3.3 En Android
- `feature/games/data/BancoMinijuegos.kt`: lee el JSON de `assets` una vez (Gson, ya está en el proyecto) y lo deja en memoria.
- Filtro por nivel del perfil (`UsuarioEntity`) y por lecciones no bloqueadas (estado del catálogo de lecciones; mientras no exista la caché de lecciones en Room, usar el último estado conocido o todo el nivel como respaldo).
- Ponderación SRS: los conceptos de `GET /api/srs/review` (o su copia local) tienen el doble de probabilidad de salir.

---

## 4. Motor común

Los cuatro juegos comparten el mismo esqueleto (Ilustración 20), así que se
implementa una vez:

```
feature/games/
├── domain/
│   ├── Minijuego.kt            ✅ catálogo
│   ├── Ronda.kt                una pregunta/reto con su concepto_id
│   ├── ResultadoPartida.kt     aciertos, errores por concepto, tiempo, XP
│   └── CalculadorXp.kt         fórmula de §4.3 (pura, con pruebas unitarias)
├── data/
│   ├── BancoMinijuegos.kt      lectura de assets (§3.3)
│   ├── PartidaDao.kt           Room: partida en curso + outbox de resultados
│   ├── MinijuegosRepository.kt
│   └── SincronizarPartidasWorker.kt   igual que SincronizarIntentosWorker
└── ui/
    ├── GamesScreen.kt          ✅
    ├── MinijuegoScreen.kt      ✅ (placeholder → despachador a cada juego)
    ├── ResultadoPartidaScreen.kt   XP + racha (paso 8)
    ├── ecovocal/ mision/ rafaga/ orden/   pantalla + ViewModel de cada juego
```

### 4.1 Ciclo de una partida
1. `iniciar()`: arma N rondas desde el banco (ponderadas por SRS) y guarda la partida en Room.
2. `responder(accion)`: valida, emite acierto/error con su feedback, guarda el avance en Room.
3. `terminar()`: calcula `ResultadoPartida`, escribe en la outbox, suma XP local, marca la racha y encola la sincronización.
4. Si el usuario sale: la partida queda en Room. Al volver al juego se pregunta "¿Retomar la partida?" (mismo patrón que CU-06 A2). Una partida guardada vence a las 24 h.

### 4.2 Persistencia (Room)
| Tabla | Campos | Uso |
|---|---|---|
| `partida_minijuego` | `id` (UUID), `juego`, `estadoJson`, `iniciadaEn`, `actualizadaEn` | Partida en curso (RNF-10, RN-12). Una por juego. |
| `resultado_minijuego` | `id` (UUID de la partida), `juego`, `aciertos`, `total`, `duracionMs`, `xp`, `conceptosJson`, `terminadaEn`, `sincronizado` | Outbox para el servidor. El UUID hace idempotente el envío. |

Requiere migración de `AppDatabase` (subir versión, sin borrar datos).

### 4.3 XP y desempeño (paso 7: precisión y tiempo)
Propuesta, ajustable en un solo lugar (`CalculadorXp`):

```
precision   = aciertos / total
xpBase      = 10 (Ráfaga, Orden) · 15 (Misión, Eco Vocal)
bonoTiempo  = 0–5 según qué tan rápido terminó respecto al tiempo objetivo del juego
xp          = round(xpBase * precision) + (precision ≥ 0.7 ? bonoTiempo : 0)
```

- Partida abandonada → 0 XP (RN-09).
- Tope diario anti-farmeo: tras 5 partidas del mismo juego en el día, la XP se reduce a la mitad. El servidor aplica el mismo tope.
- Referencia: una lección da 60 XP; un minijuego debe valer menos (≈ 10–20) para que las lecciones sigan siendo el camino principal.

### 4.4 Dificultad adaptativa (teoría del flujo)
Regla simple y común: si el usuario lleva 3 aciertos seguidos, la siguiente ronda es más difícil (oración más larga, menos tiempo, distractores del mismo tema); si lleva 2 errores seguidos, baja un paso. Sin pantallas extra.

### 4.5 Feedback (paso 6)
- Acierto: color verde + vibración corta (`HapticFeedback`) + sonido breve opcional.
- Error: color de error + la respuesta correcta + `feedback_error` cuando exista.
- Audio de palabras y oraciones con `TextToSpeech` de Android (offline con voces instaladas), igual que el campo `audio: "tts:..."` de las lecciones.

---

## 5. Los cuatro minijuegos

### 5.1 Eco Vocal — pronunciación

**Objetivo de aprendizaje:** pronunciar palabras y frases cortas del nivel con precisión y fluidez. Es el juego que conecta con Azure AI Speech, núcleo del proyecto (§1.2 del PDF).

**Mecánica (M):**
1. Se muestra una palabra o frase corta (en inglés + traducción) y se reproduce con TTS. Puede repetirse el audio.
2. El usuario mantiene presionado el micrófono y la repite.
3. Se evalúa y se muestra: puntaje de precisión (0–100), palabras con color (verde/amarillo/rojo) y, si Azure lo da, el fonema con error.
4. ≥ 70 cuenta como acierto. Hasta 2 intentos por ronda.
5. 8 rondas: empieza con palabras sueltas y sube a frases de diálogo (dificultad adaptativa).

**Dinámica (D):** "eco" — escuchar, imitar, mejorar; la segunda oportunidad invita a corregir. **Estética (A):** desafío personal y seguridad al hablar, en privado.

**Contenido:** `vocabulario` (palabras) → `gramatica.ejemplos` y `dialogo.lineas` cortas (≤ 8 palabras).

**Tecnología:**
| Pieza | Propuesta |
|---|---|
| Evaluación | **Azure AI Speech – Pronunciation Assessment** (SDK `com.microsoft.cognitiveservices.speech:client-sdk`), con `PronunciationAssessmentConfig(textoReferencia, HundredMark, Phoneme)`. Devuelve AccuracyScore, FluencyScore, CompletenessScore y detalle por palabra/fonema. |
| Credenciales | La llave de Azure **no va en la app** (RNF-06, RN-13). Endpoint nuevo `GET /api/speech/token` en el backend que pide un token de 10 min a Azure (`issueToken`) para el usuario autenticado. |
| Audio | Captura con el SDK desde el micrófono en streaming; no se guarda archivo (RN-04). Azure no almacena el audio por defecto. |
| Sin conexión | Respaldo con `SpeechRecognizer` de Android (`EXTRA_PREFER_OFFLINE`): compara la transcripción con el texto de referencia (acierto si coincide normalizado). Sin puntaje fonético; se avisa al usuario. |
| Permiso | `RECORD_AUDIO` ya está en el manifiesto; falta pedirlo en tiempo de ejecución. |
| Latencia | Frases cortas para cumplir RN-05 (≤ 2.5 s). |

**Riesgos:** costo de Azure (capa gratuita F0: ~5 h de audio/mes; suficiente para pruebas); necesidad de red para el puntaje fonético. Comparte trabajo con RF-07/RF-09 del chat.

---

### 5.2 Misión Situacional — role-play guiado

**Objetivo de aprendizaje:** usar frases funcionales en una situación cotidiana (pedir en una cafetería, comprar ropa, preguntar una dirección), alineado con los "temas de comunicación" del MCER que ya cubren las lecciones.

**Mecánica (M):**
1. Se presenta la misión: escenario + 2–3 objetivos ("Pide un café", "Pregunta el precio", "Paga y despídete").
2. El personaje (NPC) dice una línea (texto + TTS).
3. El usuario responde eligiendo entre 3 opciones (una correcta, una gramaticalmente mal, una fuera de contexto). En dificultad alta: la respuesta se escribe o se dice por voz y se compara con las respuestas aceptadas.
4. Cada respuesta correcta avanza la conversación y puede cumplir un objetivo. Una incorrecta muestra por qué y el NPC reacciona ("Sorry?").
5. La misión termina al cumplir los objetivos; desempeño = respuestas correctas al primer intento + tiempo.

**Dinámica:** decisión en contexto, consecuencia inmediata. **Estética:** fantasía y descubrimiento ("estoy en la cafetería"), sin la presión de una conversación real.

**Contenido:** **guiones nuevos** en `backend/content/minijuegos/misiones/*.json`, uno por tema de lección (≈ 1 misión por lección, reutilizando su vocabulario y su `dialogo` como base). Forma sugerida:

```json
{
  "id": "mision-a1-cafeteria",
  "nivel": "A1",
  "leccion_id": "a1-comida-bebida",
  "escenario": "Estás en una cafetería y quieres desayunar.",
  "objetivos": ["pedir", "precio", "despedida"],
  "pasos": [
    {
      "npc": "Hi! What can I get you?",
      "opciones": [
        { "en": "Can I have a coffee, please?", "correcta": true, "cumple": "pedir", "concepto_id": "func.pedir_comida" },
        { "en": "I can has coffee.", "correcta": false, "feedback": "Después de 'I can' va el verbo sin cambios: 'Can I have…?'" },
        { "en": "My name is coffee.", "correcta": false, "feedback": "No responde a lo que te preguntan." }
      ],
      "aceptadas_texto": ["can i have a coffee please", "a coffee please", "i would like a coffee"]
    }
  ]
}
```

**Tecnología:** 100% local en v1 (guion lineal con opciones). Reutiliza TTS y, opcionalmente, el STT de Eco Vocal.

**Variante con IA (v2, opcional):** usar `POST /api/chat` con un modo `mision` (prompt de sistema con escenario y objetivos, nivel restringido como el chat actual) y que la IA devuelva qué objetivos se cumplieron. Más libre, pero necesita red, cuesta tokens y contradice "opera de forma local". Recomendación: **v1 local; la IA solo si sobra tiempo.**

---

### 5.3 Ráfaga de Palabras — vocabulario contra reloj

**Objetivo de aprendizaje:** reconocimiento rápido de vocabulario (inglés ↔ español).

**Mecánica (M):**
1. 60 segundos en el reloj.
2. Aparece una palabra en inglés (con audio opcional) y 4 opciones en español; o al revés en dificultad alta.
3. Acierto: +1, suma al combo (x2 a partir de 5 seguidas) y +1 s al reloj (v1 proponía +2 s; ver §10). Error: rompe el combo y −3 s; se muestra la respuesta correcta medio segundo.
4. Termina cuando el reloj llega a 0. Desempeño = aciertos / intentos, más el puntaje del combo.
5. Distractores del mismo módulo temático (más difícil) o de otros módulos (más fácil).

Variante de la ilustración (bote de basura): en vez de 4 opciones, aparece un par "palabra – traducción" y el usuario lo **acepta** (✔) o lo **descarta** al bote si está mal emparejado. Más rápido de jugar con una mano; se puede elegir una de las dos en la implementación.

**Dinámica:** presión de tiempo, rachas de combo. **Estética:** desafío y emoción ("una ronda más").

**Contenido:** `vocabulario.items[]` de lecciones no bloqueadas, ponderado por SRS. Con 24 lecciones hay suficiente; si el usuario solo tiene una lección disponible, se repiten palabras.

**Tecnología:** 100% local. Temporizador en el ViewModel con `viewModelScope` + `delay`; pausa automática si la app pasa a segundo plano (no se pierde tiempo).

---

### 5.4 Orden Maestro — ordenar la oración

**Objetivo de aprendizaje:** orden de palabras y estructuras básicas (sujeto-verbo-complemento, preguntas con `do/does`, posición de adjetivos y adverbios de frecuencia, etc.).

**Mecánica (M):**
1. Se muestra la traducción en español como pista y las palabras de la oración en inglés desordenadas como fichas.
2. El usuario toca las fichas en orden (o las arrastra) para armar la oración; puede tocar una ficha colocada para regresarla.
3. Al completar: se valida. Acierto → la oración se lee con TTS. Error → se marcan las fichas fuera de lugar y se permite un segundo intento.
4. 8 oraciones por partida, de 3–4 palabras a 7–9 según la dificultad adaptativa.

**Dinámica:** resolver un rompecabezas. **Estética:** desafío y satisfacción al "encajar" la oración.

**Contenido:** `gramatica.ejemplos[]` (con `concepto_id` gramatical) y `dialogo.lineas[]`.

**Detalles de validación:**
- Tokenizar por espacios; la puntuación final (`.`, `?`, `!`) va pegada a la última ficha o se omite.
- Comparar en minúsculas y sin puntuación.
- Algunas oraciones aceptan más de un orden ("Today I go…" / "I go… today"): campo opcional `ordenes_alternos` en el banco.
- Evitar oraciones con palabras repetidas ambiguas o > 9 palabras.

**Tecnología:** 100% local. `FlowRow` de Compose para las fichas.

---

## 6. Servidor: registro y sincronización

Endpoint nuevo (mismo estilo que los de lecciones: token Firebase, errores, rate limiting):

`POST /api/games/result`
```json
{
  "partida_id": "uuid",
  "juego": "orden-maestro",
  "aciertos": 7, "total": 8, "duracion_ms": 94000,
  "conceptos": [{ "concepto_id": "gram.prepositions_of_place", "aciertos": 2, "errores": 1 }]
}
```

El servidor:
1. Es **idempotente** por `partidaId` (reintentos del worker no duplican XP).
2. Recalcula la XP con la misma fórmula y aplica el tope por partida y diario; guarda la suya, no la del cliente.
3. Actualiza `srs_conceptos` con los aciertos/errores por concepto (RF-13).
4. Marca la actividad del día para la racha (RN-08).

Más `GET /api/speech/token` para Eco Vocal (§5.1).

En Android, `SincronizarPartidasWorker` (WorkManager, con restricción de red) envía la outbox, igual que `SincronizarIntentosWorker` de lecciones.

---

## 7. Plan de implementación

| Fase | Entregable | Dependencias nuevas |
|---|---|---|
| 0 | ✅ Catálogo 2x2 y navegación | — |
| 1 | Banco de contenido (script de exportación + lector en Android) | — |
| 2 | Motor común: rondas, Room, XP, resultado, retomar partida | Migración de Room |
| 3 | **Orden Maestro** | — |
| 4 | **Ráfaga de Palabras** | — |
| 5 | `POST /api/games/result` + worker de sincronización + SRS + racha | — |
| 6 | **Misión Situacional** (guiones de 3–4 temas A1 primero) | Contenido nuevo |
| 7 | **Eco Vocal** con Azure + respaldo offline | SDK de Azure Speech, recurso Azure, `/api/speech/token` |
| 8 | Imágenes del catálogo, sonidos, pulido visual | Assets |

Pruebas: `CalculadorXp`, validación de Orden Maestro y selección de rondas son funciones puras → pruebas unitarias. Endpoint con las mismas utilidades de `backend/test/`.

---

## 8. Trazabilidad

| Requisito | Dónde se cubre |
|---|---|
| RF-11 | §5 (cuatro juegos en solitario de vocabulario y gramática) |
| RF-13 / RN-07 | §3.3 ponderación, §6 registro por concepto |
| RF-14 / RN-09 | §4.3 XP atómica |
| RN-03 | §2 decisión 1 (contenido de lecciones revisado), linter para guiones |
| RN-04 / RNF-05 | §5.1 audio efímero |
| RN-05 | §4.5 feedback local, §5.1 frases cortas |
| RN-08 | §6 punto 4 |
| RNF-08 / RN-11 | §3 banco en `assets` |
| RNF-10 / RN-12 | §4.1–4.2 partida en Room |
| CU-07 A2 | §2 decisión 2 (solo lecciones no bloqueadas) |

---

## 9. Decisiones (antes abiertas)

1. **Eco Vocal:** **Azure desde el inicio**, como promete el PDF (§1.2, CU-05),
   con `SpeechRecognizer` de Android como respaldo automático sin red o si el
   backend no tiene Azure configurado (responde 503). El audio ilegible
   (confianza < 30%) muestra el panel de la Ilustración 40 y se repite sin gastar
   el intento (mismo criterio que CU-05 A1).
2. **Misión Situacional:** **v1 con guiones locales** (6: cuatro A1 y dos A2).
   La variante con IA queda para v2.
3. **Ráfaga de Palabras:** **"coinciden / al bote"**, la variante de la
   Ilustración 37 (se juega con una mano y es más rápida).
4. **XP:** los valores de §4.3 (10 o 15 de base + bono 0–5; mitad desde la 6.ª
   partida del mismo juego en el día; tope duro de 60 partidas diarias).
5. **Contenido:** los ejemplos de gramática **no** necesitaron traducción: en
   Orden Maestro su pista es el tema ("Tema: Some y any") y las líneas de
   diálogo usan su traducción. Los guiones de misión siguen las reglas del
   linter (ver `backend/README.md`, módulo de Minijuegos).
6. **Juegos y nivel:** **sí**, un usuario A2 juega con sus lecciones A2 no
   bloqueadas más todo A1 como repaso.

---

## 10. Lo que cambió respecto a la propuesta

- **Rondas por dificultad (§4.4):** cada partida arma un "mazo" con rondas de
  tres niveles (fácil/medio/difícil) y la dificultad adaptativa decide de cuál
  sale la siguiente. Así el estado completo de la partida se guarda en Room y
  se retoma idéntico.
- **Orden Maestro:** las líneas de diálogo con varias oraciones se separan en
  rompecabezas distintos; la primera ficha va en minúscula (salvo "I" y nombres
  propios) y la puntuación final se muestra fija, para no delatar el orden.
- **Misión Situacional:** tras 3 aciertos seguidos al primer intento, el
  siguiente paso se responde **escribiendo** (se acepta con ≥ 80% de palabras
  coincidentes) con un botón "Ver opciones" para volver a elegir.
- **Ráfaga:** cada acierto suma **1 s** (no 2): en pruebas, con +2 s alguien que
  contesta en menos de 2 s nunca veía el reloj llegar a 0. Además, los
  distractores no pueden compartir sentido con la respuesta correcta ("Hi! →
  ¡Hola!" no puede salir como incorrecto aunque en el banco "Hi!" sea "¡Hola!
  (informal)").
- **Ráfaga:** el reloj se detiene mientras se muestra el feedback de cada par
  (0.3 s al acertar, 1.2 s al fallar); el servidor tolera ese tiempo extra al
  validar la duración.
- **XP en el teléfono:** solo se suma al perfil la XP que **confirma el
  servidor** (igual que en Lecciones). Sin conexión, la pantalla final muestra la
  XP calculada en el teléfono con la leyenda "se confirma al reconectarte".
- **Racha local (RN-08):** el perfil guarda `ultimoDiaActivo`; terminar una
  partida sube la racha una vez por día y la reinicia si pasó un día sin
  actividad (`domain/Racha.kt`). Las lecciones aún no la actualizan en el
  teléfono (el servidor sí registra su evento de actividad).
- **Tiempo de juego:** solo cuenta el tiempo con la pantalla activa; al pasar a
  segundo plano se pausa (y en Ráfaga se detiene el reloj).
