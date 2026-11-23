const { crearEndpoint } = require('../../lib/endpoint');
const { getFirebaseAdmin } = require('../../lib/firebaseAdmin');
const { eliminarDatosDeUsuario } = require('../../lib/cuenta/eliminar');

/**
 * POST /api/account/delete — "Eliminar cuenta e historial" (Ilustración 36, RN-14).
 * Borra primero los datos de Firestore y al final la cuenta de Firebase Auth: si
 * algo falla a medias, el usuario puede volver a entrar y reintentar.
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'account/delete' }, async ({ res, uid, db }) => {
  const auth = getFirebaseAdmin().auth();
  // Solo "no existe" significa que ya se borró en un intento anterior; cualquier otro
  // error (credenciales, red) debe fallar para no reportar un borrado que no ocurrió.
  const usuario = await auth.getUser(uid).catch((error) => {
    if (error?.code === 'auth/user-not-found') return null;
    throw error;
  });
  const { documentos } = await eliminarDatosDeUsuario(db, { uid, correo: usuario?.email });
  if (usuario) await auth.deleteUser(uid);
  return res.status(200).json({ eliminada: true, documentos });
});
