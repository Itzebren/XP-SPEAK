const { crearEndpoint } = require('../../lib/endpoint');
const { responderConEtag } = require('../../lib/http');
const { leccionPublica } = require('../../lib/lecciones/contenido');
const { exigirLeccion } = require('../../lib/lecciones/validacion');

/**
 * GET /api/lessons/:id — contenido completo de la lección (esquema §4.2).
 * Incluye la clave de respuestas porque Android califica offline (§6.2).
 * No se revisa el bloqueo aquí: la app descarga todo el nivel para su caché
 * y el bloqueo se hace valer al enviar el intento.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons/[id]' }, async ({ req, res }) => {
  const leccion = exigirLeccion(req.query.id);
  return responderConEtag(req, res, `${leccion.id}@${leccion.version}`, leccionPublica(leccion));
});
