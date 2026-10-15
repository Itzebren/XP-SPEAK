const { crearEndpoint } = require('../../lib/endpoint');
const { obtenerManifiesto, listarLeccionesDeNivel } = require('../../lib/lecciones/contenido');
const { calcularEstado } = require('../../lib/lecciones/estado');
const { obtenerProgresosDeUsuario, listarConceptosDeUsuario } = require('../../lib/lecciones/repositorio');
const { conceptosQueBloquean } = require('../../lib/lecciones/srs');
const { validarNivel } = require('../../lib/lecciones/validacion');

/**
 * GET /api/lessons?level=A1 — catálogo del nivel (RF-10, RN-02) con el estado
 * de cada lección para este usuario: bloqueada/disponible/en_progreso/
 * reprobada/completada (RN-06), o repaso_pendiente si antes de un tema nuevo
 * toca repasar conceptos débiles (RN-07). El nivel lo manda la app desde el perfil.
 */
module.exports = crearEndpoint({ metodo: 'GET', nombre: 'lessons' }, async ({ req, res, uid, db }) => {
  const nivel = validarNivel(req.query.level);
  const [progresos, conceptos] = await Promise.all([obtenerProgresosDeUsuario(db, uid), listarConceptosDeUsuario(db, uid)]);
  const bloqueo = conceptosQueBloquean(conceptos, { nivel, ahora: Date.now() });

  const lecciones = listarLeccionesDeNivel(nivel).map((leccion) => {
    const progreso = progresos.get(leccion.id);
    return {
      ...leccion,
      estado: calcularEstado(leccion, progreso, progresos.get(leccion.prerequisito_id), {
        repasoPendiente: bloqueo.length > 0,
      }),
      mejor_puntaje: progreso?.mejor_puntaje ?? null,
      intentos: progreso?.intentos ?? 0,
      avance: progreso?.avance ?? null,
    };
  });

  return res.status(200).json({
    nivel,
    version_contenido: obtenerManifiesto().version_contenido,
    lecciones,
    // RN-07: cuántos conceptos hay que repasar antes de empezar un tema nuevo.
    repaso_requerido: bloqueo.length,
  });
});
