const { ErrorApi } = require('../errores');

/**
 * Rate limiting de POST /attempt por uid + lección (§6.3, mitiga R10).
 * La ventana se guarda dentro del propio documento de progreso, así se
 * verifica en la misma transacción del intento sin lecturas extra.
 */
const MAX_INTENTOS_POR_VENTANA = 10;
const VENTANA_MS = 60 * 1000;

/** Devuelve los campos de ventana actualizados, o lanza 429 si ya se agotó. */
function consumirIntento(progreso, ahora) {
  const dentroDeVentana = progreso && ahora - (progreso.limite_ventana_inicio || 0) < VENTANA_MS;
  const usados = dentroDeVentana ? progreso.limite_intentos || 0 : 0;
  if (usados >= MAX_INTENTOS_POR_VENTANA) {
    throw new ErrorApi(429, 'DEMASIADOS_INTENTOS', 'Demasiados intentos seguidos. Espera un minuto.');
  }
  return {
    limite_ventana_inicio: dentroDeVentana ? progreso.limite_ventana_inicio : ahora,
    limite_intentos: usados + 1,
  };
}

module.exports = { MAX_INTENTOS_POR_VENTANA, consumirIntento };
