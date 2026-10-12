const test = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana', 'token-beto': 'uid-beto' });

const catalogo = require('../api/lessons/index');
const manifiesto = require('../api/lessons/manifest');
const leccion = require('../api/lessons/[id]');
const intento = require('../api/lessons/[id]/attempt');
const avance = require('../api/lessons/[id]/progress');
const repaso = require('../api/srs/review');
const { MAX_INTENTOS_POR_VENTANA } = require('../lib/lecciones/limite');

const L1 = 'a1-saludos-presentaciones';
const L2 = 'a1-informacion-personal';
const L3 = 'a1-numeros-hora';

const RESPUESTAS_L1 = [
  { id: 'q1', valor: 1 },
  { id: 'q2', valor: 'is' },
  { id: 'q3', valor: 'am' },
  { id: 'q4', valor: 1 },
  { id: 'q5', valor: 1 },
  {
    id: 'q6',
    valor: [
      ['Hello!', '¡Hola!'],
      ['Goodbye!', '¡Adiós!'],
      ['How are you?', '¿Cómo estás?'],
      ['See you later!', '¡Nos vemos luego!'],
    ],
  },
];
// 3/6 = 50%: falla to be (q2, q3) y el pronombre (q4).
const RESPUESTAS_L1_REPROBADAS = RESPUESTAS_L1.map((r) =>
  ({ q2: { id: 'q2', valor: 'are' }, q3: { id: 'q3', valor: 'is' }, q4: { id: 'q4', valor: 0 } })[r.id] || r
);

const enviar = (id, respuestas, { token = 'token-ana', attemptId = randomUUID() } = {}) =>
  llamar(intento, { method: 'POST', token, query: { id }, body: { attempt_id: attemptId, respuestas } });

const verCatalogo = (token = 'token-ana', level = 'A1') => llamar(catalogo, { token, query: { level } });
const estados = (res) => Object.fromEntries(res.body.lecciones.map((l) => [l.id, l.estado]));

test.beforeEach(() => db._limpiar());

test('autenticación: sin token o con token inválido responde 401', async () => {
  const sinToken = await llamar(catalogo, { query: { level: 'A1' } });
  assert.equal(sinToken.statusCode, 401);
  assert.equal(sinToken.body.codigo, 'TOKEN_AUSENTE');

  const invalido = await llamar(leccion, { token: 'falso', query: { id: L1 } });
  assert.equal(invalido.statusCode, 401);
  assert.equal(invalido.body.codigo, 'TOKEN_INVALIDO');
});

test('método incorrecto responde 405 con la forma estándar de error', async () => {
  const res = await llamar(intento, { method: 'GET', token: 'token-ana', query: { id: L1 } });
  assert.equal(res.statusCode, 405);
  assert.deepEqual(Object.keys(res.body).sort(), ['codigo', 'error']);
});

test('manifiesto: público, con ETag y 304 si no cambió', async () => {
  const res = await llamar(manifiesto);
  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body.niveles.A1.slice(0, 3).map((l) => l.id), [L1, L2, L3]);
  assert.equal(res.body.niveles.A1.length, 12);
  assert.equal(res.body.niveles.A2.length, 12);

  const revalidado = await llamar(manifiesto, { headers: { 'if-none-match': res.headers.etag } });
  assert.equal(revalidado.statusCode, 304);
});

test('contenido de lección: sin campos de autoría, con ETag, 404 si no existe', async () => {
  const res = await llamar(leccion, { token: 'token-ana', query: { id: L1 } });
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.id, L1);
  assert.equal(res.body.autoria, undefined);
  assert.equal(res.headers.etag, `"${L1}@1"`);

  const cacheado = await llamar(leccion, { token: 'token-ana', query: { id: L1 }, headers: { 'if-none-match': `"${L1}@1"` } });
  assert.equal(cacheado.statusCode, 304);

  const noExiste = await llamar(leccion, { token: 'token-ana', query: { id: 'a1-nada' } });
  assert.equal(noExiste.statusCode, 404);
  assert.equal(noExiste.body.codigo, 'LECCION_NO_ENCONTRADA');
});

