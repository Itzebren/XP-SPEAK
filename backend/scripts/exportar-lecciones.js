#!/usr/bin/env node
/**
 * Empaqueta las lecciones en el APK (RNF-08 / RN-11, CU-06 paso 2):
 *
 *   npm run exportar:lecciones
 *
 * Lee content/lessons/*.json y escribe app/src/main/assets/lecciones/lecciones.json con el
 * manifiesto y el contenido público de cada lección (sin la autoría, igual que
 * GET /api/lessons/:id). Así la app abre y califica las lecciones sin haberse conectado
 * nunca; el manifiesto del servidor solo se usa para bajar lo que cambió después.
 * Córrelo después de cambiar una lección; `npm test` falla si el APK quedó viejo.
 */
const fs = require('fs');
const path = require('path');
const { leerLeccionesDeDisco, construirManifiesto, leccionPublica } = require('../lib/lecciones/contenido');

const RUTA_LECCIONES = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'lecciones', 'lecciones.json');

function construirPaquete() {
  const lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion);
  const { version_contenido: versionContenido, niveles } = construirManifiesto(lecciones);
  return { version_contenido: versionContenido, niveles, lecciones: lecciones.map(leccionPublica) };
}

const serializarPaquete = (paquete) => `${JSON.stringify(paquete)}\n`;

if (require.main === module) {
  const paquete = construirPaquete();
  fs.mkdirSync(path.dirname(RUTA_LECCIONES), { recursive: true });
  fs.writeFileSync(RUTA_LECCIONES, serializarPaquete(paquete));
  console.log(
    `✓ Lecciones ${paquete.version_contenido}: ${paquete.lecciones.length} → ${path.relative(process.cwd(), RUTA_LECCIONES)}`
  );
}

module.exports = { RUTA_LECCIONES, construirPaquete, serializarPaquete };
