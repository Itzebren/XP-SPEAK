"""Genera el arte vectorial de XP-SPEAK (docs/diseno.md, sección Assets).

Uso: python3 docs/diseno-arte/generar.py
Escribe un SVG por pieza en docs/diseno-arte/svg/. Para pasarlos a Android
(VectorDrawable en res/drawable/): python3 docs/diseno-arte/a_vector.py
Solo se usan formas que VectorDrawable soporta: nada de
<text>, filtros ni máscaras; las letras se dibujan como trazos.
"""
import math
import os
import re

# Paleta del Trabajo Terminal (Ilustración 26)
AZUL_CLARO = "#C0D6EB"
AZUL = "#4B7BBE"
MARINO = "#232074"
NARANJA = "#FF8C00"
AMARILLO = "#FFC107"
BLANCO = "#FFFFFF"
GRIS = "#9E9E9E"
# Derivados (solo para volumen en las ilustraciones)
CARA = "#E6F0FA"
MEJILLA = "#F7A1A8"
MARINO_MEDIO = "#34409A"

SALIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "svg")


def f(n):
    return f"{n:.2f}".rstrip("0").rstrip(".")


def estrella(cx, cy, R, r, puntas=5, rot=-90):
    pts = []
    for i in range(puntas * 2):
        rad = R if i % 2 == 0 else r
        a = math.radians(rot + i * 180 / puntas)
        pts.append(f"{f(cx + rad * math.cos(a))} {f(cy + rad * math.sin(a))}")
    return "M" + " L".join(pts) + " Z"


def destello(x, y, s):
    """Destello de 4 puntas."""
    return (f"M{f(x)} {f(y - s)} Q{f(x)} {f(y)} {f(x + s)} {f(y)} Q{f(x)} {f(y)} {f(x)} {f(y + s)} "
            f"Q{f(x)} {f(y)} {f(x - s)} {f(y)} Q{f(x)} {f(y)} {f(x)} {f(y - s)} Z")


def espejo(d, ancho):
    """Refleja horizontalmente un path con comandos absolutos (M L C Q H V Z)."""
    salida, cmd, i = [], None, 0
    tokens = re.findall(r"[MLCQHVZ]|-?\d*\.?\d+", d)
    while i < len(tokens):
        t = tokens[i]
        if t in "MLCQHVZ":
            cmd = t
            salida.append(t)
            i += 1
            continue
        if cmd == "H":
            salida.append(f(ancho - float(t)))
            i += 1
        elif cmd == "V":
            salida.append(t)
            i += 1
        else:
            salida.append(f"{f(ancho - float(t))} {tokens[i + 1]}")
            i += 2
    return " ".join(salida)


def p(d, fill="none", stroke=None, sw=None, extra=""):
    s = f'<path d="{d}" fill="{fill}"'
    if stroke:
        s += f' stroke="{stroke}" stroke-width="{sw}" stroke-linecap="round" stroke-linejoin="round"'
    return s + extra + "/>"


def circulo(cx, cy, r, fill, stroke=None, sw=None):
    s = f'<circle cx="{f(cx)}" cy="{f(cy)}" r="{f(r)}" fill="{fill}"'
    if stroke:
        s += f' stroke="{stroke}" stroke-width="{sw}"'
    return s + "/>"


def rect(x, y, w, h, rx, fill, stroke=None, sw=None):
    s = f'<rect x="{f(x)}" y="{f(y)}" width="{f(w)}" height="{f(h)}" rx="{f(rx)}" fill="{fill}"'
    if stroke:
        s += f' stroke="{stroke}" stroke-width="{sw}"'
    return s + "/>"


def trazo(d, color=MARINO, sw=4):
    return p(d, "none", color, sw)


# --- Letras como trazos (x, y = esquina superior izquierda) ---
def letra(c, x, y, w, h):
    if c == "A":
        return f"M{f(x)} {f(y + h)} L{f(x + w / 2)} {f(y)} L{f(x + w)} {f(y + h)} M{f(x + w * .25)} {f(y + h * .62)} L{f(x + w * .75)} {f(y + h * .62)}"
    if c == "1":
        return f"M{f(x + w * .15)} {f(y + h * .25)} L{f(x + w * .6)} {f(y)} L{f(x + w * .6)} {f(y + h)}"
    if c == "2":
        return (f"M{f(x)} {f(y + h * .28)} C{f(x)} {f(y - h * .06)} {f(x + w)} {f(y - h * .06)} {f(x + w)} {f(y + h * .3)} "
                f"C{f(x + w)} {f(y + h * .55)} {f(x)} {f(y + h * .78)} {f(x)} {f(y + h)} L{f(x + w)} {f(y + h)}")
    if c == "7":
        return f"M{f(x)} {f(y)} L{f(x + w)} {f(y)} L{f(x + w * .35)} {f(y + h)}"
    if c == "E":
        return f"M{f(x + w)} {f(y)} L{f(x)} {f(y)} L{f(x)} {f(y + h)} L{f(x + w)} {f(y + h)} M{f(x)} {f(y + h / 2)} L{f(x + w * .8)} {f(y + h / 2)}"
    if c == "N":
        return f"M{f(x)} {f(y + h)} L{f(x)} {f(y)} L{f(x + w)} {f(y + h)} L{f(x + w)} {f(y)}"
    if c == "X":
        return f"M{f(x)} {f(y)} L{f(x + w)} {f(y + h)} M{f(x + w)} {f(y)} L{f(x)} {f(y + h)}"
    if c == "P":
        return (f"M{f(x)} {f(y + h)} L{f(x)} {f(y)} L{f(x + w * .55)} {f(y)} C{f(x + w * 1.05)} {f(y)} "
                f"{f(x + w * 1.05)} {f(y + h * .55)} {f(x + w * .55)} {f(y + h * .55)} L{f(x)} {f(y + h * .55)}")
    if c == "H":
        return f"M{f(x)} {f(y)} L{f(x)} {f(y + h)} M{f(x + w)} {f(y)} L{f(x + w)} {f(y + h)} M{f(x)} {f(y + h / 2)} L{f(x + w)} {f(y + h / 2)}"
    if c == "i":
        return f"M{f(x + w / 2)} {f(y + h * .35)} L{f(x + w / 2)} {f(y + h)} M{f(x + w / 2)} {f(y + h * .08)} L{f(x + w / 2)} {f(y + h * .1)}"
    raise ValueError(c)


