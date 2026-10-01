#!/usr/bin/env node
/**
 * Imprime un ID token de Firebase para probar los endpoints con curl, igual
 * que el que manda Android. Usa la Web API key (en app/google-services.json
 * de XP-SPEAK: client[0].api_key[0].current_key) y un usuario de prueba.
 *
 *   FIREBASE_WEB_API_KEY=... node scripts/obtener-token.js correo@prueba.com contraseña
 */
const [correo, password] = process.argv.slice(2);
const apiKey = process.env.FIREBASE_WEB_API_KEY;

if (!apiKey || !correo || !password) {
  console.error('Uso: FIREBASE_WEB_API_KEY=... node scripts/obtener-token.js <correo> <contraseña>');
  process.exit(1);
}

(async () => {
  const respuesta = await fetch(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`,
    {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email: correo, password, returnSecureToken: true }),
    }
  );
  const datos = await respuesta.json();
  if (!respuesta.ok) {
    console.error('No se pudo iniciar sesión:', datos.error?.message);
    process.exit(1);
  }
  process.stdout.write(datos.idToken);
})();
