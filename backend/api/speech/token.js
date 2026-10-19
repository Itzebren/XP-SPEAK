const { crearEndpoint } = require('../../lib/endpoint');
const { configuracionAzure, emitirToken } = require('../../lib/speech/azure');
const { consumirToken, MAX_TOKENS_POR_VENTANA } = require('../../lib/speech/limite');

/**
 * GET /api/speech/token — token temporal de Azure AI Speech para que Eco
 * Vocal evalúe la pronunciación desde el teléfono (§5.1). El audio va directo
 * del teléfono a Azure en streaming y no se guarda (RN-04).
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'speech/token' }, async ({ res, uid, db }) => {
  const configuracion = configuracionAzure();
  const ahora = Date.now();
  await consumirToken(db, uid, ahora);
  const token = await emitirToken(configuracion, ahora);
  res.setHeader('Cache-Control', 'no-store');
  return res.status(200).json(token);
});

module.exports.MAX_TOKENS_POR_VENTANA = MAX_TOKENS_POR_VENTANA;
