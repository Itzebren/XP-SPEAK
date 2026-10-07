const { ErrorApi } = require('../errores');

/**
 * Tokens de Azure AI Speech para Eco Vocal (docs/minijuegos-diseno.md §5.1).
 * La llave del recurso vive solo en el servidor (RNF-06 / RN-13): la app
 * recibe un token que Azure invalida a los 10 minutos.
 *
 * Variables de entorno: AZURE_SPEECH_KEY y AZURE_SPEECH_REGION (p. ej. "southcentralus").
 */
const VIGENCIA_TOKEN_MS = 10 * 60 * 1000;
const TIEMPO_ESPERA_MS = 5000;

function configuracionAzure(entorno = process.env) {
  const clave = entorno.AZURE_SPEECH_KEY;
  const region = entorno.AZURE_SPEECH_REGION;
  if (!clave || !region) {
    // La app interpreta este código como "usa el reconocimiento del teléfono".
    throw new ErrorApi(503, 'SPEECH_NO_CONFIGURADO', 'La evaluación de pronunciación no está disponible por ahora');
  }
  return { clave, region };
}

/** Pide a Azure un token de 10 minutos (endpoint issueToken del servicio STS). */
async function emitirToken({ clave, region }, ahora = Date.now()) {
  let respuesta;
  try {
    respuesta = await fetch(`https://${region}.api.cognitive.microsoft.com/sts/v1.0/issueToken`, {
      method: 'POST',
      headers: { 'Ocp-Apim-Subscription-Key': clave, 'Content-Length': '0' },
      signal: AbortSignal.timeout(TIEMPO_ESPERA_MS),
    });
  } catch (error) {
    console.error('Azure Speech no respondió:', error.message);
    throw new ErrorApi(502, 'SPEECH_NO_DISPONIBLE', 'El servicio de voz no respondió. Intenta de nuevo.');
  }
  if (!respuesta.ok) {
    console.error(`Azure Speech respondió ${respuesta.status} al pedir el token`);
    throw new ErrorApi(502, 'SPEECH_NO_DISPONIBLE', 'El servicio de voz no respondió. Intenta de nuevo.');
  }
  return { token: await respuesta.text(), region, expira_en: ahora + VIGENCIA_TOKEN_MS };
}

module.exports = { VIGENCIA_TOKEN_MS, configuracionAzure, emitirToken };
