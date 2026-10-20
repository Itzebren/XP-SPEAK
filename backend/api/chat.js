const { crearEndpoint } = require('../lib/endpoint');
const { ErrorApi } = require('../lib/errores');
const { configuracionChat, construirMensajes, pedirRespuesta, pedirRespuestaEnFlujo, noDisponible } = require('../lib/chat/azureOpenAI');

/**
 * POST /api/chat — una vuelta de conversación con la IA (CU-05, RF-06).
 * Body: { mensaje, nivel: "A1" | "A2", historial: [{ role, content }], stream? }
 * Respuesta: { respuesta }. Nada de la conversación se guarda (RF-09).
 *
 * Con stream: true (RNF-02) responde text/event-stream, un evento por línea "data:":
 * { delta } con cada pedazo de texto, y al final { fin: true } o { error, codigo }.
 * Los errores previos a la primera línea (token, validación, Azure caído) siguen
 * saliendo como JSON con su código HTTP.
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'chat' }, async ({ req, res }) => {
  const configuracion = configuracionChat();
  const mensajes = construirMensajes(req.body);

  if (req.body?.stream !== true) {
    const respuesta = await pedirRespuesta(configuracion, mensajes);
    res.setHeader('Cache-Control', 'no-store');
    return res.status(200).json({ respuesta });
  }

  const pedazos = await pedirRespuestaEnFlujo(configuracion, mensajes);
  res.status(200);
  res.setHeader('Content-Type', 'text/event-stream; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.setHeader('X-Accel-Buffering', 'no');

  const enviarEvento = (evento) => res.write(`data: ${JSON.stringify(evento)}\n\n`);
  let recibido = false;
  try {
    for await (const delta of pedazos) {
      recibido = true;
      enviarEvento({ delta });
    }
    if (!recibido) throw noDisponible();
    enviarEvento({ fin: true });
  } catch (error) {
    // Las cabeceras ya salieron: el error viaja como último evento.
    const errorApi = error instanceof ErrorApi ? error : noDisponible();
    if (!(error instanceof ErrorApi)) console.error('Error en chat (flujo):', error);
    enviarEvento({ error: errorApi.message, codigo: errorApi.codigo });
  }
  return res.end();
});
