#!/usr/bin/env node
/**
 * Genera los sonidos de acierto y error de los minijuegos (CU-07 paso 6:
 * feedback visual y auditivo), docs/minijuegos-diseno.md §4.5:
 *
 *   npm run generar:sonidos
 *
 * Escribe app/src/main/res/raw/sonido_acierto.wav y sonido_error.wav. Son
 * tonos sintetizados (no hay archivos de terceros ni licencias): el acierto
 * sube dos notas y el error baja dos notas graves y suaves, para que un fallo
 * no suene a castigo (filtro afectivo, Trabajo Terminal §2.2.3).
 */
const fs = require('fs');
const path = require('path');

const DIR_RAW = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'res', 'raw');
const MUESTREO = 22050;

/**
 * Una nota con ataque corto y caída exponencial. `armonicos` son pares
 * [múltiplo, volumen] sobre la fundamental, para que no suene a pitido puro.
 */
function nota(frecuencia, ms, { volumen = 0.35, armonicos = [[2, 0.15]], caida = 6 } = {}) {
  const total = Math.round((MUESTREO * ms) / 1000);
  const ataque = Math.round(MUESTREO * 0.005);
  const muestras = new Float64Array(total);
  for (let i = 0; i < total; i++) {
    const t = i / MUESTREO;
    const envolvente = Math.min(1, i / ataque) * Math.exp((-caida * i) / total);
    let valor = Math.sin(2 * Math.PI * frecuencia * t);
    for (const [multiplo, peso] of armonicos) valor += peso * Math.sin(2 * Math.PI * frecuencia * multiplo * t);
    muestras[i] = volumen * envolvente * valor;
  }
  return muestras;
}

const unir = (...partes) => Float64Array.from(partes.flatMap((p) => [...p]));

/** WAV PCM de 16 bits, mono. */
function wav(muestras) {
  const datos = Buffer.alloc(muestras.length * 2);
  muestras.forEach((m, i) => datos.writeInt16LE(Math.round(Math.max(-1, Math.min(1, m)) * 32767), i * 2));
  const cabecera = Buffer.alloc(44);
  cabecera.write('RIFF', 0);
  cabecera.writeUInt32LE(36 + datos.length, 4);
  cabecera.write('WAVE', 8);
  cabecera.write('fmt ', 12);
  cabecera.writeUInt32LE(16, 16); // tamaño del bloque fmt
  cabecera.writeUInt16LE(1, 20); // PCM
  cabecera.writeUInt16LE(1, 22); // mono
  cabecera.writeUInt32LE(MUESTREO, 24);
  cabecera.writeUInt32LE(MUESTREO * 2, 28); // bytes por segundo
  cabecera.writeUInt16LE(2, 32); // bytes por muestra
  cabecera.writeUInt16LE(16, 34); // bits por muestra
  cabecera.write('data', 36);
  cabecera.writeUInt32LE(datos.length, 40);
  return Buffer.concat([cabecera, datos]);
}

const SONIDOS = {
  // Mi5 → La5: una cuarta hacia arriba, corta y brillante.
  sonido_acierto: () => unir(nota(659.25, 90, { caida: 3 }), nota(880, 220)),
  // Mi4 → Do4: hacia abajo, más grave y bajito, sin armónicos ásperos.
  sonido_error: () =>
    unir(nota(329.63, 110, { volumen: 0.28, armonicos: [[2, 0.08]], caida: 3 }), nota(261.63, 240, { volumen: 0.28, armonicos: [[2, 0.08]] })),
};

if (require.main === module) {
  fs.mkdirSync(DIR_RAW, { recursive: true });
  for (const [nombre, generar] of Object.entries(SONIDOS)) {
    const archivo = path.join(DIR_RAW, `${nombre}.wav`);
    fs.writeFileSync(archivo, wav(generar()));
    console.log(`✓ ${path.relative(process.cwd(), archivo)} (${fs.statSync(archivo).size} bytes)`);
  }
}

module.exports = { SONIDOS, wav, MUESTREO };
