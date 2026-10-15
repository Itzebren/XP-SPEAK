/**
 * Responde con ETag para que Android pueda revalidar su caché de Room con
 * "If-None-Match" y recibir un 304 vacío si el contenido no cambió (RN-11).
 */
function responderConEtag(req, res, etag, cuerpo, cacheControl = 'private, no-cache') {
  const etagEntrecomillado = `"${etag}"`;
  res.setHeader('ETag', etagEntrecomillado);
  res.setHeader('Cache-Control', cacheControl);

  const recibido = req.headers['if-none-match'];
  if (recibido && recibido.split(',').map((v) => v.trim()).includes(etagEntrecomillado)) {
    return res.status(304).end();
  }
  return res.status(200).json(cuerpo);
}

module.exports = { responderConEtag };
