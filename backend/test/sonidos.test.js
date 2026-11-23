const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const { SONIDOS, wav, MUESTREO } = require('../scripts/generar-sonidos');

const DIR_RAW = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'res', 'raw');

test('sonidos: los WAV del APK están al día (npm run generar:sonidos)', () => {
  for (const [nombre, generar] of Object.entries(SONIDOS)) {
    const actual = fs.readFileSync(path.join(DIR_RAW, `${nombre}.wav`));
    assert.ok(actual.equals(wav(generar())), `${nombre}.wav está viejo: corre \`npm run generar:sonidos\``);
  }
});

test('sonidos: son cortos y no saturan, para no tapar la voz del Locutor (CU-07 paso 6)', () => {
  for (const [nombre, generar] of Object.entries(SONIDOS)) {
    const muestras = generar();
    const ms = (muestras.length / MUESTREO) * 1000;
    const pico = Math.max(...muestras.map(Math.abs));
    assert.ok(ms > 150 && ms < 500, `${nombre}: dura ${Math.round(ms)} ms`);
    assert.ok(pico < 0.6, `${nombre}: pico de ${pico.toFixed(2)}`);
  }
  // El de error es más bajito que el de acierto (filtro afectivo).
  const pico = (nombre) => Math.max(...SONIDOS[nombre]().map(Math.abs));
  assert.ok(pico('sonido_error') < pico('sonido_acierto'));
});

test('sonidos: cabecera WAV PCM mono de 16 bits', () => {
  const archivo = wav(Float64Array.from([0, 0.5, -0.5]));
  assert.equal(archivo.toString('ascii', 0, 4), 'RIFF');
  assert.equal(archivo.toString('ascii', 8, 12), 'WAVE');
  assert.equal(archivo.readUInt16LE(20), 1);
  assert.equal(archivo.readUInt16LE(22), 1);
  assert.equal(archivo.readUInt32LE(24), MUESTREO);
  assert.equal(archivo.readUInt16LE(34), 16);
  assert.equal(archivo.readUInt32LE(40), 6);
  assert.equal(archivo.length, 44 + 6);
});
