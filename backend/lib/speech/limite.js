const { ErrorApi } = require('../errores');

/**
 * Rate limiting por uid de los tokens de Azure AI Speech. Cada token sirve 10
 * minutos, así que una app normal pide pocos; el límite protege el costo del
 * recurso de Azure. Eco Vocal (GET) y la voz del chat / Pronunciación (POST)
 * piden el token a /api/speech-token y comparten este límite.
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

module.exports = { COLECCION_LIMITE, MAX_TOKENS_POR_VENTANA, consumirToken };
