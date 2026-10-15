const { crearEndpoint } = require('../../../lib/endpoint');
const { guardarAvance, listarConceptosDeUsuario } = require('../../../lib/lecciones/repositorio');
const { conceptosQueBloquean } = require('../../../lib/lecciones/srs');
const { exigirLeccion, validarAvance } = require('../../../lib/lecciones/validacion');

/**
 * POST /api/lessons/:id/progress — guarda el avance parcial de la teoría
 * (RN-12, CU-06 A2) para retomarla en otro momento o dispositivo. Marca la
 * lección como "en_progreso" (máquina de estados de §5.4). No otorga XP.
 * Responde 409 REPASO_PENDIENTE si es un tema nuevo y toca repasar antes (RN-07).
 *
 * Body: { seccion_actual: 2 }
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'lessons/[id]/progress' }, async ({ req, res, uid, db }) => {
  const leccion = exigirLeccion(req.query.id);
  const { seccionActual } = validarAvance(req.body, leccion);

  const conceptos = await listarConceptosDeUsuario(db, uid);
  const repasoPendiente = conceptosQueBloquean(conceptos, { nivel: leccion.nivel_mcer, ahora: Date.now() }).length > 0;

  const resultado = await guardarAvance(db, { uid, leccion, seccionActual, repasoPendiente });
  return res.status(200).json(resultado);
});
