#!/usr/bin/env node
/**
 * Servidor local que imita el ruteo de Vercel (api/…/[id].js → req.query.id)
 * para probar con curl sin ligar el proyecto a Vercel. Pensado para usarse con
 * los emuladores de Firebase:
 *
 *   npm run dev:emulador      (levanta emuladores + este servidor en :3000)
 *
 * En producción esto no se usa: Vercel sirve directamente los archivos de api/.
 */
const http = require('http');
const path = require('path');
const fs = require('fs');

const PUERTO = Number(process.env.PORT) || 3000;
const DIR_API = path.join(__dirname, '..', 'api');

// Convierte cada archivo de api/ en una ruta; los estáticos ganan a los dinámicos, como en Vercel.
function descubrirRutas(dir, prefijo = '/api') {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((entrada) => {
    const segmento = entrada.name.replace(/\.js$/, '');
    if (entrada.isDirectory()) return descubrirRutas(path.join(dir, entrada.name), `${prefijo}/${segmento}`);
    if (!entrada.name.endsWith('.js')) return [];
    const ruta = segmento === 'index' ? prefijo : `${prefijo}/${segmento}`;
    const parametros = [];
    const patron = ruta.replace(/\[(\w+)\]/g, (_, nombre) => {
      parametros.push(nombre);
      return '([^/]+)';
    });
    return [{ regex: new RegExp(`^${patron}/?$`), parametros, archivo: path.join(dir, entrada.name), dinamica: parametros.length > 0 }];
  });
}

const rutas = descubrirRutas(DIR_API).sort((a, b) => a.dinamica - b.dinamica);

http
  .createServer(async (req, resNode) => {
    const url = new URL(req.url, `http://localhost:${PUERTO}`);
    const ruta = rutas.find((r) => r.regex.test(url.pathname));
    if (!ruta) {
      resNode.writeHead(404, { 'Content-Type': 'application/json' });
      return resNode.end(JSON.stringify({ error: 'Ruta no encontrada' }));
    }

    const query = Object.fromEntries(url.searchParams);
    const coincidencia = url.pathname.match(ruta.regex);
    ruta.parametros.forEach((nombre, i) => (query[nombre] = decodeURIComponent(coincidencia[i + 1])));

    let crudo = '';
    for await (const trozo of req) crudo += trozo;
    let body;
    try {
      body = crudo ? JSON.parse(crudo) : undefined;
    } catch {
      body = crudo;
    }

    const res = {
      status(codigo) {
        resNode.statusCode = codigo;
        return res;
      },
      json(cuerpo) {
        resNode.setHeader('Content-Type', 'application/json; charset=utf-8');
        resNode.end(JSON.stringify(cuerpo));
        return res;
      },
      setHeader: (nombre, valor) => resNode.setHeader(nombre, valor),
      end: () => resNode.end(),
    };

    await require(ruta.archivo)({ method: req.method, headers: req.headers, query, body }, res);
    console.log(`${req.method} ${url.pathname} → ${resNode.statusCode}`);
  })
  .listen(PUERTO, () => console.log(`API local en http://localhost:${PUERTO}`));
