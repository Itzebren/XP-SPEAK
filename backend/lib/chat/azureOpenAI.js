const { ErrorApi } = require('../errores');

/**
 * Chat con Azure OpenAI (CU-05, RF-06). La llave vive solo en el servidor
 * (RNF-06 / R04): la app manda el mensaje con su ID token de Firebase y este
 * backend hace de proxy.
 *
 * Variables de entorno: AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY y
 * AZURE_OPENAI_DEPLOYMENT (en producción "gpt-4.1-mini-2", recurso xp-speak-bot).
 */
const NIVELES = ['A1', 'A2'];
const MAX_HISTORIAL = 10;
const MAX_CARACTERES_MENSAJE = 1000;
const TIEMPO_ESPERA_MS = 25000;

const SYSTEM_PROMPT_BASE =
  'Eres un tutor de inglés conversacional dentro de la app XP-SPEAK. ' +
  'Ayudas al usuario a practicar inglés hablado y escrito de forma natural, ' +
  'corrigiendo errores con amabilidad y adaptando tu vocabulario al nivel indicado. ' +
  'Responde siempre en inglés, salvo que el usuario pida explícitamente una explicación en español.';

function configuracionChat(entorno = process.env) {
  const endpoint = entorno.AZURE_OPENAI_ENDPOINT;
  const clave = entorno.AZURE_OPENAI_API_KEY;
  if (!endpoint || !clave) {
    throw new ErrorApi(503, 'CHAT_NO_CONFIGURADO', 'El chat no está disponible por ahora');
  }
  return { endpoint: endpoint.replace(/\/$/, ''), clave, deployment: entorno.AZURE_OPENAI_DEPLOYMENT || 'gpt-4.1-mini-2' };
}

/**
 * Valida el body { mensaje, nivel, historial } y arma los mensajes para el
 * modelo. Del historial solo se aceptan turnos user/assistant con texto: un
 * cliente no puede colar su propio mensaje "system" para saltarse el nivel (RN-03).
 */
function construirMensajes(body) {
  const { mensaje, nivel = 'A1', historial = [] } = body || {};
  if (typeof mensaje !== 'string' || !mensaje.trim()) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', 'Falta el mensaje');
  }
  if (mensaje.length > MAX_CARACTERES_MENSAJE) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', `El mensaje no puede pasar de ${MAX_CARACTERES_MENSAJE} caracteres`);
  }
  if (!NIVELES.includes(nivel)) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', 'El nivel debe ser A1 o A2');
  }
  if (!Array.isArray(historial)) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', 'El historial debe ser una lista');
  }

  const turnos = historial
    .filter((t) => t && (t.role === 'user' || t.role === 'assistant') && typeof t.content === 'string')
    .slice(-MAX_HISTORIAL)
    .map((t) => ({ role: t.role, content: t.content.slice(0, MAX_CARACTERES_MENSAJE) }));

  return [
    { role: 'system', content: `${SYSTEM_PROMPT_BASE} El nivel del usuario es ${nivel}.` },
    ...turnos,
    { role: 'user', content: mensaje },
  ];
}

/** CU-05 A5: el filtro de contenido de Azure bloqueó la pregunta o la respuesta. */
const contenidoBloqueado = () =>
  new ErrorApi(422, 'CONTENIDO_BLOQUEADO', 'Ese tema no está permitido en el chat. Intentemos hablar de otra cosa.');

async function pedirRespuesta({ endpoint, clave, deployment }, mensajes) {
  let respuesta;
  try {
    respuesta = await fetch(`${endpoint}/openai/v1/chat/completions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'api-key': clave },
      body: JSON.stringify({ model: deployment, messages: mensajes, temperature: 0.7, max_tokens: 300 }),
      signal: AbortSignal.timeout(TIEMPO_ESPERA_MS),
    });
  } catch (error) {
    // CU-05 A4 (riesgo R01): la app avisa la demora y ofrece reintentar.
    console.error('Azure OpenAI no respondió:', error.message);
    throw new ErrorApi(504, 'CHAT_SIN_RESPUESTA', 'El chatbot tardó demasiado. Intenta de nuevo.');
  }

  if (!respuesta.ok) {
    const detalle = await respuesta.text();
    if (respuesta.status === 400 && detalle.includes('content_filter')) throw contenidoBloqueado();
    console.error(`Azure OpenAI respondió ${respuesta.status}:`, detalle);
    throw new ErrorApi(502, 'CHAT_NO_DISPONIBLE', 'El chatbot no pudo responder. Intenta de nuevo.');
  }

  const datos = await respuesta.json();
  const opcion = datos.choices?.[0];
  if (opcion?.finish_reason === 'content_filter') throw contenidoBloqueado();
  const texto = opcion?.message?.content?.trim();
  if (!texto) {
    throw new ErrorApi(502, 'CHAT_NO_DISPONIBLE', 'El chatbot no pudo responder. Intenta de nuevo.');
  }
  return texto;
}

module.exports = { MAX_HISTORIAL, MAX_CARACTERES_MENSAJE, configuracionChat, construirMensajes, pedirRespuesta };
