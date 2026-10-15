# Guía de autoría de **Lecciones** — XP-SPEAK

> Complemento de [`lecciones-diseno.md`](lecciones-diseno.md). Aquel documento
> explica **cómo funciona** el módulo (arquitectura, API, estados, Firestore);
> este explica **cómo escribir el contenido** para que cualquier persona del
> equipo produzca lecciones consistentes sin leer el código.
>
> Si solo vas a escribir una lección: lee §1, busca tu lección en §2 y copia
> [`plantilla-leccion.json`](plantilla-leccion.json).
>
> **Fuente única: el MCER.** Qué dice y qué no dice el MCER sobre temas,
> vocabulario y gramática, y la revisión de las 24 lecciones, están en
> [`revision-mcer.md`](revision-mcer.md).

---

## 1. Checklist para agregar una lección

1. **Busca la lección en el plan (§2).** Ahí están su `id`, `orden`,
   prerequisito, tema, can-do y gramática. Si no está en el plan, primero
   agrégala al plan (y acuérdalo con el equipo), luego escríbela.
2. **Copia la plantilla** `docs/plantilla-leccion.json` a
   `content/lessons/<id>.json`. El nombre del archivo **debe** ser igual al `id`.
3. **Llena los metadatos** (§3.1) y el XP según §4.
4. **Busca en el MCER** los descriptores de tu nivel que respaldan cada
   can-do (§8.2) y elige vocabulario del tema de §4.2 que sirva para esas
   funciones.
5. **Escribe las secciones** siguiendo las reglas de tamaño (§3) y el estilo (§6).
6. **Asigna los `concepto_id`** (§5): reutiliza los que ya existen, crea
   nuevos solo para lo que se enseña por primera vez.
7. **Llena `autoria.fuentes`** con el tema del MCER y los descriptores
   textuales, con página (§8.3). El linter lo exige.
8. **Corre las validaciones:**
   ```bash
   npm run lint:content   # schema + reglas semánticas (§7)
   npm test               # incluye el linter sobre el contenido real
   ```
9. **Revisión humana** con la lista de §7.2 (lo que el linter no puede ver).
10. **Pruébala en la app:** `npm run dev:app`, `adb reverse tcp:3000 tcp:3000`,
    aprueba la lección anterior y verifica que la nueva se desbloquea.

---

## 2. Plan curricular (temario A1 y A2)

> **Estado: v2 (2026-09-29) — las 24 lecciones del plan están escritas** en
> `content/lessons/` y pasan el linter y las pruebas (`test/contenido.test.js`).
> El plan sigue pendiente de validar por el equipo. Cambiar el plan es válido,
> pero se cambia **aquí primero** y luego en los JSON.

### 2.1 Cómo se armó

- **Temas:** de los *temas de comunicación* del MCER (§4.2, pp. 55–56):
  identificación personal; vivienda, hogar y entorno; vida cotidiana; tiempo
  libre y ocio; viajes; relaciones con otras personas; salud y cuidado
  corporal; educación; compras; comidas y bebidas; servicios públicos;
  lugares; lengua extranjera; condiciones atmosféricas. **No** se usan temas
  fuera de esa lista.
- **Can-do:** cada uno se apoya en un **descriptor del MCER del nivel de la
  lección** (escala global, cuadro de autoevaluación o escalas ilustrativas).
  La RN-03 se aplica **por nivel**: una lección A1 no practica nada que el
  MCER ubique en A2 o más arriba (`revision-mcer.md` §1).
