const { crearEndpoint } = require('../lib/endpoint');
const { configuracionChat, construirMensajes, pedirRespuesta } = require('../lib/chat/azureOpenAI');

/**
 * POST /api/chat — una vuelta de conversación con la IA (CU-05, RF-06).
 * Body: { mensaje, nivel: "A1" | "A2", historial: [{ role, content }] }
 * Respuesta: { respuesta }. Nada de la conversación se guarda (RF-09).
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'chat' }, async ({ req, res }) => {
  const configuracion = configuracionChat();
  const mensajes = construirMensajes(req.body);
  const respuesta = await pedirRespuesta(configuracion, mensajes);
  res.setHeader('Cache-Control', 'no-store');
  return res.status(200).json({ respuesta });
});
