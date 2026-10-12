const { crearEndpoint } = require('../../lib/endpoint');
const { responderConEtag } = require('../../lib/http');
const { obtenerManifiesto } = require('../../lib/lecciones/contenido');

/**
 * GET /api/lessons/manifest — índice de versiones para la caché offline
 * (RN-11, §5.1). Es público: no contiene datos de ningún usuario, y así
 * Android puede revisar si hay contenido nuevo antes incluso de iniciar sesión.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons/manifest', publico: true }, async ({ req, res }) => {
  const manifiesto = obtenerManifiesto();
  return responderConEtag(req, res, manifiesto.version_contenido, manifiesto, 'public, max-age=0, s-maxage=300');
});
