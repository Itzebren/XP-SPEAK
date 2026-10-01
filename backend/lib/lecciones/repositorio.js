const { ErrorApi } = require('../errores');
const { ESTADOS, estaDesbloqueada, aplicarIntento } = require('./estado');
const { siguienteLeccion } = require('./contenido');
const { acumularSenal } = require('./srs');
const { consumirIntento } = require('./limite');

/**
 * Persistencia por usuario en Firestore (docs/lecciones-diseno.md §5.2).
 * Aquí solo se lee y escribe; las reglas viven en estado.js, srs.js y
 * limite.js. Las fechas son milisegundos epoch, igual que en RF-03.
 */
const COLECCIONES = {
  PROGRESO: 'progreso_lecciones',
  SRS: 'srs_conceptos',
  EVENTOS: 'eventos_actividad',
  INTENTOS: 'intentos_lecciones',
  REPASOS: 'intentos_repaso',
};

function referencias(db, uid, leccion) {
  const progreso = (leccionId) => db.collection(COLECCIONES.PROGRESO).doc(`${uid}_${leccionId}`);
  return {
    progreso: progreso(leccion.id),
    prerequisito: leccion.prerequisito_id ? progreso(leccion.prerequisito_id) : null,
    intento: (attemptId) => db.collection(COLECCIONES.INTENTOS).doc(`${uid}_${attemptId}`),
    srs: (conceptoId) => db.collection(COLECCIONES.SRS).doc(`${uid}_${conceptoId}`),
    evento: () => db.collection(COLECCIONES.EVENTOS).doc(),
  };
}

/**
 * Lee varios documentos en una sola ida a Firestore y los devuelve con el
 * mismo nombre con que se pidieron: { progreso: {...} | null, srs: [...] }.
 * Acepta una referencia, null (no leer) o una lista de referencias.
 */
async function leerEnTransaccion(tx, pedidos) {
  const refs = Object.values(pedidos).flat().filter(Boolean);
  const snapshots = refs.length ? await tx.getAll(...refs) : [];
  const datos = new Map(refs.map((ref, i) => [ref, snapshots[i].exists ? snapshots[i].data() : null]));
  const leer = (ref) => (ref ? datos.get(ref) : null);

  return Object.fromEntries(
    Object.entries(pedidos).map(([nombre, pedido]) => [nombre, Array.isArray(pedido) ? pedido.map(leer) : leer(pedido)])
  );
}

function exigirDesbloqueada(leccion, progresoPrerequisito) {
  if (!estaDesbloqueada(leccion, progresoPrerequisito)) {
    throw new ErrorApi(403, 'LECCION_BLOQUEADA', 'Primero aprueba la lección anterior para desbloquear esta (mínimo 70%).');
  }
}

async function listarPorUsuario(db, coleccion, uid) {
  const snapshot = await db.collection(coleccion).where('uid', '==', uid).get();
  return snapshot.docs.map((doc) => doc.data());
}

async function obtenerProgresosDeUsuario(db, uid) {
  const progresos = await listarPorUsuario(db, COLECCIONES.PROGRESO, uid);
  return new Map(progresos.map((progreso) => [progreso.leccion_id, progreso]));
}

function listarConceptosDeUsuario(db, uid) {
  return listarPorUsuario(db, COLECCIONES.SRS, uid);
}

/** Lo que responde POST /attempt (§6.1) y se guarda para reenvíos idempotentes. */
function construirRespuesta({ attemptId, leccion, resultado, progreso, xpGanado, primeraAprobacion }) {
  return {
    attempt_id: attemptId,
    leccion_id: leccion.id,
    puntaje: resultado.puntaje,
    correctas: resultado.correctas,
    total: resultado.total,
    umbral_aprobacion: resultado.umbral,
    aprobada: resultado.aprobada,
    estado: progreso.estado,
    mejor_puntaje: progreso.mejor_puntaje,
    xp_ganado: xpGanado,
    desbloqueada_siguiente: primeraAprobacion ? siguienteLeccion(leccion.id) : null,
    feedback: resultado.feedback,
    conceptos_debiles: resultado.conceptos_debiles,
    // CU-06 A3: con menos del 70% se sugiere repetir antes de avanzar.
    sugerencia: resultado.aprobada ? null : 'repetir_leccion',
  };
}

/**
 * Núcleo de POST /api/lessons/:id/attempt. Todo ocurre en una sola
 * transacción: si dos envíos del mismo intento llegan a la vez, Firestore
 * reintenta uno y ese ya encuentra el attempt_id procesado, así que el XP
 * nunca se duplica (RN-09 + idempotencia §6.2).
 */
