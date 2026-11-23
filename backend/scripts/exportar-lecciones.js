#!/usr/bin/env node
/**
 * Exporta las lecciones al APK (RNF-08 / RN-11):
 *
 *   npm run exportar:lecciones
 *
 * Lee content/lessons/*.json y escribe app/src/main/assets/lecciones/contenido.json
 * con el manifiesto (versiones, orden y prerrequisitos) y el contenido público de
 * cada lección, igual que lo entrega GET /api/lessons/:id. Así la app abre todas
 * las lecciones sin conexión desde la instalación. Córrelo después de cambiar una
 * lección; `npm test` falla si el contenido del APK quedó viejo.
 */
const fs = require('fs');
const path = require('path');
const { construirManifiesto, leerLeccionesDeDisco, leccionPublica } = require('../lib/lecciones/contenido');

const RUTA_CONTENIDO = path.join(__dirname, '..', '..', 'app', 'src', 'main', 'assets', 'lecciones', 'contenido.json');

function construirContenido(lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion)) {
  const manifiesto = construirManifiesto(lecciones);
  const orden = Object.values(manifiesto.niveles).flat().map((entrada) => entrada.id);
  const porId = new Map(lecciones.map((leccion) => [leccion.id, leccion]));
  return { ...manifiesto, lecciones: orden.map((id) => leccionPublica(porId.get(id))) };
}

const serializarContenido = (contenido) => `${JSON.stringify(contenido, null, 1)}\n`;

if (require.main === module) {
  const contenido = construirContenido();
  fs.mkdirSync(path.dirname(RUTA_CONTENIDO), { recursive: true });
  fs.writeFileSync(RUTA_CONTENIDO, serializarContenido(contenido));
  console.log(
    `✓ Lecciones ${contenido.version_contenido}: ${contenido.lecciones.length} lecciones → ${path.relative(process.cwd(), RUTA_CONTENIDO)}`
  );
}

module.exports = { RUTA_CONTENIDO, construirContenido, serializarContenido };
