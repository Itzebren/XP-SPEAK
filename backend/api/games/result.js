const { crearEndpoint } = require('../../lib/endpoint');
const { validarResultado } = require('../../lib/minijuegos/validacion');
const { registrarPartida } = require('../../lib/minijuegos/repositorio');

/**
 * POST /api/games/result — registra una partida terminada de un minijuego
 * (CU-07 paso 8, docs/minijuegos-diseno.md §6). El servidor recalcula la XP
 * (no confía en la del cliente), aplica el tope diario, alimenta el SRS con
 * los aciertos/errores por concepto y marca la actividad del día (RN-08).
 * Idempotente por partida_id: la app reenvía su outbox sin duplicar XP.
 *
 * Body: { partida_id, juego, aciertos, total, duracion_ms, conceptos: [{ concepto_id, aciertos, errores }] }
 */
module.exports = crearEndpoint({ metodo: 'POST', nombre: 'games/result' }, async ({ req, res, uid, db }) => {
  const resultado = validarResultado(req.body);
  const respuesta = await registrarPartida(db, { uid, resultado });
  return res.status(200).json(respuesta);
});
