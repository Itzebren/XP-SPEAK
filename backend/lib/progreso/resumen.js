const { obtenerLeccion, NIVELES } = require('../lecciones/contenido');
const { evaluar, insigniaPublica, obtenerInsignia } = require('./insignias');
const { subioDeNivel } = require('./niveles');
const { diaLocal, registrarDia } = require('./racha');

/**
 * Resumen de progreso por usuario (`resumen_usuario/{uid}`, docs/progreso-diseno.md
 * Decisión 2). Se actualiza dentro de las transacciones de cada actividad para
 * que GET /api/progress lea un documento en vez de sumar todo el historial.
 * Aquí solo hay funciones puras; la persistencia está en repositorio.js.
 */
const VERSION = 1;
const TIPOS = ['leccion', 'repaso', 'minijuego', 'chat'];

function resumenVacio(uid) {
  return {
    uid,
    xp_total: 0,
    racha_actual: 0,
    racha_maxima: 0,
    ultimo_dia: null,
    dias_activos: [],
    lecciones_aprobadas: Object.fromEntries(NIVELES.map((nivel) => [nivel, []])),
    juegos: {},
    conteos: { lecciones: 0, repasos: 0, chats: 0 },
    insignias: {},
    version: VERSION,
  };
}

function acumularJuego(previo = {}, { aciertos, total }) {
  const precision = total > 0 ? aciertos / total : 0;
  return {
    partidas: (previo.partidas || 0) + 1,
    aciertos: (previo.aciertos || 0) + aciertos,
    total: (previo.total || 0) + total,
    perfectas: (previo.perfectas || 0) + (total > 0 && aciertos === total ? 1 : 0),
    sobre_80: (previo.sobre_80 || 0) + (precision >= 0.8 ? 1 : 0),
  };
}

function agregarLeccion(aprobadas, nivel, leccionId) {
  if (!nivel || !leccionId) return aprobadas;
  const delNivel = aprobadas[nivel] || [];
  return delNivel.includes(leccionId) ? aprobadas : { ...aprobadas, [nivel]: [...delNivel, leccionId] };
}

/**
 * Aplica una actividad terminada (RN-08, RN-09). Función pura.
 *
 * @param actividad { tipo: 'leccion'|'repaso'|'minijuego'|'chat', xp, juego?, aciertos?, total?, leccionId?, nivel? }
 *                  Una lección solo se aplica cuando el intento aprobó.
 * @param opciones.retroactiva true al reconstruir el historial: las insignias se guardan sin festejo
 * @returns { resumen, insigniasNuevas: [{ id, nombre, descripcion, criterio, icono }], nivelNuevo: número | null }
 */
function aplicarActividad(previo, actividad, ahora, { retroactiva = false } = {}) {
  if (!TIPOS.includes(actividad.tipo)) throw new Error(`Tipo de actividad desconocido: ${actividad.tipo}`);
  const xp = Math.max(0, actividad.xp || 0);
  const conteos = { lecciones: 0, repasos: 0, chats: 0, ...previo.conteos };
  let { lecciones_aprobadas: leccionesAprobadas = {}, juegos = {} } = previo;

  if (actividad.tipo === 'leccion') {
    conteos.lecciones += 1;
    leccionesAprobadas = agregarLeccion(leccionesAprobadas, actividad.nivel, actividad.leccionId);
  } else if (actividad.tipo === 'repaso') {
    conteos.repasos += 1;
  } else if (actividad.tipo === 'chat') {
    conteos.chats += 1;
  } else {
    juegos = { ...juegos, [actividad.juego]: acumularJuego(juegos[actividad.juego], actividad) };
  }

  const resumen = {
    ...previo,
    ...registrarDia(previo, diaLocal(ahora)),
    xp_total: (previo.xp_total || 0) + xp,
    lecciones_aprobadas: leccionesAprobadas,
    juegos,
    conteos,
    version: VERSION,
    actualizado: ahora,
  };

  const { nuevas } = evaluar(resumen);
  resumen.insignias = { ...previo.insignias };
  for (const id of nuevas) {
    resumen.insignias[id] = { fecha: ahora, retroactiva };
  }

  return {
    resumen,
    insigniasNuevas: nuevas.map((id) => insigniaPublica(obtenerInsignia(id))),
    nivelNuevo: subioDeNivel(previo.xp_total || 0, resumen.xp_total),
  };
}

/** Datos de una partida guardada (aciertos y total no van en el evento). */
function datosDePartida(partida) {
  try {
    const { aciertos, total } = JSON.parse(partida.respuesta_json);
    return Number.isInteger(aciertos) && Number.isInteger(total) ? { aciertos, total } : null;
  } catch {
    return null;
  }
}

/** Convierte un evento de `eventos_actividad` en la actividad que lo originó (o null si no cuenta). */
function actividadDeEvento(evento, partidas) {
  const xp = evento.xp || 0;
  switch (evento.tipo) {
    case 'leccion_completada':
      return { tipo: 'leccion', xp, leccionId: evento.leccion_id, nivel: obtenerLeccion(evento.leccion_id)?.nivel_mcer };
    case 'repaso_completado':
      return { tipo: 'repaso', xp };
    case 'chat_completado':
      return { tipo: 'chat', xp };
    case 'minijuego_completado': {
      // Si no se encuentra la partida, la precisión del evento sirve como una partida de 1 ronda.
      const datos = partidas.get(evento.partida_id) || { aciertos: evento.puntaje ?? 0, total: 1 };
      return { tipo: 'minijuego', xp, juego: evento.juego, ...datos };
    }
    default:
      return null;
  }
}

/**
 * Reconstruye el resumen a partir del historial (usuarios que tuvieron
 * actividad antes de este módulo). Aplica los eventos en orden, así que la
 * XP, la racha y las insignias salen igual que si se hubieran registrado en
 * vivo; las insignias quedan como `retroactiva` para no festejarlas de golpe.
 *
 * @param fuentes.eventos   documentos de eventos_actividad del usuario
 * @param fuentes.progresos documentos de progreso_lecciones (por si falta algún evento de aprobación)
 * @param fuentes.partidas  documentos de partidas_minijuego (para aciertos y total)
 */
function reconstruirResumen(uid, { eventos = [], progresos = [], partidas = [] } = {}, ahora = Date.now()) {
  const porPartida = new Map(
    partidas.map((p) => [p.partida_id, datosDePartida(p)]).filter(([, datos]) => datos)
  );
  const ordenados = [...eventos].sort((a, b) => (a.fecha || 0) - (b.fecha || 0));

  let resumen = resumenVacio(uid);
  for (const evento of ordenados) {
    const actividad = actividadDeEvento(evento, porPartida);
    if (actividad) {
      resumen = aplicarActividad(resumen, actividad, evento.fecha || ahora, { retroactiva: true }).resumen;
    }
  }

  // Las lecciones aprobadas salen de progreso_lecciones, que es la fuente de verdad del módulo de Lecciones.
  for (const progreso of progresos.filter((p) => p.aprobada)) {
    const nivel = obtenerLeccion(progreso.leccion_id)?.nivel_mcer;
    resumen.lecciones_aprobadas = agregarLeccion(resumen.lecciones_aprobadas, nivel, progreso.leccion_id);
  }
  for (const id of evaluar(resumen).nuevas) {
    resumen.insignias[id] = { fecha: ahora, retroactiva: true };
  }

  return { ...resumen, actualizado: ahora, reconstruido: ahora };
}

module.exports = { VERSION, resumenVacio, aplicarActividad, reconstruirResumen };
