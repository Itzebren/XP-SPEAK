const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const { randomUUID } = require('crypto');
const { instalarFirebaseFalso, llamar } = require('./helpers/fakeFirebase');

// El doble debe instalarse antes de cargar los handlers.
const { db } = instalarFirebaseFalso({ 'token-ana': 'uid-ana', 'token-beto': 'uid-beto' });

const resultado = require('../api/games/result');
const tokenVoz = require('../api/speech/token');
const { calcularXp, PARTIDAS_CON_XP_COMPLETA } = require('../lib/minijuegos/xp');
const { COLECCIONES, MAX_PARTIDAS_POR_DIA, diaLocal } = require('../lib/minijuegos/repositorio');
const { COLECCIONES: COLECCIONES_LECCIONES } = require('../lib/lecciones/repositorio');
const { separarEjemplo, construirBancoDesdeDisco, leerMisionesDeDisco } = require('../lib/minijuegos/contenido');
const { leerLeccionesDeDisco } = require('../lib/lecciones/contenido');
const { RUTA_BANCO, serializarBanco } = require('../scripts/exportar-banco-minijuegos');
const { lintearMisiones, RUTA_SCHEMA_MISION } = require('../scripts/lint-content');

const partida = (cambios = {}) => ({
  partida_id: randomUUID(),
  juego: 'orden-maestro',
  aciertos: 7,
  total: 8,
  duracion_ms: 100000,
  conceptos: [
    { concepto_id: 'gram.some_any', aciertos: 1, errores: 1 },
    { concepto_id: 'gram.present_simple_negative_questions', aciertos: 2, errores: 0 },
  ],
  ...cambios,
});

const enviar = (body, token = 'token-ana') => llamar(resultado, { method: 'POST', token, body });

test.beforeEach(() => db._limpiar());

// ── XP ──────────────────────────────────────────────────────────────────────

test('xp: precisión por la base del juego, más bono por rapidez solo con 70% o más', () => {
  // 8 rondas de Orden Maestro = 120 s objetivo; en 60 s (la mitad) da el bono completo.
  assert.deepEqual(calcularXp('orden-maestro', { aciertos: 8, total: 8, duracionMs: 60000 }), { xp: 15, reducida: false });
  // En el tiempo objetivo: 5 · (1.5 − 1) = 2.5 → 3.
  assert.equal(calcularXp('orden-maestro', { aciertos: 8, total: 8, duracionMs: 120000 }).xp, 13);
  // Muy lento: sin bono.
  assert.equal(calcularXp('orden-maestro', { aciertos: 8, total: 8, duracionMs: 600000 }).xp, 10);
  // 50% de precisión: sin bono aunque sea rapidísimo.
  assert.equal(calcularXp('orden-maestro', { aciertos: 4, total: 8, duracionMs: 10000 }).xp, 5);
  assert.equal(calcularXp('eco-vocal', { aciertos: 8, total: 8, duracionMs: 80000 }).xp, 20);
});

test('xp: Ráfaga da el bono por palabras acertadas y el tope diario reduce a la mitad', () => {
  assert.equal(calcularXp('rafaga-palabras', { aciertos: 20, total: 25, duracionMs: 90000 }).xp, 8 + 4);
  assert.equal(calcularXp('rafaga-palabras', { aciertos: 60, total: 60, duracionMs: 180000 }).xp, 10 + 5);
  assert.deepEqual(
    calcularXp('orden-maestro', { aciertos: 8, total: 8, duracionMs: 60000 }, PARTIDAS_CON_XP_COMPLETA),
    { xp: 7, reducida: true }
  );
});

// ── POST /api/games/result ─────────────────────────────────────────────────

test('resultado: otorga la XP del servidor, alimenta el SRS y marca la actividad del día', async () => {
  const body = partida();
  const res = await enviar(body);

  assert.equal(res.statusCode, 200);
  assert.equal(res.body.xp_ganado, calcularXp('orden-maestro', { aciertos: 7, total: 8, duracionMs: 100000 }).xp);
  assert.equal(res.body.repetido, false);
  assert.equal(res.body.partidas_hoy, 1);

  const srs = db._doc(COLECCIONES_LECCIONES.SRS, 'uid-ana_gram.some_any');
  assert.equal(srs.aciertos, 1);
  assert.equal(srs.fallos, 1);
  assert.equal(srs.nivel_mcer, 'A1');
  assert.equal(srs.intervalo, 1, 'un fallo reprograma el concepto para mañana (SM-2)');

  const eventos = db._docs(COLECCIONES_LECCIONES.EVENTOS);
  assert.equal(eventos.length, 1);
  assert.equal(eventos[0].tipo, 'minijuego_completado');
  assert.equal(eventos[0].partida_id, body.partida_id);
});

