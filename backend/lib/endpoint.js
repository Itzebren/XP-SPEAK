const { getFirebaseAdmin } = require('./firebaseAdmin');
const { obtenerUid } = require('./auth');
const { ErrorApi, responderError } = require('./errores');

/**
 * Envoltura común de los endpoints: valida el método HTTP, verifica el token
 * (salvo en endpoints públicos) y convierte cualquier error en la respuesta
 * estándar { error, codigo }. Así cada handler solo contiene su propia lógica.
 *
 *   module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons' }, async ({ req, res, uid, db }) => …);
 */
function crearEndpoint({ metodo, nombre, publico = false }, manejar) {
  return async (req, res) => {
    try {
      if (req.method !== metodo) {
        throw new ErrorApi(405, 'METODO_NO_PERMITIDO', 'Método no permitido');
      }
      const uid = publico ? null : await obtenerUid(req);
      return await manejar({
        req,
        res,
        uid,
        // Perezoso: los endpoints que no usan Firestore no inicializan firebase-admin.
        get db() {
          return getFirebaseAdmin().firestore();
        },
      });
    } catch (error) {
      return responderError(res, error, nombre);
    }
  };
}

module.exports = { crearEndpoint };
