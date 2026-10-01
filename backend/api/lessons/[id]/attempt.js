const { crearEndpoint } = require('../../../lib/endpoint');
const { obtenerEvaluacion } = require('../../../lib/lecciones/contenido');
const { calificarIntento } = require('../../../lib/lecciones/calificador');
const { registrarIntento } = require('../../../lib/lecciones/repositorio');
const { exigirLeccion, validarIntento } = require('../../../lib/lecciones/validacion');

/**
 * POST /api/lessons/:id/attempt — endpoint central de Lecciones (§6.1).
 * Califica en servidor (autoritativo), aplica el 70% (RN-06), otorga XP una
 * sola vez (RN-09), desbloquea la siguiente lección, registra las señales del
 * SRS (RF-13) y es idempotente por attempt_id (§6.2).
 *
 * Body: { attempt_id: "<uuid>", respuestas: [{ id: "q1", valor: 1 }, ...] }
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'lessons/[id]/attempt' }, async ({ req, res, uid, db }) => {
  const leccion = exigirLeccion(req.query.id);
  const evaluacion = obtenerEvaluacion(leccion);
  const { attemptId, respuestas } = validarIntento(req.body, evaluacion);

  const resultado = calificarIntento(evaluacion, respuestas);
  const respuesta = await registrarIntento(db, { uid, leccion, attemptId, resultado });

  return res.status(200).json(respuesta);
});
