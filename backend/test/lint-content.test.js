const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const { lintearContenido, RUTA_SCHEMA } = require('../scripts/lint-content');
const { leerLeccionesDeDisco } = require('../lib/lecciones/contenido');

const schema = JSON.parse(fs.readFileSync(RUTA_SCHEMA, 'utf-8'));

function contenidoReal() {
  return leerLeccionesDeDisco();
}

/** Aplica `mutar` a una copia de las lecciones y la lintea. */
function lintearMutado(mutar) {
  const archivos = structuredClone(contenidoReal());
  mutar(Object.fromEntries(archivos.map((a) => [a.leccion.id, a.leccion])));
  return lintearContenido(archivos, schema);
}

const evaluacionDe = (leccion) => leccion.secciones.find((s) => s.tipo === 'evaluacion');

test('el contenido real pasa el linter', () => {
  assert.deepEqual(lintearContenido(contenidoReal(), schema), []);
});

test('detecta un archivo cuyo nombre no coincide con el id', () => {
  const archivos = structuredClone(contenidoReal());
  archivos[0].archivo = 'otro-nombre.json';
  assert.ok(lintearContenido(archivos, schema).some((e) => e.includes('el archivo debe llamarse')));
});

test('detecta respuesta_correcta fuera de rango', () => {
  const errores = lintearMutado((l) => {
    evaluacionDe(l['a1-saludos-presentaciones']).items[0].respuesta_correcta = 9;
  });
  assert.ok(errores.some((e) => e.includes('fuera de rango')));
});

test("detecta 'acepta' que no incluye la respuesta correcta", () => {
  const errores = lintearMutado((l) => {
    evaluacionDe(l['a1-saludos-presentaciones']).items[1].acepta = ['are'];
  });
  assert.ok(errores.some((e) => e.includes("'acepta' no incluye")));
});

test('detecta concepto_id que no sigue la convención §4.4 (vía schema)', () => {
  const errores = lintearMutado((l) => {
    evaluacionDe(l['a1-saludos-presentaciones']).items[0].concepto_id = 'voc.hello';
  });
  assert.ok(errores.some((e) => e.includes('schema')));
});

test('detecta evaluar un concepto que no se enseñó', () => {
  const errores = lintearMutado((l) => {
    evaluacionDe(l['a1-saludos-presentaciones']).items[0].concepto_id = 'voc.numeros.zero';
  });
  assert.ok(errores.some((e) => e.includes("evalúa 'voc.numeros.zero'")));
});

test('permite evaluar conceptos enseñados en una lección prerequisito (repaso en espiral)', () => {
  // La lección 2 evalúa gram.verb_to_be_present, que se enseña en la lección 1.
  const errores = lintearMutado(() => {});
  assert.deepEqual(errores, []);
});

test('detecta prerequisito inexistente y ciclos', () => {
  const inexistente = lintearMutado((l) => {
    l['a1-numeros-hora'].prerequisito_id = 'a1-no-existe';
  });
  assert.ok(inexistente.some((e) => e.includes("'a1-no-existe' no existe")));

  const ciclo = lintearMutado((l) => {
    l['a1-saludos-presentaciones'].prerequisito_id = 'a1-numeros-hora';
  });
  assert.ok(ciclo.some((e) => e.includes('ciclo de prerequisitos')));
});

test('detecta ids de ítem repetidos y orden repetido en el nivel', () => {
  const errores = lintearMutado((l) => {
    evaluacionDe(l['a1-saludos-presentaciones']).items[1].id = 'q1';
    l['a1-numeros-hora'].orden = 2;
  });
  assert.ok(errores.some((e) => e.includes('ids de ítem de evaluación repetidos')));
  assert.ok(errores.some((e) => e.includes('orden 2 repetido')));
});

test('detecta evaluación que no está al final', () => {
  const errores = lintearMutado((l) => {
    const secciones = l['a1-saludos-presentaciones'].secciones;
    secciones.push(secciones.splice(secciones.length - 1, 1)[0], { tipo: 'introduccion', titulo: 'x', cuerpo: 'y' });
  });
  assert.ok(errores.some((e) => e.includes('última sección')));
});

test('RN-03: exige citar el tema MCER §4.2 y un descriptor del nivel de la lección', () => {
  const sinTema = lintearMutado((l) => {
    l['a1-numeros-hora'].autoria.fuentes = l['a1-numeros-hora'].autoria.fuentes.filter((f) => !f.includes('§4.2'));
  });
  assert.ok(sinTema.some((e) => e.includes('a1-numeros-hora') && e.includes('§4.2')));

  const otroNivel = lintearMutado((l) => {
    const fuentes = l['a1-numeros-hora'].autoria.fuentes;
    l['a1-numeros-hora'].autoria.fuentes = fuentes.map((f) => f.replace(', A1 (p.', ', A2 (p.'));
  });
  assert.ok(otroNivel.some((e) => e.includes('descriptor A1 del MCER')));
});

test('la plantilla de docs/guia-autoria-lecciones.md pasa el linter', () => {
  const plantilla = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'docs', 'plantilla-leccion.json'), 'utf-8'));
  // Si algún día la lección de la plantilla existe de verdad, la plantilla la sustituye aquí.
  const archivos = contenidoReal().filter((a) => a.leccion.id !== plantilla.id);
  archivos.push({ archivo: `${plantilla.id}.json`, leccion: plantilla });
  assert.deepEqual(lintearContenido(archivos, schema), []);
});
