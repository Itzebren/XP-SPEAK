const { llamarAzure } = require('./azureOpenAI');

/**
 * Corrección gramatical del mensaje del usuario (RF-09, RN-05). Es una consulta
 * aparte de la respuesta del tutor y corre en paralelo, así la app la muestra
 * en su propio recuadro sin esperar a que termine la conversación. Como el
 * feedback de RF-09 es efímero, no se guarda en ningún lado.
 *
 * Nunca lanza: si Azure falla o contesta algo raro, devuelve null y el chat
 * sigue igual, solo sin corrección.
 */
const TIEMPO_ESPERA_MS = 8000;
const MAX_ERRORES = 3;
const MAX_CARACTERES = 200;
const MIN_PALABRAS = 2;

const promptCorrector = (nivel) =>
  `Eres un corrector de inglés para estudiantes hispanohablantes de nivel ${nivel} del MCER. ` +
  'Revisa SOLO el mensaje del estudiante. Señala errores de gramática, conjugación, orden de palabras, ' +
  'preposiciones, artículos o vocabulario mal usado. Ignora mayúsculas, puntuación, contracciones y el ' +
  'estilo informal: el mensaje puede venir de un dictado por voz. Si está escrito en español o no tiene ' +
  'errores, deja "errores" vacío. Responde únicamente con JSON: ' +
  '{"corregido": "<el mensaje completo corregido>", "errores": [{"original": "<fragmento con el error>", ' +
  `"correccion": "<fragmento corregido>", "explicacion": "<explicación breve en español, para nivel ${nivel}>"}]}. ` +
  `Máximo ${MAX_ERRORES} errores.`;

/** El último turno del tutor da contexto ("Do you like pizza?" → "Yes, I like"). */
function construirMensajesCorreccion({ mensaje, nivel = 'A1', historial = [] }) {
  const anterior = Array.isArray(historial)
    ? [...historial].reverse().find((t) => t?.role === 'assistant' && typeof t.content === 'string')
    : null;
  const contexto = anterior ? `Mensaje anterior del tutor (solo contexto): ${JSON.stringify(anterior.content.slice(0, 500))}\n` : '';
  return [
    { role: 'system', content: promptCorrector(nivel) },
    { role: 'user', content: `${contexto}Mensaje del estudiante: ${JSON.stringify(mensaje)}` },
  ];
}

const texto = (valor) => (typeof valor === 'string' ? valor.trim().slice(0, MAX_CARACTERES) : '');
const igual = (a, b) => a.toLowerCase().replace(/[^\p{L}\p{N}' ]/gu, '').trim() === b.toLowerCase().replace(/[^\p{L}\p{N}' ]/gu, '').trim();

/**
 * Lo que contestó el modelo, ya validado: { corregido, errores: [{ original, correccion, explicacion }] },
 * o null si no hay errores reales. Descarta los "errores" que solo cambian mayúsculas o puntuación.
 */
function normalizarCorreccion(contenido) {
  let datos;
  try {
    datos = JSON.parse(contenido);
  } catch {
    return null;
  }
  if (!datos || !Array.isArray(datos.errores)) return null;
  const errores = datos.errores
    .map((e) => ({ original: texto(e?.original), correccion: texto(e?.correccion), explicacion: texto(e?.explicacion) }))
    .filter((e) => e.original && e.correccion && e.explicacion && !igual(e.original, e.correccion))
    .slice(0, MAX_ERRORES);
  if (!errores.length) return null;
  return { corregido: texto(datos.corregido) || null, errores };
}

/** El body ya pasó por construirMensajes, así que mensaje y nivel son válidos. */
async function pedirCorreccion(configuracion, body) {
  if (body.mensaje.trim().split(/\s+/).length < MIN_PALABRAS) return null;
  try {
    const respuesta = await llamarAzure(configuracion, construirMensajesCorreccion(body), {
      temperature: 0,
      max_tokens: 300,
      response_format: { type: 'json_object' },
      tiempoEsperaMs: TIEMPO_ESPERA_MS,
    });
    const datos = await respuesta.json();
    return normalizarCorreccion(datos.choices?.[0]?.message?.content);
  } catch {
    // llamarAzure ya registró el detalle; sin corrección el chat sigue funcionando.
    return null;
  }
}

module.exports = { pedirCorreccion, normalizarCorreccion, construirMensajesCorreccion, MAX_ERRORES };
