/**
 * Error "esperado" de la API: lleva el código HTTP y un código legible por
 * la app (p. ej. LECCION_BLOQUEADA) para que Android decida qué mostrar sin
 * tener que parsear el texto del mensaje.
 */
class ErrorApi extends Error {
  constructor(status, codigo, mensaje) {
    super(mensaje);
    this.status = status;
    this.codigo = codigo;
  }
}

/**
 * Responde con la forma estándar { error, codigo }. Cualquier error que no
 * sea ErrorApi se trata como 500 genérico para no filtrar detalles internos.
 */
function responderError(res, error, contexto) {
  if (error instanceof ErrorApi) {
    return res.status(error.status).json({ error: error.message, codigo: error.codigo });
  }
  console.error(`Error en ${contexto}:`, error);
  return res.status(500).json({ error: 'Ocurrió un error. Intenta más tarde.', codigo: 'ERROR_INTERNO' });
}

module.exports = { ErrorApi, responderError };
