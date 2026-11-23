const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

/**
 * Contenido curricular (RF-10): archivos JSON versionados en git, no BD
 * (docs/lecciones-diseno.md §3.1). Se leen una vez por instancia serverless
 * y se quedan en memoria, porque solo cambian con un nuevo deploy.
 *
 * Para agregar una lección basta con crear content/lessons/<id>.json:
 * el manifiesto, el índice de conceptos y los desbloqueos se derivan de ahí.
 */
const DIR_LECCIONES = path.join(__dirname, '..', '..', 'content', 'lessons');
const NIVELES = ['A1', 'A2'];

function leerLeccionesDeDisco(dir = DIR_LECCIONES) {
  return fs
    .readdirSync(dir)
    .filter((archivo) => archivo.endsWith('.json'))
    .sort()
    .map((archivo) => ({
      archivo,
      leccion: JSON.parse(fs.readFileSync(path.join(dir, archivo), 'utf-8')),
    }));
}

function tipoDeConcepto(conceptoId) {
  if (conceptoId.startsWith('voc.')) return 'vocab';
  if (conceptoId.startsWith('gram.')) return 'gramatica';
  return 'funcion';
}

/** Conceptos que una lección enseña: su vocabulario y sus reglas gramaticales. */
function conceptosEnsenados(leccion) {
  return leccion.secciones.flatMap((seccion) => {
    if (seccion.tipo === 'vocabulario') {
      return seccion.items.map((item) => ({ concepto_id: item.concepto_id, en: item.en, es: item.es }));
    }
    if (seccion.tipo === 'gramatica') {
      return [{ concepto_id: seccion.concepto_id, titulo: seccion.titulo }];
    }
    return [];
  });
}

/**
 * Manifiesto de versiones (§5.1): lo que Android consulta para saber qué
 * lecciones cachear o refrescar. `version_contenido` cambia si cambia la
 * versión de cualquier lección, así el cliente compara un solo valor.
 */
function construirManifiesto(lecciones) {
  const ordenadas = [...lecciones].sort(
    (a, b) => a.nivel_mcer.localeCompare(b.nivel_mcer) || a.orden - b.orden || a.id.localeCompare(b.id)
  );

  const niveles = Object.fromEntries(NIVELES.map((nivel) => [nivel, []]));
  for (const leccion of ordenadas) {
    niveles[leccion.nivel_mcer].push({
      id: leccion.id,
      version: leccion.version,
      orden: leccion.orden,
      prerequisito_id: leccion.prerequisito_id,
      titulo: leccion.modulo_tematico,
      xp_recompensa: leccion.xp_recompensa,
    });
  }

  const huella = ordenadas.map((l) => `${l.id}@${l.version}`).join('|');
  const versionContenido = crypto.createHash('sha256').update(huella).digest('hex').slice(0, 12);

  return { version_contenido: versionContenido, niveles };
}

/**
 * Índice concepto_id → cómo se presenta (para mostrar los conceptos débiles
 * del SRS con texto, no solo con su id) y en qué lecciones aparece.
 */
function construirIndiceConceptos(lecciones) {
  const indice = new Map();
  for (const leccion of lecciones) {
    for (const { concepto_id: id, ...texto } of conceptosEnsenados(leccion)) {
      if (!indice.has(id)) {
        indice.set(id, { concepto_id: id, tipo: tipoDeConcepto(id), nivel_mcer: leccion.nivel_mcer, ...texto, lecciones: [] });
      }
      const lecciones = indice.get(id).lecciones;
      if (!lecciones.includes(leccion.id)) lecciones.push(leccion.id);
    }
  }
  return indice;
}

/** prerequisito_id → id de la (primera) lección que se desbloquea al aprobarlo. */
function construirDesbloqueos(lecciones) {
  const desbloqueos = new Map();
  for (const leccion of lecciones) {
    if (leccion.prerequisito_id && !desbloqueos.has(leccion.prerequisito_id)) {
      desbloqueos.set(leccion.prerequisito_id, leccion.id);
    }
  }
  return desbloqueos;
}

let cache = null;

function cargar() {
  if (!cache) {
    const lecciones = leerLeccionesDeDisco().map(({ leccion }) => leccion);
    cache = {
      porId: new Map(lecciones.map((l) => [l.id, l])),
      manifiesto: construirManifiesto(lecciones),
      conceptos: construirIndiceConceptos(lecciones),
      desbloqueos: construirDesbloqueos(lecciones),
    };
  }
  return cache;
}

const obtenerLeccion = (id) => cargar().porId.get(id) || null;
const listarLecciones = () => [...cargar().porId.values()];
const obtenerManifiesto = () => cargar().manifiesto;
const listarLeccionesDeNivel = (nivel) => cargar().manifiesto.niveles[nivel] || [];
const obtenerConcepto = (conceptoId) => cargar().conceptos.get(conceptoId) || null;
const obtenerEvaluacion = (leccion) => leccion.secciones.find((s) => s.tipo === 'evaluacion');

/** Primera lección que se desbloquea al aprobar `id` (o null si es la última). */
function siguienteLeccion(id) {
  return cargar().desbloqueos.get(id) ?? null;
}

/** Lo que se manda a la app: todo menos los campos internos de autoría (§8.5). */
function leccionPublica(leccion) {
  const { autoria, ...publica } = leccion;
  return publica;
}

module.exports = {
  NIVELES,
  leerLeccionesDeDisco,
  tipoDeConcepto,
  conceptosEnsenados,
  construirManifiesto,
  obtenerLeccion,
  listarLecciones,
  obtenerManifiesto,
  listarLeccionesDeNivel,
  obtenerConcepto,
  obtenerEvaluacion,
  siguienteLeccion,
  leccionPublica,
};
