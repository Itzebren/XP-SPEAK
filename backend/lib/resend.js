const { ErrorApi } = require('./errores');

/**
 * Envía el código de verificación por correo usando la API de Resend.
 * "onboarding@resend.dev" es el remitente de pruebas que Resend da gratis
 * sin necesidad de verificar un dominio propio — perfecto para la tesis.
 */
async function enviarCorreoCodigo(destinatario, codigo) {
  const apiKey = process.env.RESEND_API_KEY;
  if (!apiKey) {
    // Como SPEECH_NO_CONFIGURADO: la app explica que el servicio no está disponible, no un error genérico.
    throw new ErrorApi(503, 'CORREO_NO_CONFIGURADO', 'El envío de correos no está disponible por ahora. Intenta más tarde.');
  }

  const respuesta = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      from: 'XP-SPEAK <onboarding@resend.dev>',
      to: [destinatario],
      subject: 'Tu código de verificación — XP-SPEAK',
      html: `
        <div style="font-family: sans-serif; padding: 24px;">
          <h2>Recuperación de acceso — XP-SPEAK</h2>
          <p>Tu código de verificación es:</p>
          <p style="font-size: 32px; font-weight: bold; letter-spacing: 4px;">${codigo}</p>
          <p>Este código expira en 15 minutos. Si tú no solicitaste este cambio, ignora este correo.</p>
        </div>
      `,
    }),
  });

  if (!respuesta.ok) {
    const errorTexto = await respuesta.text();
    console.error(`Resend respondió ${respuesta.status}: ${errorTexto}`);
    // 403 con el remitente de pruebas onboarding@resend.dev: Resend solo entrega al correo del dueño
    // de la cuenta hasta verificar un dominio propio (QA 29-oct: producción respondía 500 genérico).
    throw new ErrorApi(503, 'CORREO_NO_DISPONIBLE', 'No pudimos enviarte el correo por ahora. Intenta más tarde.');
  }
}

module.exports = { enviarCorreoCodigo };
