const { llamarAzure } = require('./azureOpenAI');

/**
 * Corrección gramatical del mensaje del usuario (RF-09, RN-05). Es una consulta
 * aparte de la respuesta del tutor y corre en paralelo, así la app la muestra
 * en su propio recuadro sin esperar a que termine la conversación. Como el
 * feedback de RF-09 es efímero, no se guarda en ningún lado.
 *
 * Devuelve:
 *  - { corregido, errores: [{ original, correccion, explicacion, tipo? }], correcta: false } si hay errores;
 *  - { corregido: null, errores: [], correcta: true } si se revisó y está bien (la app lo confirma);
 *  - null si no hay nada que decir: una sola palabra, mensaje en español o el corrector falló.
 *
 * Nunca lanza: si Azure falla o contesta algo raro se reintenta una vez y, si
 * tampoco, el chat sigue igual, solo sin corrección.
 */
const TIEMPO_TOTAL_MS = 8000;
const TIEMPO_PRIMER_INTENTO_MS = 5000;
const TIEMPO_MINIMO_REINTENTO_MS = 1500;
const MAX_ERRORES = 4;
const MAX_CARACTERES = 200;
const MAX_CARACTERES_CORREGIDO = 1000;
const MIN_PALABRAS = 2;
// Con 300 una oración larga con varios errores se cortaba a medio JSON y la corrección se perdía.
const MAX_TOKENS = 700;

/** Etiqueta de cada error: la app la muestra tal cual. */
const TIPOS = [
  'Tiempo verbal',
  'Conjugación',
  'Falta una palabra',
  'Sobra una palabra',
  'Orden de palabras',
  'Artículo',
  'Plural',
  'Pronombre',
  'Preposición',
  'Vocabulario',
  'Ortografía',
  'Otro',
];

const ejemplo = (objeto) => JSON.stringify(objeto);

