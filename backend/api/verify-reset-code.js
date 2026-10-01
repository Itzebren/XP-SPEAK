const { getFirebaseAdmin } = require('../lib/firebaseAdmin');

const MAX_INTENTOS = 5;

function contraseñaEsValida(password) {
  return (
    typeof password === 'string' &&
    password.length >= 8 &&
    /[a-zA-Z]/.test(password) &&
    /[0-9]/.test(password)
  );
}

/**
 * RF-03 (parte 2): valida el código contra Firestore y, si es correcto,
 * actualiza la contraseña del usuario en Firebase Auth. El código es de
 * un solo uso (se borra al validarse) y expira a los 15 minutos.
 */
module.exports = async (req, res) => {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Método no permitido' });
  }

  const { correo, codigo, nuevaPassword } = req.body || {};
  if (!correo || !codigo || !nuevaPassword) {
    return res.status(400).json({ error: 'Faltan datos' });
  }

  if (!contraseñaEsValida(nuevaPassword)) {
    return res
      .status(400)
      .json({ error: 'La contraseña debe tener al menos 8 caracteres, con letra y número.' });
  }

  const correoNormalizado = correo.trim().toLowerCase();

  try {
    const admin = getFirebaseAdmin();
    const docRef = admin.firestore().collection('password_reset_codes').doc(correoNormalizado);
    const doc = await docRef.get();

    if (!doc.exists) {
      return res.status(400).json({ error: 'Código inválido o expirado' });
    }

    const datos = doc.data();

    if (Date.now() > datos.expiraEn) {
      await docRef.delete();
      return res.status(400).json({ error: 'El código expiró, solicita uno nuevo' });
    }

    if (datos.intentos >= MAX_INTENTOS) {
      await docRef.delete();
      return res.status(400).json({ error: 'Demasiados intentos fallidos, solicita un código nuevo' });
    }

    if (datos.codigo !== codigo.toUpperCase()) {
      await docRef.update({ intentos: datos.intentos + 1 });
      return res.status(400).json({ error: 'Código incorrecto' });
    }

    const usuario = await admin.auth().getUserByEmail(correoNormalizado);
    await admin.auth().updateUser(usuario.uid, { password: nuevaPassword });
    await docRef.delete();

    return res.status(200).json({ mensaje: 'Contraseña actualizada correctamente' });
  } catch (error) {
    console.error('Error en verify-reset-code:', error);
    return res.status(500).json({ error: 'Ocurrió un error. Intenta más tarde.' });
  }
};
