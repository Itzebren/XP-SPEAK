const test = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana' });

const catalogo = require('../api/lessons/index');
const intento = require('../api/lessons/[id]/attempt');
const { leerLeccionesDeDisco, obtenerEvaluacion } = require('../lib/lecciones/contenido');
const { calificarIntento } = require('../lib/lecciones/calificador');

/**
 * Pruebas sobre el contenido real (guía de autoría §2 y §3): que cada clave
 * de respuestas sea coherente con el calificador y que las cadenas de A1 y A2
 * se puedan recorrer completas por la API, de la primera a la última lección.
 */

const lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion);
const cadena = (nivel) =>
  lecciones.filter((l) => l.nivel_mcer === nivel).sort((a, b) => a.orden - b.orden);

function respuestasCorrectas(leccion) {
  return obtenerEvaluacion(leccion).items.map((item) => ({
    id: item.id,
    valor: item.tipo === 'emparejar' ? item.pares.map((p) => [p.izq, p.der]) : item.respuesta_correcta,
  }));
}

function respuestasIncorrectas(leccion) {
  return obtenerEvaluacion(leccion).items.map((item) => {
    if (item.tipo === 'opcion_multiple') return { id: item.id, valor: (item.respuesta_correcta + 1) % item.opciones.length };
    if (item.tipo === 'completar') return { id: item.id, valor: 'respuesta equivocada' };
    const [a, b, ...resto] = item.pares;
    return { id: item.id, valor: [[a.izq, b.der], [b.izq, a.der], ...resto.map((p) => [p.izq, p.der])] };
  });
}

test('plan curricular: 12 lecciones por nivel en una sola cadena (guía §2)', () => {
  for (const nivel of ['A1', 'A2']) {
    const ids = cadena(nivel);
    assert.equal(ids.length, 12, `${nivel} debe tener 12 lecciones`);
    ids.forEach((leccion, i) => {
      assert.equal(leccion.orden, i + 1);
      assert.equal(leccion.prerequisito_id, i === 0 ? null : ids[i - 1].id, `${leccion.id}: prerequisito`);
    });
  }
});

test('XP por nivel (guía §4): A1 = 710, A2 = 960', () => {
  const total = (nivel) => cadena(nivel).reduce((suma, l) => suma + l.xp_recompensa, 0);
  assert.equal(total('A1'), 710);
  assert.equal(total('A2'), 960);
});

for (const leccion of lecciones) {
  test(`${leccion.id}: la clave saca 100% y las respuestas equivocadas reprueban`, () => {
    const evaluacion = obtenerEvaluacion(leccion);

    const bien = calificarIntento(evaluacion, respuestasCorrectas(leccion));
    assert.equal(bien.puntaje, 1);
    assert.deepEqual(bien.conceptos_debiles, []);

    const mal = calificarIntento(evaluacion, respuestasIncorrectas(leccion));
    assert.equal(mal.correctas, 0);
    assert.equal(mal.aprobada, false);
    assert.ok(mal.feedback.every((f) => f.mensaje), 'cada error trae su feedback');
  });
}

for (const nivel of ['A1', 'A2']) {
  test(`${nivel}: se recorre la cadena completa por la API, desbloqueando una a una`, async () => {
    db._limpiar();
    const ids = cadena(nivel);
    let xp = 0;

    for (const [i, leccion] of ids.entries()) {
      const cat = await llamar(catalogo, { token: 'token-ana', query: { level: nivel } });
      const estados = Object.fromEntries(cat.body.lecciones.map((l) => [l.id, l.estado]));
      assert.equal(estados[leccion.id], 'disponible', `${leccion.id} debería estar disponible`);
      if (ids[i + 1]) assert.equal(estados[ids[i + 1].id], 'bloqueada');

      const res = await llamar(intento, {
        method: 'POST',
        token: 'token-ana',
        query: { id: leccion.id },
        body: { attempt_id: randomUUID(), respuestas: respuestasCorrectas(leccion) },
      });
      assert.equal(res.statusCode, 200);
      assert.equal(res.body.aprobada, true);
      assert.equal(res.body.desbloqueada_siguiente, ids[i + 1]?.id ?? null);
      xp += res.body.xp_ganado;
    }

    assert.equal(xp, ids.reduce((suma, l) => suma + l.xp_recompensa, 0));
    const final = await llamar(catalogo, { token: 'token-ana', query: { level: nivel } });
    assert.ok(final.body.lecciones.every((l) => l.estado === 'completada'));
  });
}