const promptCorrector = (nivel, voz) =>
  `Eres un profesor de inglés que revisa lo que ${voz ? 'dice' : 'escribe'} un estudiante hispanohablante de nivel ${nivel} del MCER ` +
  'en un chat de práctica. Revisa SOLO el mensaje del estudiante; el mensaje anterior del tutor es contexto para saber ' +
  'qué tiempo verbal y qué respuesta se esperaban.\n\n' +
  'Revisa el mensaje verbo por verbo y palabra por palabra, en este orden:\n' +
  '1. Tiempo verbal: cada verbo debe concordar con las marcas de tiempo (yesterday, last week, ago, in 2020, tomorrow, ' +
  'next year, now, every day, since, for) y con la pregunta del tutor (si preguntó en pasado, se contesta en pasado). ' +
  'Incluye pasados mal formados (goed, buyed, did went, didn\'t went), "will to go", "going to" sin "be", ' +
  'present perfect con una fecha terminada (have gone yesterday).\n' +
  '2. Conjugación y concordancia: la -s de tercera persona (he go, she like), am/is/are, was/were, do/does, have/has, ' +
  'don\'t/doesn\'t, "people is".\n' +
  '3. Palabras que faltan o sobran: verbo "be" ausente (I student, she happy), sujeto ausente (is raining), ' +
  '"I am agree", "I am go", doble negación, "to" que falta o sobra (I want go, I can to swim).\n' +
  '4. Orden de palabras: preguntas sin auxiliar o sin inversión (you like pizza?, where you live?), adjetivo después del sustantivo (a car red).\n' +
  '5. Artículos (a/an/the), plurales (two brother, childs, peoples), pronombres y posesivos (me like, he name), ' +
  'preposiciones (in Monday, at the morning, depend of, married with).\n' +
  '6. Vocabulario y calcos del español, aunque la gramática esté bien: "I have 20 years" (I am 20 years old), "I have hungry" (I am hungry), ' +
  '"make a question" (ask a question), "make a party" (have a party), "take a coffee" (have a coffee), ' +
  '"knew a friend / know a person" cuando es conocer por primera vez (met / meet), "say me" (tell me), ' +
  'falsos amigos (actually, assist, library por bookstore) y palabras en español en medio del inglés (da la palabra en inglés).\n\n' +
  'NO son errores: mayúsculas, puntuación, contracciones (I\'m / I am), el estilo informal (gonna, yeah), inglés británico o ' +
  'americano, los nombres propios, ni las respuestas cortas naturales en una conversación ("Yes, a lot.", "In Mexico.", "Pizza!"). ' +
  (voz
    ? 'El mensaje viene de un dictado por voz: tampoco marques ortografía, palabras que suenan igual (their/there, to/too/two, ' +
      'your/you\'re, its/it\'s) ni números escritos con cifras.\n'
    : 'Una palabra mal escrita sí es un error ("Ortografía") cuando no es solo de mayúsculas o puntuación.\n') +
  'No inventes errores ni cambies el estilo de algo que ya es correcto: si una parte está bien, no la toques.\n\n' +
  'Responde únicamente con un JSON con estos campos, en este orden:\n' +
  '- "idioma": "en" si el mensaje está en inglés (aunque tenga alguna palabra en español), "es" si está en español, "otro" si no se entiende.\n' +
  '- "analisis": una línea con cada verbo del mensaje, su tiempo y el tiempo que pide el contexto, y si alguna expresión es un calco del español (máximo 35 palabras).\n' +
  `- "errores": la lista de errores (vacía si no hay o si "idioma" no es "en"), máximo ${MAX_ERRORES}, los más importantes primero. Cada uno con ` +
  '"original" (el fragmento con el error copiado tal cual del mensaje, de 1 a 6 palabras), "correccion" (ese mismo fragmento ya corregido), ' +
  `"tipo" (uno de: ${TIPOS.join(', ')}) y "explicacion" (una sola oración en español, sencilla, para nivel ${nivel}, que diga la regla y por qué aplica aquí).\n` +
  '- "corregido": el mensaje completo ya corregido, cambiando solo lo necesario (igual al original si no hay errores).\n\n' +
  'Ejemplos:\n' +
  'Tutor: "What did you do yesterday?" Estudiante: "Yesterday I go to the park with my friends and we play soccer"\n' +
  ejemplo({
    idioma: 'en',
    analisis: 'go: presente, pide pasado (yesterday). play: presente, pide pasado.',
    errores: [
      { original: 'I go', correccion: 'I went', tipo: 'Tiempo verbal', explicacion: 'Con "yesterday" la acción ya pasó: se usa el pasado de "go", que es "went".' },
      { original: 'we play', correccion: 'we played', tipo: 'Tiempo verbal', explicacion: 'Sigues hablando de ayer, así que "play" va en pasado: "played".' },
    ],
    corregido: 'Yesterday I went to the park with my friends and we played soccer.',
  }) +
  '\nEstudiante: "She have 20 years and she don\'t like the vegetables"\n' +
  ejemplo({
    idioma: 'en',
    analisis: 'have: presente, sujeto she. don\'t like: presente, sujeto she.',
    errores: [
      { original: 'She have 20 years', correccion: 'She is 20 years old', tipo: 'Vocabulario', explicacion: 'En inglés la edad se dice con el verbo "be": "She is 20 years old", no con "have".' },
      { original: 'she don\'t like', correccion: 'she doesn\'t like', tipo: 'Conjugación', explicacion: 'Con he, she o it la negación en presente es "doesn\'t".' },
      { original: 'the vegetables', correccion: 'vegetables', tipo: 'Artículo', explicacion: 'Cuando hablas de algo en general no se usa "the".' },
    ],
    corregido: 'She is 20 years old and she doesn\'t like vegetables.',
  }) +
  '\nTutor: "Do you like music?" Estudiante: "yes i love it, i listen to rock every day"\n' +
  ejemplo({ idioma: 'en', analisis: 'love, listen: presente, correcto con every day.', errores: [], corregido: 'Yes, I love it. I listen to rock every day.' });