test('catálogo: valida el nivel y arranca con solo la primera lección disponible', async () => {
  assert.equal((await verCatalogo('token-ana', 'B2')).statusCode, 400);
  assert.equal((await llamar(catalogo, { token: 'token-ana', query: {} })).statusCode, 400);

  const res = await verCatalogo();
  assert.equal(res.statusCode, 200);
  const a1 = estados(res);
  assert.equal(a1[L1], 'disponible');
  assert.ok(Object.entries(a1).every(([id, estado]) => id === L1 || estado === 'bloqueada'));
  assert.ok(res.body.version_contenido);

  // RN-02: cada nivel es una cadena propia; A2 arranca en su primera lección.
  const a2 = await verCatalogo('token-ana', 'A2');
  assert.equal(a2.body.lecciones.length, 12);
  assert.deepEqual(
    a2.body.lecciones.filter((l) => l.estado === 'disponible').map((l) => l.id),
    ['a2-fin-de-semana']
  );
});

test('RN-06: no se puede presentar una lección bloqueada', async () => {
  const res = await enviar(L2, [{ id: 'q1', valor: 1 }]);
  assert.equal(res.statusCode, 403);
  assert.equal(res.body.codigo, 'LECCION_BLOQUEADA');
  assert.deepEqual(db._docs('progreso_lecciones'), []);
});

test('flujo completo: reprobada → conceptos débiles → aprobada → desbloqueo → XP una sola vez', async () => {
  // 1. Intento reprobado (50%): sin XP, sugiere repetir, registra SRS.
  const reprobado = await enviar(L1, RESPUESTAS_L1_REPROBADAS);
  assert.equal(reprobado.statusCode, 200);
  assert.equal(reprobado.body.puntaje, 0.5);
  assert.equal(reprobado.body.aprobada, false);
  assert.equal(reprobado.body.estado, 'reprobada');
  assert.equal(reprobado.body.xp_ganado, 0);
  assert.equal(reprobado.body.sugerencia, 'repetir_leccion');
  assert.equal(reprobado.body.desbloqueada_siguiente, null);
  assert.deepEqual(reprobado.body.conceptos_debiles.sort(), ['gram.subject_pronouns', 'gram.verb_to_be_present']);
  assert.deepEqual(db._docs('eventos_actividad'), [], 'un intento reprobado no es actividad válida (RN-08)');

  const toBe = db._doc('srs_conceptos', 'uid-ana_gram.verb_to_be_present');
  assert.equal(toBe.fallos, 2);
  assert.equal(toBe.tasa_error, 1);
  assert.equal(toBe.tipo, 'gramatica');
  assert.equal(toBe.nivel_mcer, 'A1');
  // SM-2: fallado → se repasa mañana y baja su facilidad.
  assert.equal(toBe.intervalo, 1);
  assert.equal(toBe.repeticiones, 0);
  assert.equal(toBe.ease, 1.96);
  assert.ok(toBe.proxima_revision > Date.now());

  let cat = await verCatalogo();
  assert.equal(estados(cat)[L1], 'reprobada');
  assert.equal(estados(cat)[L2], 'bloqueada');

  // 2. Conceptos débiles con su texto, ordenados por tasa de error.
  const debiles = await llamar(repaso, { token: 'token-ana' });
  assert.equal(debiles.statusCode, 200);
  const ids = debiles.body.conceptos.map((c) => c.concepto_id);
  assert.deepEqual(ids.sort(), ['gram.subject_pronouns', 'gram.verb_to_be_present']);
  assert.ok(debiles.body.conceptos.every((c) => c.titulo && c.lecciones.includes(L1)));

  // 3. Intento aprobado: XP, desbloqueo de la siguiente, evento de racha.
  const aprobado = await enviar(L1, RESPUESTAS_L1);
  assert.equal(aprobado.body.aprobada, true);
  assert.equal(aprobado.body.estado, 'completada');
  assert.equal(aprobado.body.xp_ganado, 50);
  assert.equal(aprobado.body.desbloqueada_siguiente, L2);
  assert.equal(aprobado.body.mejor_puntaje, 1);
  assert.equal(aprobado.body.sugerencia, null);

  const eventos = db._docs('eventos_actividad');
  assert.equal(eventos.length, 1);
  assert.equal(eventos[0].tipo, 'leccion_completada');
  assert.equal(eventos[0].xp, 50);

  // La tasa de error de to be baja: 2 fallos + 2 aciertos.
  assert.equal(db._doc('srs_conceptos', 'uid-ana_gram.verb_to_be_present').tasa_error, 0.5);

  cat = await verCatalogo();
  assert.equal(estados(cat)[L1], 'completada');
  assert.equal(estados(cat)[L2], 'disponible');
  assert.equal(estados(cat)[L3], 'bloqueada');
  const l1 = cat.body.lecciones.find((l) => l.id === L1);
  assert.equal(l1.intentos, 2);
  assert.equal(l1.mejor_puntaje, 1);

  // 4. Repetir una lección completada: cuenta como actividad pero no da XP (RN-09).
  const repetido = await enviar(L1, RESPUESTAS_L1_REPROBADAS);
  assert.equal(repetido.body.estado, 'completada', 'reprobar un repaso no la vuelve a bloquear');
  assert.equal(repetido.body.xp_ganado, 0);
  assert.equal(repetido.body.mejor_puntaje, 1);

  const otraVez = await enviar(L1, RESPUESTAS_L1);
  assert.equal(otraVez.body.xp_ganado, 0);
  assert.equal(otraVez.body.desbloqueada_siguiente, null);
  assert.equal(db._docs('eventos_actividad').length, 2);
  assert.equal(db._docs('eventos_actividad').reduce((suma, e) => suma + e.xp, 0), 50);
});

