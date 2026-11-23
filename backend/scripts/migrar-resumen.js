#!/usr/bin/env node
/**
 * Genera `resumen_usuario/{uid}` para cada usuario que ya tenía actividad
 * antes del módulo de Progreso (docs/progreso-diseno.md, "Orden de despliegue"):
 *
 *   FIREBASE_SERVICE_ACCOUNT_BASE64=... npm run migrar:resumen
 *
 * Con FIRESTORE_EMULATOR_HOST usa el emulador. Es idempotente: un usuario
 * que ya tiene resumen no se toca, así que se puede correr varias veces.
 * Aunque no se corra, GET /api/progress y cada actividad reconstruyen el
 * resumen que falte; la migración solo evita hacerlo en caliente.
 */
const { getFirebaseAdmin } = require('../lib/firebaseAdmin');
const { migrarResumenes } = require('../lib/progreso/repositorio');

if (require.main === module) {
  migrarResumenes(getFirebaseAdmin().firestore())
    .then(({ revisados, creados }) => {
      console.log(`✓ ${revisados} usuarios con actividad; ${creados} resúmenes nuevos, ${revisados - creados} ya existían.`);
    })
    .catch((error) => {
      console.error('No se pudo migrar:', error);
      process.exit(1);
    });
}
