const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana', 'token-beto': 'uid-beto' });

const progreso = require('../api/progress/index');
const resultadoJuego = require('../api/games/result');
const intento = require('../api/lessons/[id]/attempt');
const sesionRepaso = require('../api/srs/session');
const { nivelPorXp, subioDeNivel, xpParaNivel } = require('../lib/progreso/niveles');
const { diaLocal, registrarDia, rachaVigente, diasEnSemana, lunesDe } = require('../lib/progreso/racha');
const { CATALOGO, evaluar } = require('../lib/progreso/insignias');
const { resumenVacio, aplicarActividad, reconstruirResumen } = require('../lib/progreso/resumen');
const { COLECCIONES, MAX_CHATS_POR_DIA, migrarResumenes } = require('../lib/progreso/repositorio');
const { COLECCIONES: COLECCIONES_LECCIONES } = require('../lib/lecciones/repositorio');
const { COLECCIONES: COLECCIONES_MINIJUEGOS } = require('../lib/minijuegos/repositorio');
const { listarLeccionesDeNivel } = require('../lib/lecciones/contenido');
const { RUTA_INSIGNIAS, serializarInsignias } = require('../scripts/exportar-insignias');

const DIA_MS = 24 * 60 * 60 * 1000;
// Mediodía en México (UTC−6): sin ambigüedad de día.
const mediodia = (dia) => Date.parse(`${dia}T18:00:00Z`);

const L1 = 'a1-saludos-presentaciones';
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

const partida = (cambios = {}) => ({
  partida_id: randomUUID(),
  juego: 'orden-maestro',
  aciertos: 8,
  total: 8,
  duracion_ms: 100000,
  conceptos: [{ concepto_id: 'gram.some_any', aciertos: 2, errores: 0 }],
  ...cambios,
});

const verProgreso = (token = 'token-ana', { level = 'A1', headers } = {}) => llamar(progreso, { token, query: { level }, headers });
const jugar = (body, token = 'token-ana') => llamar(resultadoJuego, { method: 'POST', token, body });
const registrarChat = (sesionId, token = 'token-ana') => llamar(progreso, { method: 'POST', token, body: { sesion_id: sesionId } });
const insignia = (res, id) => res.body.insignias.find((i) => i.id === id);

/** Historial de antes del módulo: eventos sin resumen (como en producción hoy). */
function sembrarHistorial(uid, eventos) {
  for (const evento of eventos) {
    db.collection(COLECCIONES_LECCIONES.EVENTOS).doc().set({ uid, ...evento });
  }
}

test.beforeEach(() => db._limpiar());

// ── Niveles de experiencia ─────────────────────────────────────────────────

test('niveles: cada nivel pide 100 XP más que el anterior', () => {
  assert.deepEqual([1, 2, 3, 4, 5].map(xpParaNivel), [0, 100, 300, 600, 1000]);
  assert.deepEqual(nivelPorXp(0), { nivel: 1, xpEnNivel: 0, xpParaSiguiente: 100 });
  assert.deepEqual(nivelPorXp(99), { nivel: 1, xpEnNivel: 99, xpParaSiguiente: 100 });
  assert.deepEqual(nivelPorXp(100), { nivel: 2, xpEnNivel: 0, xpParaSiguiente: 200 });
  assert.deepEqual(nivelPorXp(720), { nivel: 4, xpEnNivel: 120, xpParaSiguiente: 400 });
  assert.equal(nivelPorXp(50 * 100 * 99).nivel, 100, 'sin errores de redondeo en niveles altos');
  assert.equal(nivelPorXp(50 * 100 * 99 - 1).nivel, 99);
});

test('niveles: subioDeNivel solo al cruzar un umbral', () => {
  assert.equal(subioDeNivel(90, 99), null);
  assert.equal(subioDeNivel(90, 100), 2);
  assert.equal(subioDeNivel(250, 650), 4, 'si se saltan niveles, devuelve el último');
});

// ── Racha ──────────────────────────────────────────────────────────────────

test('racha: el día cuenta en hora de México, no en UTC', () => {
  // 05:30 UTC del 6 de octubre son las 23:30 del 5 en la Ciudad de México.
  assert.equal(diaLocal(Date.parse('2026-10-06T05:30:00Z')), '2026-10-05');
  assert.equal(diaLocal(Date.parse('2026-10-06T06:30:00Z')), '2026-10-06');
});

