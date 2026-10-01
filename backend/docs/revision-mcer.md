# Revisión del módulo de **Lecciones** frente al MCER

> Rama `feature/lecciones` · Trabajo Terminal 2026-B162 · Revisión del 2026-09-29
>
> Complementa [`lecciones-diseno.md`](lecciones-diseno.md) y la
> [Guía de autoría](guia-autoria-lecciones.md). Revisa **todo** lo que
> planteamos para Lecciones (documentos y las 24 lecciones) contra una sola
> fuente: el **Marco Común Europeo de Referencia para las Lenguas (MCER)**.

## 1. Criterio y fuente

**Fuente única:** Consejo de Europa (2001), *Marco común europeo de
referencia para las lenguas: aprendizaje, enseñanza, evaluación*. Traducción
oficial al español: Instituto Cervantes, 2002 (Madrid: MECD / Anaya), texto
publicado en el Centro Virtual Cervantes:
<https://cvc.cervantes.es/ensenanza/biblioteca_ele/marco/cvc_mer.pdf>.
Las páginas citadas son las **impresas** en esa edición.

**Criterio (RN-03 del Documento Técnico):** *"El chatbot y las lecciones
tienen prohibido generar contenido que exceda los descriptores de desempeño
de los niveles A1 y A2 definidos por el MCER."* Decisión del equipo tomada
en esta revisión: la RN-03 se aplica **por nivel**. Una lección A1 solo puede
practicar lo que describen los descriptores A1, porque la RN-02 hace que un
usuario A1 vea únicamente el catálogo A1.

Por cada lección se revisó:

1. **Tema:** que esté en los temas de comunicación del MCER (§4.2).
2. **Can-do y funciones:** que cada *can-do* y lo que se practica en el
   diálogo y los ejercicios tenga un descriptor del MCER **del nivel de la
   lección**, y que nada dependa de un descriptor de un nivel superior.
3. **Vocabulario y gramática:** contra lo que el MCER sí dice de ellos (§2).

## 2. Lo que el MCER dice y lo que no dice

Esto define hasta dónde se puede afirmar que el contenido "está basado en el
MCER".

| El MCER **sí** define | El MCER **no** define |
|---|---|
| Los seis niveles y sus descriptores de lo que el usuario **puede hacer** (Cuadro 1, p. 26; Cuadro 2, p. 30; escalas de los capítulos 4 y 5). | **Listas de palabras** por nivel. |
| Una lista de **temas de comunicación** (§4.2, pp. 55–56), tomada de *Threshold Level 1990*. | **Estructuras gramaticales** por nivel para una lengua concreta. |
| Descriptores **generales** de vocabulario y gramática por nivel (p. 109 y p. 111). | Cuántas palabras debe saber alguien en cada nivel. |

Citas textuales:

- p. 28: *"las listas detalladas de microfunciones, las formas gramaticales y
  el vocabulario se presentan en especificaciones lingüísticas para lenguas
  concretas (por ejemplo, Threshold Level, 1990)."*
- p. 111: *"No se considera posible elaborar una escala de la progresión
  relativa a la estructura gramatical que sea aplicable a todas las
  lenguas."*
- Riqueza de vocabulario (p. 109). **A1:** *"Tiene un repertorio básico de
  palabras y frases aisladas relativas a situaciones concretas."* **A2:**
  *"Tiene suficiente vocabulario para desenvolverse en actividades habituales
  y en transacciones cotidianas que comprenden situaciones y temas
  conocidos."*
- Corrección gramatical (p. 111). **A1:** *"Manifiesta un control limitado
  sobre unas pocas estructuras gramaticales y sintácticas sencillas dentro de
  un repertorio aprendido."* **A2:** *"Utiliza algunas estructuras sencillas
  correctamente, pero sigue cometiendo errores básicos sistemáticamente…"*

**Consecuencia:** con el MCER como única fuente, **no se puede afirmar que
una palabra o una estructura "es A1" o "es A2"**. Lo que sí se puede afirmar,
y se cita en cada lección, es:

- el **tema** está en §4.2;
- lo que la lección enseña a **hacer** (can-do) corresponde a descriptores
  de su nivel;
- el vocabulario y la gramática son una **selección del equipo** acotada por
  esos descriptores.

## 3. Revisión de lo que planteamos en los documentos

