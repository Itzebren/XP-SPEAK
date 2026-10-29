#!/usr/bin/env node
/**
 * Exporta el catálogo de insignias al APK (docs/progreso-diseno.md Decisión 5,
 * RN-11 / RNF-08):
 *
 *   npm run exportar:insignias
 *
 * Escribe app/src/main/assets/progreso/insignias.json a partir de CATALOGO
 * (lib/progreso/insignias.js). La app lo usa para dibujar la cuadrícula sin
 * red ni caché; quién obtiene cada insignia lo sigue decidiendo el servidor.
 * `npm test` falla si el archivo del APK quedó viejo.
 */
const fs = require('fs');
const path = require('path');
const { CATALOGO, insigniaPublica } = require('../lib/progreso/insignias');

const RUTA_INSIGNIAS = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'progreso', 'insignias.json');

const serializarInsignias = () => `${JSON.stringify({ insignias: CATALOGO.map(insigniaPublica) }, null, 1)}\n`;

if (require.main === module) {
  fs.mkdirSync(path.dirname(RUTA_INSIGNIAS), { recursive: true });
  fs.writeFileSync(RUTA_INSIGNIAS, serializarInsignias());
  console.log(`✓ ${CATALOGO.length} insignias → ${path.relative(process.cwd(), RUTA_INSIGNIAS)}`);
}

module.exports = { RUTA_INSIGNIAS, serializarInsignias };
