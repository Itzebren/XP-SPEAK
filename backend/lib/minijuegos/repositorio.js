const { ErrorApi } = require('../errores');
const { obtenerConcepto, obtenerLeccion } = require('../lecciones/contenido');
const { acumularSenal } = require('../lecciones/srs');
const { COLECCIONES: COLECCIONES_LECCIONES } = require('../lecciones/repositorio');
const { calcularXp } = require('./xp');

/**
 * Persistencia de los minijuegos en Firestore (docs/minijuegos-diseno.md §6).
 * Reutiliza las colecciones del SRS y de eventos de actividad de Lecciones:
 * así los errores en los juegos repriorizan los mismos conceptos (RF-13) y
 * una partida cuenta como actividad para la racha (RN-08).
 */
const COLECCIONES = {
  PARTIDAS: 'partidas_minijuego',
  DIARIO: 'minijuegos_diario',
};

// Tope duro por usuario y día, sumando todos los juegos (además de la XP a la mitad).
const MAX_PARTIDAS_POR_DIA = 60;
const ZONA_HORARIA = 'America/Mexico_City';

/** "2026-10-06": el día cuenta en hora de México, no en UTC. */
function diaLocal(ahora) {
  return new Intl.DateTimeFormat('en-CA', { timeZone: ZONA_HORARIA }).format(new Date(ahora));
}

/** La lección de la que sale el concepto: el SRS la usa para su nivel MCER. */
function leccionDeConcepto(conceptoId) {
  const concepto = obtenerConcepto(conceptoId);
  return concepto ? obtenerLeccion(concepto.lecciones[0]) : null;
}

/**
 * Núcleo de POST /api/games/result. Una sola transacción, idempotente por
 * partida_id: si el Worker reenvía la misma partida, se devuelve la
 * respuesta original y la XP no se duplica.
 */
async function registrarPartida(db, { uid, resultado, ahora = Date.now() }) {
  const { partidaId, juego, aciertos, total, duracionMs, conceptos } = resultado;
  const dia = diaLocal(ahora);
  const refPartida = db.collection(COLECCIONES.PARTIDAS).doc(`${uid}_${partidaId}`);
  const refDiario = db.collection(COLECCIONES.DIARIO).doc(`${uid}_${dia}`);
  const refsSrs = conceptos.map((c) => db.collection(COLECCIONES_LECCIONES.SRS).doc(`${uid}_${c.concepto_id}`));

  return db.runTransaction(async (tx) => {
    const [partida, diario, ...srs] = (await tx.getAll(refPartida, refDiario, ...refsSrs)).map((s) =>
      s.exists ? s.data() : null
    );

    if (partida) {
      if (partida.juego !== juego) {
        throw new ErrorApi(409, 'PARTIDA_ID_REUTILIZADO', 'Ese partida_id ya se usó en otro juego');
      }
      return { ...JSON.parse(partida.respuesta_json), repetido: true };
    }

    const conteo = diario?.conteo || {};
    const partidasHoy = Object.values(conteo).reduce((suma, n) => suma + n, 0);
    if (partidasHoy >= MAX_PARTIDAS_POR_DIA) {
      throw new ErrorApi(429, 'DEMASIADAS_PARTIDAS', 'Ya jugaste muchas partidas hoy. ¡Descansa y vuelve mañana!');
    }
    const previasDelJuego = conteo[juego] || 0;
    const { xp, reducida } = calcularXp(juego, { aciertos, total, duracionMs }, previasDelJuego);

    const respuesta = {
      partida_id: partidaId,
      juego,
      aciertos,
      total,
      precision: Math.round((aciertos / total) * 10000) / 10000,
      xp_ganado: xp,
      xp_reducida: reducida,
      partidas_hoy: previasDelJuego + 1,
    };

    conceptos.forEach((conteoConcepto, i) => {
      const leccion = leccionDeConcepto(conteoConcepto.concepto_id);
      tx.set(refsSrs[i], acumularSenal(srs[i], { uid, ...conteoConcepto }, leccion, ahora));
    });
    tx.set(refDiario, { uid, dia, conteo: { ...conteo, [juego]: previasDelJuego + 1 } });
    // "Actividad válida" para la racha (RN-08) y auditoría de la XP otorgada.
    tx.set(db.collection(COLECCIONES_LECCIONES.EVENTOS).doc(), {
      uid,
      tipo: 'minijuego_completado',
      juego,
      partida_id: partidaId,
      puntaje: respuesta.precision,
      xp,
      fecha: ahora,
    });
    tx.set(refPartida, {
      uid,
      juego,
      partida_id: partidaId,
      duracion_ms: duracionMs,
      fecha: ahora,
      respuesta_json: JSON.stringify(respuesta),
    });

    return { ...respuesta, repetido: false };
  });
}

module.exports = { COLECCIONES, MAX_PARTIDAS_POR_DIA, diaLocal, registrarPartida };
