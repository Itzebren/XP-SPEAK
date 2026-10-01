const { ErrorApi } = require('../errores');
const { NIVELES, obtenerLeccion } = require('./contenido');
const { LIMITE_POR_DEFECTO, LIMITE_MAXIMO } = require('./srs');

// UUID generado en Android (UUID.randomUUID()) para hacer idempotente el envío.
const PATRON_UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const MAX_LARGO_TEXTO = 200;
const MAX_PARES = 8;
const MAX_RESPUESTAS_REPASO = 50;

function entradaInvalida(mensaje) {
  return new ErrorApi(400, 'ENTRADA_INVALIDA', mensaje);
}

/** El :id de la ruta debe ser una lección existente. */
function exigirLeccion(id) {
  const leccion = obtenerLeccion(id);
  if (!leccion) {
    throw new ErrorApi(404, 'LECCION_NO_ENCONTRADA', 'La lección no existe');
  }
  return leccion;
}

function validarNivel(nivel) {
  if (!NIVELES.includes(nivel)) {
    throw entradaInvalida(`El parámetro level debe ser uno de: ${NIVELES.join(', ')}`);
  }
  return nivel;
}

function esTextoCorto(valor) {
  return typeof valor === 'string' && valor.length <= MAX_LARGO_TEXTO;
}

function esValorValido(valor) {
  if (Number.isInteger(valor)) return valor >= 0 && valor < 100;
  if (typeof valor === 'string') return esTextoCorto(valor);
  if (Array.isArray(valor)) {
    return (
      valor.length <= MAX_PARES &&
      valor.every((par) => Array.isArray(par) && par.length === 2 && par.every(esTextoCorto))
    );
  }
  return false;
}

/**
 * Valida el body de POST /api/lessons/:id/attempt antes de tocar Firestore
 * (§6.3). Rechaza ids de ítem repetidos o que no existen en la evaluación.
 */
function validarIntento(body, evaluacion) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw entradaInvalida('El cuerpo debe ser un objeto JSON');
  }

  const { attempt_id: attemptId, respuestas } = body;
  if (typeof attemptId !== 'string' || !PATRON_UUID.test(attemptId)) {
    throw entradaInvalida('attempt_id debe ser un UUID');
  }
  if (!Array.isArray(respuestas) || respuestas.length === 0) {
    throw entradaInvalida('respuestas debe ser una lista no vacía');
  }

  const idsValidos = new Set(evaluacion.items.map((item) => item.id));
  if (respuestas.length > idsValidos.size) {
    throw entradaInvalida('Hay más respuestas que ítems en la evaluación');
  }

  const vistos = new Set();
  for (const respuesta of respuestas) {
    if (!respuesta || typeof respuesta !== 'object' || typeof respuesta.id !== 'string') {
      throw entradaInvalida('Cada respuesta debe tener un id');
    }
    if (!idsValidos.has(respuesta.id)) {
      throw entradaInvalida(`El ítem ${respuesta.id} no existe en esta evaluación`);
    }
    if (vistos.has(respuesta.id)) {
      throw entradaInvalida(`El ítem ${respuesta.id} viene repetido`);
    }
    if (!esValorValido(respuesta.valor)) {
      throw entradaInvalida(`El valor de ${respuesta.id} no tiene un formato válido`);
    }
    vistos.add(respuesta.id);
  }

  return {
    attemptId: attemptId.toLowerCase(),
    respuestas: respuestas.map(({ id, valor }) => ({ id, valor })),
  };
}

/**
 * Valida el body de POST /api/srs/session. Los ids son de ejercicios de repaso
 * ("<lección>:<ítem>[:<par>]"); se resuelven contra el contenido y luego se
 * aplica la misma validación que a una evaluación.
 * @returns { attemptId, respuestas, ejercicios: [{ ejercicio, leccion }] }
 */
function validarRepaso(body, resolverEjercicio) {
  const respuestas = body && Array.isArray(body.respuestas) ? body.respuestas : [];
  if (respuestas.length > MAX_RESPUESTAS_REPASO) {
    throw entradaInvalida(`Una sesión de repaso admite hasta ${MAX_RESPUESTAS_REPASO} respuestas`);
  }
  const ejercicios = [];
  for (const respuesta of respuestas) {
    const encontrado = typeof respuesta?.id === 'string' ? resolverEjercicio(respuesta.id) : null;
    if (encontrado && !ejercicios.some((e) => e.ejercicio.id === encontrado.ejercicio.id)) ejercicios.push(encontrado);
  }
  const validado = validarIntento(body, { items: ejercicios.map((e) => e.ejercicio) });
  return { ...validado, ejercicios };
}

function validarAvance(body, leccion) {
  if (!body || typeof body !== 'object' || Array.isArray(body)) {
    throw entradaInvalida('El cuerpo debe ser un objeto JSON');
  }
  const { seccion_actual: seccionActual } = body;
  if (!Number.isInteger(seccionActual) || seccionActual < 0 || seccionActual >= leccion.secciones.length) {
    throw entradaInvalida(`seccion_actual debe ser un entero entre 0 y ${leccion.secciones.length - 1}`);
  }
  return { seccionActual };
}

function validarLimite(valor) {
  if (valor === undefined) return LIMITE_POR_DEFECTO;
  const limite = Number(valor);
  if (!Number.isInteger(limite) || limite < 1 || limite > LIMITE_MAXIMO) {
    throw entradaInvalida(`limit debe ser un entero entre 1 y ${LIMITE_MAXIMO}`);
  }
  return limite;
}

module.exports = { exigirLeccion, validarNivel, validarLimite, validarIntento, validarRepaso, validarAvance };
