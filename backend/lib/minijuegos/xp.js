/**
 * XP de los minijuegos (docs/minijuegos-diseno.md §4.3, RF-14 / RN-09). La
 * app calcula lo mismo en CalculadorXp.kt para mostrarlo sin conexión, pero
 * la XP oficial es la que calcula el servidor con esta función.
 *
 *   precision = aciertos / total
 *   xp        = round(xpBase · precision) + (precision ≥ 0.7 ? bono : 0)
 *
 * El bono (0–5) premia la rapidez: en los juegos por rondas, terminar antes
 * del tiempo objetivo; en Ráfaga, que ya va contra reloj, cuántas palabras
 * se acertaron. Desde la 6.ª partida del mismo juego en el día, la XP se
 * reduce a la mitad para que las lecciones sigan siendo el camino principal.
 */
const JUEGOS = {
  'eco-vocal': { xpBase: 15, msPorRonda: 20000, rondas: { min: 8, max: 8 } },
  'mision-situacional': { xpBase: 15, msPorRonda: 20000, rondas: { min: 3, max: 8 } },
  'rafaga-palabras': { xpBase: 10, msPorRonda: null, rondas: { min: 1, max: 150 } },
  'orden-maestro': { xpBase: 10, msPorRonda: 15000, rondas: { min: 8, max: 8 } },
};

const BONO_MAXIMO = 5;
const PRECISION_PARA_BONO = 0.7;
const PARTIDAS_CON_XP_COMPLETA = 5;
const ACIERTOS_POR_PUNTO_RAFAGA = 5;

function bono(juego, { aciertos, total, duracionMs }) {
  const config = JUEGOS[juego];
  if (config.msPorRonda === null) {
    return Math.min(BONO_MAXIMO, Math.floor(aciertos / ACIERTOS_POR_PUNTO_RAFAGA));
  }
  // 5 puntos si se terminó en la mitad del tiempo objetivo o menos; 0 si tardó 1.5 veces o más.
  const proporcion = duracionMs / (config.msPorRonda * total);
  const puntos = Math.round(BONO_MAXIMO * (1.5 - proporcion));
  return Math.max(0, Math.min(BONO_MAXIMO, puntos));
}

/**
 * @param partidasPrevias partidas del mismo juego ya registradas hoy
 * @returns { xp, reducida }
 */
function calcularXp(juego, { aciertos, total, duracionMs }, partidasPrevias = 0) {
  const config = JUEGOS[juego];
  const precision = total > 0 ? aciertos / total : 0;
  const completa =
    Math.round(config.xpBase * precision) +
    (precision >= PRECISION_PARA_BONO ? bono(juego, { aciertos, total, duracionMs }) : 0);
  const reducida = partidasPrevias >= PARTIDAS_CON_XP_COMPLETA;
  return { xp: reducida ? Math.floor(completa / 2) : completa, reducida };
}

module.exports = { JUEGOS, PARTIDAS_CON_XP_COMPLETA, calcularXp };