test('resultado: es idempotente por partida_id (el Worker puede reenviar)', async () => {
  const body = partida();
  const primero = await enviar(body);
  const segundo = await enviar(body);

  assert.equal(segundo.statusCode, 200);
  assert.equal(segundo.body.repetido, true);
  assert.equal(segundo.body.xp_ganado, primero.body.xp_ganado);
  assert.equal(db._docs(COLECCIONES_LECCIONES.EVENTOS).length, 1);
  assert.equal(db._doc(COLECCIONES_LECCIONES.SRS, 'uid-ana_gram.some_any').fallos, 1);

  const otroJuego = await enviar({ ...body, juego: 'eco-vocal' });
  assert.equal(otroJuego.statusCode, 409);
  assert.equal(otroJuego.body.codigo, 'PARTIDA_ID_REUTILIZADO');
});

test('resultado: desde la 6.ª partida del mismo juego en el día la XP va a la mitad', async () => {
  for (let i = 0; i < PARTIDAS_CON_XP_COMPLETA; i++) {
    assert.equal((await enviar(partida())).body.xp_reducida, false);
  }
  const sexta = await enviar(partida());
  assert.equal(sexta.body.xp_reducida, true);
  assert.equal(sexta.body.partidas_hoy, PARTIDAS_CON_XP_COMPLETA + 1);

  // Otro juego tiene su propio conteo; otro usuario también.
  assert.equal((await enviar(partida({ juego: 'eco-vocal' }))).body.xp_reducida, false);
  assert.equal((await enviar(partida(), 'token-beto')).body.xp_reducida, false);
});

test('resultado: tope duro de partidas por día', async () => {
  await db
    .collection(COLECCIONES.DIARIO)
    .doc(`uid-ana_${diaLocal(Date.now())}`)
    .set({ uid: 'uid-ana', conteo: { 'orden-maestro': MAX_PARTIDAS_POR_DIA } });
  const res = await enviar(partida());
  assert.equal(res.statusCode, 429);
  assert.equal(res.body.codigo, 'DEMASIADAS_PARTIDAS');
});

test('resultado: valida el cuerpo antes de tocar Firestore', async () => {
  const casos = [
    [{ partida_id: 'no-es-uuid' }, 'partida_id'],
    [{ juego: 'ajedrez' }, 'juego'],
    [{ total: 3 }, 'total'],
    [{ aciertos: 9 }, 'aciertos'],
    [{ duracion_ms: 100 }, 'duracion_ms'],
    [{ juego: 'rafaga-palabras', aciertos: 5, total: 10, duracion_ms: 200000 }, 'Ráfaga'],
    [{ conceptos: [{ concepto_id: 'gram.some_any', aciertos: 1, errores: 0 }, { concepto_id: 'gram.some_any', aciertos: 1, errores: 0 }] }, 'repetido'],
  ];
  for (const [cambios, menciona] of casos) {
    const res = await enviar(partida(cambios));
    assert.equal(res.statusCode, 400, JSON.stringify(cambios));
    assert.match(res.body.error, new RegExp(menciona));
  }
  assert.equal(db._docs(COLECCIONES.PARTIDAS).length, 0);
});

test('resultado: ignora conceptos que ya no existen en el contenido', async () => {
  const res = await enviar(partida({ conceptos: [{ concepto_id: 'voc.viejo.no_existe', aciertos: 1, errores: 0 }] }));
  assert.equal(res.statusCode, 200);
  assert.equal(db._docs(COLECCIONES_LECCIONES.SRS).length, 0);
});

test('resultado: exige sesión', async () => {
  const res = await llamar(resultado, { method: 'POST', body: partida() });
  assert.equal(res.statusCode, 401);
});

// ── GET /api/speech/token ──────────────────────────────────────────────────

test('token de voz: 503 si Azure no está configurado (la app usa el reconocedor del teléfono)', async () => {
  delete process.env.AZURE_SPEECH_KEY;
  const res = await llamar(tokenVoz, { token: 'token-ana' });
  assert.equal(res.statusCode, 503);
  assert.equal(res.body.codigo, 'SPEECH_NO_CONFIGURADO');
});

