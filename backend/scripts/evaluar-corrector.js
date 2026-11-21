#!/usr/bin/env node
/**
 * Batería de prueba del corrector del chat (RF-09) contra el Azure OpenAI real:
 * oraciones de nivel A1/A2 con errores típicos de hispanohablantes (y otras correctas,
 * que no deben marcarse). Sirve para ajustar el prompt de lib/chat/correccion.js con
 * datos y no a ojo. No forma parte de `npm test` porque gasta tokens y necesita la llave.
 *
 *   AZURE_OPENAI_ENDPOINT=... AZURE_OPENAI_API_KEY=... node scripts/evaluar-corrector.js
 *   … node scripts/evaluar-corrector.js --detalle     (imprime cada corrección completa)
 *
 * Cada caso: [mensaje, lo que debe aparecer en la oración corregida (regex) o null si
 * está bien y no debe marcarse nada, opciones { tutor, nivel, voz }].
 */
const { configuracionChat } = require('../lib/chat/azureOpenAI');
const { pedirCorreccion } = require('../lib/chat/correccion');

const CASOS = [
  // ── Tiempo verbal ──
  ['Yesterday I go to the park with my friends', /went/],
  ['Last week I visit my grandmother and we cook together', /visited.*cooked/],
  ['I go to the beach', /went/, { tutor: 'What did you do last weekend?' }],
  ['I am going to the cinema yesterday', /went/],
  ['Last night I didn\'t went to the party', /didn't go|did not go/],
  ['Did you saw the movie?', /Did you see/i],
  ['I buyed a new phone last month', /bought/],
  ['Tomorrow I go to the doctor', /going to|will go|am going/],
  ['Next year I will to travel to Canada', /will travel/],
  ['I going to study tonight', /am going to|I'm going to/],
  ['I have seen that movie yesterday', /saw/, { nivel: 'A2' }],
  ['When I was a child I play soccer every day', /played/, { nivel: 'A2' }],
  ['She was cook when I arrived', /was cooking/, { nivel: 'A2' }],
  ['I live here since five years', /have lived|have been living|for five years/, { nivel: 'A2' }],
  ['Right now I watch TV', /am watching|I'm watching/],
  ['I am work in a bank', /I work|am working/],
  // ── Conjugación y concordancia ──
  ['My brother play soccer every weekend', /plays/],
  ['She don\'t like coffee', /doesn't|does not/],
  ['He have two dogs', /has/],
  ['My parents is teachers', /are/],
  ['Does she likes pizza?', /Does she like\b/i],
  ['The people is very nice here', /people are/],
  ['We was at home yesterday', /were/],
  // ── Faltan o sobran palabras ──
  ['I student from Mexico', /I am a student|I'm a student/],
  ['She very happy today', /is very happy|She's very happy/],
  ['I am agree with you', /I agree/],
  ['I want go to the beach', /want to go/],
  ['I can to swim very well', /can swim/],
  ['Is raining a lot today', /It is raining|It's raining/],
  ['I don\'t have nothing in my bag', /anything|have nothing/],
  // ── Orden de palabras ──
  ['Where you live?', /Where do you live/i],
  ['You like pizza?', /Do you like/i],
  ['I have a car red', /red car/],
  ['What time it is?', /What time is it/i],
  // ── Artículos, plurales, pronombres, preposiciones ──
  ['I have two brother and one sister', /brothers/],
  ['The childs are playing in the garden', /children/],
  ['I eat a apple every morning', /an apple/],
  ['My birthday is in Monday', /on Monday/],
  ['I go to bed at eleven in the night', /at night/],
  ['Can I make a party in my house?', /have a party|throw a party/, { nivel: 'A2' }],
  ['i want to by a new car', /buy/],
  ['She is married with a doctor', /married to/, { nivel: 'A2' }],
  ['Me like the music', /I like/],
  ['He name is Carlos', /His name/],
  // ── Vocabulario y calcos ──
  ['I have 20 years', /am 20|I'm 20/],
  ['I have hungry', /am hungry|I'm hungry/],
  ['I want to make a question', /ask a question/, { nivel: 'A2' }],
  ['Yesterday I knew a new friend at school', /met/, { nivel: 'A2' }],
  ['I go to the tienda every day', /store|shop/],
  // ── Con voz: sin puntuación, con homófonos y cifras ──
  ['yesterday i go to the store and i buy 2 apples', /went.*bought/, { voz: true }],
  ['my sister have a new job and she like it a lot', /has.*likes/, { voz: true }],
  // ── Correctas: no debe marcarse nada ──
  ['I usually have breakfast at seven o\'clock', null],
  ['yes i love it i listen to rock every day', null, { tutor: 'Do you like music?' }],
  ['Last weekend I went to the beach with my friends.', null, { nivel: 'A2' }],
  ['In Mexico City', null, { tutor: 'Where do you live?' }],
  ['Yes, a lot!', null, { tutor: 'Do you like pizza?' }],
  ['im gonna watch a movie tonight', null],
  ['She has worked as a nurse for three years.', null, { nivel: 'A2' }],
  ['I went their with my family', null, { voz: true }],
  ['My favourite colour is blue', null],
  ['I don\'t know, maybe pizza or tacos', null, { tutor: 'What is your favorite food?' }],
  ['Can you repeat that, please?', null],
];

const detalle = process.argv.includes('--detalle');

async function evaluar(configuracion, [mensaje, esperado, { tutor, nivel = 'A1', voz = false } = {}]) {
  const historial = tutor ? [{ role: 'assistant', content: tutor }] : [];
  const inicio = Date.now();
  const correccion = await pedirCorreccion(configuracion, { mensaje, nivel, historial, voz });
  const ms = Date.now() - inicio;
  let fallo = null;
  if (!correccion) fallo = 'sin respuesta del corrector';
  else if (esperado === null && !correccion.correcta) fallo = `marcó un error que no hay: ${correccion.errores.map((e) => `${e.original} → ${e.correccion}`).join('; ')}`;
  else if (esperado !== null && correccion.correcta) fallo = 'no detectó el error';
  else if (esperado !== null && !esperado.test(correccion.corregido || '')) fallo = `corrigió otra cosa: "${correccion.corregido}"`;
  return { mensaje, correccion, ms, fallo };
}

(async () => {
  const configuracion = configuracionChat();
  // De a 4 a la vez: rápido sin topar el límite de peticiones del despliegue.
  const resultados = [];
  for (let i = 0; i < CASOS.length; i += 4) {
    resultados.push(...(await Promise.all(CASOS.slice(i, i + 4).map((caso) => evaluar(configuracion, caso)))));
  }
  for (const { mensaje, correccion, ms, fallo } of resultados) {
    console.log(`${fallo ? '✗' : '✓'} ${mensaje}  (${ms} ms)${fallo ? `\n    ${fallo}` : ''}`);
    if (detalle && correccion && !correccion.correcta) {
      console.log(`    → ${correccion.corregido}`);
      correccion.errores.forEach((e) => console.log(`      [${e.tipo || 'sin tipo'}] ${e.original} → ${e.correccion}: ${e.explicacion}`));
    }
  }
  const fallos = resultados.filter((r) => r.fallo).length;
  const tiempos = resultados.map((r) => r.ms).sort((a, b) => a - b);
  console.log(`\n${resultados.length - fallos}/${resultados.length} correctos · mediana ${tiempos[Math.floor(tiempos.length / 2)]} ms · máximo ${tiempos.at(-1)} ms (meta RN-05: 2500 ms)`);
  process.exit(fallos ? 1 : 0);
})().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
