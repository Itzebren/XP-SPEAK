# Contexto: Trabajo Terminal XP-SPEAK (TT 2026-B162), insertar evidencias

Este archivo te da el contexto de lo que ya se hizo con el documento del TT y de la tarea que sigue.
Lee todo antes de empezar y **no supongas nada**: si algo no está claro (qué captura va en qué prueba,
o si falta una pantalla), pregúntale al usuario.

## 1. El proyecto

- **TT:** "Aplicación móvil con inteligencia artificial y gamificación para reforzar conocimientos del idioma
  inglés en niveles A1 y A2". Número **2026-B162** (así aparece en todo el documento).
- **Producto:** XP-SPEAK, una app Android (Kotlin, Jetpack Compose, MVVM, Hilt, Room) con backend serverless en
  Vercel (Node.js), Firebase Authentication y Firestore, Azure OpenAI (chat, gpt-4.1-mini), Azure AI Speech
  (pronunciación) y Resend (correo).
- **Población objetivo:** estudiantes de la **ESCOM** del IPN en niveles A1 y A2.
- **Autores:** Arellano Juarez José Juan y Parrales Ramirez Brenda Itzel. **Directora:** Dra. Nidia Asuncion Cortez Duarte.
- **Estructura:** Introducción; Cap. 1 Antecedentes; Cap. 2 Marco teórico; Cap. 3 Análisis; Cap. 4 Diseño;
  Cap. 5 Desarrollo e implementación; Cap. 6 Plan de pruebas; Cap. 7 Conclusiones y trabajo a futuro;
  Anexos (aviso de privacidad, encuesta, manual de usuario, manual técnico, API, evidencias de pruebas, repositorios).

## 2. Lo que ya se hizo (sesión anterior)

Se corrigió la redacción del documento, sin tocar el contenido técnico. Se entregaron dos archivos:

- `TT_B162_corregido_con_cambios.docx`: todos los cambios como control de cambios, autor "Claude".
- `TT_B162_corregido_limpio.docx`: los mismos cambios ya aceptados.

Decisiones que el usuario tomó y que se deben respetar:

1. **Caps. 1–3 no se tocan**: es el texto que ya se entregó y aprobó. Solo se corrigió el resumen
   (una frase duplicada y se restituyó "estudiantes de la ESCOM").
2. **Registro:** impersonal con "se" (se desarrolló, se implementó), formal, sin tono de bitácora
   ("Se agregó…", "Más adelante…", "antes…") y sin frases coloquiales ("vive en", "corre en", "a medias",
   "de punta a punta"). En los Caps. 4 a 7 se escribe "dispositivo", no "teléfono". **Los manuales de usuario
   y técnico sí van en segunda persona (tú).**
3. **Comillas:** de los Caps. 4 en adelante se usan comillas latinas « », igual que en la versión base.
4. **No hay sección de "Desviaciones respecto al diseño aprobado" (TT1)**: todo el documento describe solo
   el sistema implementado. No vuelvas a mencionar el "diseño aprobado" ni TT1.
5. **No se inventan datos.** Lo que falta quedó marcado como **[PENDIENTE: …]** y resaltado en amarillo.
   Solo se llena con datos o imágenes que dé el usuario.
6. Se redactaron las secciones 4.9.1 y 4.9.2 (estaban vacías) con datos del propio documento.

## 3. La tarea ahora: llenar las evidencias con capturas

El usuario te pasará una carpeta con capturas de las pantallas de la app. Tu trabajo es:

1. **Ver cada imagen** (ábrela y mírala) e identificar qué pantalla o estado muestra.
2. **Proponer al usuario una tabla de asignación** (captura → prueba) y **esperar su confirmación** antes de
   modificar el documento. Si una prueba no tiene captura adecuada, dilo; no la llenes con una pantalla
   que no corresponde.
3. Insertar las imágenes con el script (sección 5) y generar las dos versiones (con cambios y limpia).
4. Verificar el resultado (sección 6) y reportar qué quedó lleno y qué sigue pendiente.

### 3.1 Marcadores de imagen en el Capítulo 6 (pruebas de validación)

