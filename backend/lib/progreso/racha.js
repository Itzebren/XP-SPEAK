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
 * - Un día anterior al último (llegó tarde, p. ej. una actividad hecha sin conexión que se
 *   sincronizó después) se agrega a dias_activos y, si llena el hueco justo antes de la
 *   racha actual, la une con la racha que había antes de ese hueco.
 */
function registrarDia(resumen, dia) {
  const ultimo = resumen.ultimo_dia || null;
  let rachaActual = resumen.racha_actual || 0;
  let ultimoDia = ultimo;
  const previos = new Set(resumen.dias_activos || []);

  if (!ultimo || dia > ultimo) {
    rachaActual = ultimo && sumarDias(ultimo, 1) === dia ? rachaActual + 1 : 1;
    ultimoDia = dia;
  } else if (dia === sumarDias(ultimo, -rachaActual) && !previos.has(dia)) {
    let antes = 0;
    while (previos.has(sumarDias(dia, -(antes + 1)))) antes += 1;
    rachaActual += 1 + antes;
  }

  const dias = [...new Set([...previos, dia])].sort().slice(-DIAS_GUARDADOS);
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

// Una actividad hecha sin conexión cuenta para su día si llega en esta ventana.
const MAX_DIAS_ATRASO = 7;

/**
 * RN-08: momento en que se hizo la actividad. La app manda `completado_en` porque una
 * lección o partida hecha sin conexión se envía después; sin él (o si no es creíble:
 * en el futuro o con más de MAX_DIAS_ATRASO de atraso) se acota a la ventana.
 */
function fechaDeActividad(completadoEn, ahora) {
  if (!Number.isInteger(completadoEn)) return ahora;
  return Math.min(ahora, Math.max(completadoEn, ahora - MAX_DIAS_ATRASO * MS_POR_DIA));
}

module.exports = {
  ZONA_HORARIA,
  DIAS_GUARDADOS,
  MAX_DIAS_ATRASO,
  diaLocal,
  sumarDias,
  lunesDe,
  registrarDia,
  rachaVigente,
  diasEnSemana,
  fechaDeActividad,
};