test('idempotencia: reenviar el mismo attempt_id devuelve el resultado original sin duplicar XP', async () => {
  const attemptId = randomUUID();
  const primero = await enviar(L1, RESPUESTAS_L1, { attemptId });
  const segundo = await enviar(L1, RESPUESTAS_L1, { attemptId });

  assert.equal(primero.body.repetido, false);
  assert.equal(segundo.body.repetido, true);
  assert.equal(segundo.body.xp_ganado, 50, 'devuelve la respuesta original');
  assert.equal(db._docs('eventos_actividad').length, 1);
  assert.equal(db._doc('progreso_lecciones', `uid-ana_${L1}`).intentos, 1);
  assert.equal(db._doc('srs_conceptos', 'uid-ana_voc.saludos.hello').aciertos, 1);
});

test('idempotencia con envíos simultáneos (reintento de red en paralelo)', async () => {
  const attemptId = randomUUID();
  const respuestas = await Promise.all(
    Array.from({ length: 5 }, () => enviar(L1, RESPUESTAS_L1, { attemptId }))
  );
  assert.ok(respuestas.every((r) => r.statusCode === 200 && r.body.xp_ganado === 50));
  assert.equal(respuestas.filter((r) => !r.body.repetido).length, 1);
  assert.equal(db._docs('eventos_actividad').length, 1);
});

test('dos intentos distintos aprobados en paralelo solo otorgan XP una vez', async () => {
  const respuestas = await Promise.all([enviar(L1, RESPUESTAS_L1), enviar(L1, RESPUESTAS_L1)]);
  assert.deepEqual(respuestas.map((r) => r.body.xp_ganado).sort(), [0, 50]);
});

test('attempt_id reutilizado en otra lección responde 409', async () => {
  const attemptId = randomUUID();
  await enviar(L1, RESPUESTAS_L1, { attemptId });
  const res = await enviar(L2, [{ id: 'q1', valor: 1 }], { attemptId });
  assert.equal(res.statusCode, 409);
  assert.equal(res.body.codigo, 'ATTEMPT_ID_REUTILIZADO');
});

test('el progreso es por usuario: aprobar con Ana no desbloquea nada a Beto', async () => {
  await enviar(L1, RESPUESTAS_L1);
  const beto = await verCatalogo('token-beto');
  assert.equal(estados(beto)[L2], 'bloqueada');
  assert.equal((await enviar(L2, [{ id: 'q1', valor: 1 }], { token: 'token-beto' })).statusCode, 403);
});

