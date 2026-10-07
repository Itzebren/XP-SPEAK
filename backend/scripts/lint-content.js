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
const { leerMisionesDeDisco } = require('../lib/minijuegos/contenido');
const { normalizarTexto } = require('../lib/lecciones/calificador');

const RUTA_SCHEMA = path.join(__dirname, '..', 'content', 'schema', 'leccion.schema.json');
const RUTA_SCHEMA_MISION = path.join(__dirname, '..', 'content', 'schema', 'mision.schema.json');

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
  return fuentesMcerDeNivel(leccion.autoria?.fuentes ?? [], leccion.nivel_mcer);
}

function fuentesMcerDeNivel(fuentes, nivel) {
  const errores = [];
  if (!fuentes.some((f) => f.startsWith('MCER') && f.includes('§4.2'))) {
    errores.push('autoria.fuentes debe citar el tema del MCER §4.2');
  }
  if (!fuentes.some((f) => f.startsWith('MCER, ') && f.includes(`, ${nivel} (p.`))) {
    errores.push(`autoria.fuentes debe citar al menos un descriptor ${nivel} del MCER`);
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

// ── Reglas de las misiones (Misión Situacional, docs/minijuegos-diseno.md §5.2) ──

// Frases cortas: el usuario las lee (o escucha) y responde en segundos (RN-05).
const MAX_PALABRAS_FRASE = 14;
const contarPalabras = (texto) => texto.trim().split(/\s+/).length;

function nombreDeArchivoMision(mision, { archivo }) {
  return archivo === `${mision.id}.json` ? [] : [`el archivo debe llamarse ${mision.id}.json`];
}

/** El nivel del id, el del campo y el de la lección base coinciden (RN-03). */
function leccionBase(mision, { porId }) {
  const leccion = porId.get(mision.leccion_id);
  if (!leccion) return [`leccion_id '${mision.leccion_id}' no existe`];
  const errores = [];
  if (leccion.nivel_mcer !== mision.nivel) errores.push(`es ${mision.nivel} pero su lección es ${leccion.nivel_mcer}`);
  if (!mision.id.startsWith(`mision-${mision.nivel.toLowerCase()}-`)) errores.push(`el id debe empezar con mision-${mision.nivel.toLowerCase()}-`);
  return errores;
}

/** Cada paso tiene una sola respuesta correcta y las incorrectas explican por qué. */
function opcionesDePaso(mision) {
  return mision.pasos.flatMap((paso, i) => {
    const error = (msg) => `paso ${i + 1}: ${msg}`;
    const errores = [];
    const correctas = paso.opciones.filter((o) => o.correcta);
    if (correctas.length !== 1) errores.push(error(`debe tener exactamente una opción correcta (tiene ${correctas.length})`));
    if (hayRepetidos(paso.opciones.map((o) => normalizarTexto(o.en)))) errores.push(error('opciones repetidas'));
    for (const opcion of paso.opciones) {
      if (!opcion.correcta && !opcion.feedback) errores.push(error(`'${opcion.en}' es incorrecta y no tiene feedback`));
      if (!opcion.correcta && opcion.cumple) errores.push(error(`'${opcion.en}' es incorrecta y no puede cumplir un objetivo`));
      if (opcion.correcta && !opcion.concepto_id) errores.push(error('la opción correcta debe tener concepto_id (SRS)'));
    }
    for (const frase of [paso.npc, ...paso.opciones.map((o) => o.en)]) {
      if (contarPalabras(frase) > MAX_PALABRAS_FRASE) errores.push(error(`'${frase}' tiene más de ${MAX_PALABRAS_FRASE} palabras`));
    }
    return errores;
  });
}

/** Cada objetivo se cumple en exactamente un paso, y no se cumple nada que no exista. */
function objetivosCumplidos(mision) {
  const ids = mision.objetivos.map((o) => o.id);
  const errores = hayRepetidos(ids) ? ['ids de objetivo repetidos'] : [];
  const cumplidos = mision.pasos.flatMap((p) => p.opciones.filter((o) => o.correcta && o.cumple).map((o) => o.cumple));
  for (const id of ids) {
    const veces = cumplidos.filter((c) => c === id).length;
    if (veces !== 1) errores.push(`el objetivo '${id}' se cumple ${veces} veces (debe ser 1)`);
  }
  cumplidos.filter((c) => !ids.includes(c)).forEach((c) => errores.push(`'cumple: ${c}' no es un objetivo`));
  return errores;
}

/** Igual que la regla 6 de las lecciones: solo se practica lo que ya se enseñó. */
function conceptosDeMision(mision, { porId }) {
  const leccion = porId.get(mision.leccion_id);
  if (!leccion) return [];
  const ensenados = new Set(
    cadenaDePrerequisitos(leccion, porId).cadena.flatMap((l) => conceptosEnsenados(l).map((c) => c.concepto_id))
  );
  return mision.pasos.flatMap((paso, i) =>
    paso.opciones
      .filter((o) => o.concepto_id && !ensenados.has(o.concepto_id))
      .map((o) => `paso ${i + 1}: '${o.concepto_id}' no se enseña en ${mision.leccion_id} ni antes`)
  );
}

function fuentesMcerMision(mision) {
  return fuentesMcerDeNivel(mision.autoria?.fuentes ?? [], mision.nivel);
}

const REGLAS_MISION = [nombreDeArchivoMision, leccionBase, opcionesDePaso, objetivosCumplidos, conceptosDeMision, fuentesMcerMision];

/**
 * @param archivos [{ archivo, mision }] tal como vienen de disco
 * @param lecciones lecciones válidas, para revisar la lección base y sus conceptos
 */
function lintearMisiones(archivos, schema, lecciones) {
  const validar = new Ajv({ allErrors: true }).compile(schema);
  const errores = [];
  const validas = archivos.filter(({ archivo, mision }) => {
    if (validar(mision)) return true;
    validar.errors.forEach((e) => errores.push(`${archivo}: schema ${e.instancePath || '/'} ${e.message}`));
    return false;
  });

  const vistos = new Set();
  for (const { archivo, mision } of validas) {
    if (vistos.has(mision.id)) errores.push(`${archivo}: id de misión repetido '${mision.id}'`);
    vistos.add(mision.id);
  }

  const porId = new Map(lecciones.map((leccion) => [leccion.id, leccion]));
  for (const { archivo, mision } of validas) {
    for (const regla of REGLAS_MISION) {
      regla(mision, { archivo, porId }).forEach((error) => errores.push(`${archivo}: ${error}`));
    }
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
  const leerSchema = (ruta) => JSON.parse(fs.readFileSync(ruta, 'utf-8'));
  const archivos = leerLeccionesDeDisco();
  const misiones = leerMisionesDeDisco();
  const errores = [
    ...lintearContenido(archivos, leerSchema(RUTA_SCHEMA)),
    ...lintearMisiones(misiones, leerSchema(RUTA_SCHEMA_MISION), archivos.map(({ leccion }) => leccion)),
  ];
  if (errores.length > 0) {
    console.error(`✗ ${errores.length} error(es) de contenido:\n`);
    errores.forEach((e) => console.error(`  - ${e}`));
    process.exit(1);
  }
  console.log(`✓ ${archivos.length} lección(es) y ${misiones.length} misión(es) válidas.`);
}

module.exports = { lintearContenido, lintearMisiones, RUTA_SCHEMA, RUTA_SCHEMA_MISION };
