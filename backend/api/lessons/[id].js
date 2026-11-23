const { crearEndpoint } = require('../../lib/endpoint');
const { responderConEtag } = require('../../lib/http');
const { leccionPublica, obtenerManifiesto } = require('../../lib/lecciones/contenido');
const { exigirLeccion } = require('../../lib/lecciones/validacion');

/**
 * GET /api/lessons/manifest — índice de versiones para la caché offline
 * (RN-11, §5.1). Es público: no contiene datos de ningún usuario, y así
 * Android puede revisar si hay contenido nuevo antes incluso de iniciar sesión.
 */
async function servirManifiesto({ req, res }) {
  const manifiesto = obtenerManifiesto();
  return responderConEtag(req, res, manifiesto.version_contenido, manifiesto, 'public, max-age=0, s-maxage=300');
}

/**
 * GET /api/lessons/:id — contenido completo de la lección (esquema §4.2).
 * Incluye la clave de respuestas porque Android califica offline (§6.2).
 * No se revisa el bloqueo aquí: la app descarga todo el nivel para su caché
 * y el bloqueo se hace valer al enviar el intento.
 */
async function servirLeccion({ req, res }) {
  const leccion = exigirLeccion(req.query.id);
  return responderConEtag(req, res, `${leccion.id}@${leccion.version}`, leccionPublica(leccion));
}

/**
 * El manifiesto vive aquí por el límite de 12 funciones de Vercel: la URL
 * /api/lessons/manifest llega con id = "manifest". Cada rama es su propio
 * endpoint para que solo el manifiesto sea público; cualquier otro id exige
 * el token igual que antes.
 */
const manifiesto = crearEndpoint({ metodo: 'GET', nombre: 'lessons/manifest', publico: true }, servirManifiesto);
const leccion = crearEndpoint({ metodo: 'GET', nombre: 'lessons/[id]' }, servirLeccion);

module.exports = (req, res) => (req.query.id === 'manifest' ? manifiesto(req, res) : leccion(req, res));
