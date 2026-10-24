const fs = require('fs');
const path = require('path');
const crypto = require('crypto');
const { leerLeccionesDeDisco } = require('../lecciones/contenido');

/**
 * Contenido de los minijuegos (docs/minijuegos-diseno.md §3). Casi todo sale
 * de las lecciones, que ya están revisadas contra el MCER (RN-03); lo único
 * propio de los juegos son los guiones de Misión Situacional.
 *
 * El banco se exporta a un JSON dentro del APK (RNF-08 / RN-11), así los
 * juegos funcionan sin conexión. Ver scripts/exportar-banco-minijuegos.js.
 */
const DIR_MISIONES = path.join(__dirname, '..', '..', 'content', 'minijuegos', 'misiones');
const VERSION_BANCO = 1;

function leerMisionesDeDisco(dir = DIR_MISIONES) {
  return fs
    .readdirSync(dir)
    .filter((archivo) => archivo.endsWith('.json'))
    .sort()
    .map((archivo) => ({
      archivo,
      mision: JSON.parse(fs.readFileSync(path.join(dir, archivo), 'utf-8')),
    }));
}

const seccionesDe = (leccion, tipo) => leccion.secciones.filter((s) => s.tipo === tipo);

/**
 * Los ejemplos de gramática a veces traen pregunta y respuesta juntas
 * ("Do you want some rice? — Yes, I do."): para los juegos son dos oraciones.
 */
function separarEjemplo(ejemplo) {
  return ejemplo
    .split(/\s+[—–]\s+/)
    .map((parte) => parte.trim())
    .filter(Boolean);
}

/** Lo que los juegos necesitan de una lección (§3.1), sin la clave de respuestas. */
function leccionParaJuegos(leccion) {
  return {
    id: leccion.id,
    nivel: leccion.nivel_mcer,
    orden: leccion.orden,
    titulo: leccion.modulo_tematico,
    vocabulario: seccionesDe(leccion, 'vocabulario').flatMap((s) =>
      s.items.map((item) => ({ concepto_id: item.concepto_id, en: item.en, es: item.es }))
    ),
    oraciones: seccionesDe(leccion, 'gramatica').flatMap((s) =>
      s.ejemplos.flatMap(separarEjemplo).map((en) => ({ concepto_id: s.concepto_id, tema: s.titulo, en }))
    ),
    dialogo: seccionesDe(leccion, 'dialogo').flatMap((s) =>
      s.lineas.map((linea) => ({ hablante: linea.hablante, en: linea.en, es: linea.es }))
    ),
  };
}

/** La autoría es interna (igual que en las lecciones, §8.5). */
function misionPublica(mision) {
  const { autoria, ...publica } = mision;
  return publica;
}

/**
 * Banco que se empaqueta en la app. Determinista (mismo contenido → mismo
 * JSON), para que la prueba que lo compara con el archivo del APK sea estable.
 */
function construirBanco(lecciones, misiones) {
  const contenido = {
    lecciones: [...lecciones]
      .sort((a, b) => a.nivel_mcer.localeCompare(b.nivel_mcer) || a.orden - b.orden)
      .map(leccionParaJuegos),
    misiones: [...misiones].sort((a, b) => a.id.localeCompare(b.id)).map(misionPublica),
  };
  const huella = crypto.createHash('sha256').update(JSON.stringify(contenido)).digest('hex').slice(0, 12);
  return { version: VERSION_BANCO, version_contenido: huella, ...contenido };
}

function construirBancoDesdeDisco() {
  return construirBanco(
    leerLeccionesDeDisco().map(({ leccion }) => leccion),
    leerMisionesDeDisco().map(({ mision }) => mision)
  );
}

module.exports = {
  DIR_MISIONES,
  leerMisionesDeDisco,
  separarEjemplo,
  construirBanco,
  construirBancoDesdeDisco,
};
