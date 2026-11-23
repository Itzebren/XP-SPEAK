/**
 * Niveles de experiencia (RF-14, docs/progreso-diseno.md "Reglas del módulo").
 * Son independientes del nivel MCER: solo dependen de la XP acumulada.
 *
 *   XP para llegar al nivel n = 50 · n · (n − 1)
 *   N1 = 0, N2 = 100, N3 = 300, N4 = 600, N5 = 1000…
 *
 * Cada nivel pide 100 XP más que el anterior.
 */
const XP_BASE = 50;

/** XP acumulada que se necesita para llegar al nivel n. */
function xpParaNivel(nivel) {
  return XP_BASE * nivel * (nivel - 1);
}

/**
 * @returns { nivel, xpEnNivel, xpParaSiguiente }: "Nivel 4 · 120/400 para el siguiente"
 *          es { nivel: 4, xpEnNivel: 120, xpParaSiguiente: 400 }.
 */
function nivelPorXp(xp) {
  const total = Math.max(0, Math.floor(xp || 0));
  // Raíz de 50·n·(n−1) = xp; el ajuste corrige el redondeo de punto flotante.
  let nivel = Math.max(1, Math.floor((1 + Math.sqrt(1 + (4 * total) / XP_BASE)) / 2));
  while (xpParaNivel(nivel + 1) <= total) nivel += 1;
  while (nivel > 1 && xpParaNivel(nivel) > total) nivel -= 1;

  const base = xpParaNivel(nivel);
  return { nivel, xpEnNivel: total - base, xpParaSiguiente: xpParaNivel(nivel + 1) - base };
}

/** El nivel nuevo si la XP cruzó un umbral (para festejarlo), o null. */
function subioDeNivel(xpAntes, xpDespues) {
  const antes = nivelPorXp(xpAntes).nivel;
  const despues = nivelPorXp(xpDespues).nivel;
  return despues > antes ? despues : null;
}

module.exports = { xpParaNivel, nivelPorXp, subioDeNivel };