| # | Afirmación | Dónde estaba | Veredicto frente al MCER | Acción |
|---|---|---|---|---|
| 1 | El contenido se cura de "listas CEFR oficiales" (Cambridge A2 Key, Oxford 3000/5000, English Vocabulary Profile). | Diseño §1 (decisión 3), §2.3 (RN-03), §8.2, §9; Guía §2.1, §8 | ❌ Esas listas son de terceros (editoriales y exámenes), no del MCER. | Retiradas como fuente. El tema sale de §4.2 y el nivel se justifica con descriptores. |
| 2 | "601 palabras en A1 y 925 en A2" (Capel, 2010). | Diseño §8.1; Guía §2.1 | ❌ No es del MCER; el MCER no cuantifica vocabulario. | Retirado. El tamaño de 10–12 palabras por lección queda como decisión de diseño del equipo. |
| 3 | Temario de los *Topic Lists* de Cambridge. | Diseño §8.2; Guía §2 (columna "Topic list") | ⚠️ Casi todos coinciden con §4.2, pero la fuente citada no era el MCER. Dos temas no están en §4.2: *Communication and Technology* y *The Natural World*. | Temas reasignados a §4.2. A2-8 y A2-10 se reencuadraron (§5). |
| 4 | "Gramática núcleo por nivel" (A1: *to be*, *can*…; A2: *past simple*, *present perfect*…). | Diseño §8.3; Guía §2 | ⚠️ El MCER no asigna estructuras a niveles (p. 111). | Se mantiene como **decisión del equipo** y se declara así. Cada estructura se enseña solo para funciones que tienen descriptor en el nivel. |
| 5 | "Verificar el nivel de cada palabra" en EVP/Oxford. | Guía §8.2, §6.2, §7.2 | ❌ Con el MCER como única fuente no hay contra qué verificar palabras. | Sustituido por verificar la **función** contra un descriptor (Guía §8.2). |
| 6 | En `autoria.fuentes`: "nivel A1/A2 verificado por palabra" y "Oxford 3000 — palabras marcadas A1". | 21 lecciones nuevas | ❌ Falso: esa verificación no se hizo. | Reemplazado en las 24 lecciones por citas textuales del MCER con página. |
| 7 | Los *can-do* "Puedo…". | Todas las lecciones | ✅ El formato en primera persona coincide con el Cuadro 2 del MCER (p. 30). Varios contenidos estaban por encima del nivel (§4). | Corregidos los que excedían. |
| 8 | Temas prohibidos "según la política de Cambridge". | Guía §6.3 | ⚠️ No es del MCER. | Se conserva como **criterio del equipo**, sin atribuirlo a Cambridge. |
| 9 | Umbral de aprobación del 70%. | Diseño §2.3, §9 | — No es un tema del MCER; ya estaba documentado como decisión del Documento Técnico. | Sin cambio. |
| 10 | En la tesis, la referencia [19] del MCER es "P. C. ARG, 2026". | Documento Técnico §2.2.1 | ⚠️ No es la fuente primaria. | **Recomendación:** citar Consejo de Europa (2001) y la traducción del Instituto Cervantes (2002), como en §1 de este documento. |

## 4. Revisión lección por lección

✅ = se ajusta a descriptores de su nivel sin cambios · 🔧 = se corrigió en
esta revisión (detalle en §5). Las citas textuales completas de cada lección
están en su `autoria.fuentes`.

### A1

| # | Lección | Tema MCER §4.2 | Descriptores A1 que la respaldan | |
|---|---|---|---|---|
| 1 | `a1-saludos-presentaciones` | Relaciones con otras personas; Identificación personal | Conversación (p. 77); Cuadro 1 (p. 26); Adecuación sociolingüística (p. 119) | ✅ |
| 2 | `a1-informacion-personal` | Identificación personal | Cuadro 1 (p. 26); Intercambiar información (p. 80) | ✅ |
| 3 | `a1-numeros-hora` | Vida cotidiana | Interactuar para obtener bienes y servicios (p. 80); Intercambiar información (p. 80) | ✅ |
| 4 | `a1-familia-amigos` | Identificación personal; Relaciones con otras personas | Cuadro 1 (p. 26); Cuadro 2, Comprensión auditiva (p. 30) | ✅ |
| 5 | `a1-ropa-colores` | Compras; Identificación personal | Cuadro 1 (p. 26); Expresión oral en general (p. 62) | ✅ |
| 6 | `a1-casa-hogar` | Vivienda, hogar y entorno | Cuadro 2, Expresión oral (p. 30); Expresión oral en general (p. 62) | 🔧 can-do |
| 7 | `a1-rutina-diaria` | Vida cotidiana | Expresión oral en general (p. 62); Intercambiar información (p. 80); Monólogo sostenido (p. 62) | 🔧 can-do |
| 8 | `a1-comida-bebida` | Comidas y bebidas | Interactuar para obtener bienes y servicios (p. 80); Interacción oral en general (p. 76) | 🔧 contenido |
| 9 | `a1-ciudad-lugares` | Lugares | Intercambiar información (p. 80); Leer instrucciones (p. 73); Interacción oral en general (p. 76) | 🔧 contenido |
| 10 | `a1-tiempo-libre` | Tiempo libre y ocio | Monólogo sostenido (p. 62); Intercambiar información (p. 80) | 🔧 contenido |
| 11 | `a1-clima-ahora` | Condiciones atmosféricas | Expresión oral en general (p. 62); Interacción oral en general (p. 76) | 🔧 can-do |
| 12 | `a1-compras` | Compras | Interactuar para obtener bienes y servicios (p. 80); Adecuación sociolingüística (p. 119); Interacción oral en general (p. 76) | ⚠️ límite A1/A2 |

