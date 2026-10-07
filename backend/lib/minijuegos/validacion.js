const { ErrorApi } = require('../errores');
const { obtenerConcepto } = require('../lecciones/contenido');
const { JUEGOS } = require('./xp');

// Igual que en las lecciones: Android genera el UUID para hacer idempotente el envío.
const PATRON_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MAX_CONCEPTOS = 40;
// Nadie responde una ronda en menos de esto: frena resultados fabricados.
const MS_MINIMO_POR_RONDA = 300;
// Las partidas se pueden retomar, pero solo cuenta el tiempo jugado.
const DURACION_MAXIMA_MS = 30 * 60 * 1000;
// Ráfaga empieza con 60 s y cada acierto suma 2 s (§5.3).
const RAFAGA_MS_INICIALES = 60000;
const RAFAGA_MS_POR_ACIERTO = 2000;
const RAFAGA_MARGEN_MS = 5000;

const entradaInvalida = (mensaje) => new ErrorApi(400, 'ENTRADA_INVALIDA', mensaje);
const esEntero = (valor, min, max) => Number.isInteger(valor) && valor >= min && valor <= max;

/**
 * Valida el body de POST /api/games/result. Los conceptos que ya no existen
 * en el contenido (una app con un banco viejo) se ignoran en vez de rechazar
 * la partida: el resultado sigue siendo válido para la XP y la racha.
 */
function validarResultado(body) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw entradaInvalida('El cuerpo debe ser un objeto JSON');
  }
  const { partida_id: partidaId, juego, aciertos, total, duracion_ms: duracionMs, conceptos = [] } = body;

  if (typeof partidaId !== 'string' || !PATRON_UUID.test(partidaId)) {
    throw entradaInvalida('partida_id debe ser un UUID');
  }
  const config = JUEGOS[juego];
  if (!config) {
    throw entradaInvalida(`juego debe ser uno de: ${Object.keys(JUEGOS).join(', ')}`);
  }
  if (!esEntero(total, config.rondas.min, config.rondas.max)) {
    throw entradaInvalida(`total debe estar entre ${config.rondas.min} y ${config.rondas.max} para ${juego}`);
  }
  if (!esEntero(aciertos, 0, total)) {
    throw entradaInvalida('aciertos debe ser un entero entre 0 y total');
  }
  if (!esEntero(duracionMs, total * MS_MINIMO_POR_RONDA, DURACION_MAXIMA_MS)) {
    throw entradaInvalida('duracion_ms no es posible para esa cantidad de rondas');
  }
  if (config.msPorRonda === null && duracionMs > RAFAGA_MS_INICIALES + aciertos * RAFAGA_MS_POR_ACIERTO + RAFAGA_MARGEN_MS) {
    throw entradaInvalida('duracion_ms excede el reloj de Ráfaga');
  }

  if (!Array.isArray(conceptos) || conceptos.length > MAX_CONCEPTOS) {
    throw entradaInvalida(`conceptos debe ser una lista de máximo ${MAX_CONCEPTOS}`);
  }
  const vistos = new Set();
  const validos = [];
  for (const concepto of conceptos) {
    if (
      !concepto ||
      typeof concepto.concepto_id !== 'string' ||
      !esEntero(concepto.aciertos, 0, total) ||
      !esEntero(concepto.errores, 0, total * 2)
    ) {
      throw entradaInvalida('Cada concepto debe tener concepto_id, aciertos y errores');
    }
    if (vistos.has(concepto.concepto_id)) {
      throw entradaInvalida(`El concepto ${concepto.concepto_id} viene repetido`);
    }
    vistos.add(concepto.concepto_id);
    if (obtenerConcepto(concepto.concepto_id) && concepto.aciertos + concepto.errores > 0) {
      validos.push({ concepto_id: concepto.concepto_id, aciertos: concepto.aciertos, fallos: concepto.errores });
    }
  }

  return { partidaId, juego, aciertos, total, duracionMs, conceptos: validos };
}

module.exports = { validarResultado };