async function registrarIntento(db, { uid, leccion, attemptId, resultado, ahora = Date.now() }) {
  const refs = referencias(db, uid, leccion);
  const refIntento = refs.intento(attemptId);
  const refsSrs = resultado.conceptos.map((c) => refs.srs(c.concepto_id));

  return db.runTransaction(async (tx) => {
    const leido = await leerEnTransaccion(tx, {
      intento: refIntento,
      progreso: refs.progreso,
      prerequisito: refs.prerequisito,
      srs: refsSrs,
    });

    // 1. Idempotencia: el mismo attempt_id devuelve la respuesta original sin escribir nada.
    if (leido.intento) {
      if (leido.intento.leccion_id !== leccion.id) {
        throw new ErrorApi(409, 'ATTEMPT_ID_REUTILIZADO', 'Ese attempt_id ya se usó en otra lección');
      }
      return { ...JSON.parse(leido.intento.respuesta_json), repetido: true };
    }

    // 2. Reglas: lección desbloqueada (RN-06) y límite de intentos.
    exigirDesbloqueada(leccion, leido.prerequisito);
    const limite = consumirIntento(leido.progreso, ahora);
    const { progreso, xpGanado, primeraAprobacion } = aplicarIntento(leido.progreso, leccion, resultado, ahora);
    const respuesta = construirRespuesta({ attemptId, leccion, resultado, progreso, xpGanado, primeraAprobacion });

    // 3. Escrituras.
    tx.set(refs.progreso, { ...progreso, ...limite, uid });
    resultado.conceptos.forEach((conteo, i) => {
      tx.set(refsSrs[i], acumularSenal(leido.srs[i], { uid, ...conteo }, leccion, ahora));
    });
    if (resultado.aprobada) {
      // "Actividad válida" para la racha (RN-08) y auditoría del XP otorgado.
      tx.set(refs.evento(), {
        uid,
        tipo: 'leccion_completada',
        leccion_id: leccion.id,
        attempt_id: attemptId,
        puntaje: resultado.puntaje,
        xp: xpGanado,
        fecha: ahora,
      });
    }
    // Serializada: Firestore no admite arrays anidados (los pares de "emparejar").
    tx.set(refIntento, {
      uid,
      leccion_id: leccion.id,
      attempt_id: attemptId,
      fecha: ahora,
      respuesta_json: JSON.stringify(respuesta),
    });

    return { ...respuesta, repetido: false };
  });
}

/**
 * RN-12 / CU-06 A2: guarda en qué sección de la teoría va el usuario para
 * retomarla. Una lección completada se queda completada.
 */
async function guardarAvance(db, { uid, leccion, seccionActual, ahora = Date.now() }) {
  const refs = referencias(db, uid, leccion);

  return db.runTransaction(async (tx) => {
    const leido = await leerEnTransaccion(tx, { progreso: refs.progreso, prerequisito: refs.prerequisito });
    exigirDesbloqueada(leccion, leido.prerequisito);

    const previo = leido.progreso || {};
    const estado = previo.aprobada ? ESTADOS.COMPLETADA : ESTADOS.EN_PROGRESO;
    const avance = { seccion_actual: seccionActual, fecha: ahora };

    tx.set(refs.progreso, {
      intentos: 0,
      aprobada: false,
      xp_otorgado: false,
      mejor_puntaje: 0,
      ultimo_puntaje: null,
      ...previo,
      uid,
      leccion_id: leccion.id,
      version_leccion: leccion.version,
      estado,
      avance,
      fecha_actualizacion: ahora,
    });
    return { leccion_id: leccion.id, estado, avance };
  });
}

/**
 * Núcleo de POST /api/srs/session: registra las respuestas de una sesión de
 * repaso y reprograma cada concepto con SM-2. Idempotente por attempt_id,
 * igual que las lecciones: un reenvío no vuelve a mover los intervalos.
 * El repaso no da XP (no se puede farmear), pero cuenta como actividad (RN-08).
 *
 * @param leccionDeConcepto concepto_id → lección de donde salió su ejercicio
 */
async function registrarRepaso(db, { uid, attemptId, resultado, leccionDeConcepto, ahora = Date.now() }) {
  const refIntento = db.collection(COLECCIONES.REPASOS).doc(`${uid}_${attemptId}`);
  const refsSrs = resultado.conceptos.map((c) => db.collection(COLECCIONES.SRS).doc(`${uid}_${c.concepto_id}`));

  return db.runTransaction(async (tx) => {
    const leido = await leerEnTransaccion(tx, { intento: refIntento, srs: refsSrs });
    if (leido.intento) {
      return { ...JSON.parse(leido.intento.respuesta_json), repetido: true };
    }

    const conceptos = resultado.conceptos.map((conteo, i) => {
      const nuevo = acumularSenal(leido.srs[i], { uid, ...conteo }, leccionDeConcepto.get(conteo.concepto_id), ahora);
      tx.set(refsSrs[i], nuevo);
      return {
        concepto_id: conteo.concepto_id,
        acierto: conteo.fallos === 0,
        intervalo: nuevo.intervalo,
        proxima_revision: nuevo.proxima_revision,
      };
    });

    const respuesta = {
      attempt_id: attemptId,
      puntaje: resultado.puntaje,
      correctas: resultado.correctas,
      total: resultado.total,
      feedback: resultado.feedback,
      conceptos,
    };

    tx.set(db.collection(COLECCIONES.EVENTOS).doc(), {
      uid,
      tipo: 'repaso_completado',
      attempt_id: attemptId,
      puntaje: resultado.puntaje,
      xp: 0,
      fecha: ahora,
    });
    tx.set(refIntento, { uid, attempt_id: attemptId, fecha: ahora, respuesta_json: JSON.stringify(respuesta) });

    return { ...respuesta, repetido: false };
  });
}

module.exports = {
  COLECCIONES,
  obtenerProgresosDeUsuario,
  listarConceptosDeUsuario,
  registrarIntento,
  registrarRepaso,
  guardarAvance,
};
