#!/usr/bin/env node
/**
 * Linter de contenido (docs/lecciones-diseno.md §4.5). Red de seguridad para
 * el contenido curado a mano: valida el JSON Schema y las reglas semánticas
 * que un schema no puede expresar. Uso: npm run lint:content
 *
 * Cada regla es una función (leccion, contexto) → lista de errores. Para
 * agregar una regla nueva basta con escribirla y sumarla a REGLAS.
 */
const fs = require('fs');
const path = require('path');
const Ajv = require('ajv');
const { leerLeccionesDeDisco, conceptosEnsenados } = require('../lib/lecciones/contenido');
const { normalizarTexto } = require('../lib/lecciones/calificador');

const RUTA_SCHEMA = path.join(__dirname, '..', 'content', 'schema', 'leccion.schema.json');

const hayRepetidos = (valores) => new Set(valores).size !== valores.length;
const evaluacionDe = (leccion) => leccion.secciones.find((s) => s.tipo === 'evaluacion');

/** La lección y sus prerequisitos, en orden; `ciclo` indica si la cadena se muerde la cola. */
function cadenaDePrerequisitos(leccion, porId) {
  const cadena = [];
  const vistas = new Set();
  let actual = leccion;
  while (actual) {
    if (vistas.has(actual.id)) return { cadena, ciclo: actual.id };
    vistas.add(actual.id);
    cadena.push(actual);
    actual = actual.prerequisito_id ? porId.get(actual.prerequisito_id) : null;
  }
  return { cadena, ciclo: null };
}

// ── Reglas ──────────────────────────────────────────────────────────────────

function nombreDeArchivo(leccion, { archivo }) {
  return archivo === `${leccion.id}.json` ? [] : [`el archivo debe llamarse ${leccion.id}.json`];
}

/** La teoría va primero y la evaluación al final (CU-06 paso 5). */
function estructura(leccion) {
  const errores = [];
  const evaluaciones = leccion.secciones.filter((s) => s.tipo === 'evaluacion');
  if (evaluaciones.length !== 1) errores.push('debe tener exactamente una sección de evaluación');
  else if (leccion.secciones.at(-1).tipo !== 'evaluacion') errores.push('la evaluación debe ser la última sección');
  if (leccion.secciones[0].tipo !== 'introduccion') errores.push('la primera sección debe ser la introducción');
  return errores;
}

/** Regla 4: el prerequisito existe y no hay ciclos. */
function prerequisito(leccion, { porId }) {
  if (leccion.prerequisito_id === null) return [];
  if (!porId.has(leccion.prerequisito_id)) return [`prerequisito_id '${leccion.prerequisito_id}' no existe`];
  const { ciclo } = cadenaDePrerequisitos(leccion, porId);
  return ciclo ? [`ciclo de prerequisitos que pasa por '${ciclo}'`] : [];
}

/** Reglas 1 y 2: ids de ítem únicos y clave de respuesta coherente con su tipo. */
function clavesDeRespuesta(leccion) {
  const evaluacion = evaluacionDe(leccion);
  if (!evaluacion) return [];

  const errores = hayRepetidos(evaluacion.items.map((i) => i.id)) ? ['ids de ítem de evaluación repetidos'] : [];
  for (const item of evaluacion.items) {
    const error = (msg) => errores.push(`${item.id}: ${msg}`);
    if (item.tipo === 'opcion_multiple') {
      if (item.respuesta_correcta >= item.opciones.length) error(`respuesta_correcta (${item.respuesta_correcta}) fuera de rango`);
      if (hayRepetidos(item.opciones.map(normalizarTexto))) error('opciones repetidas');
    }
    if (item.tipo === 'completar' && item.acepta) {
      if (!item.acepta.map(normalizarTexto).includes(normalizarTexto(item.respuesta_correcta))) {
        error("'acepta' no incluye la respuesta_correcta");
      }
    }
    if (item.tipo === 'emparejar') {
      if (hayRepetidos(item.pares.map((p) => normalizarTexto(p.izq)))) error("'izq' repetido");
      if (hayRepetidos(item.pares.map((p) => normalizarTexto(p.der)))) error("'der' repetido");
    }
  }
  return errores;
}