test('token de voz: pide el token a Azure con la llave del servidor y limita por usuario', async (t) => {
  process.env.AZURE_SPEECH_KEY = 'llave-secreta';
  process.env.AZURE_SPEECH_REGION = 'southcentralus';
  const llamadas = [];
  t.mock.method(globalThis, 'fetch', async (url, opciones) => {
    llamadas.push({ url, opciones });
    return { ok: true, status: 200, text: async () => 'token-azure' };
  });
  t.after(() => {
    delete process.env.AZURE_SPEECH_KEY;
    delete process.env.AZURE_SPEECH_REGION;
  });

  const res = await llamar(tokenVoz, { token: 'token-ana' });
  assert.equal(res.statusCode, 200);
  assert.equal(res.body.token, 'token-azure');
  assert.equal(res.body.region, 'southcentralus');
  assert.ok(res.body.expira_en > Date.now());
  assert.equal(res.headers['cache-control'], 'no-store');
  assert.equal(llamadas[0].url, 'https://southcentralus.api.cognitive.microsoft.com/sts/v1.0/issueToken');
  assert.equal(llamadas[0].opciones.headers['Ocp-Apim-Subscription-Key'], 'llave-secreta');
  assert.ok(!JSON.stringify(res.body).includes('llave-secreta'), 'la llave nunca sale del servidor');

  for (let i = 1; i < tokenVoz.MAX_TOKENS_POR_VENTANA; i++) await llamar(tokenVoz, { token: 'token-ana' });
  const excedido = await llamar(tokenVoz, { token: 'token-ana' });
  assert.equal(excedido.statusCode, 429);
});

test('token de voz: si Azure falla responde 502 sin filtrar detalles', async (t) => {
  process.env.AZURE_SPEECH_KEY = 'llave-secreta';
  process.env.AZURE_SPEECH_REGION = 'southcentralus';
  t.mock.method(globalThis, 'fetch', async () => ({ ok: false, status: 401, text: async () => 'detalle interno' }));
  t.mock.method(console, 'error', () => {});
  t.after(() => {
    delete process.env.AZURE_SPEECH_KEY;
    delete process.env.AZURE_SPEECH_REGION;
  });

  const res = await llamar(tokenVoz, { token: 'token-ana' });
  assert.equal(res.statusCode, 502);
  assert.equal(res.body.codigo, 'SPEECH_NO_DISPONIBLE');
});

// ── Banco de contenido ─────────────────────────────────────────────────────

test('banco: separa los ejemplos de pregunta — respuesta en dos oraciones', () => {
  assert.deepEqual(separarEjemplo('Do you want some rice? — Yes, I do.'), ['Do you want some rice?', 'Yes, I do.']);
  assert.deepEqual(separarEjemplo('I am Carlos.'), ['I am Carlos.']);
});

test('banco: el JSON del APK está al día con las lecciones y misiones (npm run exportar:minijuegos)', () => {
  const esperado = serializarBanco(construirBancoDesdeDisco());
  const actual = fs.readFileSync(RUTA_BANCO, 'utf-8');
  assert.equal(actual, esperado, 'El banco del APK está viejo: corre `npm run exportar:minijuegos`');
});

test('banco: no incluye la clave de respuestas de las lecciones ni la autoría', () => {
  const banco = construirBancoDesdeDisco();
  const texto = JSON.stringify(banco);
  assert.ok(!texto.includes('respuesta_correcta'));
  assert.ok(!texto.includes('autoria'));
  assert.equal(banco.lecciones.length, 24);
  assert.ok(banco.lecciones.every((l) => l.vocabulario.length > 0 && l.oraciones.length > 0 && l.dialogo.length > 0));
});

// ── Linter de misiones ─────────────────────────────────────────────────────

test('linter: las misiones del repo son válidas', () => {
  const schema = JSON.parse(fs.readFileSync(RUTA_SCHEMA_MISION, 'utf-8'));
  const lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion);
  assert.deepEqual(lintearMisiones(leerMisionesDeDisco(), schema, lecciones), []);
});

test('linter: detecta errores de diseño en una misión', () => {
  const schema = JSON.parse(fs.readFileSync(RUTA_SCHEMA_MISION, 'utf-8'));
  const lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion);
  const [{ mision: original }] = leerMisionesDeDisco().filter(({ mision }) => mision.id === 'mision-a1-cafeteria');
  const mision = structuredClone(original);
  mision.pasos[0].opciones.forEach((o) => (o.correcta = true));
  delete mision.pasos[1].opciones[1].feedback;
  mision.pasos[2].opciones[2].concepto_id = 'gram.past_simple_irregular';
  mision.autoria.fuentes = ['Sin fuente'];

  const errores = lintearMisiones([{ archivo: 'mision-a1-cafeteria.json', mision }], schema, lecciones).join('\n');
  assert.match(errores, /exactamente una opción correcta/);
  assert.match(errores, /no tiene feedback/);
  assert.match(errores, /gram.past_simple_irregular' no se enseña/);
  assert.match(errores, /§4.2/);
});