test('racha: sube con días consecutivos, se reinicia con un hueco y el mismo día no cuenta doble', () => {
  let r = resumenVacio('u');
  r = { ...r, ...registrarDia(r, '2026-10-05') };
  r = { ...r, ...registrarDia(r, '2026-10-05') };
  assert.equal(r.racha_actual, 1);
  r = { ...r, ...registrarDia(r, '2026-10-06') };
  r = { ...r, ...registrarDia(r, '2026-10-07') };
  assert.equal(r.racha_actual, 3);
  r = { ...r, ...registrarDia(r, '2026-10-09') };
  assert.equal(r.racha_actual, 1, 'faltó el 8');
  assert.equal(r.racha_maxima, 3);
  // Fin de mes y de año.
  let s = { ...resumenVacio('u'), ...registrarDia(resumenVacio('u'), '2026-12-31') };
  s = { ...s, ...registrarDia(s, '2027-01-01') };
  assert.equal(s.racha_actual, 2);
});

test('racha: un día que llega tarde no rompe la racha y solo se guardan 14 días', () => {
  let r = resumenVacio('u');
  for (let d = 1; d <= 20; d++) r = { ...r, ...registrarDia(r, `2026-10-${String(d).padStart(2, '0')}`) };
  assert.equal(r.racha_actual, 20);
  assert.equal(r.dias_activos.length, 14);
  assert.equal(r.dias_activos[0], '2026-10-07');
  const tarde = registrarDia(r, '2026-10-03');
  assert.equal(tarde.racha_actual, 20);
  assert.equal(tarde.ultimo_dia, '2026-10-20');
});

test('racha vigente: hoy o ayer la conservan; antes de ayer se muestra 0', () => {
  const r = { racha_actual: 4, ultimo_dia: '2026-10-07' };
  assert.equal(rachaVigente(r, '2026-10-07'), 4);
  assert.equal(rachaVigente(r, '2026-10-08'), 4);
  assert.equal(rachaVigente(r, '2026-10-09'), 0);
  assert.equal(rachaVigente(resumenVacio('u'), '2026-10-09'), 0);
});

test('semana: de lunes a domingo', () => {
  assert.equal(lunesDe('2026-10-11'), '2026-10-05', 'domingo 11 → lunes 5');
  assert.equal(lunesDe('2026-10-12'), '2026-10-12');
  assert.equal(diasEnSemana(['2026-10-04', '2026-10-05', '2026-10-11', '2026-10-12'], '2026-10-08'), 2);
});

// ── Insignias y resumen ────────────────────────────────────────────────────

test('catálogo: las 9 insignias de la Ilustración 34, con ids únicos', () => {
  assert.equal(CATALOGO.length, 9);
  assert.equal(new Set(CATALOGO.map((i) => i.id)).size, 9);
  for (const i of CATALOGO) {
    assert.ok(i.nombre && i.descripcion && i.criterio && i.icono, i.id);
  }
});

test('resumen: la primera actividad suma XP, racha y otorga "Primer Logro"', () => {
  const ahora = mediodia('2026-10-05');
  const { resumen, insigniasNuevas, nivelNuevo } = aplicarActividad(resumenVacio('u'), { tipo: 'repaso', xp: 0 }, ahora);
  assert.equal(resumen.racha_actual, 1);
  assert.equal(resumen.conteos.repasos, 1);
  assert.deepEqual(insigniasNuevas.map((i) => i.id), ['primer-logro']);
  assert.deepEqual(resumen.insignias['primer-logro'], { fecha: ahora, retroactiva: false });
  assert.equal(nivelNuevo, null);
  // Una segunda actividad no vuelve a otorgarla.
  assert.deepEqual(aplicarActividad(resumen, { tipo: 'repaso', xp: 0 }, ahora).insigniasNuevas, []);
});

