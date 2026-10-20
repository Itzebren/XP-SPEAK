const { crearEndpoint } = require('../lib/endpoint');
const { configuracionAzure, emitirToken } = require('../lib/speech/azure');
const { consumirToken, MAX_TOKENS_POR_VENTANA } = require('../lib/speech/limite');

/**
 * GET|POST /api/speech-token — token temporal de Azure AI Speech para evaluar
 * la pronunciación desde el teléfono (§5.1, CU-05, RF-09). Eco Vocal lo pide
 * con GET (MinijuegosApi.kt) y la voz del chat / Pronunciación con POST
 * (SpeechApi.kt, venía del repo xp-speak-auth-backend). El audio va directo
 * del teléfono a Azure en streaming y no se guarda (RN-04).
 */
module.exports = crearEndpoint({ metodo: ['GET', 'POST'], nombre: 'speech-token' }, async ({ res, uid, db }) => {
  const configuracion = configuracionAzure();
  const ahora = Date.now();
  await consumirToken(db, uid, ahora);
  const token = await emitirToken(configuracion, ahora);
  res.setHeader('Cache-Control', 'no-store');
  return res.status(200).json(token);
});

module.exports.MAX_TOKENS_POR_VENTANA = MAX_TOKENS_POR_VENTANA;
