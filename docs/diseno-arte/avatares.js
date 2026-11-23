/**
 * Genera los avatares de XP-SPEAK (estilo de la hoja de avatares: personaje dentro
 * de un círculo de color con contorno marino).
 *
 * Uso: node docs/diseno-arte/avatares.js
 * Escribe docs/diseno-arte/svg/avatar_<id>.svg y app/src/main/res/drawable/avatar_<id>.xml.
 * Como en generar.py, solo se usan formas que VectorDrawable soporta.
 */
const fs = require("fs");
const path = require("path");

const AZUL_CLARO = "#C0D6EB";
const AZUL = "#4B7BBE";
const AZUL_MEDIO = "#6E9AD6";
const MARINO = "#232074";
const NARANJA = "#FF8C00";
const NARANJA_OSCURO = "#E07000";
const AMARILLO = "#FFC107";
const AMARILLO_CLARO = "#FFE9A6";
const DURAZNO = "#FFE3BF";
const CARA = "#E6F0FA";
const BLANCO = "#FFFFFF";
const MEJILLA = "#F7A1A8";
const PIEL_CLARA = "#F2B98A";
const PIEL_MEDIA = "#C98B5E";
const PIEL_OSCURA = "#8D5A3B";
const PELO = "#3A2416";

const AQUI = __dirname;
const SALIDA_SVG = path.join(AQUI, "svg");
const SALIDA_XML = path.join(AQUI, "..", "..", "app", "src", "main", "res", "drawable");

const n = (v) => (Math.round(v * 100) / 100).toString();

// --- Formas: todo se reduce a un path ---
const forma = (d, fill = "none", stroke = null, sw = null) => ({ d, fill, stroke, sw });
const elipseD = (cx, cy, rx, ry) =>
    `M${n(cx - rx)} ${n(cy)} A${n(rx)} ${n(ry)} 0 1 0 ${n(cx + rx)} ${n(cy)} A${n(rx)} ${n(ry)} 0 1 0 ${n(cx - rx)} ${n(cy)} Z`;
const circulo = (cx, cy, r, fill, stroke, sw) => forma(elipseD(cx, cy, r, r), fill, stroke, sw);
const elipse = (cx, cy, rx, ry, fill, stroke, sw) => forma(elipseD(cx, cy, rx, ry), fill, stroke, sw);
const rect = (x, y, w, h, r, fill, stroke, sw) => forma(
    `M${n(x + r)} ${n(y)} H${n(x + w - r)} A${n(r)} ${n(r)} 0 0 1 ${n(x + w)} ${n(y + r)} ` +
    `V${n(y + h - r)} A${n(r)} ${n(r)} 0 0 1 ${n(x + w - r)} ${n(y + h)} ` +
    `H${n(x + r)} A${n(r)} ${n(r)} 0 0 1 ${n(x)} ${n(y + h - r)} V${n(y + r)} A${n(r)} ${n(r)} 0 0 1 ${n(x + r)} ${n(y)} Z`,
    fill, stroke, sw);
const trazo = (d, color = MARINO, sw = 2) => forma(d, "none", color, sw);

function estrellaD(cx, cy, R, r, rot = -90) {
    const pts = [];
    for (let i = 0; i < 10; i++) {
        const rad = i % 2 === 0 ? R : r;
        const a = (rot + i * 36) * Math.PI / 180;
        pts.push(`${n(cx + rad * Math.cos(a))} ${n(cy + rad * Math.sin(a))}`);
    }
    return "M" + pts.join(" L") + " Z";
}
const estrella = (cx, cy, R, r, fill = AMARILLO, stroke = MARINO, sw = 1.5, rot = -90) =>
    forma(estrellaD(cx, cy, R, r, rot), fill, stroke, sw);

/** Destello de 4 puntas. */
const destello = (x, y, s, color = BLANCO) => forma(
    `M${n(x)} ${n(y - s)} Q${n(x)} ${n(y)} ${n(x + s)} ${n(y)} Q${n(x)} ${n(y)} ${n(x)} ${n(y + s)} ` +
    `Q${n(x)} ${n(y)} ${n(x - s)} ${n(y)} Q${n(x)} ${n(y)} ${n(x)} ${n(y - s)} Z`, color);

const moneda = (cx, cy, r) => [circulo(cx, cy, r, AMARILLO, MARINO, 1.5), estrella(cx, cy, r * 0.55, r * 0.25, NARANJA, null)];

/** Grupo girado [grados] alrededor de (px, py). */
const girado = (grados, px, py, hijos) => ({ grupo: { grados, px, py }, hijos });