test('insignias: estrella, maestro speaking y jugador experto', () => {
  const ahora = mediodia('2026-10-05');
  let r = resumenVacio('u');
  const jugarPartida = (juego, aciertos, total) => {
    const salida = aplicarActividad(r, { tipo: 'minijuego', xp: 5, juego, aciertos, total }, ahora);
    r = salida.resumen;
    return salida.insigniasNuevas.map((i) => i.id);
  };

  assert.deepEqual(jugarPartida('orden-maestro', 7, 8), ['primer-logro']);
  assert.deepEqual(jugarPartida('orden-maestro', 8, 8), ['estrella-minijuego']);
  for (let i = 0; i < 9; i++) jugarPartida('eco-vocal', 7, 8); // 87.5 % ≥ 80 %
  jugarPartida('eco-vocal', 6, 8); // 75 %: no cuenta
  assert.equal(evaluar(r).progreso['maestro-speaking'].actual, 9);
  assert.deepEqual(jugarPartida('eco-vocal', 7, 8), ['maestro-speaking']);

  // 13 partidas, pero sin los 4 juegos el experto se queda a una de la meta.
  for (let i = 0; i < 15; i++) jugarPartida('rafaga-palabras', 3, 10);
  assert.deepEqual(evaluar(r).progreso['jugador-experto'], { actual: 24, meta: 25 });
  assert.deepEqual(jugarPartida('mision-situacional', 1, 4), ['jugador-experto']);
});

test('insignias: racha de 7, desafío semanal (lunes a domingo) y habla con IA', () => {
  let r = resumenVacio('u');
  const nuevas = [];
  // Del miércoles 30 de septiembre al martes 6 de octubre: 7 días seguidos.
  for (let d = 0; d < 7; d++) {
    const salida = aplicarActividad(r, { tipo: 'repaso', xp: 0 }, mediodia('2026-09-30') + d * DIA_MS);
    r = salida.resumen;
    nuevas.push(...salida.insigniasNuevas.map((i) => i.id));
  }
  // La semana del 28-sep tuvo 5 días (mié–dom): desafío al quinto día; racha al séptimo.
  assert.deepEqual(nuevas, ['primer-logro', 'desafio-semanal', 'racha-7']);
  assert.equal(r.insignias['desafio-semanal'].fecha, mediodia('2026-10-04'));

  const conChat = aplicarActividad(r, { tipo: 'chat', xp: 0 }, mediodia('2026-10-06'));
  assert.deepEqual(conChat.insigniasNuevas.map((i) => i.id), ['habla-con-ia']);
});

test('insignias: explorador A1 al aprobar todas las lecciones del nivel', () => {
  const lecciones = listarLeccionesDeNivel('A1');
  let r = resumenVacio('u');
  let ultimas = [];
  for (const l of lecciones) {
    ({ resumen: r, insigniasNuevas: ultimas } = aplicarActividad(
      r,
      { tipo: 'leccion', xp: 10, leccionId: l.id, nivel: 'A1' },
      mediodia('2026-10-05')
    ));
  }
  assert.equal(r.lecciones_aprobadas.A1.length, lecciones.length);
  assert.ok(ultimas.some((i) => i.id === 'explorador-a1'));
  assert.equal(evaluar(r).progreso['graduado-a2'].actual, 0);
});

test('reconstrucción: suma la XP del historial, calcula la racha y marca las insignias como retroactivas', () => {
  const eventos = [
    { tipo: 'leccion_completada', leccion_id: L1, xp: 50, fecha: mediodia('2026-10-03') },
    { tipo: 'minijuego_completado', juego: 'orden-maestro', partida_id: 'p1', puntaje: 1, xp: 15, fecha: mediodia('2026-10-04') },
    { tipo: 'repaso_completado', xp: 0, fecha: mediodia('2026-10-05') },
    { tipo: 'evento_desconocido', xp: 999, fecha: mediodia('2026-10-05') },
  ];
  const partidas = [{ partida_id: 'p1', respuesta_json: JSON.stringify({ aciertos: 8, total: 8 }) }];
  const progresos = [{ leccion_id: L1, aprobada: true }];
  const r = reconstruirResumen('u', { eventos: [...eventos].reverse(), progresos, partidas }, mediodia('2026-10-06'));

  assert.equal(r.xp_total, 65);
  assert.equal(r.racha_actual, 3);
  assert.deepEqual(r.lecciones_aprobadas.A1, [L1]);
  assert.deepEqual(r.juegos['orden-maestro'], { partidas: 1, aciertos: 8, total: 8, perfectas: 1, sobre_80: 1 });
  assert.deepEqual(Object.keys(r.insignias).sort(), ['estrella-minijuego', 'primer-logro']);
  assert.ok(Object.values(r.insignias).every((i) => i.retroactiva));
  assert.equal(r.insignias['estrella-minijuego'].fecha, mediodia('2026-10-04'), 'con la fecha en que se ganó');
});

// ── GET /api/progress ──────────────────────────────────────────────────────

