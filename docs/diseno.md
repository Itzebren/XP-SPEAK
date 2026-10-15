# Diseño visual de XP-SPEAK (Cap. 4.5 del TT, Ilustraciones 26–41)

> Rama `feature/diseno` (desde `develop` `52dc32c`). Es el documento de contexto del diseño
> visual, igual que `docs/minijuegos-diseno.md` y `docs/progreso-diseno.md`: se actualiza
> conforme avance el desarrollo. Última sesión: **2026-10-11**.
>
> Lienzo de diseño (sistema visual y las 14 pantallas recreadas, navegable como prototipo):
> https://claude.ai/artifact/Vt3piWUYkdga2xeRLQioBD (privado: compártelo desde su menú Share).

## 0. Estado actual

| Pieza | Estado | Dónde |
|---|---|---|
| Paleta del PDF en el tema | ✅ | `core/ui/theme/Color.kt`, `Theme.kt` |
| Tipografía ADLaM Display (títulos) | ✅ | `res/font/adlam_display.ttf` (OFL), `core/ui/theme/Type.kt` |
| Formas (botones, tarjetas, portadas) | ✅ | `core/ui/theme/Shape.kt` |
| Tema duplicado de la plantilla (`ui/theme`) | ✅ borrado | Nadie lo importaba |
| Arte vectorial: mascota, logo, 9 insignias, 4 íconos de Inicio, 4 portadas de minijuegos, 24 portadas de lección, 8 avatares, moneda XP y racha | ✅ | `docs/diseno-arte/svg/` → `res/drawable/*.xml` |
| Componentes comunes | ✅ | `core/ui/componentes/` (`EncabezadoXP`, `BotonPrincipal`, `BotonSecundario`, `TarjetaXP`, `TituloSeccion`, `DistintivoNivel`, `CampoTexto`, `Logotipo`, `Avatar`, `recursoDrawable`) |
| Pantalla de Inicio (Il. 32) y barra inferior del PDF | ✅ | `feature/home/ui/`, `core/navigation/` |
| Acceso, recuperar contraseña, personalización con avatar (Il. 28–31) | ✅ | `feature/auth/ui/` |
| Progreso e insignias con el arte final (Il. 33–34) | ✅ | `feature/progress/ui/` |
| Ajustes de perfil (Il. 35) | ✅ | `feature/account/ui/AccountScreen.kt`: nombre, correo (con enlace de verificación), avatar y nivel |
| Nombre de usuario al crear cuenta (Il. 29) | ✅ | `AuthViewModel`, `NombreUsuario`; se guarda como `displayName` de Firebase |
| Chat con el diseño de la app | ✅ | `feature/chat/ui/ChatScreen.kt` |
| Encabezados bajo la barra de estado | ✅ | `XPSpeakNavHost` (sin inset superior + franja al hacer scroll) y `statusBarsPadding` en cada encabezado |
| Ícono de la app y splash | ✅ | `ic_launcher_*` (mascota sobre azul claro, monocromo para íconos temáticos), `values-v31/themes.xml` |
| Catálogos de minijuegos y lecciones con portadas (Il. 37–38) | ✅ | `feature/games/ui/GamesScreen.kt`, `feature/lessons/ui/LessonsScreen.kt` |
| Pantalla de lección (Il. 39) | ✅ | `LeccionScreen`, `components/SeccionTeoria.kt` (tarjetas con audio por TTS) |
| Retroalimentación de pronunciación (Il. 40) | ✅ | `games/ui/ecovocal/EcoVocalScreen.kt` |
| Confirmar eliminación (Il. 36, RN-14) | ✅ | `feature/account/ui/ConfirmarEliminacionScreen.kt`, `feature/account/data/CuentaRepository.kt`, `backend/api/account/delete.js` |
| Notificaciones (Il. 41) | ✅ | `feature/notifications/` (preferencias, `RecordatorioWorker` diario, pantalla) |
| Botones con el naranja del PDF y enlaces legibles | ✅ | `primary` = marino, `coloresBotonPrincipal()` en `Theme.kt` |