def palabra(texto, x, y, h, sw, color=MARINO, ancho=0.62, espacio=0.3):
    out, cx = [], x
    for c in texto:
        w = h * (0.25 if c == "i" else ancho)
        out.append(trazo(letra(c, cx, y, w, h), color, sw))
        cx += w + h * espacio
    return "".join(out)


# --- Mascota (coordenadas propias: 200 x 220) ---
def cabeza(expresion="feliz"):
    e = []
    # Antena
    e.append(trazo("M100 38 L100 20", MARINO, 5))
    e.append(circulo(100, 15, 9, NARANJA, MARINO, 4))
    # Diadema de los audífonos
    e.append(trazo("M40 74 C40 22 160 22 160 74", MARINO, 12))
    e.append(trazo("M40 74 C40 22 160 22 160 74", NARANJA, 6))
    # Casco y cara
    e.append(rect(46, 34, 108, 86, 40, AZUL, MARINO, 4))
    e.append(p("M60 46 C70 40 84 39 96 40", "none", "#7FA6DB", 4))
    e.append(rect(58, 46, 84, 64, 30, CARA, MARINO, 3))
    if expresion == "feliz":
        for cx in (83, 117):
            e.append(f'<ellipse cx="{cx}" cy="74" rx="9" ry="11" fill="{MARINO}"/>')
            e.append(circulo(cx + 3, 69, 3.5, BLANCO))
            e.append(circulo(cx - 3, 79, 1.6, BLANCO))
        e.append(f'<ellipse cx="69" cy="91" rx="6" ry="4" fill="{MEJILLA}"/>')
        e.append(f'<ellipse cx="131" cy="91" rx="6" ry="4" fill="{MEJILLA}"/>')
        e.append(p("M89 91 Q100 104 111 91 Z", "#E2565F", MARINO, 3))
    else:  # preocupada
        for cx in (83, 117):
            e.append(f'<ellipse cx="{cx}" cy="76" rx="8" ry="10" fill="{MARINO}"/>')
            e.append(circulo(cx + 2, 72, 3, BLANCO))
        e.append(trazo("M73 62 L90 58", MARINO, 3.5))
        e.append(trazo("M127 62 L110 58", MARINO, 3.5))
        e.append(f'<ellipse cx="100" cy="98" rx="8" ry="6" fill="#E2565F" stroke="{MARINO}" stroke-width="3"/>')
        e.append(p("M140 54 C146 62 146 68 140 70 C134 68 134 62 140 54 Z", "#8ECDF5", MARINO, 2))
    # Orejeras y micrófono
    e.append(rect(31, 58, 20, 38, 9, NARANJA, MARINO, 4))
    e.append(rect(149, 58, 20, 38, 9, NARANJA, MARINO, 4))
    e.append(trazo("M159 94 C160 112 140 112 128 109", MARINO, 4))
    e.append(circulo(126, 108, 6, NARANJA, MARINO, 3))
    return "".join(e)


