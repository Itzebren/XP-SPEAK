const test = require('node:test');
const assert = require('node:assert/strict');
const { enviarCorreoCodigo } = require('../lib/resend');
const { ErrorApi } = require('../lib/errores');

// QA 29-oct: en producción el envío fallaba con un 500 genérico; ahora la causa llega a la app como 503.

test('sin RESEND_API_KEY el envío responde 503 CORREO_NO_CONFIGURADO', async () => {
  const original = process.env.RESEND_API_KEY;
  delete process.env.RESEND_API_KEY;
  try {
    await assert.rejects(enviarCorreoCodigo('ana@example.com', '123456'), (e) => e instanceof ErrorApi && e.status === 503 && e.codigo === 'CORREO_NO_CONFIGURADO');
  } finally {
    if (original !== undefined) process.env.RESEND_API_KEY = original;
  }
});

test('si Resend rechaza el envío (p. ej. 403 del remitente de pruebas) responde 503 CORREO_NO_DISPONIBLE', async () => {
  const original = { key: process.env.RESEND_API_KEY, fetch: global.fetch };
  process.env.RESEND_API_KEY = 're_prueba';
  global.fetch = async () => ({ ok: false, status: 403, text: async () => 'You can only send testing emails to your own email address' });
  try {
    await assert.rejects(enviarCorreoCodigo('ana@example.com', '123456'), (e) => e instanceof ErrorApi && e.status === 503 && e.codigo === 'CORREO_NO_DISPONIBLE');
  } finally {
    global.fetch = original.fetch;
    if (original.key === undefined) delete process.env.RESEND_API_KEY; else process.env.RESEND_API_KEY = original.key;
  }
});
