const test = require('node:test');
const assert = require('node:assert/strict');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db, admin } = instalarFirebaseFalso({ 'token-ana': 'uid-ana', 'token-beto': 'uid-beto' });

const eliminarCuenta = require('../api/account/delete');
const { COLECCIONES_DEL_USUARIO } = require('../lib/cuenta/eliminar');

async function sembrar() {
  db._limpiar();
  for (const uid of ['uid-ana', 'uid-beto']) admin._usuarios.set(uid, { uid, email: `${uid}@prueba.com` });
  for (const coleccion of COLECCIONES_DEL_USUARIO) {
    for (const uid of ['uid-ana', 'uid-beto']) {
      await db.collection(coleccion).doc(`${uid}_1`).set({ uid, dato: 1 });
    }
  }
  await db.collection('password_reset_codes').doc('uid-ana@prueba.com').set({ codigo: 'ABC123' });
}

test('elimina los datos del usuario y su cuenta, sin tocar a los demás (RN-14)', async () => {
  await sembrar();
  const res = await llamar(eliminarCuenta, { method: 'POST', token: 'token-ana' });

  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body, { eliminada: true, documentos: COLECCIONES_DEL_USUARIO.length + 1 });
  for (const coleccion of COLECCIONES_DEL_USUARIO) {
    assert.deepEqual(db._docs(coleccion).map((d) => d.uid), ['uid-beto'], coleccion);
  }
  assert.equal(db._doc('password_reset_codes', 'uid-ana@prueba.com'), undefined);
  assert.equal(admin._usuarios.has('uid-ana'), false);
  assert.equal(admin._usuarios.has('uid-beto'), true);
});

test('cubre todas las colecciones con datos de usuario', () => {
  for (const nombre of ['progreso_lecciones', 'srs_conceptos', 'eventos_actividad', 'intentos_lecciones',
    'intentos_repaso', 'partidas_minijuego', 'minijuegos_diario', 'resumen_usuario', 'chats_registrados',
    'chats_diario', 'limite_speech']) {
    assert.ok(COLECCIONES_DEL_USUARIO.includes(nombre), nombre);
  }
});

test('reintentar después de borrar la cuenta no falla', async () => {
  await sembrar();
  admin._usuarios.delete('uid-beto');
  const res = await llamar(eliminarCuenta, { method: 'POST', token: 'token-beto' });
  assert.equal(res.statusCode, 200);
  assert.equal(db._docs('resumen_usuario').some((d) => d.uid === 'uid-beto'), false);
});

test('si Firebase Auth falla no reporta la cuenta como eliminada', async () => {
  await sembrar();
  admin._fallarAuth(Object.assign(new Error('sin credenciales'), { code: 'app/invalid-credential' }));
  try {
    const res = await llamar(eliminarCuenta, { method: 'POST', token: 'token-ana' });
    assert.equal(res.statusCode, 500);
    assert.equal(admin._usuarios.has('uid-ana'), true);
    // No se borra nada si no se pudo confirmar la cuenta.
    assert.ok(db._docs('resumen_usuario').some((d) => d.uid === 'uid-ana'));
  } finally {
    admin._fallarAuth(null);
  }
});

test('exige POST y sesión', async () => {
  assert.equal((await llamar(eliminarCuenta, { method: 'GET', token: 'token-ana' })).statusCode, 405);
  assert.equal((await llamar(eliminarCuenta, { method: 'POST' })).statusCode, 401);
});
