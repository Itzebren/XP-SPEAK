const { crearEndpoint } = require('../../lib/endpoint');
const { ErrorApi } = require('../../lib/errores');
const { configuracionAzure, emitirToken } = require('../../lib/speech/azure');

/**
 * GET /api/speech/token — token temporal de Azure AI Speech para que Eco
 * Vocal evalúe la pronunciación desde el teléfono (§5.1). El audio va directo
 * del teléfono a Azure en streaming y no se guarda (RN-04).
 *
 * Rate limiting por uid: cada token sirve 10 minutos, así que una app normal
 * pide pocos; el límite protege el costo del recurso de Azure.
 */
const COLECCION_LIMITE = 'limite_speech';
const MAX_TOKENS_POR_VENTANA = 10;
const VENTANA_MS = 60 * 60 * 1000;

async function consumirToken(db, uid, ahora) {
  const ref = db.collection(COLECCION_LIMITE).doc(uid);
  await db.runTransaction(async (tx) => {
    const snapshot = await tx.get(ref);
    const previo = snapshot.exists ? snapshot.data() : null;
    const dentroDeVentana = previo && ahora - previo.ventana_inicio < VENTANA_MS;
    const usados = dentroDeVentana ? previo.usados : 0;
    if (usados >= MAX_TOKENS_POR_VENTANA) {
      throw new ErrorApi(429, 'DEMASIADOS_TOKENS', 'Usaste mucho la evaluación de voz. Intenta en un rato.');
    }
    tx.set(ref, { uid, ventana_inicio: dentroDeVentana ? previo.ventana_inicio : ahora, usados: usados + 1 });
  });
}

module.exports = crearEndpoint({ metodo: 'GET', nombre: 'speech/token' }, async ({ res, uid, db }) => {
  const configuracion = configuracionAzure();
  const ahora = Date.now();
  await consumirToken(db, uid, ahora);
  const token = await emitirToken(configuracion, ahora);
  res.setHeader('Cache-Control', 'no-store');
  return res.status(200).json(token);
});

module.exports.MAX_TOKENS_POR_VENTANA = MAX_TOKENS_POR_VENTANA;
