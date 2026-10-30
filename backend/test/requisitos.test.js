const test = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar, enRuta } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana' });

const catalogo = require('../api/lessons/index');
const intento = require('../api/lessons/[id]/attempt');
const avance = require('../api/lessons/[id]/progress');
const sesion = enRuta(require('../api/srs/[accion]'), { accion: 'session' });
const resultadoJuego = require('../api/games/result');
const { registrarDia, diaLocal, fechaDeActividad, MAX_DIAS_ATRASO } = require('../lib/progreso/racha');
const { resumenVacio } = require('../lib/progreso/resumen');
const { listarLecciones, leccionPublica } = require('../lib/lecciones/contenido');
const { construirContenido } = require('../scripts/exportar-lecciones');

const DIA_MS = 24 * 60 * 60 * 1000;
const L1 = 'a1-saludos-presentaciones';
const L2 = 'a1-informacion-personal';
const PARES_L1 = [
  ['Hello!', '¡Hola!'],
  ['Goodbye!', '¡Adiós!'],
  ['How are you?', '¿Cómo estás?'],
  ['See you later!', '¡Nos vemos luego!'],
];
const APROBADAS_L1 = [
  { id: 'q1', valor: 1 },
  { id: 'q2', valor: 'is' },
  { id: 'q3', valor: 'am' },
  { id: 'q4', valor: 1 },
  { id: 'q5', valor: 1 },
  { id: 'q6', valor: PARES_L1 },
];
// 5/6 ≈ 83 %: aprueba, pero falla to be (q2) y lo deja como concepto débil.
const APROBADAS_CON_ERROR_L1 = APROBADAS_L1.map((r) => (r.id === 'q2' ? { id: 'q2', valor: 'are' } : r));

const enviarIntento = (id, respuestas, extra = {}) =>
  llamar(intento, { method: 'POST', token: 'token-ana', query: { id }, body: { attempt_id: randomUUID(), respuestas, ...extra } });
const verCatalogo = () => llamar(catalogo, { token: 'token-ana', query: { level: 'A1' } });
const abrir = (id) => llamar(avance, { method: 'POST', token: 'token-ana', query: { id }, body: { seccion_actual: 0 } });
const estadoDe = (cat, id) => cat.body.lecciones.find((l) => l.id === id).estado;

/** Adelanta el reloj del SRS: deja vencidos los conceptos del usuario. */
async function vencerConceptos(uid = 'uid-ana') {
  const conceptos = db._docs('srs_conceptos').filter((c) => c.uid === uid);
  await db.runTransaction(async (tx) => {
    for (const c of conceptos) {
      tx.set(db.collection('srs_conceptos').doc(`${uid}_${c.concepto_id}`), { ...c, proxima_revision: Date.now() - 1 });
    }
  });
}

test.beforeEach(() => db._limpiar());

// ── RN-07: el repaso va antes que los temas nuevos ─────────────────────────

test('RN-07: con conceptos débiles vencidos, el tema nuevo queda en repaso_pendiente y no se puede empezar', async () => {
  assert.equal((await enviarIntento(L1, APROBADAS_CON_ERROR_L1)).body.aprobada, true);

  // Hoy no toca repasar (SM-2 lo programó para mañana): L2 está disponible.
  let cat = await verCatalogo();
  assert.equal(estadoDe(cat, L2), 'disponible');
  assert.equal(cat.body.repaso_requerido, 0);

  await vencerConceptos();
  cat = await verCatalogo();
  assert.equal(estadoDe(cat, L2), 'repaso_pendiente');
  assert.equal(estadoDe(cat, L1), 'completada', 'lo ya aprobado se puede reabrir');
  assert.ok(cat.body.repaso_requerido >= 1);

  const bloqueado = await abrir(L2);
  assert.equal(bloqueado.statusCode, 409);
  assert.equal(bloqueado.body.codigo, 'REPASO_PENDIENTE');
  assert.equal((await abrir(L1)).statusCode, 200, 'reabrir una lección completada no es un tema nuevo');

  // Al hacer la sesión de repaso, SM-2 reprograma los conceptos y se levanta el bloqueo.
  const items = (await llamar(sesion, { token: 'token-ana' })).body.items;
  const respuestas = items.map((item) => ({ id: item.id, valor: item.respuesta_correcta }));
  assert.equal((await llamar(sesion, { method: 'POST', token: 'token-ana', body: { attempt_id: randomUUID(), respuestas } })).statusCode, 200);

  cat = await verCatalogo();
  assert.equal(estadoDe(cat, L2), 'disponible');
  assert.equal((await abrir(L2)).statusCode, 200);
});

test('RN-07: una lección ya empezada se puede terminar aunque haya repaso pendiente', async () => {
  await enviarIntento(L1, APROBADAS_CON_ERROR_L1);
  assert.equal((await abrir(L2)).statusCode, 200);
  await vencerConceptos();

  assert.equal(estadoDe(await verCatalogo(), L2), 'en_progreso');
  assert.equal((await abrir(L2)).statusCode, 200);
});

test('RN-07: los conceptos débiles de otro nivel no bloquean', async () => {
  await enviarIntento(L1, APROBADAS_CON_ERROR_L1);
  await vencerConceptos();
  const a2 = await llamar(catalogo, { token: 'token-ana', query: { level: 'A2' } });
  assert.equal(a2.body.repaso_requerido, 0);
  assert.ok(a2.body.lecciones.every((l) => l.estado !== 'repaso_pendiente'));
});

