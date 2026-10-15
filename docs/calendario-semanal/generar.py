"""Genera el Calendario semanal del TT 2026-B162 (agosto–diciembre 2026, periodo 2026-2)
a partir del Cronograma final.

Salidas (en esta misma carpeta):
  - Calendario semanal.xlsx
  - calendario-semanal.md

Uso:  python3 generar.py      (requiere openpyxl)

Convención de semanas (igual que el Cronograma: S1–S4 por mes):
  S1 = días 1–7, S2 = 8–14, S3 = 15–21, S4 = 22 al fin de mes.
Las actividades y sus semanas son las del Cronograma final; aquí solo se desglosan.
La columna "Avance real" registra lo que ya se desarrolló (según el historial de git),
sin mover la planeación.
"""

import calendar
import datetime as dt
from pathlib import Path

from openpyxl import Workbook
from openpyxl.formatting.rule import FormulaRule
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

AQUI = Path(__file__).parent
ANIO = 2026
MESES = ["Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
         "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"]
MES_INICIO = 8  # el calendario arranca en agosto (Sprint 3)
TITULO = ("APLICACIÓN MÓVIL CON INTELIGENCIA ARTIFICIAL Y GAMIFICACIÓN PARA REFORZAR "
          "CONOCIMIENTOS DEL IDIOMA INGLÉS EN NIVELES A1 Y A2")
B = "Brenda Itzel Parrales Ramírez"
J = "José Juan Arellano Juárez"

TT2 = "Reporte técnico TT-II"


def semana(sprint, act_b, tareas_b, act_j, tareas_j, entregable, real=""):
    return dict(sprint=sprint, act_b=act_b, tareas_b=tareas_b,
                act_j=act_j, tareas_j=tareas_j, entregable=entregable, real=real)