### A2

| # | Lección | Tema MCER §4.2 | Descriptores A2 que la respaldan | |
|---|---|---|---|---|
| 1 | `a2-fin-de-semana` | Tiempo libre y ocio | Intercambiar información (p. 80); Monólogo sostenido (p. 62); Cuadro 1 (p. 26) | ✅ |
| 2 | `a2-viajes-transporte` | Viajes | Interactuar para obtener bienes y servicios (p. 80); Monólogo sostenido (p. 62) | ✅ |
| 3 | `a2-anecdotas` | Relaciones con otras personas | Monólogo sostenido (p. 62); Conversación (p. 77) | 🔧 recibe los gustos |
| 4 | `a2-trabajo-profesiones` | Identificación personal (a lo que se dedican) | Monólogo sostenido (p. 62); Intercambiar información (p. 80) | ✅ |
| 5 | `a2-salud-ejercicio` | Salud y cuidado corporal | Conversación (p. 77); Competencia lingüística general (p. 107) | 🔧 can-do |
| 6 | `a2-planes-futuro` | Tiempo libre y ocio (cine, teatro, conciertos; radio y televisión) | Monólogo sostenido (p. 62); Adecuación sociolingüística (p. 119) | 🔧 can-do |
| 7 | `a2-comparar-lugares` | Lugares | Monólogo sostenido (p. 62); Adecuación sociolingüística (p. 119) | 🔧 can-do |
| 8 | `a2-llamadas-mensajes` | Servicios públicos (teléfono, correo); Relaciones con otras personas | Notas, mensajes y formularios (p. 82); Monólogo sostenido (p. 62) | 🔧 reencuadrada |
| 9 | `a2-novedades` | Educación | Monólogo sostenido (p. 62); Intercambiar información (p. 80) | ✅ |
| 10 | `a2-clima-excursiones` | Condiciones atmosféricas; Tiempo libre y ocio | Escuchar avisos e instrucciones (p. 70); Monólogo sostenido (p. 62); Conversación (p. 77) | 🔧 reencuadrada |
| 11 | `a2-servicios-compras` | Servicios públicos; Compras | Interactuar para obtener bienes y servicios (p. 80); Leer instrucciones (p. 73) | ✅ |
| 12 | `a2-describir-personas-cosas` | Identificación personal; Relaciones con otras personas | Monólogo sostenido (p. 62) | ✅ |

## 5. Hallazgos y cambios aplicados

### 5.1 Contenido por encima del nivel de la lección