test('GET: usuario nuevo (A1 del CU-08) — todo en cero, insignias en gris y sin_actividad', async () => {
  const res = await verProgreso();
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.sin_actividad, true);
  assert.deepEqual(res.body.xp, { total: 0, nivel: 1, xp_en_nivel: 0, xp_para_siguiente: 100 });
  assert.deepEqual(res.body.racha, { actual: 0, maxima: 0, ultimo_dia: null });
  assert.equal(res.body.dominio.aprobadas, 0);
  assert.equal(res.body.dominio.total, listarLeccionesDeNivel('A1').length);
  assert.equal(res.body.insignias.length, 9);
  assert.ok(res.body.insignias.every((i) => !i.obtenida && i.progreso.actual === 0));
  assert.ok(res.body.habilidades.every((h) => h.porcentaje === null && h.sugerencia));
  assert.deepEqual(res.body.habilidades.map((h) => h.id), ['vocabulario', 'gramatica', 'pronunciacion', 'comunicacion']);
});

test('GET: responde 304 con el ETag correcto y 200 cuando el progreso cambia', async () => {
  const primera = await verProgreso();
  const etag = primera.headers.etag;
  assert.match(etag, /^"[0-9a-f]{16}"$/);

  const repetida = await verProgreso('token-ana', { headers: { 'if-none-match': etag } });
  assert.equal(repetida.statusCode, 304);
  assert.equal(repetida.body, undefined);

  await jugar(partida());
  const cambiada = await verProgreso('token-ana', { headers: { 'if-none-match': etag } });
  assert.equal(cambiada.statusCode, 200);
  assert.notEqual(cambiada.headers.etag, etag);
});

test('GET: valida el nivel, exige sesión y separa el progreso por usuario', async () => {
  assert.equal((await verProgreso('token-ana', { level: 'C2' })).statusCode, 400);
  assert.equal((await llamar(progreso, { query: {} })).statusCode, 401);
  assert.equal((await llamar(progreso, { method: 'PUT', token: 'token-ana' })).statusCode, 405);

  await jugar(partida());
  assert.equal((await verProgreso('token-beto')).body.sin_actividad, true);
  assert.equal((await verProgreso('token-ana', { level: 'A2' })).body.nivel, 'A2');
});

test('GET: usuario con historial y sin resumen — se reconstruye una vez y se guarda', async () => {
  const hoy = Date.now();
  sembrarHistorial('uid-ana', [
    { tipo: 'leccion_completada', leccion_id: L1, xp: 100, fecha: hoy - 2 * DIA_MS },
    { tipo: 'minijuego_completado', juego: 'eco-vocal', partida_id: 'x', puntaje: 0.5, xp: 36, fecha: hoy - DIA_MS },
  ]);
  db.collection(COLECCIONES_LECCIONES.PROGRESO).doc(`uid-ana_${L1}`).set({ uid: 'uid-ana', leccion_id: L1, aprobada: true });

  const res = await verProgreso();
  assert.equal(res.body.xp.total, 136);
  assert.equal(res.body.xp.nivel, 2);
  assert.equal(res.body.racha.actual, 2, 'anteayer y ayer: la racha sigue vigente hoy');
  assert.equal(res.body.dominio.aprobadas, 1);
  assert.equal(res.body.sin_actividad, false);
  const primerLogro = insignia(res, 'primer-logro');
  assert.equal(primerLogro.obtenida, true);
  assert.equal(primerLogro.retroactiva, true);

  const guardado = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(guardado.xp_total, 136);
  assert.ok(guardado.reconstruido);
});

test('GET: las habilidades suman el SRS y el minijuego correspondiente', async () => {
  db.collection(COLECCIONES_LECCIONES.SRS).doc('a').set({ uid: 'uid-ana', tipo: 'vocab', aciertos: 3, fallos: 1 });
  db.collection(COLECCIONES_LECCIONES.SRS).doc('b').set({ uid: 'uid-ana', tipo: 'gramatica', aciertos: 1, fallos: 1 });
  assert.equal((await jugar(partida({ juego: 'rafaga-palabras', aciertos: 4, total: 6, duracion_ms: 60000, conceptos: [] }))).statusCode, 200);

  const habilidades = Object.fromEntries((await verProgreso()).body.habilidades.map((h) => [h.id, h.porcentaje]));
  assert.equal(habilidades.vocabulario, 70, '(3 + 4) / (4 + 6)');
  assert.equal(habilidades.gramatica, null, 'solo 2 respuestas: "Sin datos"');
  assert.equal(habilidades.pronunciacion, null);
});