// --- Avatares ---
function cohete() {
    const c = [];
    c.push(trazo("M42 76 L42 92 M50 84 L50 98 M58 76 L58 92", BLANCO, 2.5));
    c.push(forma("M43 68 L57 68 L55 75 L50 84 L45 75 Z", AMARILLO, NARANJA, 1.5));
    c.push(forma("M38 54 L28 65 L28 72 L38 67 Z", NARANJA, MARINO, 2.2));
    c.push(forma("M62 54 L72 65 L72 72 L62 67 Z", NARANJA, MARINO, 2.2));
    c.push(forma("M50 16 C60 24 62 37 62 50 L62 68 L38 68 L38 50 C38 37 40 24 50 16 Z", BLANCO, MARINO, 2.5));
    c.push(forma("M50 16 C55.5 20.5 58.6 26 60.3 31 L39.7 31 C41.4 26 44.5 20.5 50 16 Z", NARANJA, MARINO, 2.2));
    c.push(rect(38, 59, 24, 4, 0, NARANJA));
    c.push(trazo("M38 59 L62 59 M38 63 L62 63", MARINO, 1.5));
    c.push(circulo(50, 45, 7.5, AZUL_CLARO, MARINO, 2.2));
    c.push(circulo(47.4, 44, 1, MARINO), circulo(52.6, 44, 1, MARINO));
    c.push(trazo("M47 47.3 Q50 50 53 47.3", MARINO, 1.4));
    return {
        fondo: AZUL_CLARO,
        cuerpo: [
            girado(40, 50, 50, c),
            ...moneda(23, 70, 5.5),
            destello(30, 26, 4), destello(78, 70, 3.5), destello(20, 46, 2.5),
            estrella(80, 44, 3.5, 1.5, AMARILLO, null),
        ],
    };
}

function zorro() {
    return {
        fondo: DURAZNO,
        cuerpo: [
            trazo("M22 56 C20 24 80 24 78 56", MARINO, 7), trazo("M22 56 C20 24 80 24 78 56", AZUL, 3.5),
            forma("M28 46 L29 20 L46 32 Z", NARANJA, MARINO, 2.5), forma("M72 46 L71 20 L54 32 Z", NARANJA, MARINO, 2.5),
            forma("M31.5 38 L32 27 L40 33 Z", DURAZNO), forma("M68.5 38 L68 27 L60 33 Z", DURAZNO),
            elipse(50, 56, 25, 22, NARANJA, MARINO, 2.5),
            forma("M27 58 C33 79 67 79 73 58 C65 64 57 62 50 69 C43 62 35 64 27 58 Z", BLANCO),
            elipse(50, 56, 25, 22, "none", MARINO, 2.5),
            circulo(41, 52, 2.8, MARINO), circulo(42, 51, 0.9, BLANCO),
            trazo("M55 52 Q59 47.5 63 52", MARINO, 2.5),
            circulo(36, 61, 3, MEJILLA), circulo(64, 61, 3, MEJILLA),
            elipse(50, 63, 3.4, 2.5, MARINO),
            trazo("M50 65.5 L50 67.5 M46 68 Q48 70.5 50 68 Q52 70.5 54 68", MARINO, 1.8),
            rect(15, 47, 12, 21, 5.5, AZUL, MARINO, 2.5), rect(73, 47, 12, 21, 5.5, AZUL, MARINO, 2.5),
            rect(18, 51, 6, 13, 3, AZUL_CLARO),
            // Nota musical
            trazo("M83 16 L83 28 M83 16 L89 18.5", MARINO, 2.2), elipse(80.2, 28.5, 3.2, 2.4, MARINO),
        ],
    };
}

function astronauta() {
    return {
        fondo: AZUL,
        cuerpo: [
            destello(22, 28, 3.5), destello(80, 64, 3), destello(26, 70, 2.5), circulo(70, 16, 1.2, BLANCO),
            forma("M22 100 L22 86 C22 76 34 71 50 71 C66 71 78 76 78 86 L78 100 Z", BLANCO, MARINO, 2.5),
            elipse(50, 72.5, 17, 4.5, NARANJA, MARINO, 2),
            rect(43, 81, 14, 9, 2, AZUL_CLARO, MARINO, 2), circulo(47, 85.5, 1.4, NARANJA), circulo(53, 85.5, 1.4, AMARILLO),
            trazo("M50 23 L50 13", MARINO, 2), circulo(50, 11, 3, NARANJA, MARINO, 1.6),
            circulo(27, 47, 4.5, NARANJA, MARINO, 2), circulo(73, 47, 4.5, NARANJA, MARINO, 2),
            circulo(50, 46, 23, BLANCO, MARINO, 2.5),
            circulo(50, 47, 16.5, MARINO),
            circulo(50, 50, 12, PIEL_CLARA),
            forma("M38 48 C37 37 63 37 62 48 C57 42 43 42 38 48 Z", PELO),
            circulo(45.5, 50.5, 1.7, MARINO), circulo(54.5, 50.5, 1.7, MARINO),
            circulo(42, 54.5, 1.8, MEJILLA), circulo(58, 54.5, 1.8, MEJILLA),
            trazo("M46 55 Q50 58.5 54 55", MARINO, 1.8),
            trazo("M37 40 Q40 34.5 46 33", BLANCO, 2),
            estrella(76, 28, 7, 3, AMARILLO, MARINO, 1.6),
        ],
    };
}