def mascota(expresion="feliz", saluda=True, con_libro=False):
    e = []
    brazo = lambda d: trazo(d, MARINO, 20) + trazo(d, AZUL, 12)
    # Brazos (detrás del torso)
    if con_libro:
        e.append(brazo("M60 136 L74 160"))
        e.append(brazo("M140 136 L126 160"))
    elif expresion == "preocupada":
        e.append(brazo("M60 132 L36 102 L58 70"))
        e.append(brazo("M140 132 L164 102 L142 70"))
    else:
        e.append(brazo("M60 134 L30 100" if saluda else "M60 134 L40 160"))
        e.append(brazo("M140 134 L162 160"))
    # Piernas y pies
    e.append(rect(74, 176, 20, 28, 8, AZUL, MARINO, 4))
    e.append(rect(106, 176, 20, 28, 8, AZUL, MARINO, 4))
    e.append(rect(64, 198, 34, 15, 7, NARANJA, MARINO, 4))
    e.append(rect(102, 198, 34, 15, 7, NARANJA, MARINO, 4))
    # Torso con placa y estrella
    e.append(rect(56, 112, 88, 74, 24, AZUL, MARINO, 4))
    e.append(rect(76, 126, 48, 40, 12, NARANJA, MARINO, 3))
    e.append(p(estrella(100, 147, 13, 5.5), AMARILLO, MARINO, 2.5))
    # Manos
    if con_libro:
        pass
    elif expresion == "preocupada":
        e.append(circulo(60, 66, 11, NARANJA, MARINO, 4))
        e.append(circulo(140, 66, 11, NARANJA, MARINO, 4))
    else:
        e.append(circulo(28, 94, 11, NARANJA, MARINO, 4) if saluda else circulo(38, 166, 10, NARANJA, MARINO, 4))
        e.append(circulo(165, 165, 10, NARANJA, MARINO, 4))
    e.append(cabeza(expresion))
    if expresion == "preocupada":
        # Las manos van sobre el casco
        e.append(circulo(56, 64, 11, NARANJA, MARINO, 4))
        e.append(circulo(144, 64, 11, NARANJA, MARINO, 4))
    if con_libro:
        # Libro abierto "EN · A1|A2"
        e.append(p("M100 150 C88 142 70 142 56 146 L56 190 C70 186 88 186 100 194 Z", NARANJA, MARINO, 4))
        e.append(p(espejo("M100 150 C88 142 70 142 56 146 L56 190 C70 186 88 186 100 194 Z", 200), AMARILLO, MARINO, 4))
        # Bandera (izquierda) y "EN" (derecha)
        e.append(rect(64, 156, 24, 16, 2, "#1D3E8A", MARINO, 2))
        e.append(trazo("M64 156 L88 172 M88 156 L64 172", BLANCO, 3))
        e.append(trazo("M76 156 L76 172 M64 164 L88 164", "#E2343F", 3))
        e.append(palabra("EN", 108, 155, 14, 3.5))
        e.append(palabra("A1", 107, 175, 9, 2.4, ancho=.6))
        e.append(trazo("M122 175 L122 184", MARINO, 2))
        e.append(palabra("A2", 126, 175, 9, 2.4, ancho=.6))
        e.append(circulo(50, 192, 9, NARANJA, MARINO, 4))
        e.append(circulo(150, 192, 9, NARANJA, MARINO, 4))
    return "".join(e)


def svg(nombre, w, h, cuerpo, defs=""):
    os.makedirs(SALIDA, exist_ok=True)
    contenido = (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" viewBox="0 0 {w} {h}">'
                 + (f"<defs>{defs}</defs>" if defs else "") + cuerpo + "</svg>\n")
    with open(os.path.join(SALIDA, nombre + ".svg"), "w") as fh:
        fh.write(contenido)


def grupo(cuerpo, tx, ty, s):
    return f'<g transform="translate({f(tx)} {f(ty)}) scale({f(s)})">{cuerpo}</g>'


# --- Insignias e íconos del inicio: medallón marino con aro dorado ---
DEGRADADO_MEDALLA = (f'<radialGradient id="fondo" gradientUnits="userSpaceOnUse" cx="60" cy="44" r="62">'
                     f'<stop offset="0" stop-color="#5A86C8"/><stop offset="1" stop-color="{MARINO_MEDIO}"/></radialGradient>')


def medalla(icono, destellos=True):
    e = [circulo(60, 60, 58, MARINO), circulo(60, 60, 52, "url(#fondo)", AMARILLO, 3)]
    if destellos:
        e += [p(destello(28, 34, 5), BLANCO), p(destello(94, 88, 4), BLANCO), p(destello(90, 30, 3), AMARILLO)]
    return "".join(e) + icono


def gamepad(cx, cy, s=1.0):
    d = "M34 58 Q34 50 44 50 L76 50 Q86 50 86 58 L93 82 Q95 94 84 91 L74 80 L46 80 L36 91 Q25 94 27 82 Z"
    cuerpo = (p(d, AZUL_CLARO, MARINO, 3.5)
              + trazo("M44 59 L44 71 M38 65 L50 65", MARINO, 4)
              + circulo(73, 61, 4, NARANJA, MARINO, 2) + circulo(80, 68, 4, AMARILLO, MARINO, 2)
              + circulo(66, 68, 3, AZUL, MARINO, 1.5))
    return grupo(cuerpo, cx - 60 * s, cy - 70 * s, s)