test('rate limiting: más de N intentos por minuto en la misma lección responde 429', async () => {
  for (let i = 0; i < MAX_INTENTOS_POR_VENTANA; i++) {
    assert.equal((await enviar(L1, RESPUESTAS_L1_REPROBADAS)).statusCode, 200);
  }
  const bloqueado = await enviar(L1, RESPUESTAS_L1);
  assert.equal(bloqueado.statusCode, 429);
  assert.equal(bloqueado.body.codigo, 'DEMASIADOS_INTENTOS');
});

test('validación del body del intento', async () => {
  const casos = [
    [{ respuestas: [{ id: 'q1', valor: 1 }] }, 'attempt_id'],
    [{ attempt_id: 'no-es-uuid', respuestas: [{ id: 'q1', valor: 1 }] }, 'attempt_id'],
    [{ attempt_id: randomUUID(), respuestas: [] }, 'respuestas'],
    [{ attempt_id: randomUUID(), respuestas: [{ id: 'q99', valor: 1 }] }, 'no existe'],
    [{ attempt_id: randomUUID(), respuestas: [{ id: 'q1', valor: 1 }, { id: 'q1', valor: 0 }] }, 'repetido'],
    [{ attempt_id: randomUUID(), respuestas: [{ id: 'q1', valor: { hack: true } }] }, 'formato'],
    [{ attempt_id: randomUUID(), respuestas: [{ id: 'q2', valor: 'x'.repeat(500) }] }, 'formato'],
  ];
  for (const [body, fragmento] of casos) {
    const res = await llamar(intento, { method: 'POST', token: 'token-ana', query: { id: L1 }, body });
    assert.equal(res.statusCode, 400, JSON.stringify(body).slice(0, 80));
    assert.equal(res.body.codigo, 'ENTRADA_INVALIDA');
    assert.match(res.body.error, new RegExp(fragmento));
  }
  assert.deepEqual(db._docs('progreso_lecciones'), [], 'nada inválido llega a Firestore');
});

test('avance parcial (RN-12): marca en_progreso y se refleja en el catálogo', async () => {
  const guardar = (id, body, token = 'token-ana') => llamar(avance, { method: 'POST', token, query: { id }, body });

  assert.equal((await guardar(L1, { seccion_actual: 99 })).statusCode, 400);
  assert.equal((await guardar(L2, { seccion_actual: 1 })).statusCode, 403);

  const res = await guardar(L1, { seccion_actual: 2 });
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.estado, 'en_progreso');

  const cat = await verCatalogo();
  const l1 = cat.body.lecciones.find((l) => l.id === L1);
  assert.equal(l1.estado, 'en_progreso');
  assert.equal(l1.avance.seccion_actual, 2);

  // Una lección completada sigue completada al volver a abrir la teoría.
  await enviar(L1, RESPUESTAS_L1);
  assert.equal((await guardar(L1, { seccion_actual: 0 })).body.estado, 'completada');
  assert.equal(db._doc('progreso_lecciones', `uid-ana_${L1}`).xp_otorgado, true);
});

test('repaso SRS: filtra por nivel, respeta el límite y valida parámetros', async () => {
  await enviar(L1, RESPUESTAS_L1_REPROBADAS);

  assert.equal((await llamar(repaso, { token: 'token-ana', query: { limit: '0' } })).statusCode, 400);
  assert.equal((await llamar(repaso, { token: 'token-ana', query: { level: 'C1' } })).statusCode, 400);

  const uno = await llamar(repaso, { token: 'token-ana', query: { limit: '1' } });
  assert.equal(uno.body.conceptos.length, 1);

  const a2 = await llamar(repaso, { token: 'token-ana', query: { level: 'A2' } });
  assert.deepEqual(a2.body.conceptos, []);

  const beto = await llamar(repaso, { token: 'token-beto' });
  assert.deepEqual(beto.body.conceptos, [], 'cada usuario ve solo sus conceptos');
});
