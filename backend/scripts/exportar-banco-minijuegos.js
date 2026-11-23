#!/usr/bin/env node
/**
 * Exporta el banco de contenido de los minijuegos al APK
 * (docs/minijuegos-diseno.md §3.2, RNF-08 / RN-11):
 *
 *   npm run exportar:minijuegos
 *
 * Lee content/lessons/*.json y content/minijuegos/misiones/*.json y escribe
 * app/src/main/assets/minijuegos/banco.json. Córrelo después de cambiar una
 * lección o una misión; `npm test` falla si el banco del APK quedó viejo.
 */
const fs = require('fs');
const path = require('path');
const { construirBancoDesdeDisco } = require('../lib/minijuegos/contenido');

const RUTA_BANCO = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'minijuegos', 'banco.json');

const serializarBanco = (banco) => `${JSON.stringify(banco, null, 1)}\n`;

if (require.main === module) {
  const banco = construirBancoDesdeDisco();
  fs.mkdirSync(path.dirname(RUTA_BANCO), { recursive: true });
  fs.writeFileSync(RUTA_BANCO, serializarBanco(banco));
  console.log(
    `✓ Banco ${banco.version_contenido}: ${banco.lecciones.length} lecciones y ${banco.misiones.length} misiones → ${path.relative(process.cwd(), RUTA_BANCO)}`
  );
}

module.exports = { RUTA_BANCO, serializarBanco };
