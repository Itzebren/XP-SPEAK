const { ErrorApi } = require('../errores');
const { diaLocal } = require('./racha');
const { aplicarActividad, reconstruirResumen } = require('./resumen');

/**
 * Persistencia del módulo de Progreso (docs/progreso-diseno.md, Fase 2).
 *
 * Regla para no perder historial (Decisión 2): el resumen nunca se crea desde
 * cero dentro de una actividad. Si no existe, se reconstruye primero desde
 * eventos_actividad, en la misma transacción y con lecturas consistentes, y
 * después se aplica la actividad nueva. Así un usuario con 136 XP de antes de
 * este módulo no se queda con la XP de una sola partida, y uno nuevo sí ve
 * el festejo de su primera insignia.
 */
const COLECCIONES = {
  RESUMEN: 'resumen_usuario',
  CHATS: 'chats_registrados',
  CHATS_DIARIO: 'chats_diario',
};

const MAX_CHATS_POR_DIA = 20;

// Se piden al usarse: lecciones/ y minijuegos/repositorio también requieren este módulo.
function coleccionesDeHistorial() {
  const lecciones = require('../lecciones/repositorio').COLECCIONES;
  const minijuegos = require('../minijuegos/repositorio').COLECCIONES;
  return { eventos: lecciones.EVENTOS, progresos: lecciones.PROGRESO, partidas: minijuegos.PARTIDAS };
}

const refResumen = (db, uid) => db.collection(COLECCIONES.RESUMEN).doc(uid);

/** Todos los documentos de una colección que pertenecen al usuario (vía tx si se da). */
async function documentosDeUsuario(lector, db, coleccion, uid) {
  const snapshot = await lector.get(db.collection(coleccion).where('uid', '==', uid));
  return snapshot.docs.map((doc) => doc.data());
}

/**
 * Lee el resumen dentro de una transacción; si no existe, lo reconstruye del
 * historial. Hace solo lecturas, así que se llama antes de cualquier tx.set.
 * @returns { resumen, nuevo } — `nuevo` indica que hay que guardarlo
 */
async function leerResumenEnTx(tx, db, uid, ahora) {
  const snapshot = await tx.get(refResumen(db, uid));
  if (snapshot.exists) return { resumen: snapshot.data(), nuevo: false };

  const colecciones = coleccionesDeHistorial();
  const [eventos, progresos, partidas] = await Promise.all([
    documentosDeUsuario(tx, db, colecciones.eventos, uid),
    documentosDeUsuario(tx, db, colecciones.progresos, uid),
    documentosDeUsuario(tx, db, colecciones.partidas, uid),
  ]);
  return { resumen: reconstruirResumen(uid, { eventos, progresos, partidas }, ahora), nuevo: true };
}

/**
 * Aplica una actividad terminada al resumen y lo escribe (RN-08, RN-09).
 * @returns los campos que se agregan a la respuesta de la actividad
 */
function aplicarEnTx(tx, db, resumen, actividad, ahora) {
  const resultado = aplicarActividad(resumen, actividad, ahora);
  tx.set(refResumen(db, resumen.uid), resultado.resumen);
  return { insignias_nuevas: resultado.insigniasNuevas, nivel_xp_nuevo: resultado.nivelNuevo };
}

/** Resumen para GET /api/progress; si no existe, lo reconstruye y lo guarda. */
function obtenerResumen(db, uid, ahora = Date.now()) {
  return db.runTransaction(async (tx) => {
    const { resumen, nuevo } = await leerResumenEnTx(tx, db, uid, ahora);
    if (nuevo) tx.set(refResumen(db, uid), resumen);
    return resumen;
  });
}

/**
 * Núcleo de POST /api/progress/chat: la primera respuesta de la IA en una
 * conversación cuenta como actividad (RN-08) y desbloquea "Habla con IA".
 * Idempotente por sesion_id; no da XP.
 */
async function registrarChat(db, { uid, sesionId, ahora = Date.now() }) {
  const dia = diaLocal(ahora);
  const refChat = db.collection(COLECCIONES.CHATS).doc(`${uid}_${sesionId}`);
  const refDiario = db.collection(COLECCIONES.CHATS_DIARIO).doc(`${uid}_${dia}`);

  return db.runTransaction(async (tx) => {
    const [chat, diario] = (await tx.getAll(refChat, refDiario)).map((s) => (s.exists ? s.data() : null));
    if (chat) return { ...JSON.parse(chat.respuesta_json), repetido: true };

    const conteo = diario?.conteo || 0;
    if (conteo >= MAX_CHATS_POR_DIA) {
      throw new ErrorApi(429, 'DEMASIADOS_CHATS', 'Ya registraste muchas conversaciones hoy.');
    }
    const { resumen } = await leerResumenEnTx(tx, db, uid, ahora);

    const respuesta = { sesion_id: sesionId, ...aplicarEnTx(tx, db, resumen, { tipo: 'chat', xp: 0 }, ahora) };
    tx.set(refDiario, { uid, dia, conteo: conteo + 1 });
    tx.set(db.collection(coleccionesDeHistorial().eventos).doc(), {
      uid,
      tipo: 'chat_completado',
      sesion_id: sesionId,
      xp: 0,
      fecha: ahora,
    });
    tx.set(refChat, { uid, sesion_id: sesionId, fecha: ahora, respuesta_json: JSON.stringify(respuesta) });

    return { ...respuesta, repetido: false };
  });
}

/**
 * Genera el resumen de cada usuario con actividad que todavía no lo tenga
 * (scripts/migrar-resumen.js). Idempotente: si el resumen ya existe, no lo toca.
 * @returns { revisados, creados }
 */
async function migrarResumenes(db, { ahora = Date.now() } = {}) {
  const { eventos } = coleccionesDeHistorial();
  const snapshot = await db.collection(eventos).get();
  const uids = [...new Set(snapshot.docs.map((doc) => doc.data().uid).filter(Boolean))].sort();

  let creados = 0;
  for (const uid of uids) {
    const creado = await db.runTransaction(async (tx) => {
      const { resumen, nuevo } = await leerResumenEnTx(tx, db, uid, ahora);
      if (nuevo) tx.set(refResumen(db, uid), resumen);
      return nuevo;
    });
    if (creado) creados += 1;
  }
  return { revisados: uids.length, creados };
}

module.exports = {
  COLECCIONES,
  MAX_CHATS_POR_DIA,
  leerResumenEnTx,
  aplicarEnTx,
  obtenerResumen,
  registrarChat,
  migrarResumenes,
};
