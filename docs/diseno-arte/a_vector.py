"""Convierte los SVG de docs/diseno-arte/svg/ a VectorDrawable de Android.

Uso: python3 docs/diseno-arte/a_vector.py
Escribe app/src/main/res/drawable/<nombre>.xml. Solo entiende el subconjunto que
produce generar.py: <path>, <circle>, <ellipse>, <rect>, <g transform="translate() scale()">
y <radialGradient gradientUnits="userSpaceOnUse">.
"""
import os
import re
import xml.etree.ElementTree as ET

AQUI = os.path.dirname(os.path.abspath(__file__))
ENTRADA = os.path.join(AQUI, "svg")
SALIDA = os.path.join(AQUI, "..", "..", "app", "src", "main", "res", "drawable")
NS = "{http://www.w3.org/2000/svg}"


def n(v):
    return f"{float(v):.2f}".rstrip("0").rstrip(".")


def color(c):
    c = c.upper()
    return "#FF" + c[1:] if len(c) == 7 else c


def forma_a_path(el):
    tag = el.tag.replace(NS, "")
    a = el.attrib
    if tag == "path":
        return a["d"]
    if tag in ("circle", "ellipse"):
        cx, cy = float(a["cx"]), float(a["cy"])
        rx = float(a.get("rx", a.get("r")))
        ry = float(a.get("ry", a.get("r")))
        return (f"M{n(cx - rx)} {n(cy)} A{n(rx)} {n(ry)} 0 1 0 {n(cx + rx)} {n(cy)} "
                f"A{n(rx)} {n(ry)} 0 1 0 {n(cx - rx)} {n(cy)} Z")
    if tag == "rect":
        x, y, w, h = (float(a[k]) for k in ("x", "y", "width", "height"))
        r = min(float(a.get("rx", 0)), w / 2, h / 2)
        if r == 0:
            return f"M{n(x)} {n(y)} H{n(x + w)} V{n(y + h)} H{n(x)} Z"
        return (f"M{n(x + r)} {n(y)} H{n(x + w - r)} A{n(r)} {n(r)} 0 0 1 {n(x + w)} {n(y + r)} "
                f"V{n(y + h - r)} A{n(r)} {n(r)} 0 0 1 {n(x + w - r)} {n(y + h)} "
                f"H{n(x + r)} A{n(r)} {n(r)} 0 0 1 {n(x)} {n(y + h - r)} "
                f"V{n(y + r)} A{n(r)} {n(r)} 0 0 1 {n(x + r)} {n(y)} Z")
    return None


def degradado(g, ind):
    items = "".join(
        f'{ind}        <item android:offset="{n(s.attrib["offset"])}" android:color="{color(s.attrib["stop-color"])}"/>\n'
        for s in g.findall(f"{NS}stop"))
    return (f'{ind}    <aapt:attr name="android:fillColor">\n'
            f'{ind}      <gradient android:type="radial" android:centerX="{n(g.attrib["cx"])}" '
            f'android:centerY="{n(g.attrib["cy"])}" android:gradientRadius="{n(g.attrib["r"])}">\n'
            f'{items}{ind}      </gradient>\n{ind}    </aapt:attr>\n')


def convertir(el, degradados, ind="    "):
    out = []
    for hijo in el:
        tag = hijo.tag.replace(NS, "")
        if tag == "defs":
            continue
        if tag == "g":
            m = re.match(r"translate\(([-\d.]+) ([-\d.]+)\) scale\(([-\d.]+)\)", hijo.attrib.get("transform", ""))
            tx, ty, s = m.groups() if m else ("0", "0", "1")
            out.append(f'{ind}<group android:translateX="{n(tx)}" android:translateY="{n(ty)}" '
                       f'android:scaleX="{n(s)}" android:scaleY="{n(s)}">\n')
            out.append(convertir(hijo, degradados, ind + "    "))
            out.append(f"{ind}</group>\n")
            continue
        d = forma_a_path(hijo)
        if d is None:
            continue
        a = hijo.attrib
        attrs = [f'android:pathData="{d}"']
        fill = a.get("fill", "#000000")
        grad = None
        if fill.startswith("url("):
            grad = degradados[fill[5:-1]]
        elif fill != "none":
            attrs.append(f'android:fillColor="{color(fill)}"')
        if a.get("stroke"):
            attrs += [f'android:strokeColor="{color(a["stroke"])}"', f'android:strokeWidth="{n(a["stroke-width"])}"']
            if a.get("stroke-linecap"):
                attrs.append(f'android:strokeLineCap="{a["stroke-linecap"]}"')
            if a.get("stroke-linejoin"):
                attrs.append(f'android:strokeLineJoin="{a["stroke-linejoin"]}"')
        sep = f"\n{ind}    "
        if grad is None:
            out.append(f"{ind}<path{sep}" + sep.join(attrs) + "/>\n")
        else:
            out.append(f"{ind}<path{sep}" + sep.join(attrs) + ">\n" + degradado(grad, ind) + f"{ind}</path>\n")
    return "".join(out)


def main():
    os.makedirs(SALIDA, exist_ok=True)
    for archivo in sorted(os.listdir(ENTRADA)):
        if not archivo.endswith(".svg"):
            continue
        raiz = ET.parse(os.path.join(ENTRADA, archivo)).getroot()
        _, _, w, h = raiz.attrib["viewBox"].split()
        degradados = {g.attrib["id"]: g for g in raiz.iter(f"{NS}radialGradient")}
        xml = ('<?xml version="1.0" encoding="utf-8"?>\n'
               "<!-- Generado por docs/diseno-arte/a_vector.py: no editar a mano. -->\n"
               '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
               '    xmlns:aapt="http://schemas.android.com/aapt"\n'
               f'    android:width="{w}dp"\n    android:height="{h}dp"\n'
               f'    android:viewportWidth="{w}"\n    android:viewportHeight="{h}">\n'
               + convertir(raiz, degradados) + "</vector>\n")
        destino = os.path.join(SALIDA, archivo[:-4] + ".xml")
        with open(destino, "w") as fh:
            fh.write(xml)
        print(os.path.relpath(destino))


if __name__ == "__main__":
    main()