function monstruo() {
    return {
        fondo: AMARILLO_CLARO,
        cuerpo: [
            estrella(18, 40, 3.5, 1.5, NARANJA, null), destello(82, 30, 3, BLANCO), circulo(78, 18, 1.3, NARANJA),
            forma("M29 33 L22 15 L40 27 Z", NARANJA, MARINO, 2.2), forma("M71 33 L78 15 L60 27 Z", NARANJA, MARINO, 2.2),
            forma("M28 47 L14 40 L22 56 Z", AZUL_MEDIO, MARINO, 2.2), forma("M72 47 L86 40 L78 56 Z", AZUL_MEDIO, MARINO, 2.2),
            forma("M18 100 L20 80 C24 70 76 70 80 80 L82 100 Z", AZUL_MEDIO, MARINO, 2.5),
            elipse(50, 47, 25, 22, AZUL_MEDIO, MARINO, 2.5),
            forma("M44 26.5 L50 17 L56 26.5 Z", NARANJA, MARINO, 2),
            circulo(41, 41, 7, BLANCO, MARINO, 2), circulo(59, 41, 7, BLANCO, MARINO, 2),
            circulo(42, 42, 3.3, MARINO), circulo(60, 42, 3.3, MARINO),
            circulo(43.2, 40.6, 1.1, BLANCO), circulo(61.2, 40.6, 1.1, BLANCO),
            circulo(31, 53, 3, MEJILLA), circulo(69, 53, 3, MEJILLA),
            elipse(50, 55, 11, 6.5, "#F7C1C8", MARINO, 2),
            elipse(46.3, 54.5, 1.5, 2, MARINO), elipse(53.7, 54.5, 1.5, 2, MARINO),
            trazo("M45 63.5 Q50 66.5 55 63.5", MARINO, 1.8),
            rect(24, 72, 52, 20, 10, MARINO, MARINO, 2),
            trazo("M35 82 L43 82 M39 78 L39 86", BLANCO, 2.6),
            trazo("M47.5 80 L52.5 80", AZUL_CLARO, 1.6),
            circulo(61, 78.5, 2.2, AMARILLO), circulo(66.5, 83, 2.2, NARANJA), circulo(56.5, 84.5, 2.2, AZUL_CLARO),
            circulo(25, 83, 5.5, AZUL_MEDIO, MARINO, 2), circulo(75, 83, 5.5, AZUL_MEDIO, MARINO, 2),
        ],
    };
}

function pinguino() {
    return {
        fondo: CARA,
        cuerpo: [
            destello(20, 30, 3, AZUL), destello(84, 60, 2.5, AZUL), circulo(24, 72, 1.2, AZUL),
            ...moneda(78, 26, 5.5),
            forma("M33 64 C22 59 15 47 17 38 C24 44 31 50 37 56 Z", AZUL, MARINO, 2.2),
            forma("M67 64 C78 59 85 47 83 38 C76 44 69 50 63 56 Z", AZUL, MARINO, 2.2),
            elipse(42, 91, 6.5, 3.2, NARANJA, MARINO, 1.8), elipse(58, 91, 6.5, 3.2, NARANJA, MARINO, 1.8),
            forma("M31 66 C31 50 69 50 69 66 L69 80 C69 93 31 93 31 80 Z", AZUL, MARINO, 2.5),
            elipse(50, 76, 12, 12, BLANCO),
            forma("M57 64 L61 83 L67 81 L63 63 Z", AMARILLO, MARINO, 2),
            circulo(50, 46, 18, AZUL, MARINO, 2.5),
            forma("M36 50 C36 39 49 38 50 44 C51 38 64 39 64 50 C64 61 36 61 36 50 Z", BLANCO),
            forma("M33 60 C40 66 60 66 67 60 L67 66 C60 72 40 72 33 66 Z", AMARILLO, MARINO, 2),
            circulo(44.5, 48.5, 2.3, MARINO), circulo(55.5, 48.5, 2.3, MARINO),
            circulo(45.2, 47.7, 0.8, BLANCO), circulo(56.2, 47.7, 0.8, BLANCO),
            circulo(40, 53.5, 2.2, MEJILLA), circulo(60, 53.5, 2.2, MEJILLA),
            forma("M46.5 52.5 L53.5 52.5 L50 57 Z", NARANJA, MARINO, 1.5),
            forma("M32 41 C32 22 68 22 68 41 Z", NARANJA, MARINO, 2.5),
            rect(29.5, 37.5, 41, 7, 3.5, AMARILLO, MARINO, 2.2),
            trazo("M36 39.5 L36 42.5 M43 39.5 L43 42.5 M50 39.5 L50 42.5 M57 39.5 L57 42.5 M64 39.5 L64 42.5", NARANJA, 1.4),
            circulo(50, 21, 4.5, BLANCO, MARINO, 2),
        ],
    };
}

