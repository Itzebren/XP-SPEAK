const { listarLecciones, obtenerEvaluacion } = require('./contenido');

/**
 * Ejercicios de la sesión de repaso SRS (RF-13). No hay contenido nuevo: se
 * reutilizan los ítems de las evaluaciones (§4.3), indexados por concepto_id.
 * Un par de "emparejar" se vuelve una opción múltiple con los demás pares como
 * distractores, porque en el repaso cada concepto se pregunta por separado.
 *
 * El id de cada ejercicio dice de dónde sale ("<lección>:<ítem>[:<par>]"), así
 * el servidor lo recalifica al recibir las respuestas sin guardar la sesión.
 */
const SEPARADOR = ':';

function parComoOpcionMultiple(item, indicePar, id) {
  const par = item.pares[indicePar];
  const opciones = [...new Set(item.pares.map((p) => p.der))].sort((a, b) => a.localeCompare(b, 'es'));
  return {
    id,
    tipo: 'opcion_multiple',
    concepto_id: par.concepto_id,
    enunciado: `¿Qué significa "${par.izq}"?`,
    opciones,
    respuesta_correcta: opciones.indexOf(par.der),
    feedback_error: `"${par.izq}" significa "${par.der}".`,
  };
}

function construirIndice(lecciones) {
  const porId = new Map(); // id del ejercicio → { ejercicio, leccion }
  const porConcepto = new Map(); // concepto_id → [id del ejercicio]
  const agregar = (ejercicio, leccion) => {
    porId.set(ejercicio.id, { ejercicio, leccion });
    if (!porConcepto.has(ejercicio.concepto_id)) porConcepto.set(ejercicio.concepto_id, []);
    porConcepto.get(ejercicio.concepto_id).push(ejercicio.id);
  };

  for (const leccion of lecciones) {
    for (const item of obtenerEvaluacion(leccion).items) {
      const id = `${leccion.id}${SEPARADOR}${item.id}`;
      if (item.tipo === 'emparejar') {
        item.pares.forEach((_, i) => agregar(parComoOpcionMultiple(item, i, `${id}${SEPARADOR}${i}`), leccion));
      } else {
        agregar({ ...item, id }, leccion);
      }
    }
  }
  return { porId, porConcepto };
}

let indice = null;
const obtenerIndice = () => (indice ??= construirIndice(listarLecciones()));

/**
 * Un ejercicio para el concepto, o null si ya no hay ninguno en el contenido.
 * Rota entre los disponibles según cuántas veces se ha visto el concepto,
 * para no preguntar siempre lo mismo.
 */
function ejercicioParaConcepto(concepto) {
  const ids = obtenerIndice().porConcepto.get(concepto.concepto_id);
  if (!ids) return null;
  const vistas = (concepto.aciertos || 0) + (concepto.fallos || 0);
  return obtenerIndice().porId.get(ids[vistas % ids.length]).ejercicio;
}

/** { ejercicio, leccion } a partir del id que manda la app, o null si no existe. */
function resolverEjercicio(id) {
  return obtenerIndice().porId.get(id) || null;
}

module.exports = { ejercicioParaConcepto, resolverEjercicio, construirIndice };
