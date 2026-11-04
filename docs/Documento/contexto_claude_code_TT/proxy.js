// Proxy para las demos: :3000 → backend local (:3001), salvo voz y chat, que van a producción
// con el token real de la cuenta de diseño (el backend local no tiene llaves de Azure).
const http = require('http');
const https = require('https');

const KEY = process.env.FIREBASE_API_KEY;
const PROD = 'xp-speak-auth-backend.vercel.app';
let idToken = null, vence = 0, refresh = null;
let tokenVoz = null, tokenVozVence = 0;

function pedir(opciones, cuerpo) {
  return new Promise((ok, mal) => {
    const r = https.request(opciones, (res) => {
      let d = '';
      res.on('data', (c) => (d += c));
      res.on('end', () => ok({ status: res.statusCode, body: d }));
    });
    r.on('error', mal);
    if (cuerpo) r.write(cuerpo);
    r.end();
  });
}

async function token() {
  if (idToken && Date.now() < vence) return idToken;
  const r = refresh
    ? await pedir({ host: 'securetoken.googleapis.com', path: `/v1/token?key=${KEY}`, method: 'POST', headers: { 'Content-Type': 'application/json' } },
        JSON.stringify({ grant_type: 'refresh_token', refresh_token: refresh }))
    : await pedir({ host: 'identitytoolkit.googleapis.com', path: `/v1/accounts:signInWithPassword?key=${KEY}`, method: 'POST', headers: { 'Content-Type': 'application/json' } },
        JSON.stringify({ email: process.env.DEMO_EMAIL, password: process.env.DEMO_PASSWORD, returnSecureToken: true }));
  const j = JSON.parse(r.body);
  idToken = j.idToken || j.id_token;
  refresh = j.refreshToken || j.refresh_token;
  vence = Date.now() + 45 * 60 * 1000;
  return idToken;
}


// Bandeja de correo simulada (solo entorno de pruebas): muestra el correo de verificación que manda
// Firebase y, al tocar el botón, abre el enlace real del emulador de Auth.
function pagina(cuerpo) {
  return `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>body{margin:0;font-family:Roboto,Arial,sans-serif;background:#f5f7fa;color:#1b1b2f}
.bar{background:#232074;color:#fff;padding:18px 20px;font-size:20px;font-weight:700}
.tag{font-size:13px;opacity:.8;font-weight:400}
.mail{background:#fff;margin:16px;border-radius:16px;padding:22px;box-shadow:0 2px 8px rgba(0,0,0,.08)}
.de{color:#5b6070;font-size:14px;margin-bottom:4px}.asunto{font-size:21px;font-weight:700;margin:6px 0 18px}
.btn{display:block;text-align:center;background:#ff8c00;color:#232074;font-weight:700;font-size:19px;padding:16px;border-radius:14px;text-decoration:none;margin:22px 0}
.ok{font-size:64px;text-align:center}.c{text-align:center}</style></head><body>${cuerpo}</body></html>`;
}
async function demo(req, res, ruta) {
  const email = new URL(req.url, 'http://x').searchParams.get('email') || '';
  const codigos = await new Promise((ok) => http.get('http://localhost:9099/emulator/v1/projects/demo-xpspeak/oobCodes', (r) => {
    let d = ''; r.on('data', (c) => (d += c)); r.on('end', () => ok(JSON.parse(d).oobCodes || []));
  }).on('error', () => ok([])));
  const codigo = codigos.filter((c) => c.email === email && c.requestType === 'VERIFY_EMAIL').pop();
  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  if (ruta === '/demo/codigo') {
    const c = codigosReset[email] || await leerCodigo(email);
    return res.end(pagina(`<div class="bar">📬 Bandeja de entrada <div class="tag">${email}</div></div>
<div class="mail"><div class="de">De: XP-SPEAK &lt;onboarding@resend.dev&gt;</div>
<div class="asunto">Tu código de verificación — XP-SPEAK</div>
<h3>Recuperación de acceso — XP-SPEAK</h3><p>Tu código de verificación es:</p>
<p style="font-size:34px;font-weight:bold;letter-spacing:4px">${c || '—'}</p>
<p class="de">Este código expira en 15 minutos. Si tú no solicitaste este cambio, ignora este correo.</p></div>`));
  }
  if (ruta === '/demo/correo') {
    return res.end(pagina(`<div class="bar">📬 Bandeja de entrada <div class="tag">${email}</div></div>
<div class="mail"><div class="de">De: XP-SPEAK &lt;noreply@xp-speak.firebaseapp.com&gt;</div>
<div class="asunto">Verifica tu correo para XP-SPEAK</div>
<p>Hola:</p><p>Toca el botón para confirmar tu dirección de correo y empezar a practicar inglés con XP-SPEAK.</p>
<a class="btn" href="/demo/verificar?email=${encodeURIComponent(email)}">Verificar mi correo</a>
<p class="de">Si no creaste esta cuenta, ignora este mensaje.</p></div>`));
  }
  if (ruta === '/demo/verificar' && codigo) {
    await new Promise((ok) => http.get(codigo.oobLink.replace('127.0.0.1', 'localhost'), (r) => { r.resume(); r.on('end', ok); }).on('error', ok));
    return res.end(pagina(`<div class="bar">XP-SPEAK</div><div class="mail"><div class="ok">✅</div>
<h2 class="c">¡Correo verificado!</h2><p class="c">Ya puedes volver a la app y tocar «Ya verifiqué».</p></div>`));
  }
  res.end(pagina('<div class="mail">No hay correo para esta cuenta.</div>'));
}