### 0.1 Verificación (2026-10-11)

- Android: **74/74** con `testDebugUnitTest` (nuevas: `core/actividad/RachaSemanaTest`,
  `feature/notifications/domain/RecordatorioTest` y `feature/auth/domain/NombreUsuarioTest`).
- Backend: **124/124** con `npm test` (5 nuevas en `test/cuenta.test.js`).
- Emulador Pixel_10a con `npm run dev:app` y una cuenta nueva (`diseno.prueba@xpspeak.com`,
  creada para esta revisión): login, error de credenciales, crear cuenta, personalización
  (A1 + avatar), Inicio, Lecciones, Minijuegos, Progreso con las 9 insignias bloqueadas y
  Ajustes con el avatar guardado, lección, Eco Vocal, Notificaciones (permiso del sistema y
  recordatorio real: se adelantó el reloj del emulador y se forzó el job, llegó "Es hora de
  practicar tu inglés con XP-SPEAK.") y Confirmar eliminación. En una segunda pasada: Crear
  cuenta con nombre, guardar nombre en Ajustes (se refleja en el encabezado), cambiar a A2 (se
  conserva al volver a entrar), chat, ícono en el escritorio y encabezados bajo la barra de estado.
- La eliminación con el backend local devolvió 200 pero la cuenta seguía en Firebase Auth: el
  servidor local no tiene credenciales de administrador y el endpoint tomaba ese error como
  "ya borrada". Se corrigió (solo `auth/user-not-found` se tolera, lo demás responde 500) y hay
  prueba. **La cuenta `diseno.prueba@xpspeak.com` sigue existiendo**: hay que borrarla desde la
  consola de Firebase o con el endpoint desplegado.
- Para compilar desde la terminal: `JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`
  (no hay otro JDK en la Mac); el emulador de Firestore también lo necesita en el `PATH`.

## 1. Lo que dice el PDF

- **Paleta (Il. 26):** azul claro `#C0D6EB`, azul `#4B7BBE`, marino `#232074`, naranja
  `#FF8C00`, amarillo `#FFC107`, blanco `#FFFFFF` y gris `#9E9E9E`. Azules para la estructura,
  naranja y amarillo para lo interactivo y las recompensas, neutros para el contenido.
- **Tipografía:** ADLaM Display.
- **Logo (Il. 27):** robot con audífonos y micrófono que lee un libro "EN A1|A2", rodeado de
  monedas, estrellas y una corona.
- **Pantallas (Il. 28–41):** fondo gris muy claro, encabezado azul claro con la mascota,
  tarjetas azules redondeadas, botones naranjas de esquinas suaves, distintivo A1 marino y
  barra inferior de 5 íconos: Chat, Lecciones, Inicio, Progreso (listón) y Minijuegos.

## 2. Decisiones

1. **Texto marino sobre naranja.** El blanco sobre `#FF8C00` da 2.3:1 y no pasa ni para texto
   grande (RNF-01). El marino da 5.9:1. El naranja se queda exacto. Por lo mismo, `primary` de
   Material es el **marino** (botones de texto, enlaces, radios y campos se pintan con él) y los
   botones de acción usan `coloresBotonPrincipal()` o `BotonPrincipal`. Para texto naranja sobre
   fondo claro está `XPNaranjaTexto` (`#B85400`, 4.9:1).
2. **Colores derivados:** fondo `#F5F7FA`, tarjeta `#DCE7F3`, texto `#1B1B2F` y texto secundario
   `#5B6070` (el gris `#9E9E9E` del PDF solo se usa en bordes: como texto da 2.7:1). En el
   esquema de Material: `primary` = marino, `secondary` = naranja, `primaryContainer` = azul claro.
3. **Solo tema claro.** El PDF no define uno oscuro y el arte está pensado para fondos claros.
4. **ADLaM solo en títulos y cifras**, ya con color marino desde `Typography`. El cuerpo sigue en
   la fuente del sistema para leer cómodo en las lecciones.
5. **Arte propio en vectores**, recreado con la paleta exacta en lugar del raster del PDF: pesa
   poco, escala sin perder calidad y el fondo es transparente. La mascota también protagoniza las
   portadas de los minijuegos (el PDF usa a un chico como placeholder).
6. **Las lecciones tienen portada propia.** En la Il. 38 se repiten las portadas de los
   minijuegos; ahora cada lección tiene la suya, con un ícono de su módulo temático.
7. **Barra inferior con etiquetas.** El PDF solo muestra íconos; las etiquetas ayudan a
   reconocer cada sección (RNF-01). Inicio va al centro y es el destino después de entrar.
   Cuenta deja de ser pestaña: se abre desde el avatar del Inicio.
8. **Estados con texto, no solo color:** lección bloqueada/completada en un chip con ícono,
   insignia bloqueada en gris con candado.
9. **Notificaciones locales.** El recordatorio lo decide el teléfono (`Recordatorio.mensaje`):
   si hay racha vigente avisa que se puede perder, si no manda el recordatorio diario, y no
   avisa si ya practicaste hoy o el día no entra en la frecuencia. Corre con WorkManager una
   vez al día a la hora elegida; no hace falta servidor de push.
10. **Eliminar cuenta (RN-14):** primero los datos de Firestore (todas las colecciones con
    `uid`, más el código de recuperación por correo) y al final la cuenta de Auth, para poder
    reintentar si algo falla a medias. En el teléfono se cancelan los envíos pendientes y se
    vacía Room. Sin conexión no se borra nada.

## 3. Arte (docs/diseno-arte/)

- `generar.py` dibuja todo con la paleta del PDF y escribe un SVG por pieza en `svg/`.
  Solo usa formas que VectorDrawable soporta: las letras se trazan como líneas.
- `a_vector.py` convierte esos SVG a VectorDrawable en `app/src/main/res/drawable/`. Se hizo
  propio porque `svg2vectordrawable` descartaba los círculos y los degradados.
- Para cambiar una pieza: editar `generar.py`, correr los dos scripts y recompilar.

| Archivo | Uso |
|---|---|
| `mascota`, `mascota_preocupada` | Encabezados; confirmar eliminación |
| `logo_xpspeak` | Acceso, crear cuenta, recuperar |
| `insignia_<id>` (9) | Se cargan solas por id (`IconoInsignia`) |
| `inicio_lecciones`, `inicio_minijuegos`, `inicio_ia`, `inicio_progreso` | Accesos del Inicio |
| `juego_<id>` (4) | Catálogo de minijuegos |
| `leccion_<id>` (24) | Catálogo de lecciones; si falta una, se usa `inicio_lecciones` |
| `avatar_<id>` (8) | Personalización y ajustes (`UsuarioEntity.avatar`) |
| `moneda_xp`, `icono_racha` | Tarjetas de XP y racha, Inicio |

## 4. Pendiente

Ninguna pantalla del PDF queda sin implementar. Lo que sigue es de despliegue o de pulido:

1. **Despliegue:** `api/account/delete` necesita las credenciales de administrador de Firebase
   en Vercel (las mismas de `verify-reset-code`).
2. **Cuenta de prueba:** borrar `diseno.prueba@xpspeak.com` desde la consola de Firebase.
3. ~~**Arranque con sesión**~~ ✅ (rama `feature/requisitos-tt`): `ArranqueViewModel` entra directo a Inicio
   con la sesión guardada de Firebase, también sin conexión.
4. **Resto de minijuegos y repaso:** usan los colores y el encabezado nuevos, pero su contenido
   conserva la maquetación anterior; pueden adoptar `TarjetaXP`.
