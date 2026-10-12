const { obtenerConcepto, tipoDeConcepto } = require('./contenido');
const { aplicarSm2, calidadDeSenal } = require('./sm2');

/**
 * Lógica del SRS (RF-13 / RN-07), sin Firestore. Cada señal (de una lección o
 * de una sesión de repaso) acumula aciertos/fallos y reprograma el concepto
 * con SM-2 (sm2.js).
 */
const UMBRAL_TASA_ERROR = 0.3;
const LIMITE_SESION = 10;
const LIMITE_POR_DEFECTO = 20;
const LIMITE_MAXIMO = 50;

/** Nuevo estado del concepto tras las señales de un intento. */
function acumularSenal(previo, { uid, concepto_id, aciertos, fallos }, leccion, ahora) {
  const totalAciertos = (previo?.aciertos || 0) + aciertos;
  const totalFallos = (previo?.fallos || 0) + fallos;
  // Acertar antes de que toque (p. ej. repetir la lección el mismo día) no
  // alarga el intervalo; fallar sí lo reinicia siempre.
  const antesDeTiempo = previo?.proxima_revision != null && previo.proxima_revision > ahora;
  const programacion =
    antesDeTiempo && fallos === 0
      ? { intervalo: previo.intervalo, ease: previo.ease, repeticiones: previo.repeticiones, proxima_revision: previo.proxima_revision }
      : aplicarSm2(previo, calidadDeSenal({ fallos }), ahora);
  return {
    uid,
    concepto_id,
    nivel_mcer: previo?.nivel_mcer || leccion.nivel_mcer,
    tipo: tipoDeConcepto(concepto_id),
    aciertos: totalAciertos,
    fallos: totalFallos,
    tasa_error: Math.round((totalFallos / (totalAciertos + totalFallos)) * 10000) / 10000,
    ultima_vista: ahora,
    ultima_leccion_id: leccion.id,
    ...programacion,
  };
}

/**
 * Conceptos que toca repasar hoy (RF-13). Primero los de mayor tasa de error
 * (RN-07); a igualdad, el que lleva más tiempo vencido. Un concepto guardado
 * antes de SM-2 (sin proxima_revision) cuenta como vencido.
 */
function seleccionarParaRepaso(conceptos, { nivel = null, ahora, limite = LIMITE_SESION }) {
  const delNivel = conceptos.filter((c) => !nivel || c.nivel_mcer === nivel);
  const vencidos = delNivel
    .filter((c) => (c.proxima_revision ?? 0) <= ahora)
    .sort(
      (a, b) =>
        b.tasa_error - a.tasa_error || (a.proxima_revision ?? 0) - (b.proxima_revision ?? 0) || a.concepto_id.localeCompare(b.concepto_id)
    );
  const futuras = delNivel.map((c) => c.proxima_revision).filter((fecha) => fecha > ahora);

  return {
    vencidos: vencidos.slice(0, limite),
    total_vencidos: vencidos.length,
    // Para decirle al usuario cuándo vuelve a haber repaso si hoy no toca nada.
    proxima_revision: futuras.length ? Math.min(...futuras) : null,
  };
}

/**
 * Un concepto es "débil" si ya se falló y su tasa de error es alta (RN-07).
 * La lista se muestra en el catálogo; la sesión de repaso usa SM-2.
 */
function seleccionarConceptosDebiles(conceptos, { nivel = null, limite = LIMITE_POR_DEFECTO } = {}) {
  return conceptos
    .filter((c) => c.fallos > 0 && c.tasa_error >= UMBRAL_TASA_ERROR)
    .filter((c) => !nivel || c.nivel_mcer === nivel)
    .sort((a, b) => b.tasa_error - a.tasa_error || b.fallos - a.fallos || b.ultima_vista - a.ultima_vista)
    .slice(0, limite)
    .map(conTextoDelContenido);
}

function conTextoDelContenido(concepto) {
  const contenido = obtenerConcepto(concepto.concepto_id);
  return {
    concepto_id: concepto.concepto_id,
    tipo: concepto.tipo,
    nivel_mcer: concepto.nivel_mcer,
    aciertos: concepto.aciertos,
    fallos: concepto.fallos,
    tasa_error: concepto.tasa_error,
    ultima_vista: concepto.ultima_vista,
    // Texto para mostrarlo en la app (null si el concepto ya no está en el contenido).
    en: contenido?.en ?? null,
    es: contenido?.es ?? null,
    titulo: contenido?.titulo ?? null,
    lecciones: contenido?.lecciones ?? [],
  };
}

module.exports = {
  LIMITE_POR_DEFECTO,
  LIMITE_MAXIMO,
  LIMITE_SESION,
  acumularSenal,
  seleccionarConceptosDebiles,
  seleccionarParaRepaso,
  conTextoDelContenido,
};
