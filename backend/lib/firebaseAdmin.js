const admin = require('firebase-admin');

/**
 * Inicializa Firebase Admin una sola vez (los serverless de Vercel pueden
 * reutilizar la misma instancia entre invocaciones "calientes").
 * La credencial viene de una variable de entorno en Vercel, NUNCA de un
 * archivo dentro del repositorio.
 */
function getFirebaseAdmin() {
  if (!admin.apps.length && process.env.FIRESTORE_EMULATOR_HOST && !process.env.FIREBASE_SERVICE_ACCOUNT_BASE64) {
    // Desarrollo/pruebas con los emuladores de Firebase: no hacen falta credenciales.
    admin.initializeApp({ projectId: process.env.GCLOUD_PROJECT || 'demo-xpspeak' });
  }
  if (!admin.apps.length) {
    const serviceAccountBase64 = process.env.FIREBASE_SERVICE_ACCOUNT_BASE64;
    if (!serviceAccountBase64) {
      throw new Error('Falta la variable de entorno FIREBASE_SERVICE_ACCOUNT_BASE64');
    }
    const serviceAccountJson = Buffer.from(serviceAccountBase64, 'base64').toString('utf-8');
    const serviceAccount = JSON.parse(serviceAccountJson);

    admin.initializeApp({
      credential: admin.credential.cert(serviceAccount),
    });
  }
  return admin;
}

module.exports = { getFirebaseAdmin };