// ── RN-08: la actividad cuenta el día en que se hizo ───────────────────────

test('racha: un día atrasado que llena el hueco une las dos rachas', () => {
  let r = resumenVacio('u');
  for (const dia of ['2026-10-01', '2026-10-02', '2026-10-04', '2026-10-05']) r = { ...r, ...registrarDia(r, dia) };
  assert.equal(r.racha_actual, 2, 'faltó el 3');

  r = { ...r, ...registrarDia(r, '2026-10-03') };
  assert.equal(r.racha_actual, 5);
  assert.equal(r.racha_maxima, 5);
  assert.equal(r.ultimo_dia, '2026-10-05');

  // Un día viejo que no toca la racha actual solo se agrega al historial.
  const viejo = registrarDia(r, '2026-09-20');
  assert.equal(viejo.racha_actual, 5);
  assert.ok(viejo.dias_activos.includes('2026-09-20'));
});

test('fechaDeActividad: usa la fecha de la app, pero no en el futuro ni con más de una semana de atraso', () => {
  const ahora = Date.parse('2026-10-14T18:00:00Z');
  assert.equal(fechaDeActividad(null, ahora), ahora);
  assert.equal(fechaDeActividad(undefined, ahora), ahora);
  assert.equal(fechaDeActividad(ahora - DIA_MS, ahora), ahora - DIA_MS);
  assert.equal(fechaDeActividad(ahora + DIA_MS, ahora), ahora);
  assert.equal(fechaDeActividad(ahora - 30 * DIA_MS, ahora), ahora - MAX_DIAS_ATRASO * DIA_MS);
});

test('RN-08: una lección aprobada sin conexión ayer cuenta para ayer al sincronizar hoy', async () => {
  const ayer = Date.now() - DIA_MS;
  const res = await enviarIntento(L1, APROBADAS_L1, { completado_en: ayer });
  assert.equal(res.statusCode, 200);

  const [evento] = db._docs('eventos_actividad');
  assert.equal(evento.fecha, ayer);
  const resumen = db._doc('resumen_usuario', 'uid-ana');
  assert.deepEqual(resumen.dias_activos, [diaLocal(ayer)]);

  // Y hoy otra lección: la racha queda en 2 días, no en 1.
  assert.equal((await enviarIntento(L2, respuestasCorrectas(L2))).body.aprobada, true);
  assert.equal(db._doc('resumen_usuario', 'uid-ana').racha_actual, 2);
});

test('RN-08: una partida jugada sin conexión cuenta para el día en que se jugó', async () => {
  const ayer = Date.now() - DIA_MS;
  const res = await llamar(resultadoJuego, {
    method: 'POST',
    token: 'token-ana',
    body: {
      partida_id: randomUUID(),
      juego: 'orden-maestro',
      aciertos: 8,
      total: 8,
      duracion_ms: 100000,
      conceptos: [],
      completado_en: ayer,
    },
  });
  assert.equal(res.statusCode, 200);
  assert.equal(db._docs('eventos_actividad')[0].fecha, ayer);
  assert.deepEqual(db._doc('resumen_usuario', 'uid-ana').dias_activos, [diaLocal(ayer)]);
  assert.ok(db._doc('minijuegos_diario', `uid-ana_${diaLocal(ayer)}`), 'el tope diario es el del día jugado');
});

test('completado_en inválido responde 400', async () => {
  assert.equal((await enviarIntento(L1, APROBADAS_L1, { completado_en: 'ayer' })).statusCode, 400);
});

// ── QA 29-oct: el resultado nombra los conceptos débiles y la lección desbloqueada ──

test('el intento trae el nombre de los conceptos débiles y el título de la lección desbloqueada', async () => {
  const malas = respuestasCorrectas(L1).map((r, i) => (i === 0 ? { ...r, valor: (r.valor + 1) % 3 } : r));
  const res = await enviarIntento(L1, malas);
  assert.equal(res.statusCode, 200);
  assert.ok(res.body.conceptos_debiles.length > 0);
  assert.deepEqual(res.body.conceptos_debiles_detalle.map((c) => c.concepto_id), res.body.conceptos_debiles);
  assert.ok(res.body.conceptos_debiles_detalle.every((c) => c.titulo || c.en), 'cada concepto trae un nombre');
  if (res.body.desbloqueada_siguiente) assert.equal(typeof res.body.desbloqueada_titulo, 'string');
});

// ── RNF-08 / RN-11: lecciones dentro del APK ───────────────────────────────

test('lecciones del APK: las 24 con su manifiesto y sin la autoría', () => {
  const paquete = construirContenido();
  assert.equal(paquete.lecciones.length, 24);
  assert.equal(paquete.niveles.A1.length + paquete.niveles.A2.length, 24);
  assert.ok(!JSON.stringify(paquete).includes('"autoria"'));
  assert.deepEqual(paquete.lecciones[0], leccionPublica(listarLecciones().find((l) => l.id === paquete.lecciones[0].id)));
});

/** Respuestas que aprueban la evaluación de una lección, sacadas de su clave. */
function respuestasCorrectas(id) {
  const leccion = listarLecciones().find((l) => l.id === id);
  const evaluacion = leccion.secciones.find((s) => s.tipo === 'evaluacion');
  return evaluacion.items.map((item) => {
    if (item.tipo === 'emparejar') return { id: item.id, valor: item.pares.map((p) => [p.izq, p.der]) };
    return { id: item.id, valor: item.respuesta_correcta };
  });
}
