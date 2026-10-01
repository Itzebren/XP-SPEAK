/**
 * Prueba de integración contra los emuladores de Firebase (Firestore + Auth),
 * usando firebase-admin REAL: transacciones, getAll, consultas y verificación
 * de ID tokens. No toca el proyecto de producción. Uso:
 *
 *   npm run test:emulador
 *
 * (requiere Java 11+; firebase-tools se descarga con npx).
 */
const test = require('node:test');
const assert = require('node:assert/strict');
const crypto = require('crypto');
const { llamar } = require('../helpers/fakeFirebase');

const PROYECTO = 'demo-xpspeak';
const AUTH_HOST = process.env.FIREBASE_AUTH_EMULATOR_HOST;

if (!process.env.FIRESTORE_EMULATOR_HOST || !AUTH_HOST) {
  test('emulador', { skip: 'Corre con `npm run test:emulador` (necesita los emuladores de Firebase)' }, () => {});
  return;
}

const { getFirebaseAdmin } = require('../../lib/firebaseAdmin');
const catalogo = require('../../api/lessons/index');
const intento = require('../../api/lessons/[id]/attempt');
const avance = require('../../api/lessons/[id]/progress');
const repaso = require('../../api/srs/review');

const L1 = 'a1-saludos-presentaciones';
const L2 = 'a1-informacion-personal';
const APROBADAS_L1 = [
  { id: 'q1', valor: 1 }, { id: 'q2', valor: 'is' }, { id: 'q3', valor: 'am' },
  { id: 'q4', valor: 1 }, { id: 'q5', valor: 1 },
  { id: 'q6', valor: [['Hello!', '¡Hola!'], ['Goodbye!', '¡Adiós!'], ['How are you?', '¿Cómo estás?'], ['See you later!', '¡Nos vemos luego!']] },
];
const REPROBADAS_L1 = [{ id: 'q1', valor: 0 }, { id: 'q2', valor: 'are' }];

async function crearUsuarioYToken() {
  const respuesta = await fetch(
    `http://${AUTH_HOST}/identitytoolkit.googleapis.com/v1/accounts:signUp?key=clave-falsa`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: `${crypto.randomUUID()}@prueba.com`, password: 'secreta123', returnSecureToken: true }),
    }
  );
  const datos = await respuesta.json();
  return { token: datos.idToken, uid: datos.localId };
}

const enviar = (token, id, respuestas, attemptId = crypto.randomUUID()) =>
  llamar(intento, { method: 'POST', token, query: { id }, body: { attempt_id: attemptId, respuestas } });

test('flujo completo contra Firestore + Auth emulados', async () => {
  const { token, uid } = await crearUsuarioYToken();
  const db = getFirebaseAdmin().firestore();

  const estados = async () => {
    const res = await llamar(catalogo, { token, query: { level: 'A1' } });
    assert.equal(res.statusCode, 200, JSON.stringify(res.body));
    return Object.fromEntries(res.body.lecciones.map((l) => [l.id, l.estado]));
  };

  assert.equal((await estados())[L2], 'bloqueada');
  assert.equal((await enviar(token, L2, [{ id: 'q1', valor: 1 }])).statusCode, 403);

  const guardado = await llamar(avance, { method: 'POST', token, query: { id: L1 }, body: { seccion_actual: 3 } });
  assert.equal(guardado.body.estado, 'en_progreso');

  const reprobado = await enviar(token, L1, REPROBADAS_L1);
  assert.equal(reprobado.statusCode, 200, JSON.stringify(reprobado.body));
  assert.equal(reprobado.body.aprobada, false);

  const debiles = await llamar(repaso, { token });
  assert.ok(debiles.body.conceptos.some((c) => c.concepto_id === 'gram.verb_to_be_present'));

  // Cinco envíos simultáneos del mismo intento: una sola escritura real.
  const attemptId = crypto.randomUUID();
  const paralelos = await Promise.all(Array.from({ length: 5 }, () => enviar(token, L1, APROBADAS_L1, attemptId)));
  assert.ok(paralelos.every((r) => r.statusCode === 200 && r.body.xp_ganado === 50));
  assert.equal(paralelos.filter((r) => !r.body.repetido).length, 1);
  assert.equal(paralelos[0].body.desbloqueada_siguiente, L2);

  const eventos = await db.collection('eventos_actividad').where('uid', '==', uid).get();
  assert.equal(eventos.size, 1);

  const progreso = (await db.collection('progreso_lecciones').doc(`${uid}_${L1}`).get()).data();
  assert.equal(progreso.intentos, 2);
  assert.equal(progreso.xp_otorgado, true);
  assert.equal(progreso.avance.seccion_actual, 3);

  const final = await estados();
  assert.equal(final[L1], 'completada');
  assert.equal(final[L2], 'disponible');
  assert.ok(Object.entries(final).every(([id, estado]) => [L1, L2].includes(id) || estado === 'bloqueada'));
});

test('token alterado se rechaza con 401', async () => {
  const res = await llamar(catalogo, { token: 'no.es.un.token', query: { level: 'A1' } });
  assert.equal(res.statusCode, 401);
});