Cada uno es un párrafo con el texto `Evidencia: [PENDIENTE: captura de pantalla de la prueba <código> con su leyenda.]`,
al final de la ficha de esa prueba. Qué debe mostrar la captura (según el objetivo y los pasos de cada ficha):

| Código  | Prueba | Qué debe verse en la captura |
|---|---|---|
| PV1-R   | Registro de cuenta | Formulario «Crear cuenta», mensaje de política de contraseña, «Verifica tu correo» y/o «¡Personaliza tu experiencia!» |
| PV2-IS  | Inicio de sesión | Pantalla de inicio de sesión y el Inicio con avatar, XP y racha |
| PV3-RA  | Recuperar acceso | Solicitud del código, captura del código y nueva contraseña (y, si existe, el correo recibido) |
| PV4-NP  | Nivel y perfil | Selección de nivel A1/A2 y avatar; cambio posterior en «Ajustes de perfil» |
| PV5-CH  | Chat con IA | Conversación con el tutor, corrección gramatical bajo el mensaje (tachado rojo/verde), modo voz o en vivo |
| PV6-PR  | Pronunciación | Resultado de pronunciación con palabras en rojo y el sonido que falló |
| PV7-LE  | Lecciones y evaluaciones | Teoría de una lección, evaluación, resultado (≥ 70 %), desbloqueo o aviso «Primero, un repaso» |
| PV8-MJ  | Minijuegos | Un minijuego en curso y la pantalla de resultado (precisión, XP, racha) |
| PV9-PG  | Progreso y logros | Pantalla de Progreso: XP, nivel, racha, habilidades e insignias |
| PV10-NT | Notificaciones | Ajustes de notificaciones y/o el recordatorio mostrado en el sistema |
| PV11-EC | Cuenta y eliminación de datos | «Tu perfil»/«Ajustes de perfil» y la confirmación «Eliminar cuenta e historial» |

- Si una prueba tiene varias capturas, van **lado a lado en el mismo párrafo** (el script las escala a la
  misma altura). Si son más de 3, conviene dividirlas en dos ilustraciones o preguntar al usuario.
- **Leyenda:** `Evidencia de la prueba <código>: <nombre de la prueba en minúsculas>`
  (ej. `Evidencia de la prueba PV1-R: registro de cuenta`). Queda como "Ilustración N. …".

### 3.2 Marcadores en el anexo "Evidencias de pruebas"

Estos piden capturas de **terminal o de Android Studio, no de la app**. Probablemente no vengan en la carpeta
de pantallas; si no están, déjalos pendientes y avísale al usuario:

1. Ejecución de las 121 pruebas unitarias de la app (`./gradlew :app:testDebugUnitTest` o el informe de Android Studio).
2. Ejecución de las 154 pruebas del servidor (`npm test` en `backend`).
3. Ejecución de las pruebas con emuladores (`npm run test:emulador`).
4. Registro de tiempos `XPS-TIEMPOS` en Logcat.
5. Capturas de PV1–PV11 y PA-F1–PA-F4 que no quepan en el capítulo.

Para estos, el párrafo es una viñeta que termina en `[PENDIENTE: captura].`. Inserta las imágenes **después**
de la viñeta y cambia solo el texto del marcador por el número de ilustración, o pregúntale al usuario cómo
lo prefiere.

### 3.3 Pendientes que NO son imágenes (no los llenes sin datos del usuario)

- En las fichas: `[mm:ss]`, `[dd/mm/aaaa]`, `[PENDIENTE: EXITOSA / FALLIDA]`, `[PENDIENTE: descripción…]`
  y las columnas OK/NOK/Comentarios.
- Tablas de resultados: validación (OK/NOK), integración (CORRECTO/INCORRECTO), "Mediciones de tiempo"
  (promedio y máximo en ms) y "Resumen de las pruebas".
- 6.7 Conclusiones de las pruebas: `[__] %` y cuatro viñetas `[PENDIENTE: …]`.
- 7.1: el párrafo `[PENDIENTE: …]` sobre aprendizajes y resultados.
- Agradecimientos de cada integrante.