def trofeo_alado():
    ala = "M44 58 C30 60 18 52 14 38 C22 42 26 42 31 42 C24 38 22 33 22 26 C29 33 36 36 44 38 Z"
    e = [p(ala, BLANCO, MARINO, 2.5), p(espejo(ala, 120), BLANCO, MARINO, 2.5)]
    e.append(trazo("M44 40 C33 40 33 57 46 58", MARINO, 3))
    e.append(trazo(espejo("M44 40 C33 40 33 57 46 58", 120), MARINO, 3))
    e.append(p("M43 33 L77 33 L77 50 C77 63 69 71 60 71 C51 71 43 63 43 50 Z", AMARILLO, MARINO, 3))
    e.append(p(estrella(60, 48, 8, 3.4), BLANCO))
    e.append(rect(55, 70, 10, 10, 1, NARANJA, MARINO, 2.5))
    e.append(rect(44, 80, 32, 9, 2.5, NARANJA, MARINO, 2.5))
    e.append(p("M49 30 L51 18 L56 25 L60 15 L64 25 L69 18 L71 30 Z", AMARILLO, MARINO, 2.2))
    return "".join(e)


def cabeza_robot(cx, cy, s):
    return grupo(cabeza(), cx - 100 * s, cy - 66 * s, s)


def libro_abierto():
    hoja = "M60 44 C50 37 36 37 26 41 L26 80 C36 76 50 76 60 83 Z"
    e = [rect(28, 82, 64, 9, 3, NARANJA, MARINO, 2.5), rect(33, 90, 54, 8, 3, AZUL_CLARO, MARINO, 2.5)]
    e += [p(hoja, BLANCO, MARINO, 3), p(espejo(hoja, 120), "#FFF3D6", MARINO, 3)]
    e.append(trazo("M33 50 C40 48 47 48 53 51 M33 58 C40 56 47 56 53 59 M33 66 C40 64 47 64 53 67", AZUL, 2))
    e.append(trazo(espejo("M33 50 C40 48 47 48 53 51 M33 58 C40 56 47 56 53 59 M33 66 C40 64 47 64 53 67", 120), AZUL, 2))
    e.append(p(estrella(86, 32, 7, 3), AMARILLO, MARINO, 1.8))
    return "".join(e)


def flama(cx=60, cy=60, s=1.0, con_siete=False):
    exterior = "M60 20 C67 35 85 44 85 67 C85 84 74 96 60 96 C46 96 35 84 35 67 C35 55 43 48 46 39 C50 47 52 51 56 53 C56 41 56 31 60 20 Z"
    interior = "M60 50 C64 60 75 64 75 77 C75 86 68 92 60 92 C52 92 45 86 45 77 C45 68 54 62 60 50 Z"
    cuerpo = p(exterior, NARANJA, MARINO, 3) + p(interior, AMARILLO)
    if con_siete:
        cuerpo += trazo(letra("7", 53, 66, 14, 20), MARINO, 5)
    return grupo(cuerpo, cx - 60 * s, cy - 58 * s, s)


INSIGNIAS = {
    "primer_logro": trofeo_alado(),
    "habla_con_ia": cabeza_robot(60, 64, .4) + p("M82 26 L104 26 Q108 26 108 30 L108 40 Q108 44 104 44 L92 44 L86 50 L87 44 L82 44 Q78 44 78 40 L78 30 Q78 26 82 26 Z", BLANCO, MARINO, 2) + trazo("M85 35 L101 35", AZUL, 2.5),
    "estrella_minijuego": gamepad(60, 72, .85) + p(estrella(60, 34, 13, 5.5), AMARILLO, MARINO, 2.5),
    "racha_7": flama(con_siete=True),
    "explorador_a1": (p("M60 21 L65 31 L55 31 Z", NARANJA, MARINO, 1.5) + circulo(60, 62, 28, AZUL_CLARO, MARINO, 3)
                      + circulo(60, 62, 22, "none", AZUL, 2) + palabra("A1", 45, 52, 20, 4, ancho=.75)),
    "graduado_a2": (rect(36, 28, 48, 60, 4, BLANCO, MARINO, 3) + palabra("A2", 46, 36, 16, 3.5, ancho=.75)
                    + trazo("M44 62 L76 62 M44 70 L68 70", AZUL_CLARO, 3)
                    + p("M70 86 L67 100 L73 96 L78 100 L80 87 Z", NARANJA, MARINO, 2) + circulo(76, 82, 9, AMARILLO, MARINO, 2.5)
                    + p(estrella(76, 82, 4.5, 2), NARANJA)),
    "maestro_speaking": (rect(48, 34, 24, 38, 12, AZUL_CLARO, MARINO, 3) + trazo("M53 46 L67 46 M53 54 L67 54 M53 62 L67 62", AZUL, 2)
                         + trazo("M42 58 C42 79 78 79 78 58", MARINO, 3.5) + trazo("M60 76 L60 88 M48 90 L72 90", MARINO, 4)
                         + p("M46 31 L48 19 L54 25 L60 15 L66 25 L72 19 L74 31 Z", AMARILLO, MARINO, 2)),
    "jugador_experto": (gamepad(60, 70, 1.0) + p(estrella(40, 32, 6, 2.6), AMARILLO, MARINO, 1.8)
                        + p(estrella(60, 26, 9, 3.8), AMARILLO, MARINO, 2) + p(estrella(80, 32, 6, 2.6), AMARILLO, MARINO, 1.8)),
    "desafio_semanal": (trazo("M50 62 L26 86 M58 70 L40 92 M44 56 L24 72", NARANJA, 4.5)
                        + p(estrella(68, 50, 22, 9), AMARILLO, MARINO, 3) + p(destello(88, 80, 5), AMARILLO)),
}

