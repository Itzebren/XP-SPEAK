const { listarLeccionesDeNivel } = require('../lecciones/contenido');
const { diasEnSemana } = require('./racha');

/**
 * Las 9 insignias de la Ilustración 34 (RF-14, docs/progreso-diseno.md).
 * El servidor decide cuáles se obtienen (RN-09) evaluándolas contra el
 * resumen del usuario al terminar cada actividad; nunca se revocan.
 *
 * `icono` es una clave que la app traduce a un ícono propio
 * (feature/progress/ui/IconosInsignia.kt). El catálogo se exporta al APK con
 * `npm run exportar:insignias` para dibujar la cuadrícula sin conexión.
 *
 * Cada `progreso(resumen, hoy)` devuelve { actual, meta }: la insignia se
 * obtiene cuando actual ≥ meta, y la tarjeta bloqueada muestra "3 de 7".
 */
const PRECISION_SPEAKING = 0.8;
const PARTIDAS_SPEAKING = 10;
const PARTIDAS_EXPERTO = 25;
const DIAS_DESAFIO = 5;
const RACHA_INSIGNIA = 7;
const JUEGOS = ['eco-vocal', 'mision-situacional', 'rafaga-palabras', 'orden-maestro'];

const juegos = (resumen) => Object.values(resumen.juegos || {});
const sumar = (lista, campo) => lista.reduce((suma, j) => suma + (j[campo] || 0), 0);
const totalLecciones = (nivel) => listarLeccionesDeNivel(nivel).length;

function actividades(resumen) {
  const conteos = resumen.conteos || {};
  return (conteos.lecciones || 0) + (conteos.repasos || 0) + (conteos.chats || 0) + sumar(juegos(resumen), 'partidas');
}

function progresoExperto(resumen) {
  const partidas = sumar(juegos(resumen), 'partidas');
  const todosLosJuegos = JUEGOS.every((juego) => (resumen.juegos?.[juego]?.partidas || 0) > 0);
  // Sin haber probado los 4 juegos se queda a una partida de la meta.
  return { actual: todosLosJuegos ? Math.min(partidas, PARTIDAS_EXPERTO) : Math.min(partidas, PARTIDAS_EXPERTO - 1), meta: PARTIDAS_EXPERTO };
}

const CATALOGO = [
  {
    id: 'primer-logro',
    nombre: 'Primer Logro',
    descripcion: '¡Diste el primer paso en tu aprendizaje!',
    criterio: 'Termina tu primera actividad: una lección, un repaso, un minijuego o una conversación.',
    icono: 'estrella',
    progreso: (resumen) => ({ actual: Math.min(actividades(resumen), 1), meta: 1 }),
  },
  {
    id: 'habla-con-ia',
    nombre: 'Habla con IA',
    descripcion: 'Practicaste tu inglés conversando con el chatbot.',
    criterio: 'Ten tu primera conversación con el chatbot.',
    icono: 'chat',
    progreso: (resumen) => ({ actual: Math.min(resumen.conteos?.chats || 0, 1), meta: 1 }),
  },
  {
    id: 'estrella-minijuego',
    nombre: 'Estrella del Minijuego',
    descripcion: 'Una partida perfecta, sin un solo error.',
    criterio: 'Logra 100 % de precisión en una partida de cualquier minijuego.',
    icono: 'juego',
    progreso: (resumen) => ({ actual: Math.min(sumar(juegos(resumen), 'perfectas'), 1), meta: 1 }),
  },
  {
    id: 'racha-7',
    nombre: 'Racha de 7 Días',
    descripcion: 'Una semana entera practicando sin faltar un día.',
    criterio: 'Practica 7 días seguidos.',
    icono: 'fuego',
    progreso: (resumen, { racha }) => ({ actual: Math.min(racha ?? resumen.racha_actual ?? 0, RACHA_INSIGNIA), meta: RACHA_INSIGNIA }),
  },
  {
    id: 'explorador-a1',
    nombre: 'Explorador A1',
    descripcion: 'Completaste todo el nivel Principiante.',
    criterio: `Aprueba las ${totalLecciones('A1')} lecciones del nivel A1.`,
    icono: 'brujula',
    progreso: (resumen) => {
      const meta = totalLecciones('A1');
      return { actual: Math.min(resumen.lecciones_aprobadas?.A1?.length || 0, meta), meta };
    },
  },
  {
    id: 'graduado-a2',
    nombre: 'Graduado A2',
    descripcion: 'Completaste todo el nivel Elemental.',
    criterio: `Aprueba las ${totalLecciones('A2')} lecciones del nivel A2.`,
    icono: 'birrete',
    progreso: (resumen) => {
      const meta = totalLecciones('A2');
      return { actual: Math.min(resumen.lecciones_aprobadas?.A2?.length || 0, meta), meta };
    },
  },
  {
    id: 'maestro-speaking',
    nombre: 'Maestro Speaking',
    descripcion: 'Tu pronunciación es cada vez más clara.',
    criterio: `Juega ${PARTIDAS_SPEAKING} partidas de Eco Vocal con ${PRECISION_SPEAKING * 100} % de precisión o más.`,
    icono: 'microfono',
    progreso: (resumen) => ({
      actual: Math.min(resumen.juegos?.['eco-vocal']?.sobre_80 || 0, PARTIDAS_SPEAKING),
      meta: PARTIDAS_SPEAKING,
    }),
  },
  {
    id: 'jugador-experto',
    nombre: 'Jugador Experto',
    descripcion: 'Dominas todos los minijuegos.',
    criterio: `Juega los 4 minijuegos y ${PARTIDAS_EXPERTO} partidas en total.`,
    icono: 'trofeo',
    progreso: progresoExperto,
  },
  {
    id: 'desafio-semanal',
    nombre: 'Desafío Semanal',
    descripcion: 'Practicaste casi toda la semana.',
    criterio: `Practica ${DIAS_DESAFIO} días distintos de una misma semana (lunes a domingo).`,
    icono: 'calendario',
    progreso: (resumen, { hoy }) => {
      const dia = hoy || resumen.ultimo_dia;
      return { actual: dia ? Math.min(diasEnSemana(resumen.dias_activos, dia), DIAS_DESAFIO) : 0, meta: DIAS_DESAFIO };
    },
  },
];

const POR_ID = new Map(CATALOGO.map((insignia) => [insignia.id, insignia]));

/**
 * Evalúa todas las insignias contra el resumen.
 * @param opciones.hoy día de referencia para la semana del desafío (por defecto, el último día activo)
 * @param opciones.racha racha a mostrar (la vigente); por defecto, la guardada
 * @returns { nuevas: [ids que se obtienen ahora y no se tenían], progreso: { [id]: { actual, meta } } }
 */
function evaluar(resumen, { hoy = null, racha = null } = {}) {
  const progreso = {};
  const nuevas = [];
  for (const insignia of CATALOGO) {
    progreso[insignia.id] = insignia.progreso(resumen, { hoy, racha });
    const { actual, meta } = progreso[insignia.id];
    if (actual >= meta && !resumen.insignias?.[insignia.id]) nuevas.push(insignia.id);
  }
  return { nuevas, progreso };
}

/** Lo que se manda a la app y se exporta al APK: todo menos la función de progreso. */
function insigniaPublica({ id, nombre, descripcion, criterio, icono }) {
  return { id, nombre, descripcion, criterio, icono };
}

const obtenerInsignia = (id) => POR_ID.get(id) || null;

module.exports = { CATALOGO, JUEGOS, evaluar, insigniaPublica, obtenerInsignia };