Si el usuario te da estos datos, redáctalos con el registro de la sección 2 y usa `Doc.replace(...)`, que deja
el cambio controlado.

## 4. Archivos que vienen junto con este contexto

- `tc.py`: motor de edición con control de cambios sobre `word/document.xml`. Funciones clave:
  - `Doc(ruta_document_xml)`: carga el documento.
  - `replace(prefijo, texto_nuevo, after=None, nth=0, hl=[regex])`: reemplaza el texto de un párrafo con un
    diff por palabras (inserciones y borrados controlados) y conserva el formato de los runs. Con `` `texto` ``
    se marca formato de código (estilo CodeChar); `hl` resalta en amarillo los fragmentos que coinciden.
  - `delete_para(prefijo)`: borra el párrafo con control de cambios.
  - `insert_after(prefijo, [párrafos], style_from=prefijo_modelo, exact=True)`: inserta párrafos nuevos.
  - `insert_images_after(prefijo, [imágenes], leyenda)`: inserta un párrafo centrado con las imágenes
    lado a lado (máx. 15 × 11 cm, sin deformar) y la leyenda "Ilustración N." con campo `SEQ Ilustración`,
    estilo `Descripcin`, igual que las ilustraciones existentes. Devuelve N.
  - `find` busca por **prefijo** del texto del párrafo. Si el prefijo aparece también en el índice (TOC),
    usa `exact=True`, `after=` o `nth=`.
- `accept.py`: acepta todos los cambios de un `document.xml` → sirve para generar la versión limpia
  sin pasar por LibreOffice, que renombra estilos.
- `insertar_evidencias.py`: script listo. Llena el diccionario `EVIDENCIAS` y `CARPETA`, y ejecuta:
  `python insertar_evidencias.py TT_B162_corregido_limpio.docx TT_B162_evidencias`
  Genera `TT_B162_evidencias_con_cambios.docx` y `TT_B162_evidencias_limpio.docx`.

Dependencias: `pip install lxml pillow`.

## 5. Reglas técnicas importantes

- **Archivo base:** usa la versión **limpia** (`TT_B162_corregido_limpio.docx`) o la última que el usuario
  haya guardado en Word, no la versión con cambios. Así, las evidencias son los únicos cambios controlados
  y no se mezclan con los anteriores; el motor no está pensado para editar párrafos que ya tienen cambios
  controlados.
- **Nunca modifiques el archivo original del usuario**: trabaja sobre una copia y genera archivos nuevos.
- Formatos de imagen: **png o jpg**. Si vienen en webp o heic, conviértelos antes con Pillow
  (`Image.open(x).convert('RGB').save(y.jpg)`).
- Si las capturas tienen datos personales reales (correos, nombres), avísale al usuario antes de insertarlas.
- Insertar ilustraciones en el Cap. 6 desplaza la numeración de las ilustraciones de la encuesta (anexos).
  Las referencias escritas a mano en el texto ("Ilustraciones 38 a 51" en el manual, "Ilustración 49" en 5.1.6)
  apuntan a ilustraciones anteriores al Cap. 6, así que no se ven afectadas. Aun así, revisa que nada nuevo
  quede con un número escrito a mano.
- Después de abrir el resultado en Word, hay que **actualizar campos** (Ctrl+A y luego F9) para refrescar
  la numeración, el índice general y el índice de ilustraciones. Recuérdaselo al usuario.

## 6. Verificación antes de entregar

1. Que los `.docx` abran: `python -c "import zipfile;zipfile.ZipFile('x.docx').testzip()"` y que
   `word/document.xml` sea XML válido (`lxml.etree.parse`).
2. Si hay LibreOffice: `soffice --headless --convert-to pdf archivo.docx`, y luego revisar con `pdftoppm`
   las páginas de las fichas para ver cada imagen, su tamaño y su leyenda.
3. Comparar el texto de la versión limpia contra la base: solo deben aparecer las leyendas nuevas y
   desaparecer los marcadores llenados.
4. Reportar: qué pruebas quedaron con evidencia (con su número de ilustración), cuáles siguen pendientes
   y por qué.