INICIO = {
    "inicio_lecciones": libro_abierto(),
    "inicio_minijuegos": gamepad(60, 66, 1.05) + circulo(30, 88, 6, AMARILLO, MARINO, 2) + circulo(92, 92, 5, AMARILLO, MARINO, 2) + p(estrella(60, 30, 7, 3), AMARILLO, MARINO, 1.8),
    "inicio_ia": cabeza_robot(60, 62, .5),
    "inicio_progreso": trofeo_alado(),
}


def moneda(cx, cy, r):
    return (circulo(cx, cy, r, AMARILLO, "#E08A00", r * .12) + circulo(cx, cy, r * .74, "none", "#FFE082", r * .08)
            + palabra("XP", cx - r * .52, cy - r * .3, r * .6, r * .14, "#B86E00", ancho=.75, espacio=.2))


def portada(nombre, fondo, contenido):
    svg(nombre, 160, 160, f'<rect x="0" y="0" width="160" height="160" rx="28" fill="{fondo}"/>' + contenido)


def burbuja(x, y, w, h, fill=BLANCO, cola_izq=True):
    cx = x + (16 if cola_izq else w - 16)
    d = (f"M{x + 8} {y} L{x + w - 8} {y} Q{x + w} {y} {x + w} {y + 8} L{x + w} {y + h - 8} Q{x + w} {y + h} {x + w - 8} {y + h} "
         f"L{cx + 6} {y + h} L{cx - 4 if cola_izq else cx + 10} {y + h + 9} L{cx - 2} {y + h} L{x + 8} {y + h} Q{x} {y + h} {x} {y + h - 8} "
         f"L{x} {y + 8} Q{x} {y} {x + 8} {y} Z")
    return p(d, fill, MARINO, 2.5)


def ficha(x, y, w, color):
    return rect(x, y, w, 18, 6, color, MARINO, 2.5) + trazo(f"M{x + 8} {y + 9} L{x + w - 8} {y + 9}", BLANCO, 3)


PIEL = "#F2C7A0"
CREMA = "#FFE3BF"
AMARILLO_CLARO = "#FFF1C2"

