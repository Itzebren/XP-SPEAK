const { crearEndpoint } = require('../lib/endpoint');
const { configuracionAzure, emitirToken } = require('../lib/speech/azure');
const { consumirToken } = require('../lib/speech/limite');

/**
 * POST /api/speech-token — token temporal de Azure AI Speech para la pantalla
 * de Pronunciación (CU-05, RF-09). Es la ruta que ya usa SpeechApi.kt (venía
 * del repo xp-speak-auth-backend); Eco Vocal usa GET /api/speech/token. Misma
 * respuesta { token, region, expira_en } y mismo límite por usuario.
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'speech-token' }, async ({ res, uid, db }) => {
  const configuracion = configuracionAzure();
  const ahora = Date.now();
  await consumirToken(db, uid, ahora);
  const token = await emitirToken(configuracion, ahora);
  res.setHeader('Cache-Control', 'no-store');
  return res.status(200).json(token);
});
