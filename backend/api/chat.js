const { crearEndpoint } = require('../lib/endpoint');
const { ErrorApi } = require('../lib/errores');
const { configuracionChat, construirMensajes, pedirRespuesta, pedirRespuestaEnFlujo, noDisponible } = require('../lib/chat/azureOpenAI');
const { pedirCorreccion } = require('../lib/chat/correccion');

/** Lo más que se espera la corrección después de la respuesta, para no retrasar el { fin }. */
const ESPERA_CORRECCION_MS = 3000;

const conLimite = (promesa, ms) => {
  let reloj;
  const limite = new Promise((resolver) => {
    reloj = setTimeout(() => resolver(null), ms);
  });
  return Promise.race([promesa, limite]).finally(() => clearTimeout(reloj));
};

/**
 * POST /api/chat — una vuelta de conversación con la IA (CU-05, RF-06).
 * Body: { mensaje, nivel: "A1" | "A2", historial: [{ role, content }], stream? }
 * Respuesta: { respuesta, correccion? }. Nada de la conversación se guarda (RF-09).
 *
 * `correccion` (RF-09) son los errores del mensaje del usuario:
 * { corregido, errores: [{ original, correccion, explicacion }] }. Se pide a Azure en
 * paralelo con la respuesta y solo viene si hay errores.
 *
 * Con stream: true (RNF-02) responde text/event-stream, un evento por línea "data:":
 * { delta } con cada pedazo de texto, { correccion } en cuanto está lista, y al final
 * { fin: true } o { error, codigo }. Los errores previos a la primera línea (token,
 * validación, Azure caído) siguen saliendo como JSON con su código HTTP.
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'chat' }, async ({ req, res }) => {
  const configuracion = configuracionChat();
  const mensajes = construirMensajes(req.body);

  if (req.body?.stream !== true) {
    const principal = pedirRespuesta(configuracion, mensajes);
    const correccion = pedirCorreccion(configuracion, req.body);
    const respuesta = await principal;
    const errores = await conLimite(correccion, ESPERA_CORRECCION_MS);
    res.setHeader('Cache-Control', 'no-store');
    return res.status(200).json({ respuesta, ...(errores && { correccion: errores }) });
  }

  // La respuesta se pide primero; la corrección corre a la par sin retrasarla.
  const flujo = pedirRespuestaEnFlujo(configuracion, mensajes);
  const correccion = pedirCorreccion(configuracion, req.body);
  const pedazos = await flujo;
  res.status(200);
  res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Accel-Buffering', 'no');

  let abierto = true;
  const enviarEvento = (evento) => abierto && res.write(`data: ${JSON.stringify(evento)}\n\n`);
  let correccionEnviada = false;
  const enviarCorreccion = (errores) => {
    if (!errores || correccionEnviada) return;
    correccionEnviada = true;
    enviarEvento({ correccion: errores });
  };
  // RN-05: sale en cuanto Azure la tiene, aunque la respuesta siga llegando.
  correccion.then(enviarCorreccion);

  let recibido = false;
  try {
    for await (const delta of pedazos) {
      recibido = true;
      enviarEvento({ delta });
    }
    if (!recibido) throw noDisponible();
    enviarCorreccion(await conLimite(correccion, ESPERA_CORRECCION_MS));
    enviarEvento({ fin: true });
  } catch (error) {
    // Las cabeceras ya salieron: el error viaja como último evento.
    const errorApi = error instanceof ErrorApi ? error : noDisponible();
    if (!(error instanceof ErrorApi)) console.error('Error en chat (flujo):', error);
    enviarEvento({ error: errorApi.message, codigo: errorApi.codigo });
  }
  abierto = false;
  return res.end();
});