// Recuperación: el backend local no tiene Resend. Si responde CORREO_NO_CONFIGURADO, se lee el código
// que ya guardó en el emulador de Firestore y se muestra en la bandeja simulada /demo/codigo.
const codigosReset = {};
function leerCodigo(email) {
  return new Promise((ok) => http.get({ host: 'localhost', port: 8080, path: `/v1/projects/demo-xpspeak/databases/(default)/documents/password_reset_codes/${encodeURIComponent(email)}`, headers: { Authorization: 'Bearer owner' } }, (r) => {
    let d = ''; r.on('data', (c) => (d += c)); r.on('end', () => { try { ok(JSON.parse(d).fields.codigo.stringValue); } catch { ok(null); } });
  }).on('error', () => ok(null)));
}

http.createServer(async (req, res) => {
  let cuerpo = [];
  for await (const c of req) cuerpo.push(c);
  cuerpo = Buffer.concat(cuerpo);
  const ruta = req.url.split('?')[0];
  if (ruta.startsWith('/demo/')) return demo(req, res, ruta);
  if (ruta === '/api/send-reset-code') {
    const r2 = await new Promise((ok) => {
      const q = http.request({ host: 'localhost', port: 3001, path: req.url, method: 'POST', headers: req.headers }, (pr) => {
        let d = ''; pr.on('data', (c) => (d += c)); pr.on('end', () => ok({ s: pr.statusCode, d }));
      });
      q.on('error', (e) => ok({ s: 502, d: String(e) }));
      q.write(cuerpo); q.end();
    });
    if (r2.s === 503) {
      const email = JSON.parse(cuerpo.toString()).correo.trim().toLowerCase();
      codigosReset[email] = await leerCodigo(email);
      console.log('código de recuperación', email, codigosReset[email]);
      res.writeHead(200, { 'Content-Type': 'application/json' });
      return res.end(JSON.stringify({ mensaje: 'Si el correo existe, se envió un código de verificación.' }));
    }
    res.writeHead(r2.s, { 'Content-Type': 'application/json' });
    return res.end(r2.d);
  }
  const aProd = ruta === '/api/speech-token' || ruta === '/api/chat';
  console.log(new Date().toISOString(), req.method, req.url, aProd ? '→ prod' : '→ local');

  if (ruta === '/api/speech-token' && tokenVoz && Date.now() < tokenVozVence) {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    return res.end(tokenVoz);
  }

  if (aProd) {
    const headers = { 'Content-Type': req.headers['content-type'] || 'application/json', Authorization: `Bearer ${await token()}` };
    if (cuerpo.length) headers['Content-Length'] = cuerpo.length;
    const r = https.request({ host: PROD, path: req.url, method: req.method, headers }, (pr) => {
      res.writeHead(pr.statusCode, { 'Content-Type': pr.headers['content-type'] || 'application/json', 'Cache-Control': 'no-store' });
      if (ruta === '/api/speech-token' && pr.statusCode === 200) {
        let d = '';
        pr.on('data', (c) => { d += c; res.write(c); });
        pr.on('end', () => { tokenVoz = d; tokenVozVence = Date.now() + 8 * 60 * 1000; res.end(); });
      } else {
        pr.on('data', (c) => res.write(c));
        pr.on('end', () => res.end());
      }
    });
    r.on('error', (e) => { res.writeHead(502); res.end(String(e)); });
    if (cuerpo.length) r.write(cuerpo);
    return r.end();
  }

  const r = http.request({ host: 'localhost', port: 3001, path: req.url, method: req.method, headers: req.headers }, (pr) => {
    res.writeHead(pr.statusCode, pr.headers);
    pr.pipe(res);
  });
  r.on('error', (e) => { res.writeHead(502); res.end(String(e)); });
  if (cuerpo.length) r.write(cuerpo);
  r.end();
}).listen(3000, () => console.log('proxy demo en :3000'));
