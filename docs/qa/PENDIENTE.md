# QA · Lo que falta por probar (al 30 de octubre de 2026)

Todo lo demás está en `reporte-qa-2026-10-29.md`: 24 apartados en Android 17, recorrido en
Android 8.0, verificación contra producción y 9 hallazgos corregidos. Esto es lo único abierto.

## 1. Puntaje real de pronunciación en pantalla (Eco Vocal, chat en vivo, Práctica)

**Qué falta:** oír una voz humana y ver el puntaje de Azure en la app. La cadena ya funciona
(app → token de producción → SDK de Azure → pantalla, con latencias dentro de RN-05 y R01), y la
evaluación real se verificó por API; solo no se ha visto un puntaje con micrófono.

**Por qué no se hizo:** el emulador no capta audio. macOS negó el micrófono al emulador lanzado
desde terminal (`coreaudio: Could not initialize record` en su log).

**Cómo cerrarlo (10 minutos), una de dos:**

- **Emulador con micrófono.** Ajustes del sistema → Privacidad y seguridad → Micrófono → activar
  el emulador de Android (`qemu-system-aarch64`). Después:
  ```sh
  cd backend && npm run dev:emulador            # backend local con emuladores de Firebase
  emulator -avd Pixel_10a -allow-host-audio     # micrófono del Mac dentro del emulador
  sh ./gradlew :app:assembleDebug -PfirebaseAuthEmulador=true -PleccionesBaseUrl=http://localhost:3000/
  adb install -r -g app/build/outputs/apk/debug/app-debug.apk
  adb reverse tcp:3000 tcp:3000 && adb reverse tcp:9099 tcp:9099
  ```
  El backend local no tiene llaves de Azure: para tokens reales hay que poner `AZURE_SPEECH_KEY` y
  `AZURE_SPEECH_REGION` en el entorno al correr `dev:emulador` (o apuntar la app a producción con
  una cuenta real verificada). Luego Juegos → Eco Vocal → micrófono → decir la frase.
- **Teléfono físico.** Depuración USB activada, `adb install -r app-debug.apk` (compilado con
  `-PleccionesBaseUrl=https://xp-speak-auth-backend.vercel.app/` y sin el emulador de Auth) y entrar
  con una cuenta real con correo verificado. Probar Eco Vocal, Práctica y el chat en vivo, y tocar
  una palabra para ver sus fonemas.

**Qué anotar en el reporte:** captura del puntaje (anillo, palabras en verde/amarillo/rojo, detalle
de una palabra con sus sonidos) y las líneas `XPS-TIEMPOS` del logcat para la Tabla 93.

## 2. Opcional: correo de recuperación con código propio

Hoy, si Resend no puede enviar, la app manda el enlace de restablecimiento de Firebase y funciona.
Para volver al código de 6 dígitos: verificar un dominio en Resend y cambiar el remitente
`onboarding@resend.dev` en `backend/lib/resend.js`; luego probar `POST /api/send-reset-code` en
producción (hoy responde 503 `CORREO_NO_DISPONIBLE`).

## 3. Opcional: pruebas que ya pasaron pero conviene repetir en un teléfono

- Notificación programada a la hora elegida (se verificó forzando el job el 11-oct).
- Rendimiento y tamaño del APK (59 MB) en un equipo de gama baja con Android 8.0.
