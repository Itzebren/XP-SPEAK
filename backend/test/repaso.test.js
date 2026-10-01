const test = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana', 'token-beto': 'uid-beto' });

const intento = require('../api/lessons/[id]/attempt');
const sesion = require('../api/srs/session');
const { aplicarSm2, DIA_MS } = require('../lib/lecciones/sm2');
const { seleccionarParaRepaso } = require('../lib/lecciones/srs');
const { construirIndice, resolverEjercicio } = require('../lib/lecciones/repaso');
const { listarLecciones } = require('../lib/lecciones/contenido');

const L1 = 'a1-saludos-presentaciones';
// 3/6: falla to be (q2, q3) y el pronombre (q4); acierta el resto.
const RESPUESTAS_L1 = [
  { id: 'q1', valor: 1 },
  { id: 'q2', valor: 'are' },
  { id: 'q3', valor: 'is' },
  { id: 'q4', valor: 0 },
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

const hacerLeccion = () =>
  llamar(intento, { method: 'POST', token: 'token-ana', query: { id: L1 }, body: { attempt_id: randomUUID(), respuestas: RESPUESTAS_L1 } });
const verSesion = (token = 'token-ana', query = {}) => llamar(sesion, { token, query });
const enviarSesion = (respuestas, { attemptId = randomUUID(), token = 'token-ana' } = {}) =>
  llamar(sesion, { method: 'POST', token, body: { attempt_id: attemptId, respuestas } });

/** Adelanta el reloj: deja vencidos todos los conceptos del usuario. */
async function vencerConceptos(uid = 'uid-ana') {
  const conceptos = db._docs('srs_conceptos').filter((c) => c.uid === uid);
  await db.runTransaction(async (tx) => {
    for (const c of conceptos) {
      tx.set(db.collection('srs_conceptos').doc(`${uid}_${c.concepto_id}`), { ...c, proxima_revision: Date.now() - 1 });
    }
  });
}

/** La respuesta correcta de un ejercicio de repaso, según su clave. */
function respuestaCorrecta(item) {
  return { id: item.id, valor: item.respuesta_correcta };
}

test.beforeEach(() => db._limpiar());

test('SM-2: al acertar el intervalo crece 1 → 6 → ×facilidad; al fallar vuelve a 1', () => {
  const ahora = 1_000_000;
  const primero = aplicarSm2(null, 4, ahora);
  assert.deepEqual(primero, { intervalo: 1, ease: 2.5, repeticiones: 1, proxima_revision: ahora + DIA_MS });

  const segundo = aplicarSm2(primero, 4, ahora);
  assert.equal(segundo.intervalo, 6);
  const tercero = aplicarSm2(segundo, 4, ahora);
  assert.equal(tercero.intervalo, 15, '6 × 2.5');

  const fallo = aplicarSm2(tercero, 1, ahora);
  assert.equal(fallo.intervalo, 1);
  assert.equal(fallo.repeticiones, 0);
  assert.equal(fallo.ease, 1.96);
});

test('SM-2: la facilidad nunca baja de 1.3', () => {
  let estado = null;
  for (let i = 0; i < 10; i++) estado = aplicarSm2(estado, 1, 0);
  assert.equal(estado.ease, 1.3);
});

test('selección: solo vencidos, primero los de mayor error (RN-07), y avisa la próxima fecha', () => {
  const ahora = 10 * DIA_MS;
  const conceptos = [
    { concepto_id: 'a', nivel_mcer: 'A1', tasa_error: 0.2, proxima_revision: ahora - 1 },
    { concepto_id: 'b', nivel_mcer: 'A1', tasa_error: 0.9, proxima_revision: ahora - 1 },
    { concepto_id: 'c', nivel_mcer: 'A1', tasa_error: 1, proxima_revision: ahora + DIA_MS },
    { concepto_id: 'd', nivel_mcer: 'A2', tasa_error: 1, proxima_revision: ahora - 1 },
    { concepto_id: 'e', nivel_mcer: 'A1', tasa_error: 0, proxima_revision: null },
  ];
  const r = seleccionarParaRepaso(conceptos, { nivel: 'A1', ahora });
  assert.deepEqual(r.vencidos.map((c) => c.concepto_id), ['b', 'a', 'e']);
  assert.equal(r.total_vencidos, 3);
  assert.equal(r.proxima_revision, ahora + DIA_MS);
  assert.equal(seleccionarParaRepaso(conceptos, { ahora, limite: 1 }).vencidos.length, 1);
});

test('contenido: todo concepto evaluado tiene un ejercicio de repaso', () => {
  const { porConcepto, porId } = construirIndice(listarLecciones());
  for (const leccion of listarLecciones()) {
    const evaluacion = leccion.secciones.find((s) => s.tipo === 'evaluacion');
    for (const item of evaluacion.items) {
      const conceptos = item.tipo === 'emparejar' ? item.pares.map((p) => p.concepto_id) : [item.concepto_id];
      for (const id of conceptos) assert.ok(porConcepto.has(id), `${leccion.id}/${item.id}: ${id} sin ejercicio`);
    }
  }
  // Los pares de "emparejar" se vuelven opción múltiple con la respuesta en su lugar.
  const par = porId.get(`${L1}:q6:1`).ejercicio;
  assert.equal(par.tipo, 'opcion_multiple');
  assert.equal(par.concepto_id, 'voc.saludos.goodbye');
  assert.equal(par.opciones[par.respuesta_correcta], '¡Adiós!');
  assert.equal(resolverEjercicio('no-existe:q1'), null);
});

test('sesión: hoy no toca nada tras la lección; al vencer, primero lo más fallado', async () => {
  await hacerLeccion();

  const hoy = await verSesion();
  assert.equal(hoy.statusCode, 200);
  assert.deepEqual(hoy.body.items, []);
  assert.ok(hoy.body.proxima_revision > Date.now(), 'dice cuándo vuelve a haber repaso');

  await vencerConceptos();
  const manana = await verSesion(undefined, { level: 'A1' });
  const conceptos = manana.body.items.map((i) => i.concepto_id);
  assert.equal(manana.body.total_vencidos, conceptos.length);
  assert.deepEqual(conceptos.slice(0, 2).sort(), ['gram.subject_pronouns', 'gram.verb_to_be_present']);
  assert.ok(manana.body.items.every((i) => i.concepto.concepto_id === i.concepto_id && i.enunciado));

  assert.deepEqual((await verSesion('token-beto')).body.items, [], 'cada usuario ve solo lo suyo');
  assert.deepEqual((await verSesion(undefined, { level: 'A2' })).body.items, []);
});

test('responder la sesión reprograma con SM-2, es idempotente y no da XP', async () => {
  await hacerLeccion();
  await vencerConceptos();
  const { items } = (await verSesion()).body;
  const toBe = items.find((i) => i.concepto_id === 'gram.verb_to_be_present');
  const pronombres = items.find((i) => i.concepto_id === 'gram.subject_pronouns');

  const attemptId = randomUUID();
  const respuestas = [respuestaCorrecta(toBe), { id: pronombres.id, valor: 3 }];
  const r = await enviarSesion(respuestas, { attemptId });
  assert.equal(r.statusCode, 200);
  assert.equal(r.body.correctas, 1);
  assert.equal(r.body.total, 2);
  assert.equal(r.body.repetido, false);

  const porConcepto = Object.fromEntries(r.body.conceptos.map((c) => [c.concepto_id, c]));
  assert.equal(porConcepto['gram.verb_to_be_present'].acierto, true);
  assert.equal(porConcepto['gram.subject_pronouns'].acierto, false);
  assert.equal(porConcepto['gram.subject_pronouns'].intervalo, 1);

  const guardado = db._doc('srs_conceptos', 'uid-ana_gram.verb_to_be_present');
  assert.equal(guardado.repeticiones, 1, 'el acierto cuenta como primera repetición tras el fallo');
  assert.equal(guardado.aciertos, 1);

  // Reenvío del mismo intento: misma respuesta, sin mover nada.
  const otra = await enviarSesion(respuestas, { attemptId });
  assert.equal(otra.body.repetido, true);
  assert.deepEqual(db._doc('srs_conceptos', 'uid-ana_gram.verb_to_be_present'), guardado);

  const eventos = db._docs('eventos_actividad').filter((e) => e.tipo === 'repaso_completado');
  assert.equal(eventos.length, 1);
  assert.equal(eventos[0].xp, 0);

  // Ya repasados: salen de la sesión de hoy.
  const despues = (await verSesion()).body.items.map((i) => i.concepto_id);
  assert.ok(!despues.includes('gram.verb_to_be_present'));
  assert.ok(!despues.includes('gram.subject_pronouns'));
});

test('responder la sesión valida el body', async () => {
  const valido = { id: `${L1}:q1`, valor: 1 };
  assert.equal((await enviarSesion([{ id: 'inventado:q1', valor: 1 }])).statusCode, 400);
  assert.equal((await enviarSesion([])).statusCode, 400);
  assert.equal((await enviarSesion([valido, valido])).statusCode, 400);
  assert.equal((await llamar(sesion, { method: 'POST', token: 'token-ana', body: { respuestas: [valido] } })).statusCode, 400);
  assert.equal((await llamar(sesion, { method: 'POST', body: { attempt_id: randomUUID(), respuestas: [valido] } })).statusCode, 401);
  assert.equal((await llamar(sesion, { method: 'PUT', token: 'token-ana' })).statusCode, 405);
  assert.equal((await enviarSesion([valido])).statusCode, 200);
});

test('repetir la lección el mismo día no alarga los intervalos; fallar sí los reinicia', async () => {
  await hacerLeccion();
  const hello = () => db._doc('srs_conceptos', 'uid-ana_voc.saludos.hello');
  const antes = hello();
  assert.equal(antes.intervalo, 1);

  await hacerLeccion(); // vuelve a acertar "Hello!" antes de que toque
  assert.equal(hello().intervalo, 1);
  assert.equal(hello().repeticiones, antes.repeticiones);
  assert.equal(hello().aciertos, 2, 'el acierto sí cuenta para la tasa de error');
});
