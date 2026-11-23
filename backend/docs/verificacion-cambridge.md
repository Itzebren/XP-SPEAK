# Verificación del vocabulario contra listas de **Cambridge**

> Trabajo Terminal 2026-B162 · Verificación del 2026-11-05
>
> Complementa [`revision-mcer.md`](revision-mcer.md). **La fuente del
> contenido sigue siendo el MCER**: de él salen el tema de cada lección
> (§4.2) y lo que el usuario aprende a hacer (descriptores del nivel).
> Cambridge solo se usa para **comprobar** que el vocabulario elegido
> corresponde al nivel.

## 1. Por qué Cambridge

El MCER no trae listas de palabras por nivel. Lo dice en la p. 28: las
formas gramaticales y el vocabulario «se presentan en especificaciones
lingüísticas para lenguas concretas (por ejemplo, Threshold Level, 1990)».
El Volumen complementario (2020; trad. 2021, p. 36) remite a esas mismas
especificaciones por lengua.

Para el inglés, las listas de vocabulario por nivel MCER más usadas son las
de Cambridge. La lista A2 Key explica de dónde sale: *«It drew on vocabulary
from the Council of Europe's Waystage (1990) specification and other
vocabulary which corpus evidence shows is high frequency»*. Se actualiza con
el Cambridge Learner Corpus y el English Vocabulary Profile.

## 2. Listas usadas

| Lista | Nivel MCER | Archivo oficial |
|---|---|---|
| *A2 Key and A2 Key for Schools Vocabulary List* (agosto de 2025) | A2 (incluye el vocabulario de A1) | https://www.cambridgeenglish.org/images/506886-a2-key-2020-vocabulary-list.pdf |
| *Pre A1 Starters, A1 Movers and A2 Flyers Wordlists* (2025) | Pre A1, A1 y A2, por separado | https://www.cambridgeenglish.org/images/506166-starters-movers-flyers-word-list-2025.pdf |

**Límite de las listas.** Starters, Movers y Flyers son exámenes para niños
de 6 a 12 años. Por eso traen *crayon* o *alien*, pero no *money* ni *price*.
Una palabra que no aparece en A1 Movers no por eso es A2 para un adulto.
Cuando Cambridge y el MCER no coinciden, decide el **descriptor del MCER**,
porque es la fuente (§4.2).

## 3. Método

1. Se extrajeron las 278 entradas de vocabulario de las 24 lecciones, que
   corresponden a 271 conceptos (7 se repiten entre lecciones).
2. Cada entrada se buscó en las listas:
   - primero la expresión completa, por ejemplo *turn left* o *See you later*;
   - si no estaba, cada palabra por separado, con su forma base (*vegetables* → *vegetable*).
3. Se registró la lista de **menor nivel** en la que aparece. Si es una
   expresión revisada palabra por palabra, cuenta su palabra más difícil.
4. Los casos dudosos se revisaron a mano en el PDF. Por ejemplo,
   *kilo(gram[me])* del Apéndice 1 y las variantes *liter/litre* y *stomachache/stomach-ache*.
5. El resultado quedó en [`../content/verificacion-cambridge.json`](../content/verificacion-cambridge.json),
   una entrada por concepto.

El linter (`verificarCambridge` en `scripts/lint-content.js`) falla en tres casos:
- una palabra de vocabulario no está en ese archivo;
- su texto cambió sin volver a verificarla;
- una palabra fuera de las listas no tiene justificación escrita.

## 4. Resultados

| | Conceptos | En listas de Cambridge |
|---|---|---|
| Total | 271 | **267 (98.5 %)** |
| Lecciones A1 | 135 | 134: 113 en Pre A1 Starters o A1 Movers y 21 solo en listas A2 (§4.1) |
| Lecciones A2 | 142 | 139 |

Seis conceptos aparecen en lecciones A1 y A2 (*weather*, *sunny*, *rain*, *phone number*, *weekend*, *cook*): cuentan en las dos filas.

