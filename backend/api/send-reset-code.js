const { getFirebaseAdmin } = require('../lib/firebaseAdmin');
const { generarCodigo } = require('../lib/codeGenerator');
const { enviarCorreoCodigo } = require('../lib/resend');

const MENSAJE_GENERICO = { mensaje: 'Si el correo existe, se envió un código de verificación.' };
const EXPIRACION_MINUTOS = 15;

/**
 * RF-03 (parte 1): genera un código alfanumérico temporal y lo envía por
 * correo. Siempre responde el mismo mensaje genérico exista o no la cuenta,
 * para no revelar qué correos están registrados (RNF-06 seguridad).
 */
module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Método no permitido' });
  }

  const { correo } = req.body || {};
  if (!correo || typeof correo !== 'string' || !correo.includes('@')) {
    return res.status(400).json({ error: 'Correo inválido' });
  }

  const correoNormalizado = correo.trim().toLowerCase();

  try {
    const admin = getFirebaseAdmin();

    let usuarioExiste = true;
    try {
      await admin.auth().getUserByEmail(correoNormalizado);
    } catch {
      usuarioExiste = false;
    }

    if (usuarioExiste) {
      const codigo = generarCodigo();
      const expiraEn = Date.now() + EXPIRACION_MINUTOS * 60 * 1000;

      await admin
        .firestore()
        .collection('password_reset_codes')
        .doc(correoNormalizado)
        .set({ codigo, expiraEn, intentos: 0 });

      await enviarCorreoCodigo(correoNormalizado, codigo);
    }

    return res.status(200).json(MENSAJE_GENERICO);
  } catch (error) {
    console.error('Error en send-reset-code:', error);
    return res.status(500).json({ error: 'Ocurrió un error. Intenta más tarde.' });
  }
};