function chica() {
    return {
        fondo: "#FFD666",
        cuerpo: [
            destello(82, 34, 3.5), destello(80, 74, 2.5), circulo(20, 60, 1.3, BLANCO),
            forma("M22 100 C22 81 32 73 50 73 C68 73 78 81 78 100 Z", AZUL, MARINO, 2.5),
            forma("M40 73 C42 80 58 80 60 73", AZUL_MEDIO, MARINO, 2),
            trazo("M45 78 L45 88 M55 78 L55 88", BLANCO, 1.8),
            rect(45, 64, 10, 11, 3, PIEL_MEDIA, MARINO, 2),
            forma("M63 36 C80 30 87 48 80 64 C78 56 72 51 66 50 Z", PELO, MARINO, 2),
            elipse(50, 51, 15.5, 16.5, PIEL_MEDIA, MARINO, 2.5),
            forma("M34.5 49 C32 32 47 28 55 31 C63 33 67 41 65.5 49 C61 42 53 39 46 41 C41 43 37 46 34.5 49 Z", PELO, MARINO, 2),
            circulo(44.5, 52, 1.9, MARINO), circulo(55.5, 52, 1.9, MARINO),
            circulo(40.5, 57, 2.3, MEJILLA), circulo(59.5, 57, 2.3, MEJILLA),
            forma("M44 57.5 Q50 65 56 57.5 Z", BLANCO, MARINO, 1.8),
            trazo("M32 52 C31 27 69 27 68 52", MARINO, 6), trazo("M32 52 C31 27 69 27 68 52", AMARILLO, 2.8),
            rect(28, 45, 8.5, 15, 4, AMARILLO, MARINO, 2), rect(63.5, 45, 8.5, 15, 4, AMARILLO, MARINO, 2),
            forma("M23 18 L37 18 Q42 18 42 23 L42 29 Q42 34 37 34 L33 34 L28.5 39.5 L28.5 34 L23 34 Q18 34 18 29 L18 23 Q18 18 23 18 Z",
                BLANCO, MARINO, 2),
            estrella(30, 26, 5, 2.2, NARANJA, MARINO, 1.1),
        ],
    };
}

function chico() {
    return {
        fondo: AZUL_CLARO,
        cuerpo: [
            destello(20, 32, 3.5), destello(26, 76, 2.5), circulo(84, 52, 1.2, BLANCO),
            ...moneda(79, 28, 5.5),
            forma("M20 100 C20 81 32 73 50 73 C68 73 80 81 80 100 Z", MARINO, MARINO, 2.5),
            trazo("M45 76 L45 87 M55 76 L55 87", AMARILLO, 1.8),
            forma("M63 76 L67 63 L78 65 L77 82 Z", MARINO, MARINO, 2),
            rect(66, 55, 12, 12, 4, PIEL_OSCURA, MARINO, 2),
            forma("M68.5 56 L68.5 47 Q68.5 42.5 72.5 43.5 L72.5 56 Z", PIEL_OSCURA, MARINO, 2),
            trazo("M66 59.5 L72 59.5 M66 63 L72 63", MARINO, 1.3),
            rect(45, 64, 10, 11, 3, PIEL_OSCURA, MARINO, 2),
            circulo(33.5, 52, 3.5, PIEL_OSCURA, MARINO, 2),
            elipse(48, 51, 15, 16.5, PIEL_OSCURA, MARINO, 2.5),
            circulo(42.5, 51, 3.1, BLANCO, MARINO, 1.4), circulo(53.5, 51, 3.1, BLANCO, MARINO, 1.4),
            circulo(43, 51.4, 1.5, MARINO), circulo(54, 51.4, 1.5, MARINO),
            forma("M40.5 57.5 Q48 66 55.5 57.5 Z", BLANCO, MARINO, 1.8),
            forma("M32 45 C32 28 64 28 64 45 Z", NARANJA, MARINO, 2.5),
            forma("M40 44.5 L68 42.5 Q74 44 70 48 L40 48 Z", NARANJA_OSCURO, MARINO, 2),
            estrella(47, 37, 4.5, 2, AMARILLO, MARINO, 1),
            circulo(48, 29.2, 1.7, NARANJA, MARINO, 1.3),
        ],
    };
}