# Portadas de lección: un ícono del módulo temático sobre un azulejo (160 x 160).
# La clave es el id de la lección con "_" en vez de "-".
LECCIONES = {
    "a1_saludos_presentaciones": (AZUL_CLARO,
        "".join(rect(x, y, 14, 40, 7, PIEL, MARINO, 3) for x, y in ((52, 40), (68, 32), (84, 34), (100, 42)))
        + p("M50 70 L116 70 L116 98 C116 116 104 126 84 126 C64 126 50 116 50 98 Z", PIEL, MARINO, 3)
        + p("M50 84 C40 76 32 82 38 92 L52 108", PIEL, MARINO, 3) + p(destello(124, 40, 8), AMARILLO)),
    "a1_informacion_personal": (CREMA,
        rect(30, 46, 100, 70, 12, BLANCO, MARINO, 3) + rect(30, 46, 100, 16, 8, AZUL, MARINO, 3)
        + circulo(58, 86, 11, PIEL, MARINO, 2.5) + p("M42 110 C42 98 74 98 74 110 Z", NARANJA, MARINO, 2.5)
        + trazo("M84 82 L116 82 M84 94 L110 94 M84 106 L100 106", AZUL, 4)),
    "a1_numeros_hora": (AMARILLO_CLARO,
        circulo(80, 82, 42, BLANCO, MARINO, 3.5) + circulo(80, 82, 34, "none", AZUL_CLARO, 3)
        + trazo("M80 52 L80 58 M80 106 L80 112 M50 82 L56 82 M104 82 L110 82", MARINO, 3.5)
        + trazo("M80 82 L80 62 M80 82 L96 90", NARANJA, 5) + circulo(80, 82, 4, MARINO)),
    "a1_familia_amigos": (AZUL_CLARO,
        circulo(56, 64, 15, PIEL, MARINO, 3) + p("M34 118 C34 92 78 92 78 118 Z", AZUL, MARINO, 3)
        + circulo(104, 64, 15, "#8D5A3B", MARINO, 3) + p("M82 118 C82 92 126 92 126 118 Z", NARANJA, MARINO, 3)
        + circulo(80, 92, 10, "#E8B48A", MARINO, 3) + p("M66 126 C66 108 94 108 94 126 Z", AMARILLO, MARINO, 3)),
    "a1_ropa_colores": (CREMA,
        p("M58 36 L46 40 L24 58 L38 76 L50 68 L50 124 L110 124 L110 68 L122 76 L136 58 L114 40 L102 36 C98 46 62 46 58 36 Z", NARANJA, MARINO, 3.5)
        + trazo("M50 92 L110 92", AMARILLO, 6) + trazo("M50 104 L110 104", AZUL, 6)),
    "a1_casa_hogar": (AMARILLO_CLARO,
        p("M80 30 L130 72 L30 72 Z", NARANJA, MARINO, 3.5) + rect(42, 72, 76, 52, 4, BLANCO, MARINO, 3.5)
        + rect(70, 92, 20, 32, 4, AZUL, MARINO, 3) + rect(50, 82, 14, 14, 3, AZUL_CLARO, MARINO, 2.5)
        + rect(96, 82, 14, 14, 3, AZUL_CLARO, MARINO, 2.5)),
    "a1_rutina_diaria": (AZUL_CLARO,
        p("M40 50 C40 34 60 30 66 42 Z", AMARILLO, MARINO, 3) + p(espejo("M40 50 C40 34 60 30 66 42 Z", 160), AMARILLO, MARINO, 3)
        + trazo("M56 118 L48 128 M104 118 L112 128", MARINO, 4)
        + circulo(80, 84, 38, BLANCO, MARINO, 3.5) + trazo("M80 84 L80 62 M80 84 L98 84", MARINO, 4.5) + circulo(80, 84, 4, NARANJA)),
    "a1_comida_bebida": (CREMA,
        p("M80 52 C64 38 34 46 36 78 C38 108 60 126 80 116 C100 126 122 108 124 78 C126 46 96 38 80 52 Z", "#E2565F", MARINO, 3.5)
        + trazo("M80 52 C80 42 84 34 90 30", MARINO, 4) + p("M86 40 C96 28 112 32 114 36 C106 44 94 44 86 40 Z", "#2E9E5B", MARINO, 2.5)
        + p("M54 66 C50 74 50 82 52 88", "none", BLANCO, 5)),
    "a1_ciudad_lugares": (AZUL_CLARO,
        rect(28, 60, 34, 66, 4, AZUL, MARINO, 3) + rect(62, 34, 38, 92, 4, BLANCO, MARINO, 3) + rect(100, 72, 32, 54, 4, NARANJA, MARINO, 3)
        + "".join(rect(x, y, 8, 8, 1.5, AMARILLO) for x in (70, 84) for y in (44, 60, 76, 92))
        + "".join(rect(36, y, 18, 6, 1.5, AZUL_CLARO) for y in (70, 84, 98)) + trazo("M20 126 L140 126", MARINO, 4)),
    "a1_tiempo_libre": (AMARILLO_CLARO,
        circulo(80, 80, 42, BLANCO, MARINO, 3.5)
        + p("M80 62 L97 74 L91 94 L69 94 L63 74 Z", MARINO)
        + trazo("M80 62 L80 40 M97 74 L118 66 M91 94 L104 114 M69 94 L56 114 M63 74 L42 66", MARINO, 3)),
    "a1_clima_ahora": (AZUL_CLARO,
        circulo(64, 62, 22, AMARILLO, MARINO, 3)
        + trazo("M64 28 L64 34 M30 62 L36 62 M40 38 L44 42 M88 38 L84 42", NARANJA, 4)
        + p("M52 120 C36 120 32 98 50 94 C52 78 76 74 84 88 C92 76 116 80 116 98 C132 100 130 120 114 120 Z", BLANCO, MARINO, 3.5)),
    "a1_compras": (CREMA,
        p("M40 58 L120 58 L114 128 L46 128 Z", NARANJA, MARINO, 3.5) + trazo("M62 66 L62 50 C62 30 98 30 98 50 L98 66", MARINO, 4)
        + p(estrella(80, 96, 14, 6), AMARILLO, MARINO, 2.5)),
    "a2_anecdotas": (AMARILLO_CLARO,
        p("M80 70 C68 62 48 62 32 66 L32 122 C48 118 68 118 80 126 Z", BLANCO, MARINO, 3)
        + p(espejo("M80 70 C68 62 48 62 32 66 L32 122 C48 118 68 118 80 126 Z", 160), BLANCO, MARINO, 3)
        + burbuja(78, 22, 56, 32, AZUL_CLARO) + circulo(94, 38, 3, MARINO) + circulo(106, 38, 3, MARINO) + circulo(118, 38, 3, MARINO)),
    "a2_clima_excursiones": (AZUL_CLARO,
        circulo(116, 46, 14, AMARILLO, MARINO, 3)
        + p("M18 128 L64 54 L92 98 L106 78 L142 128 Z", AZUL, MARINO, 3.5)
        + p("M64 54 L76 74 L66 70 L58 78 L54 70 Z", BLANCO, MARINO, 2.5)),
    "a2_comparar_lugares": (CREMA,
        p("M26 46 L60 34 L100 46 L134 34 L134 118 L100 130 L60 118 L26 130 Z", BLANCO, MARINO, 3.5)
        + trazo("M60 34 L60 118 M100 46 L100 130", AZUL_CLARO, 3)
        + p("M80 60 C68 60 62 70 66 80 L80 102 L94 80 C98 70 92 60 80 60 Z", NARANJA, MARINO, 3) + circulo(80, 74, 5, BLANCO)),
    "a2_describir_personas_cosas": (AMARILLO_CLARO,
        circulo(70, 70, 32, AZUL_CLARO, MARINO, 4) + circulo(70, 64, 9, PIEL, MARINO, 2.5)
        + p("M54 90 C54 78 86 78 86 90 Z", NARANJA, MARINO, 2.5) + trazo("M94 94 L124 124", MARINO, 12) + trazo("M94 94 L124 124", NARANJA, 6)),
    "a2_fin_de_semana": (AZUL_CLARO,
        rect(32, 40, 96, 86, 12, BLANCO, MARINO, 3.5) + rect(32, 40, 96, 22, 10, NARANJA, MARINO, 3.5)
        + trazo("M54 32 L54 48 M106 32 L106 48", MARINO, 4.5)
        + "".join(rect(x, y, 12, 10, 2, AZUL_CLARO) for x in (44, 62, 80) for y in (72, 90, 108))
        + p(estrella(106, 98, 14, 6), AMARILLO, MARINO, 2.5)),
    "a2_llamadas_mensajes": (CREMA,
        rect(42, 26, 54, 104, 12, MARINO) + rect(48, 38, 42, 76, 4, AZUL_CLARO) + circulo(69, 122, 3.5, BLANCO)
        + burbuja(84, 52, 50, 30, BLANCO) + trazo("M94 64 L124 64 M94 72 L114 72", AZUL, 3)),
    "a2_novedades": (AMARILLO_CLARO,
        p("M36 68 L60 68 L112 40 L112 120 L60 92 L36 92 Z", NARANJA, MARINO, 3.5) + rect(42, 92, 16, 28, 4, AZUL, MARINO, 3)
        + trazo("M124 62 C132 72 132 88 124 98", MARINO, 4) + trazo("M134 52 C146 68 146 92 134 108", MARINO, 4)),
    "a2_planes_futuro": (AZUL,
        p("M80 22 C104 40 110 72 100 104 L60 104 C50 72 56 40 80 22 Z", BLANCO, MARINO, 3.5) + circulo(80, 62, 10, AZUL_CLARO, MARINO, 3)
        + p("M60 84 L42 108 L60 104 Z", NARANJA, MARINO, 3) + p(espejo("M60 84 L42 108 L60 104 Z", 160), NARANJA, MARINO, 3)
        + p("M66 104 L80 132 L94 104 Z", AMARILLO, MARINO, 3) + p(destello(30, 40, 6), AMARILLO) + p(destello(130, 120, 5), BLANCO)),
    "a2_salud_ejercicio": (CREMA,
        p("M80 128 C40 100 24 80 24 60 C24 40 40 30 54 30 C66 30 76 38 80 46 C84 38 94 30 106 30 C120 30 136 40 136 60 C136 80 120 100 80 128 Z", "#E2565F", MARINO, 3.5)
        + trazo("M34 78 L60 78 L68 62 L80 96 L90 70 L98 78 L126 78", BLANCO, 5)),
    "a2_servicios_compras": (AZUL_CLARO,
        rect(32, 66, 96, 62, 4, BLANCO, MARINO, 3.5) + rect(66, 92, 28, 36, 3, AZUL, MARINO, 3)
        + p("M26 66 L36 36 L124 36 L134 66 C126 76 114 76 108 66 C100 76 88 76 80 66 C72 76 60 76 52 66 C46 76 34 76 26 66 Z", NARANJA, MARINO, 3.5)
        + trazo("M56 36 L52 66 M80 36 L80 66 M104 36 L108 66", MARINO, 2.5)),
    "a2_trabajo_profesiones": (AMARILLO_CLARO,
        p("M62 52 L62 40 C62 34 66 32 70 32 L90 32 C94 32 98 34 98 40 L98 52", "none", MARINO, 4.5)
        + rect(28, 52, 104, 72, 12, "#A0612E", MARINO, 3.5) + trazo("M28 82 L132 82", MARINO, 3) + rect(72, 74, 16, 16, 3, AMARILLO, MARINO, 3)),
    "a2_viajes_transporte": (AZUL_CLARO,
        p("M24 92 L64 84 L96 40 L110 40 L96 84 L130 80 L140 66 L148 66 L144 96 L96 102 L110 136 L96 136 L64 104 L26 104 C18 104 18 92 24 92 Z", BLANCO, MARINO, 3.5)
        + p(destello(40, 44, 6), BLANCO) + trazo("M30 124 L60 124 M118 46 L138 46", BLANCO, 4)),
}