// ── Integración con las actividades ────────────────────────────────────────

test('partida de un usuario con historial y sin resumen: no crea un resumen vacío, y el GET da la XP correcta', async () => {
  const hoy = Date.now();
  sembrarHistorial('uid-ana', [
    { tipo: 'leccion_completada', leccion_id: L1, xp: 100, fecha: hoy - 3 * DIA_MS },
    { tipo: 'repaso_completado', xp: 0, fecha: hoy - 3 * DIA_MS },
    { tipo: 'minijuego_completado', juego: 'orden-maestro', partida_id: 'vieja', puntaje: 0.5, xp: 36, fecha: hoy - 3 * DIA_MS },
  ]);

  const res = await jugar(partida());
  assert.equal(res.statusCode, 200);
  // El primer logro ya lo merecía: no se festeja otra vez. La partida perfecta sí.
  assert.deepEqual(res.body.insignias_nuevas.map((i) => i.id), ['estrella-minijuego']);
  assert.equal(res.body.nivel_xp_nuevo, null);

  const guardado = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(guardado.xp_total, 136 + res.body.xp_ganado, 'conserva el historial');
  assert.equal(guardado.racha_actual, 1, 'hubo un hueco de dos días');
  assert.equal(guardado.insignias['primer-logro'].retroactiva, true);
  assert.equal(guardado.insignias['estrella-minijuego'].retroactiva, false);

  const vista = await verProgreso();
  assert.equal(vista.body.xp.total, 136 + res.body.xp_ganado);
  assert.equal(vista.body.racha.actual, 1);
});

test('partida: la primera de un usuario nuevo festeja "Primer Logro" y las repetidas no aplican nada', async () => {
  const body = partida({ aciertos: 7 });
  const res = await jugar(body);
  assert.deepEqual(res.body.insignias_nuevas.map((i) => i.id), ['primer-logro']);
  assert.ok(res.body.insignias_nuevas[0].nombre);
  const antes = db._doc(COLECCIONES.RESUMEN, 'uid-ana');

  const repetida = await jugar(body);
  assert.equal(repetida.body.repetido, true);
  assert.deepEqual(repetida.body.insignias_nuevas, res.body.insignias_nuevas, 'devuelve la respuesta original');
  assert.deepEqual(db._doc(COLECCIONES.RESUMEN, 'uid-ana'), antes);
  assert.equal(antes.juegos['orden-maestro'].partidas, 1);
});

test('lección aprobada: escribe el resumen, festeja la insignia y avisa la subida de nivel de XP', async () => {
  const res = await llamar(intento, {
    method: 'POST',
    token: 'token-ana',
    query: { id: L1 },
    body: { attempt_id: randomUUID(), respuestas: RESPUESTAS_L1 },
  });
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.aprobada, true);
  assert.deepEqual(res.body.insignias_nuevas.map((i) => i.id), ['primer-logro']);
  const resumen = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(resumen.xp_total, res.body.xp_ganado);
  assert.deepEqual(resumen.lecciones_aprobadas.A1, [L1]);
  assert.equal(res.body.nivel_xp_nuevo, res.body.xp_ganado >= 100 ? 2 : null);
});

test('lección reprobada: no cuenta como actividad ni crea resumen', async () => {
  const res = await llamar(intento, {
    method: 'POST',
    token: 'token-ana',
    query: { id: L1 },
    body: { attempt_id: randomUUID(), respuestas: RESPUESTAS_L1.map((r) => ({ ...r, valor: r.id === 'q6' ? r.valor : 0 })) },
  });
  assert.equal(res.body.aprobada, false);
  assert.deepEqual(res.body.insignias_nuevas, []);
  assert.equal(db._doc(COLECCIONES.RESUMEN, 'uid-ana'), undefined);
});

test('nivel de XP: subir de nivel se avisa en la respuesta', async () => {
  db.collection(COLECCIONES.RESUMEN).doc('uid-ana').set({ ...resumenVacio('uid-ana'), xp_total: 95 });
  const res = await jugar(partida());
  assert.ok(res.body.xp_ganado >= 5);
  assert.equal(res.body.nivel_xp_nuevo, 2);
});

