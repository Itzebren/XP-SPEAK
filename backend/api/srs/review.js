const { crearEndpoint } = require('../../lib/endpoint');
const { listarConceptosDeUsuario } = require('../../lib/lecciones/repositorio');
const { seleccionarConceptosDebiles } = require('../../lib/lecciones/srs');
const { validarNivel, validarLimite } = require('../../lib/lecciones/validacion');

/**
 * GET /api/srs/review[?level=A1&limit=20] — conceptos "débiles" del usuario,
 * ordenados por tasa de error (base de RF-13 / RN-07). El algoritmo SM-2 y la
 * sesión de repaso se implementan en la siguiente iteración.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'srs/review' }, async ({ req, res, uid, db }) => {
  const nivel = req.query.level ? validarNivel(req.query.level) : null;
  const limite = validarLimite(req.query.limit);

  const conceptos = await listarConceptosDeUsuario(db, uid);
  return res.status(200).json({ conceptos: seleccionarConceptosDebiles(conceptos, { nivel, limite }) });
});
