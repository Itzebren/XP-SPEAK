const test = require('node:test');
const assert = require('node:assert/strict');
const { calificarIntento, normalizarTexto } = require('../lib/lecciones/calificador');
const { obtenerLeccion, obtenerEvaluacion } = require('../lib/lecciones/contenido');

const saludos = obtenerEvaluacion(obtenerLeccion('a1-saludos-presentaciones'));
const personal = obtenerEvaluacion(obtenerLeccion('a1-informacion-personal'));
const numeros = obtenerEvaluacion(obtenerLeccion('a1-numeros-hora'));

// Respuestas correctas de la lección 1 (6 ítems).
const correctasSaludos = () => [
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

test('todas correctas: puntaje 1 y aprobada', () => {
  const r = calificarIntento(saludos, correctasSaludos());
  assert.equal(r.puntaje, 1);
  assert.equal(r.aprobada, true);
  assert.equal(r.correctas, 6);
  assert.deepEqual(r.conceptos_debiles, []);
  assert.ok(r.feedback.every((f) => f.correcta && f.mensaje === undefined));
});

test('umbral 70%: 5/6 aprueba, 4/6 no (RN-06)', () => {
  const cinco = correctasSaludos();
  cinco[0].valor = 0;
  assert.equal(calificarIntento(saludos, cinco).aprobada, true);

  const cuatro = correctasSaludos();
  cuatro[0].valor = 0;
  cuatro[1].valor = 'are';
  const r = calificarIntento(saludos, cuatro);
  assert.equal(r.puntaje, 0.6667);
  assert.equal(r.aprobada, false);
});

test('umbral exacto: 7/10 cuenta como aprobada pese al punto flotante', () => {
  const items = Array.from({ length: 10 }, (_, i) => ({
    id: `q${i + 1}`,
    tipo: 'opcion_multiple',
    concepto_id: 'voc.x.y',
    enunciado: '?',
    opciones: ['a', 'b'],
    respuesta_correcta: 0,
    feedback_error: 'no',
  }));
  const respuestas = items.map((item, i) => ({ id: item.id, valor: i < 7 ? 0 : 1 }));
  const r = calificarIntento({ umbral_aprobacion: 0.7, items }, respuestas);
  assert.equal(r.puntaje, 0.7);
  assert.equal(r.aprobada, true);
});

test('respuesta incorrecta trae mensaje y respuesta correcta (CU-06 A1)', () => {
  const respuestas = correctasSaludos();
  respuestas[1].valor = 'are';
  const q2 = calificarIntento(saludos, respuestas).feedback.find((f) => f.id === 'q2');
  assert.equal(q2.correcta, false);
  assert.equal(q2.respuesta_correcta, 'is');
  assert.match(q2.mensaje, /is/);
});

test('ítems sin responder cuentan como incorrectos pero no generan señales SRS', () => {
  const r = calificarIntento(saludos, [{ id: 'q1', valor: 1 }]);
  assert.equal(r.correctas, 1);
  assert.equal(r.total, 6);
  assert.equal(r.aprobada, false);
  assert.equal(r.feedback.find((f) => f.id === 'q2').correcta, false);
  assert.deepEqual(r.conceptos, [{ concepto_id: 'voc.saludos.good_morning', aciertos: 1, fallos: 0 }]);
  assert.deepEqual(r.conceptos_debiles, []);
});

test('completar normaliza mayúsculas, espacios, puntuación y apóstrofos', () => {
  assert.equal(normalizarTexto('  IS. '), 'is');
  assert.equal(normalizarTexto('o’clock'), "o'clock");

  const respuestas = [{ id: 'q3', valor: 'O’CLOCK!' }];
  const q3 = calificarIntento(numeros, respuestas).feedback.find((f) => f.id === 'q3');
  assert.equal(q3.correcta, true);
});

test('opción múltiple no acepta el índice como texto', () => {
  const r = calificarIntento(saludos, [{ id: 'q1', valor: '1' }]);
  assert.equal(r.feedback[0].correcta, false);
});

test('emparejar: el ítem exige todos los pares, pero el SRS recibe señal por par', () => {
  const respuestas = correctasSaludos();
  respuestas[5].valor = [
    ['Hello!', '¡Hola!'],
    ['Goodbye!', '¿Cómo estás?'],
    ['How are you?', '¡Adiós!'],
    ['See you later!', '¡Nos vemos luego!'],
  ];
  const r = calificarIntento(saludos, respuestas);
  const q6 = r.feedback.find((f) => f.id === 'q6');
  assert.equal(q6.correcta, false);
  assert.equal(q6.respuesta_correcta.length, 4);

  const porConcepto = Object.fromEntries(r.conceptos.map((c) => [c.concepto_id, c]));
  assert.deepEqual(porConcepto['voc.saludos.hello'], { concepto_id: 'voc.saludos.hello', aciertos: 1, fallos: 0 });
  assert.equal(porConcepto['voc.saludos.goodbye'].fallos, 1);
  assert.equal(porConcepto['voc.saludos.how_are_you'].fallos, 1);
  assert.deepEqual(r.conceptos_debiles.sort(), ['voc.saludos.goodbye', 'voc.saludos.how_are_you']);
});

test('emparejar con pares de más no es correcto', () => {
  const respuestas = correctasSaludos();
  respuestas[5].valor.push(['Hi!', '¡Hola! (informal)']);
  const q6 = calificarIntento(saludos, respuestas).feedback.find((f) => f.id === 'q6');
  assert.equal(q6.correcta, false);
});

test('un concepto evaluado dos veces acumula ambas señales', () => {
  // En la lección 2, q2 y q3 evalúan voc.personal.years_old.
  const r = calificarIntento(personal, [
    { id: 'q2', valor: 'years' },
    { id: 'q3', valor: 0 },
  ]);
  const yearsOld = r.conceptos.find((c) => c.concepto_id === 'voc.personal.years_old');
  assert.deepEqual(yearsOld, { concepto_id: 'voc.personal.years_old', aciertos: 1, fallos: 1 });
});