test('repaso: actualiza el resumen sin dar XP', async () => {
  // Una lección deja conceptos en el SRS; el repaso se puede enviar aunque no estén vencidos.
  await llamar(intento, { method: 'POST', token: 'token-ana', query: { id: L1 }, body: { attempt_id: randomUUID(), respuestas: RESPUESTAS_L1 } });
  const xpAntes = db._doc(COLECCIONES.RESUMEN, 'uid-ana').xp_total;

  const res = await llamar(sesionRepaso, {
    method: 'POST',
    token: 'token-ana',
    body: { attempt_id: randomUUID(), respuestas: [{ id: `${L1}:q2`, valor: 'is' }] },
  });
  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body.insignias_nuevas, []);
  const resumen = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(resumen.conteos.repasos, 1);
  assert.equal(resumen.xp_total, xpAntes);
});

// ── POST /api/progress (chat) ──────────────────────────────────────────────

test('chat: desbloquea "Habla con IA", cuenta para la racha y es idempotente', async () => {
  const res = await registrarChat('sesion-1');
  assert.equal(res.statusCode, 200);
  assert.deepEqual(res.body.insignias_nuevas.map((i) => i.id), ['primer-logro', 'habla-con-ia']);
  assert.equal(res.body.repetido, false);

  const repetido = await registrarChat('sesion-1');
  assert.equal(repetido.body.repetido, true);
  const resumen = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(resumen.conteos.chats, 1);
  assert.equal(resumen.racha_actual, 1);
  assert.equal(resumen.xp_total, 0);
  assert.equal(db._docs(COLECCIONES_LECCIONES.EVENTOS).filter((e) => e.tipo === 'chat_completado').length, 1);

  const vista = await verProgreso();
  assert.equal(insignia(vista, 'habla-con-ia').obtenida, true);
  assert.equal(insignia(vista, 'habla-con-ia').retroactiva, false);
});

test('chat: valida el sesion_id y limita las conversaciones por día', async () => {
  for (const malo of [undefined, '', 'a/b', 'x'.repeat(101), 42]) {
    const res = await llamar(progreso, { method: 'POST', token: 'token-ana', body: { sesion_id: malo } });
    assert.equal(res.statusCode, 400, String(malo));
  }
  for (let i = 0; i < MAX_CHATS_POR_DIA; i++) assert.equal((await registrarChat(`s${i}`)).statusCode, 200);
  const res = await registrarChat('una-mas');
  assert.equal(res.statusCode, 429);
  assert.equal(res.body.codigo, 'DEMASIADOS_CHATS');
});

test('progress: un error inesperado se registra como progress/get o progress/chat', async (t) => {
  const contextos = [];
  t.mock.method(console, 'error', (mensaje) => contextos.push(mensaje));
  const roto = new Proxy({}, { get() { throw new Error('falla interna'); } });

  const get = await llamar(progreso, { token: 'token-ana', query: roto });
  const post = await llamar(progreso, { method: 'POST', token: 'token-ana', body: roto });
  assert.equal(get.statusCode, 500);
  assert.equal(post.statusCode, 500);
  assert.deepEqual(contextos, ['Error en progress/get:', 'Error en progress/chat:']);
});

// ── Migración y catálogo del APK ───────────────────────────────────────────

test('migración: crea el resumen de quien tiene actividad y es idempotente', async () => {
  const hoy = Date.now();
  sembrarHistorial('uid-ana', [{ tipo: 'leccion_completada', leccion_id: L1, xp: 100, fecha: hoy }]);
  sembrarHistorial('uid-beto', [
    { tipo: 'repaso_completado', xp: 0, fecha: hoy },
    { tipo: 'repaso_completado', xp: 0, fecha: hoy },
  ]);

  assert.deepEqual(await migrarResumenes(db), { revisados: 2, creados: 2 });
  const ana = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.equal(ana.xp_total, 100);
  assert.equal(db._doc(COLECCIONES.RESUMEN, 'uid-beto').conteos.repasos, 2);

  // Una actividad después de migrar actualiza ese resumen; volver a migrar no lo toca.
  await jugar(partida());
  const despues = db._doc(COLECCIONES.RESUMEN, 'uid-ana');
  assert.deepEqual(await migrarResumenes(db), { revisados: 2, creados: 0 });
  assert.deepEqual(db._doc(COLECCIONES.RESUMEN, 'uid-ana'), despues);
  assert.equal(db._docs(COLECCIONES_MINIJUEGOS.PARTIDAS).length, 1);
});

test('catálogo del APK: assets/progreso/insignias.json está al día (npm run exportar:insignias)', () => {
  assert.equal(fs.readFileSync(RUTA_INSIGNIAS, 'utf-8'), serializarInsignias());
});
