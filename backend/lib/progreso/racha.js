/**
 * Racha de días con actividad (RN-08, docs/progreso-diseno.md "Reglas del
 * módulo"). Los días son cadenas "AAAA-MM-DD" en hora de México: así una
 * partida a las 23:30 cuenta para ese día y no para el siguiente en UTC.
 */
const ZONA_HORARIA = 'America/Mexico_City';
const DIAS_GUARDADOS = 14;
const MS_POR_DIA = 24 * 60 * 60 * 1000;

/** "2026-10-06": el día cuenta en hora de México, no en UTC. */
function diaLocal(ahora) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_HORARIA }).format(new Date(ahora));
}

// Aritmética de calendario sobre "AAAA-MM-DD" (en UTC para no depender de horarios de verano).
const aFecha = (dia) => new Date(`${dia}T00:00:00Z`);
const aDia = (fecha) => fecha.toISOString().slice(0, 10);

function sumarDias(dia, n) {
  return aDia(new Date(aFecha(dia).getTime() + n * MS_POR_DIA));
}

/** Lunes de la semana (lunes a domingo) a la que pertenece el día. */
function lunesDe(dia) {
  const diaSemana = (aFecha(dia).getUTCDay() + 6) % 7; // 0 = lunes
  return sumarDias(dia, -diaSemana);
}

/**
 * Registra actividad en `dia`. Devuelve solo los campos de racha del resumen:
 * { racha_actual, racha_maxima, ultimo_dia, dias_activos }.
 *
 * - El mismo día no cambia la racha.
 * - El día siguiente al último la aumenta; un hueco la reinicia en 1.
 * - Un día anterior al último (llegó tarde) solo se agrega a dias_activos.
 */
function registrarDia(resumen, dia) {
  const ultimo = resumen.ultimo_dia || null;
  let rachaActual = resumen.racha_actual || 0;
  let ultimoDia = ultimo;

  if (!ultimo || dia > ultimo) {
    rachaActual = ultimo && sumarDias(ultimo, 1) === dia ? rachaActual + 1 : 1;
    ultimoDia = dia;
  }

  const dias = [...new Set([...(resumen.dias_activos || []), dia])].sort().slice(-DIAS_GUARDADOS);
  return {
    racha_actual: rachaActual,
    racha_maxima: Math.max(resumen.racha_maxima || 0, rachaActual),
    ultimo_dia: ultimoDia,
    dias_activos: dias,
  };
}

/** La racha que se muestra hoy: si el último día activo fue antes de ayer, ya se perdió. */
function rachaVigente(resumen, hoy) {
  const ultimo = resumen.ultimo_dia;
  if (!ultimo) return 0;
  return ultimo === hoy || sumarDias(ultimo, 1) === hoy ? resumen.racha_actual || 0 : 0;
}

/** Cuántos días distintos de la semana de `dia` tuvieron actividad. */
function diasEnSemana(diasActivos, dia) {
  const lunes = lunesDe(dia);
  return new Set((diasActivos || []).filter((d) => lunesDe(d) === lunes)).size;
}

module.exports = { ZONA_HORARIA, DIAS_GUARDADOS, diaLocal, sumarDias, lunesDe, registrarDia, rachaVigente, diasEnSemana };
