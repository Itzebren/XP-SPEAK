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
const { llamar, enRuta } = require('../helpers/fakeFirebase');

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
const repaso = enRuta(require('../../api/srs/[accion]'), { accion: 'review' });
const progreso = require('../../api/progress/index');
const resultadoJuego = require('../../api/games/result');
const { migrarResumenes } = require('../../lib/progreso/repositorio');

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

  const progresoL1 = (await db.collection('progreso_lecciones').doc(`${uid}_${L1}`).get()).data();
  assert.equal(progresoL1.intentos, 2);
  assert.equal(progresoL1.xp_otorgado, true);
  assert.equal(progresoL1.avance.seccion_actual, 3);

  const final = await estados();
  assert.equal(final[L1], 'completada');
  assert.equal(final[L2], 'disponible');
  assert.ok(Object.entries(final).every(([id, estado]) => [L1, L2].includes(id) || estado === 'bloqueada'));
});

test('token alterado se rechaza con 401', async () => {
  const res = await llamar(catalogo, { token: 'no.es.un.token', query: { level: 'A1' } });
  assert.equal(res.statusCode, 401);
});

test('progreso: reconstrucción en la transacción, ETag, chat y migración contra Firestore real', async () => {
  const { token, uid } = await crearUsuarioYToken();
  const db = getFirebaseAdmin().firestore();
  const ayer = Date.now() - 24 * 60 * 60 * 1000;

  // Historial de antes del módulo, sin resumen.
  await db.collection('eventos_actividad').add({ uid, tipo: 'leccion_completada', leccion_id: L1, xp: 100, fecha: ayer });
  await db.collection('eventos_actividad').add({ uid, tipo: 'minijuego_completado', juego: 'eco-vocal', partida_id: 'x', puntaje: 0.5, xp: 36, fecha: ayer });

  // La partida reconstruye el resumen dentro de su transacción (tx.get de consultas) y no lo crea vacío.
  const partida = await llamar(resultadoJuego, {
    method: 'POST',
    token,
    body: { partida_id: crypto.randomUUID(), juego: 'orden-maestro', aciertos: 8, total: 8, duracion_ms: 60000, conceptos: [] },
  });
  assert.equal(partida.statusCode, 200, JSON.stringify(partida.body));
  assert.deepEqual(partida.body.insignias_nuevas.map((i) => i.id), ['estrella-minijuego']);
  assert.equal(partida.body.nivel_xp_nuevo, null);

  const vista = await llamar(progreso, { token, query: { level: 'A1' } });
  assert.equal(vista.statusCode, 200, JSON.stringify(vista.body));
  assert.equal(vista.body.xp.total, 136 + partida.body.xp_ganado);
  assert.equal(vista.body.racha.actual, 2);
  assert.equal((await llamar(progreso, { token, query: { level: 'A1' }, headers: { 'if-none-match': vista.headers.etag } })).statusCode, 304);

  // Chat: dos registros simultáneos de la misma conversación cuentan una vez.
  const chats = await Promise.all([1, 2].map(() => llamar(progreso, { method: 'POST', token, body: { sesion_id: 'conv-1' } })));
  assert.ok(chats.every((r) => r.statusCode === 200));
  assert.equal(chats.filter((r) => !r.body.repetido).length, 1);
  const resumen = (await db.collection('resumen_usuario').doc(uid).get()).data();
  assert.equal(resumen.conteos.chats, 1);
  assert.ok(resumen.insignias['habla-con-ia']);

  // Migración: crea el resumen de otro usuario con historial y no toca el que ya existe.
  const otro = crypto.randomUUID();
  await db.collection('eventos_actividad').add({ uid: otro, tipo: 'repaso_completado', xp: 0, fecha: ayer });
  const primera = await migrarResumenes(db);
  assert.ok(primera.creados >= 1);
  assert.equal((await db.collection('resumen_usuario').doc(otro).get()).data().conteos.repasos, 1);
  assert.equal((await migrarResumenes(db)).creados, 0);
  assert.deepEqual((await db.collection('resumen_usuario').doc(uid).get()).data(), resumen);
});
