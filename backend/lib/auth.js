const { getFirebaseAdmin } = require('./firebaseAdmin');
const { ErrorApi } = require('./errores');

/**
 * Verifica el ID token de Firebase que manda Android en
 * "Authorization: Bearer <token>" y devuelve el uid. El uid SIEMPRE sale del
 * token verificado en servidor; nunca se confía en un uid del body (RNF-06).
 */
async function obtenerUid(req) {
  const encabezado = req.headers.authorization || req.headers.Authorization || '';
  const [tipo, token] = encabezado.split(' ');
  if (tipo !== 'Bearer' || !token) {
    throw new ErrorApi(401, 'TOKEN_AUSENTE', 'Falta el token de autenticación');
  }

  try {
    const decodificado = await getFirebaseAdmin().auth().verifyIdToken(token);
    return decodificado.uid;
  } catch {
    throw new ErrorApi(401, 'TOKEN_INVALIDO', 'Token inválido o expirado');
  }
}

module.exports = { obtenerUid };