/** Para el primer intento: el modelo no puede salirse de este formato ni cortar campos. */
const ESQUEMA = {
  type: 'json_schema',
  json_schema: {
    name: 'correccion',
    strict: true,
    schema: {
      type: 'object',
      additionalProperties: false,
      required: ['idioma', 'analisis', 'errores', 'corregido'],
      properties: {
        idioma: { type: 'string', enum: ['en', 'es', 'otro'] },
        analisis: { type: 'string' },
        errores: {
          type: 'array',
          items: {
            type: 'object',
            additionalProperties: false,
            required: ['original', 'correccion', 'tipo', 'explicacion'],
            properties: {
              original: { type: 'string' },
              correccion: { type: 'string' },
              tipo: { type: 'string', enum: TIPOS },
              explicacion: { type: 'string' },
            },
          },
        },
        corregido: { type: 'string' },
      },
    },
  },
};

/** El último turno del tutor da contexto ("Do you like pizza?" → "Yes, I like"). */
function construirMensajesCorreccion({ mensaje, nivel = 'A1', historial = [], voz = false }) {
  const anterior = Array.isArray(historial)
    ? [...historial].reverse().find((t) => t?.role === 'assistant' && typeof t.content === 'string')
    : null;
  const contexto = anterior ? `Mensaje anterior del tutor (solo contexto): ${JSON.stringify(anterior.content.slice(0, 500))}\n` : '';
  return [
    { role: 'system', content: promptCorrector(nivel, voz === true) },
    { role: 'user', content: `${contexto}Mensaje del estudiante: ${JSON.stringify(mensaje)}` },
  ];
}