| Lección | Qué excedía | Dónde lo ubica el MCER | Cambio |
|---|---|---|---|
| A1-8 Comida | Expresar gustos (*I like…*) | A2 — Conversación: *"Sabe expresar lo que le gusta y lo que no le gusta"* (p. 77) | La lección pasa a **pedir** (A1: *"Es capaz de pedirle a alguien alguna cosa"*, p. 80) con *want*. `voc.comida.like` → `voc.comida.want`. |
| A1-10 Tiempo libre | *like + -ing* (gustos) | A2 — Conversación (p. 77) | `gram.like_ing` se movió a **A2-3**. En A1 queda solo *can* (lo que sé hacer). |
| A1-9 Ciudad | **Dar** indicaciones | A2 — Intercambiar información: *"Da y comprende indicaciones e instrucciones sencillas; por ejemplo, explica cómo ir a un lugar"* (p. 80) | A1 solo **pide y entiende** indicaciones (A1: *"comprende indicaciones breves y sencillas sobre cómo ir a un lugar"*, p. 80). Los ejercicios de imperativo ahora son de comprensión. |
| A1-7 Rutina | "Contar mi rutina" (describir costumbres) | A2 — Monólogo sostenido: *"Describe… costumbres, actividades habituales…"* (p. 62) | El can-do pasa a *frases sencillas* + *a qué hora* (A1, pp. 62 y 80). El contenido ya eran frases simples. |
| A2-10 Naturaleza | "Decir qué pasará si ocurre algo" (hipótesis) | B2 — Conversación informal: *"realizando hipótesis"* (p. 77) | Reencuadrada como **Clima y excursiones**: el primer condicional se usa para **planes** que dependen del clima (A2: *"Describe planes y citas"*, p. 62; *"Realiza invitaciones y sugerencias"*, p. 77). |
| A2-5 Salud | "Dar consejos" | Solo aparece en B2 — Intercambiar información: *"…información compleja y consejos…"* (p. 80) | *should* se presenta como **sugerencia** (A2: *"Realiza invitaciones y sugerencias"*, p. 77). |
| A2-7 Comparar | "Decir cuál es el mejor **y por qué**" | B1 — Cuadro 1: *"justificar brevemente sus opiniones"* (p. 26) | Queda *"qué lugar me parece el mejor"* (A2: *"expresa opiniones y actitudes de forma sencilla"*, p. 119). |
| A2-6 Planes | "Hacer predicciones" (sin descriptor A2) | — | Pasa a **opinión sencilla** sobre el futuro (A2, p. 119). |

Ajustes de redacción sin cambio de contenido: A1-6 (*"describir con frases
sencillas el lugar donde vivo"*, Cuadro 2 A1) y A1-11 (*"con frases
sencillas"*).

### 5.2 Temas fuera de §4.2

| Antes | Después | Tema MCER |
|---|---|---|
| `a2-tecnologia-comunicacion` (apps, contraseñas, descargas) | `a2-llamadas-mensajes`: llamar, contestar, dejar y tomar mensajes, cartas, correo | Servicios públicos (teléfono, correo); A2 — Notas, mensajes y formularios: *"Toma mensajes breves y sencillos…"* (p. 82) |
| `a2-naturaleza-clima` (planeta, basura, medio ambiente) | `a2-clima-excursiones`: pronóstico, temperatura, excursión | Condiciones atmosféricas; Tiempo libre y ocio |

La gramática de ambas (`gram.present_perfect_ever_never`,
`gram.first_conditional`) no cambió. Como no se habían publicado, se
cambiaron sus `id` y los conceptos de vocabulario que ya no se usan.

### 5.3 Caso límite que se deja documentado

**A1-12 Compras.** *"Realiza compras sencillas diciendo lo que quiere y
preguntando el precio"* es A2 (p. 80). La lección se sostiene en A1 porque
lo que practica cabe en *"Es capaz de pedirle a alguien alguna cosa… Se
desenvuelve bien con números, cantidades, precios y horarios"* (A1, p. 80) y
en *"por favor, gracias"* (A1, p. 119). Si el equipo prefiere no quedar en
el límite, la alternativa es mover *How much…?* a A2-11.

### 5.4 Trazabilidad automática

- Cada lección cita en `autoria.fuentes` su tema de §4.2 y los descriptores
  de su nivel, **textuales y con página**.
- El linter (`fuentesMcer` en `scripts/lint-content.js`) **falla** si falta
  el tema de §4.2 o si no hay al menos un descriptor del nivel de la
  lección.

## 6. Pendientes

1. **Volumen complementario (Consejo de Europa, 2020).** Actualiza y amplía
   los descriptores; por ejemplo, agrega la escala de interacción en línea,
   que podría respaldar un tema de tecnología. Esta revisión usó el texto de
   2001/2002 porque el sitio del Consejo de Europa bloqueó la descarga
   automática. Conviene cotejar las citas de §4 con el volumen de 2020 antes
   de la entrega de la tesis.
2. **Referencia [19] de la tesis:** reemplazarla por la fuente primaria
   (§3, fila 10).
3. **Gramática por lección:** es decisión del equipo (§2). Debe presentarse
   así en la tesis, no como "gramática del MCER".
4. **Validación del equipo** de las correcciones de §5, sobre todo A1-8 y
   A1-10, que cambiaron de contenido.