- **Gramática:** el MCER **no** asigna estructuras a niveles (*"No se
  considera posible elaborar una escala de la progresión relativa a la
  estructura gramatical que sea aplicable a todas las lenguas"*, p. 111).
  Las estructuras de las tablas son una **decisión del equipo**: dos por
  lección como máximo, en orden de dificultad, y solo al servicio de
  funciones que tienen descriptor en el nivel.
- **Tamaño:** 12 lecciones por nivel × ~10–12 palabras. Es una decisión de
  diseño: el MCER no cuantifica vocabulario. Describe su alcance: A1
  *"repertorio básico de palabras y frases aisladas relativas a situaciones
  concretas"*; A2 *"suficiente vocabulario para desenvolverse en actividades
  habituales y en transacciones cotidianas…"* (p. 109). XP-SPEAK es una app de
  **refuerzo** (RF-10), no un curso completo.
- **Una sola cadena por nivel.** RN-02 dice que el nivel elegido rige "de forma
  exclusiva" el catálogo, así que un usuario A2 **nunca ve** las lecciones A1.
  Por eso la primera lección de cada nivel tiene `prerequisito_id: null`.

### 2.2 Nivel A1

| # | `id` | Tema (MCER §4.2) | Can-do (resumen) | Gramática (`concepto_id`) | Estado |
|---|---|---|---|---|---|
| 1 | `a1-saludos-presentaciones` | Relaciones con otras personas | Saludar, despedirse, presentarse | `gram.subject_pronouns`, `gram.verb_to_be_present` | ✅ hecha |
| 2 | `a1-informacion-personal` | Identificación personal | Dar y pedir nombre, país, ciudad, edad | `gram.wh_questions_basic`, `gram.have_got` | ✅ hecha |
| 3 | `a1-numeros-hora` | Vida cotidiana | Contar 0–20, decir la hora | `gram.there_is_are`, `gram.how_many_questions` | ✅ hecha |
| 4 | `a1-familia-amigos` | Identificación personal | Presentar a la familia; decir de quién es algo | `gram.possessive_adjectives`, `gram.possessive_s` | ✅ hecha |
| 5 | `a1-ropa-colores` | Compras; Identificación personal | Nombrar mi ropa y su color | `gram.articles_a_an_the`, `gram.plural_nouns` | ✅ hecha |
| 6 | `a1-casa-hogar` | Vivienda, hogar y entorno | Describir con frases sencillas dónde vivo y dónde están las cosas | `gram.prepositions_of_place` (+ repaso `gram.there_is_are`) | ✅ hecha |
| 7 | `a1-rutina-diaria` | Vida cotidiana | Decir con frases sencillas qué hago y a qué hora | `gram.present_simple_affirmative`, `gram.present_simple_third_person` | ✅ hecha |
| 8 | `a1-comida-bebida` | Comidas y bebidas | Pedir comida y bebida; preguntar si hay algo | `gram.present_simple_negative_questions`, `gram.some_any` | ✅ hecha |
| 9 | `a1-ciudad-lugares` | Lugares | Pedir indicaciones y entender indicaciones sencillas | `gram.imperatives`, `gram.this_that_these_those` | ✅ hecha |
| 10 | `a1-tiempo-libre` | Tiempo libre y ocio | Decir qué sé hacer y qué no | `gram.can_ability` | ✅ hecha |
| 11 | `a1-clima-ahora` | Condiciones atmosféricas | Describir el clima y lo que pasa ahora | `gram.present_continuous` | ✅ hecha |
| 12 | `a1-compras` | Compras | Preguntar y entender precios; pedir algo con please | `gram.how_much_price`, `gram.would_like` | ✅ hecha |

Prerequisito de cada lección = la anterior (#1 tiene `null`).

### 2.3 Nivel A2

| # | `id` | Tema (MCER §4.2) | Can-do (resumen) | Gramática (`concepto_id`) | Estado |
|---|---|---|---|---|---|
| 1 | `a2-fin-de-semana` | Tiempo libre y ocio | Contar qué hice el fin de semana | `gram.past_to_be`, `gram.past_simple_regular` | ✅ hecha |
| 2 | `a2-viajes-transporte` | Viajes | Contar un viaje que hice | `gram.past_simple_irregular`, `gram.past_simple_negative_questions` | ✅ hecha |
| 3 | `a2-anecdotas` | Relaciones con otras personas | Contar algo que me pasó, cómo me sentí y qué me gusta | `gram.past_continuous`, `gram.like_ing` | ✅ hecha |
| 4 | `a2-trabajo-profesiones` | Identificación personal | Hablar de mi trabajo o estudios y con qué frecuencia hago algo | `gram.adverbs_of_frequency`, `gram.present_simple_vs_continuous` | ✅ hecha |
| 5 | `a2-salud-ejercicio` | Salud y cuidado corporal | Decir qué me duele; hacer sugerencias sencillas | `gram.should_advice`, `gram.have_to_obligation` | ✅ hecha |
| 6 | `a2-planes-futuro` | Tiempo libre y ocio | Hablar de planes; opinar de forma sencilla sobre el futuro | `gram.going_to_plans`, `gram.will_predictions` | ✅ hecha |
| 7 | `a2-comparar-lugares` | Lugares | Comparar lugares y decir cuál me parece mejor | `gram.comparatives`, `gram.superlatives` | ✅ hecha |
| 8 | `a2-llamadas-mensajes` | Servicios públicos (teléfono, correo) | Dejar y tomar mensajes; contar experiencias | `gram.present_perfect_ever_never` | ✅ hecha |
| 9 | `a2-novedades` | Educación | Decir lo que ya hice o todavía no hago | `gram.present_perfect_just_already_yet` | ✅ hecha |
| 10 | `a2-clima-excursiones` | Condiciones atmosféricas; Tiempo libre y ocio | Hablar del clima y hacer planes según el clima | `gram.first_conditional` | ✅ hecha |
| 11 | `a2-servicios-compras` | Servicios públicos; Compras | Resolver trámites y compras con cantidades; entender normas | `gram.quantifiers_much_many`, `gram.must_mustnt` | ✅ hecha |
| 12 | `a2-describir-personas-cosas` | Identificación personal | Describir personas y cosas; decir para qué sirve algo | `gram.relative_clauses_basic`, `gram.infinitive_of_purpose` | ✅ hecha |

Prerequisito de cada lección = la anterior (#1 tiene `null`, ver §2.1).

### 2.4 Qué hacer con A1 que se necesita en A2

Un usuario A2 no pasó por A1, y el linter solo acepta evaluar conceptos
enseñados **en la cadena del propio nivel**. Si una lección A2 necesita algo de
A1 (por ejemplo *to be*), puede **volver a enseñarlo** en una sección de
gramática breve **con el mismo `concepto_id`** (`gram.verb_to_be_present`).
Así el SRS lo sigue tratando como un solo concepto (§5.3).

---

## 3. Estructura y tamaño de una lección

### 3.1 Metadatos

| Campo | Regla |
|---|---|
| `id` | `a1-` o `a2-` + tema en español, minúsculas, sin acentos, con guiones. Igual al del plan. |
| `version` | `1` al crear. Súbelo cada vez que edites contenido que ve el usuario (§9). |
| `modulo_tematico` | Nombre visible en el catálogo, en español, 2–4 palabras. |
| `orden` | El número del plan. Único dentro del nivel. |
| `prerequisito_id` | El `id` de la lección anterior del **mismo nivel**; `null` en la #1. |
| `can_do` | 1–3 frases en primera persona que empiezan con "Puedo…". Cada una debe apoyarse en un descriptor del MCER **del nivel de la lección** (§8.2). |

### 3.2 Secciones (en este orden)

| Sección | Cuántas | Tamaño |
|---|---|---|
| `introduccion` | 1, siempre la primera | `cuerpo` de 1–2 frases (≤ 250 caracteres): qué vas a aprender. |
| `vocabulario` | 1–2 | **10–12 ítems en total** (máximo 15). Palabras y también expresiones hechas. |
| `gramatica` | 1–2 | `explicacion` ≤ 400 caracteres; 2–4 `ejemplos`. |
| `dialogo` | 1 | 4–6 líneas, 2–3 hablantes; usa el vocabulario y la gramática de la lección. |
| `evaluacion` | 1, siempre la última | **6–8 ítems**, umbral `0.7` (RN-06). |

### 3.3 Evaluación

- **Mezcla de tipos:** al menos 1 `opcion_multiple`, 1 `completar` y
  **exactamente 1** `emparejar` (con 4 pares), igual que las lecciones hechas.
- **Cobertura:** cada `gram.*` que enseña la lección tiene **al menos 1 ítem**;
  el resto evalúa vocabulario.
- **Repaso en espiral:** máximo **2 ítems** de conceptos de lecciones anteriores.
- **Posición de la respuesta:** varía el índice de `respuesta_correcta`. No
  pongas siempre la correcta en el mismo lugar (en las lecciones 1–3 de A1,
  8 de 9 ítems la tienen en `1`; corregirlo en su próxima edición).
- **Ids:** `q1`, `q2`, … en orden.
- **Por qué 70% con 6–8 ítems:** con 6 ítems se aprueba con 5 (83%); con 7,
  con 5 (71%); con 8, con 6 (75%). Menos de 6 ítems hace que un solo error
  cambie demasiado el resultado.

---

## 4. XP

> **Propuesta, a confirmar con Gamificación (RF-14).**

| Nivel | XP por lección |
|---|---|
| A1 | **60** (la lección 1 se queda en 50 como lección de bienvenida) |
| A2 | **80** |

- El XP es fijo por nivel, **no** depende de la dificultad percibida de cada
  lección: así el total por nivel es predecible (A1 = 50 + 11 × 60 = 710 XP).
- Solo se otorga la primera vez que se aprueba (RN-09, ya implementado).

---

## 5. `concepto_id`

### 5.1 Formato (lo valida el schema)

| Tipo | Formato | Ejemplo |
|---|---|---|
| Vocabulario | `voc.<tema>.<lema>` | `voc.familia.grandmother` |
| Gramática | `gram.<estructura>` | `gram.possessive_adjectives` |
| Función (opcional) | `func.<funcion>` | `func.pedir_indicaciones` |

- `<tema>`: una palabra en español, la misma para toda la lección
  (`saludos`, `personal`, `numeros`, `familia`…).
- `<lema>`: la palabra o expresión en inglés en `snake_case`
  (`good_morning`, `years_old`).
- `<estructura>`: nombre en inglés en `snake_case`, como en §2.

### 5.2 ¿Reutilizo o creo uno nuevo?

Antes de crear un `concepto_id`, búscalo:

```bash
grep -rho '"concepto_id": "[^"]*"' content/lessons | sort | uniq -c
```

- **Ya existe y es lo mismo** (misma palabra con el mismo significado, misma
  regla) → **reutilízalo**, aunque la lección sea de otro tema. No crees
  `voc.familia.hello` si ya existe `voc.saludos.hello`.
- **Misma palabra, otro significado** → concepto nuevo.
- **Nunca** cambies ni borres un `concepto_id` que ya se publicó: se pierde el
  historial SRS de los usuarios. Si deja de usarse, simplemente ya no aparece.

### 5.3 Por qué importa

El SRS (RF-13, RN-07) acumula aciertos y fallos **por concepto**, sumando todas
las lecciones donde aparece. Un mismo concepto con dos ids se contaría como
dos, y el usuario vería el mismo punto débil repetido.

---

## 6. Guía de estilo

### 6.1 Español (instrucciones y traducciones)

- **Tuteo** y español de México neutro: palabras de uso general ("papá",
  "celular"), sin modismos regionales ni coloquiales ("¿Qué onda?").
- Frases cortas; el usuario es principiante. Nada de jerga gramatical sin
  explicarla ("sustantivo" sí, "determinante" no).
- Traducciones **naturales**, no palabra por palabra. Si hay dos formas comunes,
  sepáralas con `/`: `"mamá / madre"`.
- Usa `nota` para advertir falsos amigos o diferencias de uso
  (`parents` ≠ parientes; `Good evening` vs `Good night`).

### 6.2 Inglés

- **Nada que exceda el nivel** (RN-03): lo que el diálogo y los ejercicios
  hacen practicar debe tener un descriptor del MCER del nivel de la lección
  (§8.2). El vocabulario se elige para las situaciones concretas del tema:
  A1 = *"palabras y frases aisladas relativas a situaciones concretas"*;
  A2 = *"actividades habituales y transacciones cotidianas"* (MCER, p. 109).
- Inglés estándar; cuando haya diferencia británico/estadounidense, usa
  el estadounidense y menciona el otro en `nota` si es común.
- Contracciones solo en diálogos y ejemplos de conversación, y después de
  haberlas explicado (`I'm`, `she's`).

### 6.3 Personajes y contexto

- Nombres latinos (Laura, Diego, Ana, Sofía, Luis…) y lugares de México
  (Puebla, Oaxaca, CDMX). El usuario es universitario mexicano.
- Situaciones cotidianas de un estudiante: clase, casa, familia, amigos,
  transporte, comida.
- Temas prohibidos (criterio del equipo; no viene del MCER): guerra,
  política, religión, muerte, temas que puedan incomodar.

### 6.4 Ejercicios

- **`enunciado`**: instrucción en español; si el ítem es una frase en inglés,
  que se entienda qué hay que hacer.
- **`completar`**: un solo hueco `___`, idealmente de una palabra.
  - `acepta` debe listar **todas** las respuestas válidas. El calificador solo
    ignora mayúsculas, espacios, puntuación final y apóstrofos curvos; **no**
    entiende contracciones: si vale `I'm` y `I am`, lista las dos.
- **`opcion_multiple`**: 3–4 opciones. Los distractores deben ser **errores
  típicos** de hispanohablantes (`the father of Diego's`, `parientes`), no
  opciones absurdas. Nada de "todas las anteriores".
- **`emparejar`**: 4 pares; `izq` en inglés, `der` en español; sin textos
  repetidos.
- **`feedback_error`**: explica **la regla** en ≤ 140 caracteres, no solo la
  respuesta. ✗ "La respuesta es 'her'." ✓ "Sofía es mujer, así que usamos 'her'."

---

## 7. Validaciones

### 7.1 Lo que revisa la máquina (`npm run lint:content`)

**Schema** (`content/schema/leccion.schema.json`):
- `id` con patrón `a1-…`/`a2-…`; `version` y `orden` ≥ 1; `xp_recompensa` 1–500.
- `concepto_id` con el formato de §5.1; `audio` empieza con `tts:` o `https://`.
- Campos obligatorios de cada sección e ítem (incluidos `enunciado` y
  `feedback_error` en **todos** los tipos, también `emparejar`).
- `opcion_multiple`: 2–6 opciones. `emparejar`: 2–8 pares.
  `completar`: el enunciado contiene `___`. Evaluación: mínimo 3 ítems;
  `umbral_aprobacion` entre 0.5 y 1.

**Reglas semánticas** (`scripts/lint-content.js`):
1. El archivo se llama `<id>.json`.
2. `introduccion` es la primera sección; hay **una** `evaluacion` y es la última.
3. `prerequisito_id` existe y no forma ciclos.
4. `respuesta_correcta` dentro del rango de opciones; opciones sin repetir.
5. `acepta` incluye la `respuesta_correcta`.
6. En `emparejar`, sin `izq` ni `der` repetidos.
7. Ids de ítem únicos en la lección; `id` de lección único; `orden` único por nivel.
8. **No se evalúa nada que no se haya enseñado** en la lección o en su cadena
   de prerequisitos.
9. **RN-03:** `autoria.fuentes` cita el tema del MCER §4.2 y al menos un
   descriptor del nivel de la lección (formato de §8.3).

### 7.2 Lo que revisa una persona

- [ ] Cada can-do, y lo que practican el diálogo y los ejercicios, tiene un
      descriptor del MCER **de su nivel** (§8.2); nada que el MCER ubique en
      un nivel superior.
- [ ] Los tamaños de §3 (el linter solo pone mínimos amplios).
- [ ] Hay al menos un ítem por cada gramática enseñada y la mezcla de tipos de §3.3.
- [ ] `acepta` cubre contracciones y variantes válidas.
- [ ] Los distractores son creíbles y la posición de la correcta varía.
- [ ] Traducciones naturales; tuteo; sin temas sensibles.
- [ ] `concepto_id` reutilizados donde corresponde (§5.2).
- [ ] `autoria.fuentes` cita textualmente los descriptores que respaldan la
      lección, con página, y el tema de §4.2.

---

## 8. Fuentes

**Fuente única: el MCER.** Consejo de Europa (2001), *Marco común europeo de
referencia para las lenguas*, trad. Instituto Cervantes (2002):
<https://cvc.cervantes.es/ensenanza/biblioteca_ele/marco/cvc_mer.pdf>. Las
páginas son las impresas en esa edición. La revisión completa está en
[`revision-mcer.md`](revision-mcer.md).

### 8.1 Qué parte del MCER se usa para qué

| Decisión | Parte del MCER |
|---|---|
| Nivel general de la lección | Cuadro 1, escala global (p. 26); Cuadro 2, autoevaluación (p. 30) |
| Tema | §4.2 Temas de comunicación (pp. 55–56) |
| Can-do y funciones (qué se practica) | Escalas ilustrativas del cap. 4: expresión oral y monólogo (p. 62), escuchar avisos (p. 70), leer instrucciones (p. 73), interacción oral y conversación (pp. 76–77), bienes y servicios e intercambiar información (p. 80), notas y mensajes (p. 82) |
| Alcance del vocabulario | Riqueza de vocabulario (p. 109) |
| Alcance de la gramática | Corrección gramatical (p. 111) y competencia lingüística general (p. 107) |
| Cortesía y registro | Adecuación sociolingüística (p. 119) |

**Lo que el MCER no da:** listas de palabras ni de estructuras por nivel
(pp. 28 y 111). Las palabras y la gramática de cada lección son selección del
equipo, acotada por los descriptores. No se deben presentar como "palabras
A1" ni "gramática A2 del MCER".

### 8.2 Cómo verificar que algo es del nivel

1. Escribe qué **hace** el usuario en la lección (can-do) y en cada ejercicio:
   pedir algo, entender una indicación, describir su casa…
2. Busca esa acción en las escalas de §8.1 y fíjate en qué **nivel** aparece.
3. Si aparece en el nivel de la lección, o en uno inferior, es válida. Copia
   el descriptor textual a `autoria.fuentes`.
4. Si solo aparece en un nivel **superior**, cambia la función o muévela a
   otra lección. Ejemplos de la revisión: expresar gustos es A2 y no entra
   en A1; dar consejos aparece hasta B2 y en A2 se usan *sugerencias*.
5. Si no hay descriptor en ningún nivel, redacta el can-do con la función
   más cercana que sí lo tenga.

### 8.3 Cómo llenar `autoria`

- `fuentes`, en este formato (el linter revisa las dos primeras formas):
  - Tema: `"MCER (Consejo de Europa, 2001; trad. Instituto Cervantes, 2002), §4.2 Temas de comunicación (pp. 55–56): Comidas y bebidas."`
  - Un renglón por descriptor, textual: `"MCER, Interactuar para obtener bienes y servicios, A1 (p. 80): «Es capaz de pedirle a alguien alguna cosa, y viceversa…»"`
  - Siempre, el de vocabulario: `"MCER, Riqueza de vocabulario, A1 (p. 109): «…»"`
- `notas`: qué es original (ejemplos, diálogo, ejercicios), qué conceptos se
  repasan en espiral y cualquier caso límite de nivel.
- **No copies** oraciones, diálogos ni ejercicios de libros o exámenes. Los
  textos se escriben desde cero (`lecciones-diseno.md` §8.5).

---

## 9. Editar una lección que ya existe

- **Sube `version`** si cambias algo que ve el usuario (texto, ítems,
  respuestas). Así la app sabe que su copia quedó vieja.
- **No cambies** `id` ni `concepto_id` ya publicados (§5.2).
- Cambiar `autoria` no requiere subir `version` (no se envía a la app).
- Si cambias la evaluación, los intentos viejos no se recalculan: el progreso
  y el XP ya otorgados se quedan como estaban.

---

## 10. Plantilla

[`plantilla-leccion.json`](plantilla-leccion.json) es un ejemplo completo que
sigue todas las reglas de esta guía; es una copia de la lección 4
(`a1-familia-amigos`), que se publicó tal cual desde la plantilla. Un test (`test/lint-content.test.js`) comprueba que la
plantilla siempre pase el linter, así que si alguien cambia el schema, la
plantilla se actualiza junto con él.
