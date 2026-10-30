/**
 * Calificación de la evaluación corta (RF-12, RN-06). Es una función pura:
 * recibe la sección de evaluación del JSON y las respuestas del usuario, y
 * no toca Firestore. Android aplica la misma lógica offline; la del servidor
 * es la autoritativa para XP y desbloqueo (docs/lecciones-diseno.md §6.2).
 */

// Normaliza respuestas de texto libre: minúsculas, sin espacios de sobra,
// apóstrofo tipográfico → recto y sin signo final ("Is." cuenta como "is").
function normalizarTexto(texto) {
  return String(texto)
    .normalize('NFC')
    .toLowerCase()
    .replace(/[‘’ʼ]/g, "'")
    .replace(/\s+/g, ' ')
    .trim()
    .replace(/[.!?¡¿]+$/u, '')
    .trim();
}

function calificarOpcionMultiple(item, valor) {
  const correcta = Number.isInteger(valor) && valor === item.respuesta_correcta;
  return {
    correcta,
    senales: [{ concepto_id: item.concepto_id, acierto: correcta }],
    respuestaCorrecta: item.respuesta_correcta,
  };
}

function calificarCompletar(item, valor) {
  const aceptadas = new Set([item.respuesta_correcta, ...(item.acepta || [])].map(normalizarTexto));
  const correcta = typeof valor === 'string' && aceptadas.has(normalizarTexto(valor));
  return {
    correcta,
    senales: [{ concepto_id: item.concepto_id, acierto: correcta }],
    respuestaCorrecta: item.respuesta_correcta,
  };
}

/**
 * Emparejar: el ítem cuenta como correcto solo si TODOS los pares están bien
 * y no sobra ninguno (§4.3). Para el SRS la señal es por par, porque cada par
 * es un concepto distinto y así sabemos exactamente cuál se confundió.
 */
function calificarEmparejar(item, valor) {
  const elegidos = new Map();
  let formatoValido = Array.isArray(valor);
  if (formatoValido) {
    for (const par of valor) {
      if (!Array.isArray(par) || par.length !== 2 || typeof par[0] !== 'string' || typeof par[1] !== 'string') {
        formatoValido = false;
        break;
      }
      elegidos.set(normalizarTexto(par[0]), normalizarTexto(par[1]));
    }
  }

  const senales = item.pares.map((par) => ({
    concepto_id: par.concepto_id,
    acierto: formatoValido && elegidos.get(normalizarTexto(par.izq)) === normalizarTexto(par.der),
  }));
  const correcta =
    formatoValido && valor.length === item.pares.length && senales.every((senal) => senal.acierto);

  return {
    correcta,
    senales,
    respuestaCorrecta: item.pares.map((par) => [par.izq, par.der]),
  };
}

const CALIFICADORES = {
  opcion_multiple: calificarOpcionMultiple,
  completar: calificarCompletar,
  emparejar: calificarEmparejar,
};

/**
 * @param evaluacion sección { tipo: 'evaluacion', umbral_aprobacion, items }
 * @param respuestas [{ id, valor }] — un ítem sin respuesta cuenta como incorrecto
 */
function calificarIntento(evaluacion, respuestas) {
  const porId = new Map(respuestas.map((r) => [r.id, r.valor]));
  const feedback = [];
  const conceptos = new Map(); // concepto_id → { aciertos, fallos }
  let correctas = 0;

  for (const item of evaluacion.items) {
    const resultado = CALIFICADORES[item.tipo](item, porId.get(item.id));
    if (resultado.correcta) correctas++;

    const entrada = { id: item.id, correcta: resultado.correcta };
    if (!resultado.correcta) {
      // CU-06 A1: resaltar el error, mostrar la respuesta correcta y una nota breve.
      entrada.mensaje = item.feedback_error;
      entrada.respuesta_correcta = resultado.respuestaCorrecta;
    }
    feedback.push(entrada);

    // Un ítem sin responder cuenta como error para el puntaje, pero no se sabe
    // si el concepto es débil: no se manda señal al SRS para no ensuciarlo.
    if (!porId.has(item.id)) continue;
    for (const senal of resultado.senales) {
      const acumulado = conceptos.get(senal.concepto_id) || { aciertos: 0, fallos: 0 };
      if (senal.acierto) acumulado.aciertos++;
      else acumulado.fallos++;
      conceptos.set(senal.concepto_id, acumulado);
    }
  }

  const total = evaluacion.items.length;
  const puntaje = total === 0 ? 0 : correctas / total;
  // Tolerancia para que 7/10 contra 0.70 no falle por redondeo de punto flotante.
  const aprobada = puntaje >= evaluacion.umbral_aprobacion - 1e-9;

  return {
    correctas,
    total,
    puntaje: Math.round(puntaje * 10000) / 10000,
    umbral: evaluacion.umbral_aprobacion,
    aprobada,
    feedback,
    conceptos: [...conceptos.entries()].map(([concepto_id, conteo]) => ({ concepto_id, ...conteo })),
    conceptos_debiles: [...conceptos.entries()].filter(([, c]) => c.fallos > 0).map(([id]) => id),
  };
}

module.exports = { calificarIntento, normalizarTexto };
