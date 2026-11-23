const crypto = require('crypto');
const { crearEndpoint } = require('../../lib/endpoint');
const { ErrorApi, responderError } = require('../../lib/errores');
const { responderConEtag } = require('../../lib/http');
const { obtenerProgresosDeUsuario, listarConceptosDeUsuario } = require('../../lib/lecciones/repositorio');
const { validarNivel } = require('../../lib/lecciones/validacion');
const { diaLocal } = require('../../lib/progreso/racha');
const { obtenerResumen, registrarChat } = require('../../lib/progreso/repositorio');
const { vistaDeProgreso } = require('../../lib/progreso/vista');

/**
 * GET /api/progress[?level=A1] — pestaña Progreso (CU-08, RF-05, RF-14,
 * docs/progreso-diseno.md Fase 2): nivel MCER con su dominio, XP y nivel de
 * experiencia, racha, habilidades e insignias con su avance. Si el usuario
 * aún no tiene resumen (actividad de antes de este módulo), se reconstruye.
 * Responde con ETag: si nada cambió, la app recibe un 304 vacío.
 */
async function leerProgreso({ req, res, uid, db }) {
  const nivel = req.query.level ? validarNivel(req.query.level) : 'A1';
  const ahora = Date.now();
  const [resumen, progresos, conceptos] = await Promise.all([
    obtenerResumen(db, uid, ahora),
    obtenerProgresosDeUsuario(db, uid),
    listarConceptosDeUsuario(db, uid),
  ]);

  const cuerpo = vistaDeProgreso({ resumen, nivel, progresos: [...progresos.values()], conceptos, hoy: diaLocal(ahora) });
  const etag = crypto.createHash('sha256').update(JSON.stringify(cuerpo)).digest('hex').slice(0, 16);
  return responderConEtag(req, res, etag, cuerpo);
}

/**
 * POST /api/progress — la app lo llama al recibir la primera respuesta de la
 * IA en una conversación (docs/progreso-diseno.md Fase 2). Cuenta para la
 * racha (RN-08) y desbloquea "Habla con IA"; no da XP. Idempotente por
 * sesion_id. Antes era POST /api/progress/chat; se unió aquí por el límite
 * de 12 funciones de Vercel.
 *
 * Body: { sesion_id: "<id de la conversación>" }
 */
const PATRON_SESION = /^[A-Za-z0-9_-]{1,100}$/;

async function registrarChatDeIA({ req, res, uid, db }) {
  const sesionId = req.body?.sesion_id;
  if (typeof sesionId !== 'string' || !PATRON_SESION.test(sesionId)) {
    throw new ErrorApi(400, 'ENTRADA_INVALIDA', 'sesion_id debe ser un identificador de 1 a 100 caracteres (letras, números, - o _)');
  }
  const respuesta = await registrarChat(db, { uid, sesionId });
  return res.status(200).json(respuesta);
}

/**
 * Cada rama responde sus propios errores para que el log diga cuál falló:
 * en la app el POST corre dentro de runCatching y no muestra nada.
 */
const conNombre = (nombre, manejar) => async (ctx) => {
  try {
    return await manejar(ctx);
  } catch (error) {
    return responderError(ctx.res, error, nombre);
  }
};

const porMetodo = {
  GET: conNombre('progress/get', leerProgreso),
  POST: conNombre('progress/chat', registrarChatDeIA),
};

module.exports = crearEndpoint({ metodo: ['GET', 'POST'], nombre: 'progress' }, (ctx) => porMetodo[ctx.req.method](ctx));
