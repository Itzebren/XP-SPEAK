"""Llena fichas y tablas de resultados del Cap. 6 con control de cambios (autor Claude)."""
import os, sys, json, copy, shutil, tempfile, zipfile, subprocess
CTX = '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/contexto_claude_code_TT'
sys.path.insert(0, CTX)
from tc import Doc, q
from lxml import etree

S = os.path.dirname(os.path.abspath(__file__))
BASE = os.environ.get('BASE', '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/TT_B162_evidencias_limpio.docx')
OUT = os.environ.get('OUT', '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/TT_B162_resultados')
DATOS = json.load(open(os.path.join(S, 'datos_doc.json')))


def unpack(docx, dst):
    with zipfile.ZipFile(docx) as z:
        z.extractall(dst)


def pack(src, docx):
    if os.path.exists(docx):
        os.remove(docx)
    with zipfile.ZipFile(docx, 'w', zipfile.ZIP_DEFLATED) as z:
        z.write(os.path.join(src, '[Content_Types].xml'), '[Content_Types].xml')
        for root, _, files in os.walk(src):
            for f in files:
                full = os.path.join(root, f)
                arc = os.path.relpath(full, src).replace(os.sep, '/')
                if arc != '[Content_Types].xml':
                    z.write(full, arc)


def ptxt(el):
    return ''.join(x.text or '' for x in el.iter(q('t')))


def tabla(d, caption):
    body = d.root.find(q('body'))
    hijos = list(body)
    for i, el in enumerate(hijos):
        if el.tag == q('p') and ptxt(el).strip() == caption:
            for el2 in hijos[i + 1:i + 4]:
                if el2.tag == q('tbl'):
                    return el2
    raise KeyError(caption)


def celda(d, tc, texto, centrar=False):
    """Escribe texto en una celda vacía como inserción controlada, con el formato de la tabla."""
    p = tc.find(q('p'))
    # formato: copiar rPr del primer run con texto de la fila
    fila = tc.getparent()
    rpr = None
    for r in fila.iter(q('r')):
        if r.find(q('t')) is not None and r.find(q('rPr')) is not None:
            rpr = r.find(q('rPr')); break
    if rpr is not None:
        rpr = copy.deepcopy(rpr)
        for h in rpr.findall(q('highlight')) + rpr.findall(q('b')) + rpr.findall(q('bCs')):
            rpr.remove(h)
    run = d.mk_run(rpr, texto)
    p.append(d.wrap('ins', [run]))
    if centrar:
        ppr = p.find(q('pPr'))
        if ppr is None:
            ppr = etree.SubElement(p, q('pPr')); p.remove(ppr); p.insert(0, ppr)
        if ppr.find(q('jc')) is None:
            jc = etree.SubElement(ppr, q('jc')); jc.set(q('val'), 'center')


def sin_resaltado(d, prefijo, after=None):
    i = d.find(prefijo, after=after)
    for h in list(d.paras[i].iter(q('highlight'))):
        h.getparent().remove(h)


def main():
    tmp = tempfile.mkdtemp()
    w = os.path.join(tmp, 'w')
    unpack(BASE, w)
    d = Doc(os.path.join(w, 'word', 'document.xml'))

    # 1) fichas de validación
    for pv in DATOS['fichas']:
        cod = pv['codigo']
        ancla = '%s · Duración:' % cod
        nuevo = '%s · Duración: %s · Fecha: %s' % (cod, pv['duracion'], pv['fecha'])
        d.replace(ancla, nuevo)
        sin_resaltado(d, nuevo)
        res_ini = '[PENDIENTE: EXITOSA / FALLIDA]'
        d.replace(res_ini, pv['resultado'], after=nuevo)
        sin_resaltado(d, pv['resultado'][:40], after=nuevo)
        t = tabla(d, 'Tabla %s. Pasos de la prueba %s' % (pv['tabla_pasos'], cod))
        for tr in t.findall(q('tr'))[1:]:
            tcs = tr.findall(q('tc'))
            paso = ptxt(tcs[0]).strip()
            if paso not in pv['pasos']:
                print('sin dato', cod, paso); continue
            ok, com = pv['pasos'][paso]
            celda(d, tcs[3 if ok else 4], '✓', centrar=True)
            if com:
                celda(d, tcs[5], com)
        d.refresh()
        print('ficha', cod)

    # 2) tablas de resultados: {caption: {fila: [col1, col2...]}}
    for cap, filas in DATOS['tablas'].items():
        t = tabla(d, cap)
        for tr in t.findall(q('tr'))[1:]:
            tcs = tr.findall(q('tc'))
            k = ptxt(tcs[0]).strip()
            if k in filas:
                for j, val in filas[k].items():
                    if val:
                        celda(d, tcs[int(j)], val, centrar=(int(j) != len(tcs) - 1 or len(val) < 12))
        print('tabla', cap)
    d.refresh()

    # 3) párrafos que se reescriben
    for prefijo, nuevo in DATOS['parrafos']:
        d.replace(prefijo, nuevo)
        sin_resaltado(d, nuevo[:45])
        print('párrafo', prefijo[:40])
    # en las viñetas, solo la etiqueta (hasta los dos puntos) va en negrita
    for prefijo, nuevo in DATOS['parrafos']:
        if ':' not in nuevo[:30]:
            continue
        etiqueta = len(nuevo.split(':')[0]) + 1
        i = d.find(nuevo[:30])
        pos = 0
        for r in d.paras[i].iter(q('r')):
            t = r.find(q('t'))
            if t is None:
                continue
            ini = pos; pos += len(t.text or '')
            if ini < etiqueta < pos:
                corte = etiqueta - ini
                r2 = copy.deepcopy(r)
                r2.find(q('t')).text = t.text[corte:]
                r2.find(q('t')).set('{http://www.w3.org/XML/1998/namespace}space', 'preserve')
                t.text = t.text[:corte]
                r.addnext(r2)
                rp = r2.find(q('rPr'))
                if rp is not None:
                    for b in rp.findall(q('b')) + rp.findall(q('bCs')):
                        rp.remove(b)
                continue
            if ini >= etiqueta:
                rp = r.find(q('rPr'))
                if rp is not None:
                    for b in rp.findall(q('b')) + rp.findall(q('bCs')):
                        rp.remove(b)
    d.refresh()
    # textos dentro de tablas (filas ya escritas)
    for prefijo, nuevo in DATOS.get('textos_tabla', []):
        d.replace(prefijo, nuevo)
    d.save()
    pack(w, OUT + '_con_cambios.docx')
    c = os.path.join(tmp, 'c')
    shutil.copytree(w, c)
    subprocess.run([sys.executable, os.path.join(CTX, 'accept.py'),
                    os.path.join(w, 'word', 'document.xml'), os.path.join(c, 'word', 'document.xml')], check=True)
    pack(c, OUT + '_limpio.docx')
    print('Listo')


main()
