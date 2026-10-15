const crypto = require('crypto');
const { crearEndpoint } = require('../../lib/endpoint');
const { responderConEtag } = require('../../lib/http');
const { obtenerProgresosDeUsuario, listarConceptosDeUsuario } = require('../../lib/lecciones/repositorio');
const { validarNivel } = require('../../lib/lecciones/validacion');
const { diaLocal } = require('../../lib/progreso/racha');
const { obtenerResumen } = require('../../lib/progreso/repositorio');
const { vistaDeProgreso } = require('../../lib/progreso/vista');

/**
 * GET /api/progress[?level=A1] — pestaña Progreso (CU-08, RF-05, RF-14,
 * docs/progreso-diseno.md Fase 2): nivel MCER con su dominio, XP y nivel de
 * experiencia, racha, habilidades e insignias con su avance. Si el usuario
 * aún no tiene resumen (actividad de antes de este módulo), se reconstruye.
 * Responde con ETag: si nada cambió, la app recibe un 304 vacío.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'progress' }, async ({ req, res, uid, db }) => {
  const nivel = req.query.level ? validarNivel(req.query.level) : 'A1';
  const ahora = Date.now();
  const [resumen, progresos, conceptos] = await Promise.all([
    obtenerResumen(db, uid, ahora),
    obtenerProgresosDeUsuario(db, uid),
    listarConceptosDeUsuario(db, uid),
  ]);

  const cuerpo = vistaDeProgreso({ resumen, nivel, progresos: [...progresos.values()], conceptos, hoy: diaLocal(ahora) });
  const etag = crypto.createHash('sha256').update(JSON.stringify(cuerpo)).digest('hex').slice(0, 16);
  return responderConEtag(req, res, etag, cuerpo);
});
