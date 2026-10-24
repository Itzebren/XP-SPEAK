const { crearEndpoint } = require('../../lib/endpoint');
const { obtenerManifiesto, listarLeccionesDeNivel } = require('../../lib/lecciones/contenido');
const { calcularEstado } = require('../../lib/lecciones/estado');
const { obtenerProgresosDeUsuario } = require('../../lib/lecciones/repositorio');
const { validarNivel } = require('../../lib/lecciones/validacion');

/**
 * GET /api/lessons?level=A1 — catálogo del nivel (RF-10, RN-02) con el estado
 * de cada lección para este usuario: bloqueada/disponible/en_progreso/
 * reprobada/completada (RN-06). El nivel lo manda la app desde el perfil.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons' }, async ({ req, res, uid, db }) => {
  const nivel = validarNivel(req.query.level);
  const progresos = await obtenerProgresosDeUsuario(db, uid);

  const lecciones = listarLeccionesDeNivel(nivel).map((leccion) => {
    const progreso = progresos.get(leccion.id);
    return {
      ...leccion,
      estado: calcularEstado(leccion, progreso, progresos.get(leccion.prerequisito_id)),
      mejor_puntaje: progreso?.mejor_puntaje ?? null,
      intentos: progreso?.intentos ?? 0,
      avance: progreso?.avance ?? null,
    };
  });

  return res.status(200).json({ nivel, version_contenido: obtenerManifiesto().version_contenido, lecciones });
});
