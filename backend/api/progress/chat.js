const { crearEndpoint } = require('../../lib/endpoint');
const { ErrorApi } = require('../../lib/errores');
const { registrarChat } = require('../../lib/progreso/repositorio');

/**
 * POST /api/progress/chat — la app lo llama al recibir la primera respuesta
 * de la IA en una conversación (docs/progreso-diseno.md Fase 2). Cuenta para
 * la racha (RN-08) y desbloquea "Habla con IA"; no da XP. Vive aquí porque el
 * backend del chat es otro proyecto. Idempotente por sesion_id.
 *
 * Body: { sesion_id: "<id de la conversación>" }
 */
const PATRON_SESION = /^[A-Za-z0-9_-]{1,100}$/;

module.exports = crearEndpoint({ metodo: 'POST', nombre: 'progress/chat' }, async ({ req, res, uid, db }) => {
  const sesionId = req.body?.sesion_id;
  if (typeof sesionId !== 'string' || !PATRON_SESION.test(sesionId)) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', 'sesion_id debe ser un identificador de 1 a 100 caracteres (letras, números, - o _)');
  }
  const respuesta = await registrarChat(db, { uid, sesionId });
  return res.status(200).json(respuesta);
});