const texto = (valor, maximo = MAX_CARACTERES) => (typeof valor === 'string' ? valor.trim().slice(0, maximo) : '');
/** Sin mayúsculas, puntuación ni apóstrofos: "im gonna" y "I'm gonna" son lo mismo. */
const llano = (valor) => valor.toLowerCase().replace(/['’`]/g, '').replace(/[^\p{L}\p{N} ]/gu, ' ').replace(/\s+/g, ' ').trim();

/** Palabras que suenan igual: en un dictado el error es del reconocedor, no del estudiante. */
const HOMOFONOS = [
  ['their', 'there', 'theyre'],
  ['to', 'too', 'two', '2'],
  ['your', 'youre'],
  ['its'],
  ['for', 'four', '4'],
  ['buy', 'by', 'bye'],
  ['know', 'no'],
  ['here', 'hear'],
  ['write', 'right'],
  ['whose', 'whos'],
  ['then', 'than'],
  ['see', 'sea'],
  ['meet', 'meat'],
  ['son', 'sun'],
  ['wear', 'where'],
  ['one', 'won', '1'],
  ['ate', 'eight', '8'],
  ['hour', 'our'],
  ['week', 'weak'],
  ['new', 'knew'],
];
const CLASE_HOMOFONO = new Map(HOMOFONOS.flatMap((grupo) => grupo.map((palabra) => [palabra, grupo[0]])));
const comoSuena = (valor) => llano(valor).split(' ').map((palabra) => CLASE_HOMOFONO.get(palabra) || palabra).join(' ');

/** Con [voz] también son iguales dos textos que solo cambian una palabra por otra que suena igual. */
const igual = (a, b, voz = false) => (voz ? comoSuena(a) === comoSuena(b) : llano(a) === llano(b));

/**
 * El modelo corrigió la oración pero no desglosó el error (pasa cuando falta una palabra:
 * no hay "fragmento original" que copiar). Se saca el tramo que cambió comparando palabra
 * por palabra, con una palabra de contexto a cada lado.
 */
function diferencia(mensaje, corregido, voz) {
  const antes = mensaje.trim().split(/\s+/);
  const despues = corregido.trim().split(/\s+/);
  let inicio = 0;
  while (inicio < antes.length && inicio < despues.length && igual(antes[inicio], despues[inicio], voz)) inicio++;
  let fin = 0;
  while (fin < antes.length - inicio && fin < despues.length - inicio && igual(antes[antes.length - 1 - fin], despues[despues.length - 1 - fin], voz)) fin++;
  const desde = Math.max(0, inicio - 1);
  const tramo = (palabras) => palabras.slice(desde, Math.min(palabras.length, palabras.length - fin + 1)).join(' ');
  return { original: texto(tramo(antes)), correccion: texto(tramo(despues)) };
}

/**
 * Lo que contestó el modelo, ya validado. `valida: false` es una respuesta que no sirve
 * (JSON cortado, sin los campos) y amerita reintento; con `valida: true`, `correccion`
 * es lo que se manda a la app o null si no hay nada que mostrar (mensaje en español).
 */
function interpretar(contenido, mensaje = '', voz = false) {
  let datos;
  try {
    datos = JSON.parse(contenido);
  } catch {
    return { valida: false, correccion: null };
  }
  if (!datos || !Array.isArray(datos.errores)) return { valida: false, correccion: null };
  if (typeof datos.idioma === 'string' && datos.idioma !== 'en') return { valida: true, correccion: null };

  const corregido = texto(datos.corregido, MAX_CARACTERES_CORREGIDO);
  // Se descartan los "errores" que solo cambian mayúsculas o puntuación y los que no traen el fragmento.
  const errores = datos.errores
    .map((e) => ({
      original: texto(e?.original),
      correccion: texto(e?.correccion),
      explicacion: texto(e?.explicacion) || 'Así se dice de forma correcta.',
      ...(TIPOS.includes(e?.tipo) && { tipo: e.tipo }),
    }))
    .filter((e) => e.original && e.correccion && !igual(e.original, e.correccion, voz))
    .slice(0, MAX_ERRORES);

  if (!errores.length && corregido && mensaje && !igual(corregido, mensaje, voz)) {
    const cambio = diferencia(mensaje, corregido, voz);
    if (cambio.correccion && !igual(cambio.original, cambio.correccion, voz)) {
      errores.push({ ...cambio, explicacion: 'Así se dice de forma correcta.' });
    }
  }
  if (!errores.length) return { valida: true, correccion: { corregido: null, errores: [], correcta: true } };
  return { valida: true, correccion: { corregido: corregido || null, errores, correcta: false } };
}

/** Compatibilidad y pruebas: la corrección lista para la app, o null. */
const normalizarCorreccion = (contenido, mensaje, voz) => interpretar(contenido, mensaje, voz).correccion;

async function intentar(configuracion, body, formato, tiempoEsperaMs) {
  const respuesta = await llamarAzure(configuracion, construirMensajesCorreccion(body), {
    temperature: 0,
    max_tokens: MAX_TOKENS,
    response_format: formato,
    tiempoEsperaMs,
  });
  const datos = await respuesta.json();
  return interpretar(datos.choices?.[0]?.message?.content, body.mensaje, body.voz === true);
}

/** El body ya pasó por construirMensajes, así que mensaje y nivel son válidos. */
async function pedirCorreccion(configuracion, body) {
  if (body.mensaje.trim().split(/\s+/).length < MIN_PALABRAS) return null;
  const inicio = Date.now();
  // Primero con esquema estricto; si el despliegue no lo acepta, Azure está saturado (429)
  // o el JSON llegó cortado, un segundo intento con JSON libre (el prompt ya describe el formato).
  const intentos = [
    { formato: ESQUEMA, espera: () => TIEMPO_PRIMER_INTENTO_MS },
    { formato: { type: 'json_object' }, espera: () => TIEMPO_TOTAL_MS - (Date.now() - inicio) },
  ];
  for (const { formato, espera } of intentos) {
    const tiempoEsperaMs = espera();
    if (tiempoEsperaMs < TIEMPO_MINIMO_REINTENTO_MS) break;
    try {
      const resultado = await intentar(configuracion, body, formato, tiempoEsperaMs);
      if (resultado.valida) return resultado.correccion;
    } catch {
      // llamarAzure ya registró el detalle; sin corrección el chat sigue funcionando.
    }
  }
  return null;
}

module.exports = { pedirCorreccion, normalizarCorreccion, construirMensajesCorreccion, MAX_ERRORES, TIPOS, TIEMPO_TOTAL_MS };