### 4.1 Vocabulario de lecciones A1 que Cambridge solo trae en listas A2

Las 21 palabras aparecen en A2 Flyers o en A2 Key, pero no en las listas
infantiles de Pre A1 o A1. Se mantienen en A1 porque el MCER ubica la función en A1:

| Lección | Palabras | Descriptor A1 del MCER |
|---|---|---|
| `a1-compras` | money, price, pay, cheap, expensive, cash, card | Interactuar para obtener bienes y servicios, A1 (p. 80): «Se desenvuelve bien con números, cantidades, precios y horarios.» Esta lección ya estaba marcada como caso límite A1/A2 (`revision-mcer.md` §5.3). |
| `a1-numeros-hora` | zero, What time is it?, half past…, hour, minute | Ídem: «…precios y horarios» (p. 80). |
| `a1-ciudad-lugares` | bank, restaurant, turn left, turn right | Intercambiar información, A1 (p. 80): «comprende indicaciones breves y sencillas sobre cómo ir a un lugar». |
| `a1-saludos-presentaciones` | See you later!, Nice to meet you! | Conversación, A1 (p. 77): «Se presenta y utiliza saludos y expresiones de despedida básicos». |
| `a1-tiempo-libre` | free time | Es el nombre del tema del MCER (§4.2, Tiempo libre y ocio). |
| `a1-informacion-personal`, `a1-clima-ahora` | student, umbrella | Riqueza de vocabulario, A1 (p. 109): «repertorio básico de palabras y frases aisladas relativas a situaciones concretas». |

### 4.2 Vocabulario que no está en ninguna lista

**Se cambió** el 2026-11-05. Estas tres palabras seguramente rebasan A2:

| Lección | Antes | Después | Lista de Cambridge |
|---|---|---|---|
| `a2-clima-excursiones` (v2) | weather forecast | What will the weather be like? | Todas sus palabras están en A2 Key; *What's the weather like?* es un ejemplo de la propia lista. |
| `a2-salud-ejercicio` (v2) | sore throat | a cough | A1 Movers |
| `a2-trabajo-profesiones` (v2) | part-time | factory | A2 Flyers y A2 Key |

Se ajustaron también:
- los diálogos y ejercicios de esas lecciones;
- el diálogo de `a2-servicios-compras` (v2), que decía *sore throat*;
- la misión `mision-a2-doctor` (v2).

**Se conserva con justificación** (detalle en el JSON):

| Concepto | Justificación |
|---|---|
| pants | Forma de inglés de EE. UU. de *trousers*, que está en Pre A1 Starters. La app usa inglés de EE. UU. |
| ATM, hairdresser, package | Vocabulario del tema Servicios públicos (§4.2) que necesita la función A2 «Interactuar para obtener bienes y servicios» (p. 80). La lista A2 Key aclara que no es exhaustiva: *«The list does not provide an exhaustive list of all words…»*. |

## 5. Cómo decirlo en la tesis

- ✅ «El vocabulario se **verificó** contra las listas de Cambridge alineadas
  al MCER: el 98.5 % de los conceptos aparece en ellas y el resto tiene
  justificación.»
- ✅ «El tema y los objetivos de cada lección se tomaron del MCER.»
- ❌ «El vocabulario se tomó de Cambridge y Oxford»: no se tomó de ahí, y
  Oxford no se usó.
- ❌ «Palabras A1 del MCER»: el MCER no clasifica palabras.

## 6. Cómo mantenerla

Al agregar o cambiar una palabra de vocabulario:
1. búscala en las dos listas de §2;
2. agrega o actualiza su entrada en `content/verificacion-cambridge.json`;
   si no está en ninguna lista, escribe su `justificacion`;
3. corre `npm run lint:content`.

Las listas no se guardan en el repositorio porque tienen derechos de
Cambridge; se descargan de las URL de §2.
