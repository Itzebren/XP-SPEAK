/**
 * Algoritmo SM-2 (RF-13, Trabajo Terminal §2.6.2): programa cuándo repasar
 * cada concepto. Si se acierta, el intervalo crece (1 día, 6 días y luego
 * × facilidad); si se falla, vuelve a 1 día y la facilidad baja, así los
 * conceptos difíciles aparecen más seguido. Función pura, sin Firestore.
 *
 * Las respuestas se califican solas (no hay botones "fácil/difícil"), así que
 * la calidad de SM-2 (0–5) se deriva del resultado: acierto = 4 ("bueno"),
 * fallo = 1.
 */
const DIA_MS = 24 * 60 * 60 * 1000;
const FACILIDAD_INICIAL = 2.5;
const FACILIDAD_MINIMA = 1.3;
const CALIDAD_ACIERTO = 4;
const CALIDAD_FALLO = 1;

/**
 * @param previo { intervalo, ease, repeticiones } guardados del concepto (o null si es nuevo)
 * @param calidad 0–5; ≥ 3 cuenta como recordado
 * @returns { intervalo (días), ease, repeticiones, proxima_revision (ms epoch) }
 */
function aplicarSm2(previo, calidad, ahora) {
  const ease = previo?.ease ?? FACILIDAD_INICIAL;
  const repeticiones = previo?.repeticiones ?? 0;
  const intervaloPrevio = previo?.intervalo ?? 0;

  let intervalo;
  let nuevasRepeticiones;
  if (calidad >= 3) {
    if (repeticiones === 0) intervalo = 1;
    else if (repeticiones === 1) intervalo = 6;
    else intervalo = Math.round(intervaloPrevio * ease);
    nuevasRepeticiones = repeticiones + 1;
  } else {
    intervalo = 1;
    nuevasRepeticiones = 0;
  }

  const diferencia = 5 - calidad;
  const nuevaEase = Math.max(FACILIDAD_MINIMA, ease + 0.1 - diferencia * (0.08 + diferencia * 0.02));

  return {
    intervalo,
    ease: Math.round(nuevaEase * 100) / 100,
    repeticiones: nuevasRepeticiones,
    proxima_revision: ahora + intervalo * DIA_MS,
  };
}

/** Calidad de un intento: basta un fallo del concepto para contarlo como no recordado. */
function calidadDeSenal({ fallos }) {
  return fallos > 0 ? CALIDAD_FALLO : CALIDAD_ACIERTO;
}

module.exports = { DIA_MS, FACILIDAD_INICIAL, FACILIDAD_MINIMA, aplicarSm2, calidadDeSenal };