# Una entrada por semana del Cronograma, de Agosto S1 a Diciembre S4 (20). Cada lista de tareas es lo que hace
# esa persona esa semana; la actividad es el renglón del Cronograma que la cubre.
SEMANAS = [
    # ───────────── SPRINT 3 · 2026-2 · Agosto–Diciembre ─────────────
    semana(3, ["Creación del product backlog", TT2],
           ["Organizar el product backlog por épicas: Acceso y cuenta, Chatbot con IA, Voz",
            "Ligar Historias de Usuario a RF/CU y escribir criterios de aceptación",
            "Abrir el capítulo 5 Desarrollo e Implementación en el reporte"],
           ["Creación del product backlog", TT2],
           ["Organizar las épicas Lecciones, Minijuegos, Progreso y logros, Notificaciones",
            "Ligar Historias de Usuario a RF/CU y escribir criterios de aceptación"],
           "Product backlog por épicas"),
    semana(3, ["Creación del product backlog", "Creación del sprint backlog", "Diseño de la interfaz", TT2],
           ["Priorizar el backlog con la Product Owner",
            "Seleccionar las HU del Sprint 3 y crear el tablero Kanban (Sin empezar / En desarrollo / Finalizado)",
            "Diseño: paleta de colores y logotipo (Ilustraciones 26–27)"],
           ["Creación del product backlog", "Creación del sprint backlog", "Diseño de la interfaz", TT2],
           ["Estimar las HU y repartir el trabajo del sprint",
            "Diseño: sistema visual (tipografía, botones, tarjetas, íconos)"],
           "Backlog priorizado · Tablero Kanban · Paleta y logotipo"),
    semana(3, ["Creación del sprint backlog", "Diseño de la interfaz", TT2],
           ["Desglosar las HU del sprint en tareas técnicas",
            "Diseño de pantallas: Iniciar sesión, Crear cuenta, Recuperar contraseña, Personalización (Ilustraciones 28–31)"],
           ["Creación del sprint backlog", "Diseño de la interfaz", TT2],
           ["Desglosar las HU del sprint en tareas técnicas",
            "Diseño de pantallas: Inicio, Progreso y logros, Minijuegos, Catálogo de lecciones, Lección (Ilustraciones 32–34, 37–39)"],
           "Sprint backlog · Mockups Il. 28–34, 37–39"),
    semana(3, ["Diseño de la interfaz", "Interfaz y estructura básica", TT2],
           ["Diseño de pantallas: Ajustes de perfil, Confirmar eliminación, Error de pronunciación, Notificaciones (Ilustraciones 35–36, 40–41)",
            "Crear el proyecto Android (Kotlin, MVVM, Hilt, Navigation Compose, Room)"],
           ["Diseño de la interfaz", "Interfaz y estructura básica", TT2],
           ["Prototipo navegable con las 14 pantallas y revisión con la directora",
            "Crear el servidor de servicios (Vercel) y el proyecto de Firebase (Auth y Firestore)"],
           "Sección 4.5 · Prototipo navegable · Esqueleto de la app y del servidor",
           "Arquitectura base de la app: 14-sep (Sep S2), 2 semanas después de lo planeado."),
    semana(3, ["Creación del product backlog", "Interfaz y estructura básica", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Registro de cuenta e inicio de sesión con Firebase Auth (CU-01, CU-02, RN-01)",
            "Validación de contraseña y mensajes de error"],
           ["Creación del product backlog", "Interfaz y estructura básica", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Tema de la app (colores, tipografía, formas) y componentes comunes",
            "Barra de navegación inferior y pantalla de Inicio"],
           "Registro e inicio de sesión funcionando",
           "Firebase Auth (registro, login, validación): 20-sep (Sep S3)."),
    semana(3, ["Interfaz y estructura básica", TT2],
           ["Recuperación de acceso por código de verificación (CU-03): envío por correo con Resend, vigencia e intentos",
            "Nivel inicial A1/A2 al registrarse (CU-04, RN-02)",
            "Documentar 5.1 Arquitectura base y módulos de registro y recuperación"],
           ["Interfaz y estructura básica", TT2],
           ["Personalización: nombre de usuario y avatar (Ilustración 31)",
            "Pantalla de cuenta: datos del perfil, cerrar sesión, eliminar cuenta (CU-10, RN-14)"],
           "Incremento: acceso, perfil y cuenta",
           "Recuperación de acceso: 25-sep (Sep S4). Cuenta: 29-sep. Eliminación de cuenta en servidor: 11-oct."),
    semana(3, ["Integración de chatbot con IA", TT2],
           ["Conectar el motor de IA generativa desde el servidor (RF-06)",
            "System prompt restringido a A1/A2 según el MCER (RN-03, riesgo R03)"],
           ["Integración de chatbot con IA", TT2],
           ["Armar un banco de conversaciones de prueba por nivel para validar el prompt",
            "Revisar que el vocabulario de las respuestas no exceda A1/A2"],
           "Chatbot respondiendo desde el servidor"),
    semana(3, ["Integración de chatbot con IA", TT2],
           ["Pantalla de chat (CU-05) con ChatViewModel e historial de la conversación",
            "Correcciones gramaticales inmediatas y efímeras (RF-09, RN-05)"],
           ["Integración de chatbot con IA", TT2],
           ["Manejo de errores y sin conexión en el chat (RNF-10)",
            "Pruebas del chat contra el banco de conversaciones"],
           "Incremento: chat con IA en texto",
           "Módulo de chat con IA (interfaz y lógica): 29-sep (Sep S4), a tiempo."),
    semana(3, ["Creación del product backlog", "Integración de chatbot con IA", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Pruebas unitarias del chat y ajustes del prompt",
            "Documentar el módulo de chat en el capítulo 5"],
           ["Creación del product backlog", "Integración de chatbot con IA", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Revisión del chat con la directora (reunión quincenal)"],
           "Chat con IA cerrado · Backlog refinado"),
    semana(3, ["Módulo de reconocimiento de voz", TT2],
           ["Voz a texto en el chat (RF-07): botón de micrófono, permiso de micrófono y envío del texto reconocido",
            "Manejo de audio sin almacenarlo (RN-04, RNF-05)"],
           ["Módulo de reconocimiento de voz", TT2],
           ["Endpoint de token de Azure AI Speech en el servidor (la llave no vive en la app)",
            "Evaluación de pronunciación con Azure y respaldo con el reconocedor del teléfono"],
           "Voz a texto en el chat",
           "Adelantado: token de Azure y evaluación de pronunciación hechos el 06-oct para Eco Vocal. Pendiente: micrófono en el chat."),
    semana(3, ["Módulo de reconocimiento de voz", TT2],
           ["Texto a voz de las respuestas del chatbot (RF-08)",
            "Respaldo en modo texto si el servicio de voz no responde a tiempo (riesgo R01)"],
           ["Módulo de reconocimiento de voz", TT2],
           ["Pantalla de error de pronunciación (Ilustración 40, RF-09)",
            "Medir la latencia de voz (RNF-03) y configurar Azure en producción"],
           "Incremento: conversación por voz",
           "Pendiente: llaves AZURE_SPEECH_KEY / AZURE_SPEECH_REGION en Vercel."),
    semana(3, ["Desarrollo de módulos de gramática y vocabulario", TT2],
           ["Plan curricular A1 y A2 alineado al MCER (temas por nivel)",
            "Revisar los temas contra los descriptores del MCER"],
           ["Desarrollo de módulos de gramática y vocabulario", TT2],
           ["Estructura de una lección (introducción, vocabulario, gramática, diálogos, evaluación) y su esquema JSON",
            "API de lecciones en el servidor y pantallas de catálogo y lección (Ilustraciones 38–39)"],
           "Plan curricular · Estructura de lección · Catálogo",
           "Adelantado: cliente, catálogo y pantalla de lección 26-sep; servidor de lecciones 30-sep."),
    semana(3, ["Creación del product backlog", "Desarrollo de módulos de gramática y vocabulario", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Redactar el contenido de las lecciones A1",
            "Revisión humana del contenido (guía de autoría)"],
           ["Creación del product backlog", "Desarrollo de módulos de gramática y vocabulario", TT2],
           ["Refinar el product backlog (inicio de mes)",
            "Redactar el contenido de las lecciones A2",
            "Validación automática del contenido y caché fuera de línea (RN-11)"],
           "Lecciones A1 y A2 publicadas",
           "Adelantado: 24 lecciones (12 A1 y 12 A2) y caché offline (29-sep)."),
    semana(3, ["Creación del product backlog", "Ejercicios de gramática y vocabulario", TT2],
           ["Refinar el product backlog",
            "Evaluaciones cortas al final de cada lección (RF-12): tipos de ejercicio y calificación",
            "Regla de aprobación con 70 % y desbloqueo (RN-06)"],
           ["Ejercicios de gramática y vocabulario", TT2],
           ["XP solo en la primera aprobación y registro de errores por concepto",
            "Envío de intentos al servidor con reintento si no hay conexión"],
           "Evaluaciones por lección",
           "Adelantado: calificación local y outbox de intentos (29-sep)."),
    semana(3, ["Ejercicios de gramática y vocabulario", TT2],
           ["Pruebas del flujo completo de lección: aprobar, reprobar, desbloquear y XP",
            "Documentar el módulo de lecciones en el capítulo 5"],
           ["Ejercicios de gramática y vocabulario", TT2],
           ["Repaso con repetición espaciada SM-2 (RF-13, RN-07): sesión de repaso en el servidor y pantalla de repaso"],
           "Incremento: lecciones con evaluación y repaso SRS",
           "Adelantado: repaso SRS con SM-2 (29-sep)."),
    semana(3, ["Desarrollo de mini-juegos", "Pruebas y mejoras", "Integración de todos los módulos", TT2],
           ["Minijuegos Misión Situacional y Eco Vocal (RF-11, CU-07)",
            "Pruebas y mejoras de chat y voz",
            "Integración: notificaciones de recordatorio configurables (RF-15, CU-09, RN-15)"],
           ["Desarrollo de mini-juegos", "Pruebas y mejoras", "Integración de todos los módulos", TT2],
           ["Catálogo de minijuegos (Ilustración 37) y motor común (rondas, XP, racha)",
            "Minijuegos Orden Maestro y Ráfaga de Palabras",
            "Integración: progreso y logros (CU-08, RF-05, RF-14, RN-08, RN-09)"],
           "Cuatro minijuegos · Progreso · Notificaciones",
           "Adelantado: los 4 minijuegos (06-oct), progreso e insignias (11-oct), notificaciones y las 14 pantallas del diseño (11-oct)."),
    semana(3, ["Creación del product backlog", "Desarrollo de mini-juegos", "Pruebas y mejoras", "Integración de todos los módulos", TT2],
           ["Refinar el product backlog (último del proyecto)",
            "Pruebas de los minijuegos en dispositivo real",
            "Integración: flujo completo registro → nivel → chat → lección"],
           ["Desarrollo de mini-juegos", "Pruebas y mejoras", "Integración de todos los módulos", TT2],
           ["Sincronizar resultados de partidas con el servidor (XP, racha y SRS)",
            "Racha unificada entre lecciones, minijuegos y chat",
            "Documentar minijuegos y progreso en el capítulo 5"],
           "Incremento: minijuegos y gamificación · Cierre del Sprint 3"),

    # ───────────── SPRINT 4 · 2026-2 · Diciembre ─────────────
    semana(4, ["Integración de todos los módulos", "Realización de pruebas del sistema", "Corrección de pruebas", "Segunda evaluación de TT-II", TT2],
           ["Cerrar la integración y desplegar el servidor en producción",
            "Pruebas del sistema por caso de uso (CU-01 a CU-05, CU-10)",
            "Preparar la presentación del TT-II"],
           ["Integración de todos los módulos", "Realización de pruebas del sistema", "Corrección de pruebas", "Segunda evaluación de TT-II", TT2],
           ["Pruebas del sistema por caso de uso (CU-06 a CU-09) y de RNF (latencia, offline, Android 8.0+)",
            "Corregir los defectos encontrados"],
           "Sistema integrado · Reporte de pruebas"),
    semana(4, ["Realización de pruebas del sistema", "Corrección de pruebas", "Segunda evaluación de TT-II", "Retroalimentación del jurado", TT2],
           ["Corregir los defectos pendientes y repetir las pruebas",
            "Presentar el TT-II ante el jurado y registrar observaciones"],
           ["Realización de pruebas del sistema", "Corrección de pruebas", "Segunda evaluación de TT-II", "Retroalimentación del jurado", TT2],
           ["Corregir los defectos pendientes y repetir las pruebas",
            "Presentar el TT-II ante el jurado y registrar observaciones"],
           "Presentación TT-II · Versión estable de la app"),
    semana(4, ["Pruebas piloto con usuarios reales", "Segunda evaluación de TT-II", "Retroalimentación del jurado", TT2],
           ["Prueba piloto con estudiantes (A1/A2): instalar la app y aplicar el cuestionario de uso",
            "Atender las observaciones del jurado"],
           ["Pruebas piloto con usuarios reales", "Segunda evaluación de TT-II", "Retroalimentación del jurado", TT2],
           ["Analizar los resultados de la prueba piloto",
            "Cerrar el reporte técnico TT-II con resultados y conclusiones"],
           "Resultados de la prueba piloto · Reporte TT-II final"),
]

# Diferencias puntuales entre los dos renglones del Cronograma.
NOTA_DIFERENCIAS = (
    "Diferencia entre los dos renglones del Cronograma final: Brenda tiene la marca de product backlog de "
    "noviembre en la S2 y José Juan en la S1. El calendario respeta esas semanas."
)


def rango(mes, s):
    ini = 1 + 7 * (s - 1)
    fin = calendar.monthrange(ANIO, mes)[1] if s == 4 else ini + 6
    return dt.date(ANIO, mes, ini), dt.date(ANIO, mes, fin)


def filas():
    assert len(SEMANAS) == 4 * (13 - MES_INICIO)
    for i, w in enumerate(SEMANAS):
        mes, s = MES_INICIO + i // 4, i % 4 + 1
        ini, fin = rango(mes, s)
        yield dict(w, num=i + 1, mes=MESES[mes - 1], s=f"S{s}", ini=ini, fin=fin, periodo="2026-2")


def lista(items):
    return "\n".join(f"• {t}" for t in items)


# ───────────────────────────── Excel ─────────────────────────────
AZUL = "1F4E79"
FILL_SPRINT = {3: "FFF2CC", 4: "FCE4D6"}
FUENTE = "Arial"


def generar_xlsx(ruta):
    wb = Workbook()
    ws = wb.active
    ws.title = "Calendario semanal"
    borde = Border(*(Side(style="thin", color="A6A6A6"),) * 4)
    cols = [("Sem.", 6), ("Periodo", 9), ("Mes", 11), ("Semana", 8), ("Inicio", 11), ("Fin", 11),
            ("Sprint", 7), (f"Actividades del Cronograma — {B}", 30), ("Tareas de la semana — Brenda", 58),
            (f"Actividades del Cronograma — {J}", 30), ("Tareas de la semana — José Juan", 58),
            ("Entregable / evidencia", 32), ("Avance real (git)", 38), ("Estado", 12)]
    n = len(cols)
    ultima = get_column_letter(n)

    ws.merge_cells(f"A1:{ultima}1")
    ws["A1"] = f"Calendario semanal agosto–diciembre 2026 · TT 2026-B162 · {TITULO}"
    ws["A1"].font = Font(name=FUENTE, bold=True, size=13, color="FFFFFF")
    ws["A1"].fill = PatternFill("solid", fgColor=AZUL)
    ws["A1"].alignment = Alignment(wrap_text=True, vertical="center")
    ws.row_dimensions[1].height = 36
    ws.merge_cells(f"A2:{ultima}2")
    ws["A2"] = (f"Integrantes: {B} (Scrum Master / Lead Developer) y {J} (Developer). "
                "Product Owner: M. en C. Nidia Asunción Cortez Duarte. Basado en el Cronograma final; "
                "semanas S1 = días 1–7, S2 = 8–14, S3 = 15–21, S4 = 22 a fin de mes.")
    ws["A2"].font = Font(name=FUENTE, italic=True, size=9)
    ws["A2"].alignment = Alignment(wrap_text=True)
    ws.row_dimensions[2].height = 28

    for c, (h, w) in enumerate(cols, 1):
        cel = ws.cell(row=4, column=c, value=h)
        cel.font = Font(name=FUENTE, bold=True, color="FFFFFF", size=10)
        cel.fill = PatternFill("solid", fgColor=AZUL)
        cel.alignment = Alignment(wrap_text=True, horizontal="center", vertical="center")
        cel.border = borde
        ws.column_dimensions[get_column_letter(c)].width = w
    ws.row_dimensions[4].height = 32

    r = 5
    for f in filas():
        fila = [f["num"], f["periodo"], f["mes"], f["s"], f["ini"], f["fin"], f["sprint"],
                lista(f["act_b"]), lista(f["tareas_b"]), lista(f["act_j"]), lista(f["tareas_j"]),
                f["entregable"], f["real"],
                f'=IF(TODAY()>F{r},"Concluida",IF(TODAY()>=E{r},"En curso","Pendiente"))']
        for c, v in enumerate(fila, 1):
            cel = ws.cell(row=r, column=c, value=v)
            cel.font = Font(name=FUENTE, size=9, bold=c in (8, 10))
            cel.alignment = Alignment(wrap_text=True, vertical="top",
                                      horizontal="center" if c <= 7 or c == n else "left")
            cel.border = borde
            if c <= 7:
                cel.fill = PatternFill("solid", fgColor=FILL_SPRINT[f["sprint"]])
            if c in (5, 6):
                cel.number_format = "dd/mm/yyyy"
        lineas = max(len(f["tareas_b"]), len(f["tareas_j"])) * 2 + 1
        ws.row_dimensions[r].height = max(48, 12.5 * lineas)
        r += 1

    rango_estado = f"{ultima}5:{ultima}{r - 1}"
    for texto, color in (("En curso", "FFD966"), ("Concluida", "C6EFCE"), ("Pendiente", "EDEDED")):
        ws.conditional_formatting.add(rango_estado, FormulaRule(
            formula=[f'{ultima}5="{texto}"'], fill=PatternFill("solid", fgColor=color),
            font=Font(name=FUENTE, bold=texto == "En curso")))
    ws.freeze_panes = "H5"
    ws.auto_filter.ref = f"A4:{ultima}{r - 1}"
    ws.page_setup.orientation = "landscape"
    ws.page_setup.fitToWidth = 1
    ws.page_setup.fitToHeight = 0
    ws.sheet_properties.pageSetUpPr.fitToPage = True
    ws.print_title_rows = "4:4"

    notas = wb.create_sheet("Notas")
    texto = [
        ("Cómo leer el calendario", True),
        ("Cada renglón es una semana del Cronograma final, de agosto a diciembre de 2026 (periodo 2026-2, 20 semanas).", False),
        ("“Actividades del Cronograma” repite el renglón del Cronograma que cubre esa semana; "
         "“Tareas de la semana” lo desglosa en trabajo concreto.", False),
        ("El reporte técnico TT-II corre en paralelo de agosto a diciembre "
         "todas las semanas: cada semana se documenta lo que se hizo.", False),
        ("La columna Estado se calcula con la fecha del equipo (HOY): Concluida, En curso o Pendiente.", False),
        ("La columna Avance real registra lo ya desarrollado según el historial del repositorio. "
         "Varios módulos se adelantaron; la planeación sigue apegada al Cronograma y esas semanas se "
         "usarán para pruebas, ajustes y documentación del módulo correspondiente.", False),
        (NOTA_DIFERENCIAS, False),
        ("", False),
        ("Colores de Sprint", True),
        ("Sprint 3 — Desarrollo (ago–dic)", 3), ("Sprint 4 — Integración, pruebas y evaluación (nov–dic)", 4),
        ("", False),
        ("Reuniones de seguimiento (sección 3.1.3)", True),
        ("Quincenal: revisión de avances con la directora (Product Owner).", False),
        ("Semanal: seguimiento de metodología y tablero Kanban (Sin empezar / En desarrollo / Finalizado).", False),
        ("", False),
        ("Este archivo se genera con docs/calendario-semanal/generar.py; para cambiarlo, edita el script y vuelve a correrlo.", False),
    ]
    notas.column_dimensions["A"].width = 110
    for i, (t, estilo) in enumerate(texto, 1):
        cel = notas.cell(row=i, column=1, value=t)
        cel.font = Font(name=FUENTE, size=10, bold=estilo is True)
        cel.alignment = Alignment(wrap_text=True, vertical="top")
        if isinstance(estilo, int) and estilo is not True and estilo is not False:
            cel.fill = PatternFill("solid", fgColor=FILL_SPRINT[estilo])
    wb.save(ruta)


# ──────────────────────────── Markdown ────────────────────────────
def generar_md(ruta, hoy):
    def estado(f):
        return "✅" if hoy > f["fin"] else ("▶️ **en curso**" if hoy >= f["ini"] else "")

    sprints = {3: "Sprint 3 — Desarrollo (2026-2, agosto–noviembre)",
               4: "Sprint 4 — Integración, pruebas y evaluación (2026-2, diciembre)"}
    out = [
        "# Calendario semanal del TT 2026-B162 · agosto–diciembre 2026",
        "",
        f"> {TITULO.capitalize().replace('a1 y a2', 'A1 y A2')}.",
        ">",
        f"> Integrantes: **{B}** (Scrum Master / Lead Developer) y **{J}** (Developer).",
        "> Product Owner: M. en C. Nidia Asunción Cortez Duarte.",
        "",
        "Desglose por semana del **Cronograma final** de agosto a diciembre (periodo 2026-2). Las actividades y sus semanas son las del "
        "Cronograma; aquí se detalla qué hace cada integrante cada semana. Semanas: S1 = días 1–7, "
        "S2 = 8–14, S3 = 15–21, S4 = 22 a fin de mes.",
        "",
        "La versión para entregar es `Calendario semanal.xlsx` (misma información, con estado "
        "automático según la fecha). Ambos archivos se generan con `generar.py`.",
        "",
        f"Estado al **{hoy:%d-%m-%Y}**: ✅ concluida · ▶️ en curso · (vacío) pendiente.",
        "",
        "## Plan contra lo desarrollado",
        "",
        "La planeación se mantiene apegada al Cronograma. Lo que ya está desarrollado antes de su "
        "semana se marca en *Avance real*; esas semanas se usan para pruebas, ajustes y para "
        "documentar el módulo en el capítulo 5 del reporte.",
        "",
        "| Actividad del Cronograma | Semanas planeadas | Avance real |",
        "|---|---|---|",
        "| Interfaz y estructura básica | Ago S4 – Sep S2 | Arquitectura 14-sep, Auth 20-sep, recuperación 25-sep (≈2 semanas tarde) |",
        "| Integración de chatbot con IA | Sep S3 – Oct S1 | Chat en texto 29-sep (a tiempo) |",
        "| Módulo de reconocimiento de voz | Oct S2 – Oct S3 | Azure + evaluación de pronunciación ya hechos (06-oct); **falta voz en el chat** |",
        "| Módulos de gramática y vocabulario | Oct S4 – Nov S1 | Hecho 26-sep – 01-oct; 24 lecciones (adelantado) |",
        "| Ejercicios de gramática y vocabulario | Nov S2 – Nov S3 | Evaluaciones y SRS 29-sep (adelantado) |",
        "| Desarrollo de mini-juegos | Nov S4 – Dic S1 | Los 4 juegos 06-oct (adelantado) |",
        "| Integración (progreso, notificaciones) | Nov S4 – Dic S2 | Progreso, insignias, notificaciones y 14 pantallas 11-oct (adelantado) |",
        "",
        f"{NOTA_DIFERENCIAS}",
        "",
    ]
    actual = None
    for f in filas():
        if f["sprint"] != actual:
            actual = f["sprint"]
            out += [f"## {sprints[actual]}", ""]
        est = estado(f)
        out += [f"### Semana {f['num']} · {f['mes']} {f['s']} ({f['ini']:%d/%m} – {f['fin']:%d/%m}) {est}".rstrip(), ""]
        out += [f"**{B.split()[0]}** — {' · '.join(f['act_b'])}", "", lista(f["tareas_b"]).replace("• ", "- "), ""]
        out += [f"**{J.split()[0]} {J.split()[1]}** — {' · '.join(f['act_j'])}", "",
                lista(f["tareas_j"]).replace("• ", "- "), ""]
        out += [f"**Entregable:** {f['entregable']}", ""]
        if f["real"]:
            out += [f"**Avance real:** {f['real']}", ""]
    ruta.write_text("\n".join(out), encoding="utf-8")


if __name__ == "__main__":
    generar_xlsx(AQUI / "Calendario semanal.xlsx")
    generar_md(AQUI / "calendario-semanal.md", dt.date.today())
    print("Listo:", AQUI / "Calendario semanal.xlsx", AQUI / "calendario-semanal.md")
