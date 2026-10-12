const { crearEndpoint } = require('../../lib/endpoint');
const { calificarIntento } = require('../../lib/lecciones/calificador');
const { listarConceptosDeUsuario, registrarRepaso } = require('../../lib/lecciones/repositorio');
const { ejercicioParaConcepto, resolverEjercicio } = require('../../lib/lecciones/repaso');
const { conTextoDelContenido, seleccionarParaRepaso } = require('../../lib/lecciones/srs');
const { validarNivel, validarRepaso } = require('../../lib/lecciones/validacion');

/**
 * GET /api/srs/session[?level=A1] — sesión de repaso de hoy (RF-13): un
 * ejercicio por cada concepto vencido según SM-2, primero los de mayor tasa
 * de error (RN-07). Si no toca nada, `items` viene vacío y `proxima_revision`
 * dice cuándo vuelve a haber repaso.
 */
const obtener = crearEndpoint({ metodo: 'GET', nombre: 'srs/session' }, async ({ req, res, uid, db }) => {
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
});

/**
 * POST /api/srs/session — califica la sesión y reprograma cada concepto con
 * SM-2. Idempotente por attempt_id. No da XP.
 *
 * Body: { attempt_id: "<uuid>", respuestas: [{ id: "<lección>:<ítem>", valor }, ...] }
 */
const enviar = crearEndpoint({ metodo: 'POST', nombre: 'srs/session' }, async ({ req, res, uid, db }) => {
  const { attemptId, respuestas, ejercicios } = validarRepaso(req.body, resolverEjercicio);
  const resultado = calificarIntento({ umbral_aprobacion: 0, items: ejercicios.map((e) => e.ejercicio) }, respuestas);
  const leccionDeConcepto = new Map(ejercicios.map((e) => [e.ejercicio.concepto_id, e.leccion]));

  const respuesta = await registrarRepaso(db, { uid, attemptId, resultado, leccionDeConcepto });
  return res.status(200).json(respuesta);
});

module.exports = (req, res) => (req.method === 'POST' ? enviar(req, res) : obtener(req, res));
