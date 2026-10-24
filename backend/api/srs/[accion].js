const { crearEndpoint } = require('../../lib/endpoint');
const { ErrorApi, responderError } = require('../../lib/errores');
const { calificarIntento } = require('../../lib/lecciones/calificador');
const { listarConceptosDeUsuario, registrarRepaso } = require('../../lib/lecciones/repositorio');
const { ejercicioParaConcepto, resolverEjercicio } = require('../../lib/lecciones/repaso');
const { conTextoDelContenido, seleccionarConceptosDebiles, seleccionarParaRepaso } = require('../../lib/lecciones/srs');
const { validarNivel, validarLimite, validarRepaso } = require('../../lib/lecciones/validacion');

/**
 * /api/srs/session y /api/srs/review en un solo archivo (límite de 12
 * funciones de Vercel). Vercel pone el último segmento de la URL en
 * req.query.accion; las URLs no cambian para la app.
 */

/**
 * GET /api/srs/session[?level=A1] — sesión de repaso de hoy (RF-13): un
 * ejercicio por cada concepto vencido según SM-2, primero los de mayor tasa
 * de error (RN-07). Si no toca nada, `items` viene vacío y `proxima_revision`
 * dice cuándo vuelve a haber repaso.
 */
async function obtenerSesion({ req, res, uid, db }) {
  const nivel = req.query.level ? validarNivel(req.query.level) : null;
  const conceptos = await listarConceptosDeUsuario(db, uid);
  const seleccion = seleccionarParaRepaso(conceptos, { nivel, ahora: Date.now() });

  const items = seleccion.vencidos
    .map((concepto) => ({ concepto, ejercicio: ejercicioParaConcepto(concepto) }))
    .filter(({ ejercicio }) => ejercicio)
    .map(({ concepto, ejercicio }) => ({ ...ejercicio, concepto: conTextoDelContenido(concepto) }));

  return res.status(200).json({
    total_vencidos: seleccion.total_vencidos,
    proxima_revision: seleccion.proxima_revision,
    items,
  });
}

/**
 * POST /api/srs/session — califica la sesión y reprograma cada concepto con
 * SM-2. Idempotente por attempt_id. No da XP.
 *
 * Body: { attempt_id: "<uuid>", respuestas: [{ id: "<lección>:<ítem>", valor }, ...] }
 */
async function enviarSesion({ req, res, uid, db }) {
  const { attemptId, respuestas, ejercicios } = validarRepaso(req.body, resolverEjercicio);
  const resultado = calificarIntento({ umbral_aprobacion: 0, items: ejercicios.map((e) => e.ejercicio) }, respuestas);
  const leccionDeConcepto = new Map(ejercicios.map((e) => [e.ejercicio.concepto_id, e.leccion]));

  const respuesta = await registrarRepaso(db, { uid, attemptId, resultado, leccionDeConcepto });
  return res.status(200).json(respuesta);
}

/**
 * GET /api/srs/review[?level=A1&limit=20] — conceptos "débiles" del usuario,
 * ordenados por tasa de error (base de RF-13 / RN-07).
 */
async function conceptosDebiles({ req, res, uid, db }) {
  const nivel = req.query.level ? validarNivel(req.query.level) : null;
  const limite = validarLimite(req.query.limit);

  const conceptos = await listarConceptosDeUsuario(db, uid);
  return res.status(200).json({ conceptos: seleccionarConceptosDebiles(conceptos, { nivel, limite }) });
}

/**
 * Cada rama es su propio endpoint: valida su método y el token, y registra sus
 * errores con su nombre. Un método que la acción no tiene cae en su GET, que
 * responde 405 (POST /api/srs/review, PUT /api/srs/session).
 */
const ramas = {
  session: {
    GET: crearEndpoint({ metodo: 'GET', nombre: 'srs/session/get' }, obtenerSesion),
    POST: crearEndpoint({ metodo: 'POST', nombre: 'srs/session/post' }, enviarSesion),
  },
  review: {
    GET: crearEndpoint({ metodo: 'GET', nombre: 'srs/review' }, conceptosDebiles),
  },
};

module.exports = (req, res) => {
  const accion = req.query.accion;
  if (!Object.hasOwn(ramas, accion)) {
    return responderError(res, new ErrorApi(404, 'RUTA_NO_ENCONTRADA', 'Ruta no encontrada'), 'srs');
  }
  const rama = ramas[accion];
  return (Object.hasOwn(rama, req.method) ? rama[req.method] : rama.GET)(req, res);
};
