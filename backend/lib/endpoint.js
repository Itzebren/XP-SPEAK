const { getFirebaseAdmin } = require('./firebaseAdmin');
const { obtenerUid } = require('./auth');
const { ErrorApi, responderError } = require('./errores');

/**
 * Envoltura común de los endpoints: valida el método HTTP, verifica el token
 * (salvo en endpoints públicos) y convierte cualquier error en la respuesta
 * estándar { error, codigo }. Así cada handler solo contiene su propia lógica.
 * `metodo` puede ser uno ('GET') o una lista (['GET', 'POST']).
 *
 *   module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons' }, async ({ req, res, uid, db }) => …);
 */
function crearEndpoint({ metodo, nombre, publico = false }, manejar) {
  const metodos = [].concat(metodo);
  return async (req, res) => {
    try {
      if (!metodos.includes(req.method)) {
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