function estrellita() {
    return {
        fondo: MARINO,
        cuerpo: [
            trazo("M12 42 L28 42 M16 53 L30 53 M14 64 L26 64", AZUL_CLARO, 3),
            destello(78, 22, 4), destello(84, 68, 3), destello(28, 26, 2.5), circulo(70, 82, 1.5, BLANCO), circulo(40, 82, 1.2, BLANCO),
            estrella(56, 52, 28, 13, AMARILLO, NARANJA, 2.8, -100),
            circulo(50.5, 50, 2.3, MARINO), circulo(61.5, 48.5, 2.3, MARINO),
            circulo(51.2, 49.2, 0.8, BLANCO), circulo(62.2, 47.7, 0.8, BLANCO),
            circulo(46.5, 56, 2.4, MEJILLA), circulo(66, 53.5, 2.4, MEJILLA),
            trazo("M51 56.5 Q57 61.5 62 55", MARINO, 2),
        ],
    };
}

const AVATARES = { cohete, zorro, astronauta, monstruo, pinguino, chica, chico, estrella: estrellita };

// --- Salida ---
const BORDE = elipseD(50, 50, 48, 48);

function aSvg(nodos) {
    return nodos.map((x) => {
        if (x.hijos) return `<g transform="rotate(${x.grupo.grados} ${x.grupo.px} ${x.grupo.py})">${aSvg(x.hijos)}</g>`;
        let s = `<path d="${x.d}" fill="${x.fill}"`;
        if (x.stroke) s += ` stroke="${x.stroke}" stroke-width="${x.sw}" stroke-linecap="round" stroke-linejoin="round"`;
        return s + "/>";
    }).join("");
}

const argb = (c) => "#FF" + c.slice(1).toUpperCase();

function aXml(nodos, ind) {
    return nodos.map((x) => {
        if (x.hijos) {
            return `${ind}<group android:rotation="${x.grupo.grados}" android:pivotX="${x.grupo.px}" android:pivotY="${x.grupo.py}">\n`
                + aXml(x.hijos, ind + "    ") + `${ind}</group>\n`;
        }
        const attrs = [`android:pathData="${x.d}"`];
        if (x.fill !== "none") attrs.push(`android:fillColor="${argb(x.fill)}"`);
        if (x.stroke) {
            attrs.push(`android:strokeColor="${argb(x.stroke)}"`, `android:strokeWidth="${x.sw}"`,
                'android:strokeLineCap="round"', 'android:strokeLineJoin="round"');
        }
        return `${ind}<path\n${ind}    ` + attrs.join(`\n${ind}    `) + "/>\n";
    }).join("");
}

for (const [id, dibujar] of Object.entries(AVATARES)) {
    const { fondo, cuerpo } = dibujar();
    const borde = forma(BORDE, "none", MARINO, 2.5);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="100" height="100" viewBox="0 0 100 100">`
        + `<defs><clipPath id="c"><path d="${BORDE}"/></clipPath></defs>`
        + aSvg([forma(BORDE, fondo)]) + `<g clip-path="url(#c)">${aSvg(cuerpo)}</g>` + aSvg([borde]) + "</svg>\n";
    fs.writeFileSync(path.join(SALIDA_SVG, `avatar_${id}.svg`), svg);

    const xml = '<?xml version="1.0" encoding="utf-8"?>\n'
        + "<!-- Generado por docs/diseno-arte/avatares.js: no editar a mano. -->\n"
        + '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        + '    android:width="100dp"\n    android:height="100dp"\n'
        + '    android:viewportWidth="100"\n    android:viewportHeight="100">\n'
        + aXml([forma(BORDE, fondo)], "    ")
        + "    <group>\n"
        + `        <clip-path android:pathData="${BORDE}"/>\n`
        + aXml(cuerpo, "        ")
        + "    </group>\n"
        + aXml([borde], "    ")
        + "</vector>\n";
    fs.writeFileSync(path.join(SALIDA_XML, `avatar_${id}.xml`), xml);
    console.log(`avatar_${id}`);
}