/** Regla 6: no se evalúa lo que no se enseñó (en la lección o en sus prerequisitos). */
function conceptosEnsenadosAntes(leccion, { porId }) {
  const evaluacion = evaluacionDe(leccion);
  if (!evaluacion) return [];

  const ensenados = new Set(
    cadenaDePrerequisitos(leccion, porId).cadena.flatMap((l) => conceptosEnsenados(l).map((c) => c.concepto_id))
  );
  return evaluacion.items.flatMap((item) => {
    const evaluados = item.tipo === 'emparejar' ? item.pares.map((p) => p.concepto_id) : [item.concepto_id];
    return evaluados.filter((c) => !ensenados.has(c)).map((c) => `${item.id} evalúa '${c}', que no se enseñó antes`);
  });
}

/**
 * RN-03: el MCER es la fuente del contenido. `autoria.fuentes` debe citar el
 * tema de §4.2 y al menos un descriptor del nivel de la lección
 * (formato en docs/guia-autoria-lecciones.md §8.3).
 */
function fuentesMcer(leccion) {
  const fuentes = leccion.autoria?.fuentes ?? [];
  const errores = [];
  if (!fuentes.some((f) => f.startsWith('MCER') && f.includes('§4.2'))) {
    errores.push('autoria.fuentes debe citar el tema del MCER §4.2');
  }
  if (!fuentes.some((f) => f.startsWith('MCER, ') && f.includes(`, ${leccion.nivel_mcer} (p.`))) {
    errores.push(`autoria.fuentes debe citar al menos un descriptor ${leccion.nivel_mcer} del MCER`);
  }
  return errores;
}

const REGLAS = [nombreDeArchivo, estructura, prerequisito, clavesDeRespuesta, conceptosEnsenadosAntes, fuentesMcer];

/** Reglas que comparan lecciones entre sí: ids únicos y un solo `orden` por nivel. */
function reglasGlobales(validas) {
  const errores = [];
  const vistos = new Set();
  for (const { archivo, leccion } of validas) {
    if (vistos.has(leccion.id)) errores.push(`${archivo}: id de lección repetido '${leccion.id}'`);
    vistos.add(leccion.id);
  }
  const ordenes = new Set();
  for (const { archivo, leccion } of validas) {
    const clave = `${leccion.nivel_mcer}#${leccion.orden}`;
    if (ordenes.has(clave)) errores.push(`${archivo}: orden ${leccion.orden} repetido en ${leccion.nivel_mcer}`);
    ordenes.add(clave);
  }
  return errores;
}

// ── Ejecución ───────────────────────────────────────────────────────────────

/**
 * @param archivos [{ archivo, leccion }] tal como vienen de disco
 * @returns lista de errores legibles; vacía si todo está bien
 */
function lintearContenido(archivos, schema) {
  const validar = new Ajv({ allErrors: true }).compile(schema);
  const errores = [];

  // Las reglas semánticas asumen un JSON con la forma correcta: primero el schema.
  const validas = archivos.filter(({ archivo, leccion }) => {
    if (validar(leccion)) return true;
    validar.errors.forEach((e) => errores.push(`${archivo}: schema ${e.instancePath || '/'} ${e.message}`));
    return false;
  });

  errores.push(...reglasGlobales(validas));

  const porId = new Map(validas.map(({ leccion }) => [leccion.id, leccion]));
  for (const { archivo, leccion } of validas) {
    for (const regla of REGLAS) {
      regla(leccion, { archivo, porId }).forEach((error) => errores.push(`${archivo}: ${error}`));
    }
  }
  return errores;
}

if (require.main === module) {
  const archivos = leerLeccionesDeDisco();
  const errores = lintearContenido(archivos, JSON.parse(fs.readFileSync(RUTA_SCHEMA, 'utf-8')));
  if (errores.length > 0) {
    console.error(`✗ ${errores.length} error(es) de contenido:\n`);
    errores.forEach((e) => console.error(`  - ${e}`));
    process.exit(1);
  }
  console.log(`✓ ${archivos.length} lección(es) válidas.`);
}

module.exports = { lintearContenido, RUTA_SCHEMA };
