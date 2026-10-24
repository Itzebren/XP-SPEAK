/**
 * Doble de prueba de firebase-admin: Firestore en memoria + verifyIdToken.
 * Implementa solo lo que usa el backend (doc/get/where ==/get de colección/
 * runTransaction con getAll/get de documento o de consulta/set). Las transacciones se ejecutan de una en una, igual que el
 * aislamiento serializable que garantiza Firestore.
 */
const { randomUUID } = require('crypto');
const path = require('path');

// Igual que Firestore real: "Property … contains an invalid nested entity".
function exigirSinArraysAnidados(valor, ruta = '') {
  if (Array.isArray(valor)) {
    valor.forEach((v, i) => {
      if (Array.isArray(v)) throw new Error(`INVALID_ARGUMENT: array anidado en ${ruta}[${i}]`);
      exigirSinArraysAnidados(v, `${ruta}[${i}]`);
    });
  } else if (valor && typeof valor === 'object') {
    Object.entries(valor).forEach(([k, v]) => exigirSinArraysAnidados(v, `${ruta}.${k}`));
  }
}

function crearFirestoreFalso() {
  const colecciones = new Map();
  let cola = Promise.resolve();

  const tabla = (nombre) => {
    if (!colecciones.has(nombre)) colecciones.set(nombre, new Map());
    return colecciones.get(nombre);
  };

  const snapshot = (datos, ref) => ({
    exists: datos !== undefined,
    data: () => (datos === undefined ? undefined : structuredClone(datos)),
    ref,
  });

  const referencia = (nombre, id) => ({
    coleccion: nombre,
    id,
    get: async () => snapshot(tabla(nombre).get(id)),
    set: async (datos) => {
      exigirSinArraysAnidados(datos);
      tabla(nombre).set(id, structuredClone(datos));
    },
    delete: async () => {
      tabla(nombre).delete(id);
    },
  });

  const consulta = (nombre, filtro) => ({
    esConsulta: true,
    get: async () => ({
      docs: [...tabla(nombre).entries()]
        .filter(([, datos]) => filtro(datos))
        .map(([id, datos]) => snapshot(datos, referencia(nombre, id))),
    }),
  });

  const db = {
    collection(nombre) {
      return {
        doc: (id = randomUUID()) => referencia(nombre, id),
        where(campo, operador, valor) {
          if (operador !== '==') throw new Error(`Operador no soportado en el doble: ${operador}`);
          return consulta(nombre, (datos) => datos[campo] === valor);
        },
        get: () => consulta(nombre, () => true).get(),
      };
    },

    runTransaction(fn) {
      const ejecucion = cola.then(async () => {
        const escrituras = [];
        const tx = {
          getAll: async (...refs) => refs.map((ref) => snapshot(tabla(ref.coleccion).get(ref.id))),
          get: async (ref) => (ref.esConsulta ? ref.get() : snapshot(tabla(ref.coleccion).get(ref.id))),
          set: (ref, datos) => {
            exigirSinArraysAnidados(datos);
            escrituras.push([ref, structuredClone(datos)]);
          },
        };
        const resultado = await fn(tx);
        escrituras.forEach(([ref, datos]) => tabla(ref.coleccion).set(ref.id, datos));
        return resultado;
      });
      cola = ejecucion.catch(() => {});
      return ejecucion;
    },

    // Utilidades para las aserciones de las pruebas.
    _docs: (nombre) => [...tabla(nombre).values()].map((d) => structuredClone(d)),
    _doc: (nombre, id) => structuredClone(tabla(nombre).get(id)),
    _limpiar: () => colecciones.clear(),
  };
  return db;
}

/**
 * Sustituye lib/firebaseAdmin.js en el caché de require ANTES de cargar los
 * handlers, para que getFirebaseAdmin() devuelva el doble.
 * @param tokens { "<token>": "<uid>" }
 */
function instalarFirebaseFalso(tokens) {
  const db = crearFirestoreFalso();
  // Usuarios de Firebase Auth: uid → { uid, email }. Las pruebas pueden agregar más.
  let falloAuth = null;
  const usuarios = new Map(Object.values(tokens).map((uid) => [uid, { uid, email: `${uid}@prueba.com` }]));
  const admin = {
    firestore: () => db,
    auth: () => ({
      verifyIdToken: async (token) => {
        if (!tokens[token]) throw new Error('token inválido');
        return { uid: tokens[token] };
      },
      getUser: async (uid) => {
        if (falloAuth) throw falloAuth;
        if (!usuarios.has(uid)) throw Object.assign(new Error('no existe'), { code: 'auth/user-not-found' });
        return { ...usuarios.get(uid) };
      },
      deleteUser: async (uid) => {
        if (!usuarios.delete(uid)) throw Object.assign(new Error('no existe'), { code: 'auth/user-not-found' });
      },
    }),
    _usuarios: usuarios,
    /** Simula que Firebase Auth falla (p. ej. sin credenciales). */
    _fallarAuth: (error) => {
      falloAuth = error;
    },
  };

  const ruta = require.resolve(path.join(__dirname, '..', '..', 'lib', 'firebaseAdmin'));
  require.cache[ruta] = {
    id: ruta,
    filename: ruta,
    loaded: true,
    exports: { getFirebaseAdmin: () => admin },
  };
  return { db, admin };
}

/** req/res mínimos con la interfaz que usa Vercel (res.status().json()). */
async function llamar(handler, { method = 'GET', token, query = {}, body, headers = {} } = {}) {
  const req = {
    method,
    query,
    body,
    headers: { ...headers, ...(token ? { authorization: `Bearer ${token}` } : {}) },
  };
  const res = {
    statusCode: 200,
    headers: {},
    body: undefined,
    status(codigo) {
      this.statusCode = codigo;
      return this;
    },
    json(cuerpo) {
      this.body = JSON.parse(JSON.stringify(cuerpo));
      return this;
    },
    setHeader(nombre, valor) {
      this.headers[nombre.toLowerCase()] = valor;
    },
    /** Respuestas en flujo (chat con stream): se acumulan en `texto`. */
    write(trozo) {
      this.texto = (this.texto || '') + trozo;
      return true;
    },
    end() {
      return this;
    },
  };
  await handler(req, res);
  return res;
}

/** Fija el segmento dinámico de la URL, como lo hace Vercel ([accion].js → req.query.accion). */
const enRuta = (handler, parametros) => (req, res) => handler({ ...req, query: { ...req.query, ...parametros } }, res);

module.exports = { crearFirestoreFalso, instalarFirebaseFalso, llamar, enRuta };
