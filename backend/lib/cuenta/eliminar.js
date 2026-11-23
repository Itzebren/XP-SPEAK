const { COLECCIONES: LECCIONES } = require('../lecciones/repositorio');
const { COLECCIONES: MINIJUEGOS } = require('../minijuegos/repositorio');
const { COLECCIONES: PROGRESO } = require('../progreso/repositorio');

/**
 * RN-14 (soberanía de datos) / Ilustración 36: borra todo lo que el backend guarda
 * de un usuario. Cada documento lleva su `uid`, así que basta con una consulta por
 * colección. `password_reset_codes` se indexa por correo.
 */
const COLECCIONES_DEL_USUARIO = [
  ...Object.values(LECCIONES),
  ...Object.values(MINIJUEGOS),
  ...Object.values(PROGRESO),
  'limite_speech',
];

const TAMANO_LOTE = 100;

async function eliminarDatosDeUsuario(db, { uid, correo }) {
  const refs = [];
  for (const coleccion of COLECCIONES_DEL_USUARIO) {
    const snapshot = await db.collection(coleccion).where('uid', '==', uid).get();
    snapshot.docs.forEach((doc) => refs.push(doc.ref));
  }
  if (correo) refs.push(db.collection('password_reset_codes').doc(correo.trim().toLowerCase()));

  for (let i = 0; i < refs.length; i += TAMANO_LOTE) {
    await Promise.all(refs.slice(i, i + TAMANO_LOTE).map((ref) => ref.delete()));
  }
  return { documentos: refs.length };
}

module.exports = { COLECCIONES_DEL_USUARIO, eliminarDatosDeUsuario };
