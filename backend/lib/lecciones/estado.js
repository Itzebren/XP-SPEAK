/**
 * Máquina de estados de una lección por usuario (docs/lecciones-diseno.md §5.4):
 *
 *   bloqueada ─(prerequisito aprobado)─► disponible ─(inicia teoría)─► en_progreso
 *   en_progreso ─(intento < umbral)─► reprobada ─(reintenta)─► …
 *   en_progreso/reprobada ─(intento ≥ umbral)─► completada (sigue siendo reabrible)
 *   disponible ─(hay conceptos débiles vencidos)─► repaso_pendiente ─(repasa)─► disponible  (RN-07)
 *
 * "bloqueada" y "disponible" no se guardan en Firestore: se derivan del
 * progreso del prerequisito, así nunca quedan desincronizados.
 */
const ESTADOS = {
  BLOQUEADA: 'bloqueada',
  DISPONIBLE: 'disponible',
  EN_PROGRESO: 'en_progreso',
  REPROBADA: 'reprobada',
  COMPLETADA: 'completada',
  REPASO_PENDIENTE: 'repaso_pendiente',
};

/** RN-06: sin prerequisito, o con el prerequisito aprobado por este usuario. */
function estaDesbloqueada(leccion, progresoPrerequisito) {
  return leccion.prerequisito_id == null || progresoPrerequisito?.aprobada === true;
}

/** Una lección que el usuario nunca abrió ni intentó: empezarla es avanzar a un tema nuevo (RN-07). */
function esTemaNuevo(progreso) {
  return !progreso || (progreso.aprobada !== true && progreso.estado !== ESTADOS.EN_PROGRESO && progreso.estado !== ESTADOS.REPROBADA);
}

/**
 * @param repasoPendiente hay conceptos con alta tasa de error que ya toca repasar: los temas
 *                        nuevos esperan al repaso (RN-07); lo ya empezado sigue disponible.
 */
function calcularEstado(leccion, progreso, progresoPrerequisito, { repasoPendiente = false } = {}) {
  if (progreso?.aprobada) return ESTADOS.COMPLETADA;
  if (!estaDesbloqueada(leccion, progresoPrerequisito)) return ESTADOS.BLOQUEADA;
  if (progreso?.estado === ESTADOS.EN_PROGRESO || progreso?.estado === ESTADOS.REPROBADA) {
    return progreso.estado;
  }
  return repasoPendiente ? ESTADOS.REPASO_PENDIENTE : ESTADOS.DISPONIBLE;
}

/**
 * Nuevo documento de progreso tras un intento calificado. XP atómico (RN-09):
 * solo se otorga al aprobar y solo la primera vez (`xp_otorgado`); repetir una
 * lección completada mejora `mejor_puntaje` pero no vuelve a dar XP.
 */
function aplicarIntento(progresoPrevio, leccion, resultado, ahora) {
  const previo = progresoPrevio || {};
  const yaAprobada = previo.aprobada === true;
  const aprobada = yaAprobada || resultado.aprobada;
  const otorgaXp = resultado.aprobada && previo.xp_otorgado !== true;

  return {
    progreso: {
      ...previo,
      leccion_id: leccion.id,
      version_leccion: leccion.version,
      estado: aprobada ? ESTADOS.COMPLETADA : ESTADOS.REPROBADA,
      ultimo_puntaje: resultado.puntaje,
      mejor_puntaje: Math.max(previo.mejor_puntaje || 0, resultado.puntaje),
      intentos: (previo.intentos || 0) + 1,
      aprobada,
      xp_otorgado: previo.xp_otorgado === true || otorgaXp,
      fecha_aprobacion: previo.fecha_aprobacion || (resultado.aprobada ? ahora : null),
      fecha_actualizacion: ahora,
    },
    xpGanado: otorgaXp ? leccion.xp_recompensa : 0,
    primeraAprobacion: resultado.aprobada && !yaAprobada,
  };
}

module.exports = { ESTADOS, estaDesbloqueada, esTemaNuevo, calcularEstado, aplicarIntento };