def generar():
    # Ícono adaptable (108 x 108; la zona segura es el círculo central de 66).
    svg("ic_launcher_background", 108, 108, rect(0, 0, 108, 108, 0, AZUL_CLARO))
    svg("ic_launcher_foreground", 108, 108, cabeza_robot(54, 56, .46) + p(estrella(82, 28, 5, 2.2), AMARILLO, MARINO, 1.2))
    for nombre, (fondo, cuerpo) in LECCIONES.items():
        portada("leccion_" + nombre, fondo, cuerpo)
    # Avatar del tutor en el chat. Los de los usuarios se generan aparte: node docs/diseno-arte/avatares.js
    svg("avatar_robot", 100, 100, circulo(50, 50, 48, AZUL_CLARO) + grupo(cabeza(), 50 - 100 * .4, 52 - 66 * .4, .4)
        + circulo(50, 50, 48, "none", MARINO, 2.5))
    svg("mascota", 200, 220, mascota())
    svg("mascota_preocupada", 200, 220, mascota("preocupada"))
    defs_logo = (f'<radialGradient id="aura" gradientUnits="userSpaceOnUse" cx="120" cy="100" r="130">'
                 f'<stop offset="0" stop-color="#EAF2FB"/><stop offset="1" stop-color="{AZUL_CLARO}"/></radialGradient>')
    logo = [circulo(120, 120, 114, "url(#aura)", AZUL, 5), circulo(120, 120, 104, "none", BLANCO, 3)]
    logo.append(grupo(mascota(con_libro=True), 30, 34, .9))
    logo.append(burbuja(26, 44, 48, 28, BLANCO, cola_izq=False) + palabra("Hi", 40, 50, 15, 3.5, NARANJA, ancho=.55))
    logo.append(p("M164 46 L168 34 L174 41 L180 30 L186 41 L192 34 L196 46 Z", AMARILLO, MARINO, 2.2))
    logo += [moneda(196, 150, 11), moneda(184, 178, 9), moneda(46, 176, 10)]
    logo += [p(estrella(40, 112, 7, 3), AMARILLO, MARINO, 1.8), p(estrella(202, 98, 6, 2.6), AMARILLO, MARINO, 1.8),
             p(destello(60, 30, 5), BLANCO), p(destello(150, 206, 5), BLANCO)]
    svg("logo_xpspeak", 240, 240, "".join(logo), defs_logo)

    for nombre, icono in INSIGNIAS.items():
        svg("insignia_" + nombre, 120, 120, medalla(icono), DEGRADADO_MEDALLA)
    for nombre, icono in INICIO.items():
        svg(nombre, 120, 120, medalla(icono), DEGRADADO_MEDALLA)

    svg("moneda_xp", 64, 64, moneda(32, 32, 29))
    svg("icono_racha", 64, 64, flama(32, 32, .62))

    # Portadas de los minijuegos (la mascota es el protagonista)
    ondas = trazo("M118 58 C126 66 126 82 118 90", NARANJA, 5) + trazo("M130 48 C144 62 144 86 130 100", NARANJA, 5)
    portada("juego_eco_vocal", AZUL_CLARO,
            p(destello(28, 28, 7), BLANCO) + cabeza_robot(70, 76, .62) + ondas
            + rect(18, 108, 18, 28, 9, AZUL, MARINO, 3) + trazo("M14 126 C14 144 40 144 40 126 M27 141 L27 150", MARINO, 3)
            + moneda(130, 128, 15) + p(estrella(140, 30, 8, 3.4), AMARILLO, MARINO, 2))
    portada("juego_mision_situacional", "#FFE3BF",
            rect(0, 118, 160, 42, 0, AZUL) + trazo("M0 118 L160 118", MARINO, 3)
            + cabeza_robot(52, 84, .5) + burbuja(78, 22, 66, 34) + trazo("M88 34 L132 34 M88 44 L116 44", AZUL, 3)
            + burbuja(14, 14, 44, 24, AMARILLO, cola_izq=True) + trazo("M24 26 L48 26", MARINO, 3)
            + p("M100 96 L128 96 L125 120 Q124 124 120 124 L108 124 Q104 124 103 120 Z", BLANCO, MARINO, 3)
            + trazo("M128 102 C138 102 138 114 126 114", MARINO, 3) + trazo("M108 84 C104 90 112 88 108 94 M118 84 C114 90 122 88 118 94", GRIS, 2.5))
    portada("juego_rafaga_palabras", AZUL,
            ficha(18, 26, 54, NARANJA) + ficha(84, 18, 58, AMARILLO) + ficha(30, 58, 62, AZUL_CLARO) + ficha(100, 52, 42, BLANCO)
            + ficha(16, 92, 48, AMARILLO)
            + p("M98 80 L80 112 L94 112 L86 142 L118 104 L102 104 L112 80 Z", AMARILLO, MARINO, 3)
            + circulo(132, 124, 18, BLANCO, MARINO, 3) + trazo("M132 124 L132 113 M132 124 L140 128", MARINO, 3)
            + rect(128, 100, 8, 5, 2, NARANJA, MARINO, 2) + trazo("M20 128 L40 128 M24 140 L52 140", AZUL_CLARO, 4))
    portada("juego_orden_maestro", MARINO,
            trazo("M34 40 C60 40 60 80 84 80 C108 80 108 120 128 120", NARANJA, 6) + trazo("M34 40 C60 40 60 80 84 80 C108 80 108 120 128 120", AMARILLO, 2)
            + ficha(14, 31, 40, AZUL_CLARO) + ficha(64, 71, 40, AMARILLO) + ficha(106, 111, 42, NARANJA)
            + ficha(90, 26, 50, "#5A86C8") + ficha(16, 112, 46, "#5A86C8")
            + circulo(136, 60, 13, "#2E9E5B", BLANCO, 3) + trazo("M130 60 L134 65 L143 55", BLANCO, 3.5)
            + p(destello(30, 80, 5), AMARILLO) + p(destello(120, 146, 4), BLANCO))


if __name__ == "__main__":
    generar()
    print("\n".join(sorted(os.listdir(SALIDA))))
