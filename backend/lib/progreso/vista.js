const { listarLeccionesDeNivel } = require('../lecciones/contenido');
const { CATALOGO, evaluar, insigniaPublica } = require('./insignias');
const { nivelPorXp } = require('./niveles');
const { rachaVigente } = require('./racha');

/**
 * Lo que responde GET /api/progress (CU-08, docs/progreso-diseno.md Fase 2)
 * a partir del resumen, el progreso de lecciones y el SRS. Función pura.
 */
const MIN_RESPUESTAS_HABILIDAD = 5;

// Cada habilidad suma la precisión de un tipo de concepto del SRS y la de un minijuego.
const HABILIDADES = [
  { id: 'vocabulario', nombre: 'Vocabulario', tipoSrs: 'vocab', juego: 'rafaga-palabras', sugerencia: 'Juega Ráfaga de Palabras' },
  { id: 'gramatica', nombre: 'Gramática', tipoSrs: 'gramatica', juego: 'orden-maestro', sugerencia: 'Juega Orden Maestro' },
  { id: 'pronunciacion', nombre: 'Pronunciación', tipoSrs: null, juego: 'eco-vocal', sugerencia: 'Juega Eco Vocal' },
  { id: 'comunicacion', nombre: 'Comunicación', tipoSrs: 'funcion', juego: 'mision-situacional', sugerencia: 'Juega Misión Situacional' },
];

const porcentaje = (parte, total) => (total > 0 ? Math.round((parte / total) * 100) : 0);

function calcularHabilidades(resumen, conceptos) {
  return HABILIDADES.map(({ id, nombre, tipoSrs, juego, sugerencia }) => {
    const delTipo = tipoSrs ? conceptos.filter((c) => c.tipo === tipoSrs) : [];
    const delJuego = resumen.juegos?.[juego] || {};
    const aciertos = delTipo.reduce((suma, c) => suma + (c.aciertos || 0), 0) + (delJuego.aciertos || 0);
    const respuestas = delTipo.reduce((suma, c) => suma + (c.aciertos || 0) + (c.fallos || 0), 0) + (delJuego.total || 0);
    return {
      id,
      nombre,
      porcentaje: respuestas >= MIN_RESPUESTAS_HABILIDAD ? porcentaje(aciertos, respuestas) : null,
      sugerencia,
    };
  });
}

function calcularDominio(nivel, progresos) {
  const lecciones = listarLeccionesDeNivel(nivel);
  const aprobadas = new Set(progresos.filter((p) => p.aprobada).map((p) => p.leccion_id));
  const aprobadasDelNivel = lecciones.filter((l) => aprobadas.has(l.id)).length;
  return { aprobadas: aprobadasDelNivel, total: lecciones.length, porcentaje: porcentaje(aprobadasDelNivel, lecciones.length) };
}

/**
 * @param datos.resumen   documento de resumen_usuario
 * @param datos.nivel     nivel MCER del perfil (A1/A2)
 * @param datos.progresos documentos de progreso_lecciones del usuario
 * @param datos.conceptos documentos de srs_conceptos del usuario
 * @param datos.hoy       día local ("AAAA-MM-DD") para la racha vigente
 */
function vistaDeProgreso({ resumen, nivel, progresos, conceptos, hoy }) {
  const racha = rachaVigente(resumen, hoy);
  const xp = nivelPorXp(resumen.xp_total);
  const { progreso } = evaluar(resumen, { hoy, racha });

  return {
    nivel,
    dominio: calcularDominio(nivel, progresos),
    xp: { total: resumen.xp_total || 0, nivel: xp.nivel, xp_en_nivel: xp.xpEnNivel, xp_para_siguiente: xp.xpParaSiguiente },
    // ultimo_dia: la app lo guarda para seguir contando la racha sin conexión.
    racha: { actual: racha, maxima: resumen.racha_maxima || 0, ultimo_dia: resumen.ultimo_dia || null },
    habilidades: calcularHabilidades(resumen, conceptos),
    insignias: CATALOGO.map((insignia) => {
      const obtenida = resumen.insignias?.[insignia.id] || null;
      const { meta, actual } = progreso[insignia.id];
      return {
        ...insigniaPublica(insignia),
        obtenida: Boolean(obtenida),
        fecha: obtenida?.fecha ?? null,
        retroactiva: obtenida?.retroactiva ?? false,
        progreso: { actual: obtenida ? meta : actual, meta },
      };
    }),
    sin_actividad: (resumen.xp_total || 0) === 0 && (resumen.dias_activos || []).length === 0,
  };
}

module.exports = { HABILIDADES, MIN_RESPUESTAS_HABILIDAD, vistaDeProgreso };
