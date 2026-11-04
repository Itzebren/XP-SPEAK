# Motor de cambios con control de cambios (track changes) para document.xml
import re, copy, difflib, datetime
from lxml import etree

W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
XML = 'http://www.w3.org/XML/1998/namespace'
def q(t): return '{%s}%s' % (W, t)

AUTHOR = 'Claude'
DATE = datetime.datetime.utcnow().strftime('%Y-%m-%dT%H:%M:%SZ')

class Doc:
    def __init__(self, path):
        self.path = path
        parser = etree.XMLParser(remove_blank_text=False, huge_tree=True)
        self.tree = etree.parse(path, parser)
        self.root = self.tree.getroot()
        self.nid = 70000
        self.log = []
        self.refresh()

    def refresh(self):
        self.paras = list(self.root.iter(q('p')))
        self.texts = [self.ptext(p) for p in self.paras]

    @staticmethod
    def ptext(p):
        out = []
        for el in p.iter(q('t'), q('delText')):
            # ignora texto ya borrado
            if el.tag == q('delText'):
                continue
            # ignora texto dentro de textboxes anidados (otro w:p)
            par = el.getparent()
            anc = par
            nested = False
            while anc is not None and anc is not p:
                if anc.tag == q('p'):
                    nested = True; break
                anc = anc.getparent()
            if nested: continue
            out.append(el.text or '')
        return ''.join(out)

    def id(self):
        self.nid += 1
        return str(self.nid)

    def find(self, prefix, after=None, nth=0, exact=False):
        start = 0
        if after:
            idx = [i for i, t in enumerate(self.texts) if t.strip().startswith(after)]
            if not idx:
                raise KeyError('ancla no encontrada: ' + after)
            start = idx[0]
        hits = []
        for i in range(start, len(self.paras)):
            t = self.texts[i].strip()
            if (t == prefix.strip()) if exact else t.startswith(prefix.strip()):
                hits.append(i)
        if not hits:
            raise KeyError('no encontrado: %r (after=%r)' % (prefix, after))
        if not after and nth == 0 and len(hits) > 1:
            raise KeyError('ambiguo (%d): %r' % (len(hits), prefix))
        return hits[nth]

    # ---------- runs ----------
    def text_runs(self, p):
        """runs directos (o dentro de hyperlink/smartTag) con w:t, en orden"""
        runs = []
        for r in p.iter(q('r')):
            # descartar runs de párrafos anidados
            anc = r.getparent(); nested = False
            while anc is not None and anc is not p:
                if anc.tag == q('p'): nested = True; break
                if anc.tag in (q('del'),): nested = True; break
                anc = anc.getparent()
            if nested: continue
            if r.find(q('t')) is not None:
                runs.append(r)
        return runs

    def mk_run(self, rpr, text, deleted=False, highlight=False):
        r = etree.Element(q('r'))
        if rpr is not None:
            rp = copy.deepcopy(rpr)
            for ch in rp.findall(q('rPrChange')): rp.remove(ch)
        else:
            rp = None
        if highlight:
            if rp is None: rp = etree.Element(q('rPr'))
            self._set_hl(rp)
        if rp is not None and len(rp):
            r.append(rp)
        t = etree.SubElement(r, q('delText') if deleted else q('t'))
        t.text = text
        t.set('{%s}space' % XML, 'preserve')
        return r

    @staticmethod
    def _set_hl(rp):
        for h in rp.findall(q('highlight')): rp.remove(h)
        h = etree.Element(q('highlight')); h.set(q('val'), 'yellow')
        # orden del esquema: highlight va antes de u/effect/... ; insertar tras color/sz si existen
        order = ['rStyle','rFonts','b','bCs','i','iCs','caps','smallCaps','strike','dstrike','outline','shadow','emboss','imprint','noProof','snapToGrid','vanish','webHidden','color','spacing','w','kern','position','sz','szCs']
        pos = 0
        for i, ch in enumerate(rp):
            tag = etree.QName(ch).localname
            if tag in order: pos = i + 1
        rp.insert(pos, h)

    def wrap(self, kind, runs):
        w = etree.Element(q(kind))
        w.set(q('id'), self.id()); w.set(q('author'), AUTHOR); w.set(q('date'), DATE)
        for r in runs: w.append(r)
        return w

    # ---------- operaciones ----------
    def replace(self, prefix, new, after=None, nth=0, hl=None, code_rpr_from=None, force_full=False):
        i = self.find(prefix, after, nth)
        return self.replace_at(i, new, hl=hl, code_rpr_from=code_rpr_from, force_full=force_full, label=prefix)

    def replace_at(self, i, new, hl=None, code_rpr_from=None, force_full=False, label=''):
        prefix = label or self.texts[i][:40]
        p = self.paras[i]
        if p.find('.//' + q('fldSimple')) is not None or p.find('.//' + q('instrText')) is not None:
            raise ValueError('párrafo con campo: ' + prefix)
        runs = self.text_runs(p)
        if not runs:
            raise ValueError('sin runs: ' + prefix)
        for r in runs:
            if r.find('.//' + q('fldChar')) is not None or r.find('.//' + q('drawing')) is not None:
                raise ValueError('run complejo en: ' + prefix)
        chars = []  # (char, rPr)
        for r in runs:
            rp = r.find(q('rPr'))
            t = ''.join(x.text or '' for x in r.findall(q('t')))
            chars += [(c, rp) for c in t]
        old = ''.join(c for c, _ in chars)
        # rPr "normal" (sin estilo de código) para insertar texto
        plain = None
        code = None
        for r in runs:
            rp = r.find(q('rPr'))
            isc = rp is not None and rp.find(q('rStyle')) is not None and 'Code' in rp.find(q('rStyle')).get(q('val'))
            if isc and code is None: code = rp
            if not isc and plain is None: plain = rp
        if plain is None and runs: plain = runs[0].find(q('rPr'))
        # parsear marcas en el texto nuevo: `codigo`
        segs = []  # (char, kind) kind in {'n','c'}
        incode = False
        for c in new:
            if c == '`': incode = not incode; continue
            segs.append((c, 'c' if incode else 'n'))
        newtext = ''.join(c for c, _ in segs)
        hlmask = [False] * len(newtext)
        for rx in (hl or []):
            for m in re.finditer(rx, newtext):
                for k in range(m.start(), m.end()): hlmask[k] = True
        if newtext == old and not any(hlmask):
            return
        # tokenización por palabras
        tok = lambda s: re.findall(r'\w+|\s+|[^\w\s]', s)
        A, B = tok(old), tok(newtext)
        sm = difflib.SequenceMatcher(None, A, B, autojunk=False)
        ops = sm.get_opcodes()
        if force_full or sm.ratio() < 0.45:
            ops = [('replace', 0, len(A), 0, len(B))]
        # offsets de tokens -> caracteres
        def offs(T):
            o = [0]
            for t in T: o.append(o[-1] + len(t))
            return o
        oa, ob = offs(A), offs(B)
        out = []
        def emit_old(a0, a1, deleted):
            # agrupa por rPr
            k = a0
            while k < a1:
                rp = chars[k][1]; j = k
                while j < a1 and chars[j][1] is rp: j += 1
                txt = old[k:j]
                if deleted:
                    out.append(self.wrap('del', [self.mk_run(rp, txt, deleted=True)]))
                else:
                    # resaltar parte de texto conservado (con rPrChange)
                    # posición en texto nuevo: calculada por el llamador mediante mapa
                    out.append(('keep', rp, txt, k))
                k = j
        def emit_new(b0, b1):
            k = b0
            while k < b1:
                kind = segs[k][1]; h = hlmask[k]; j = k
                while j < b1 and segs[j][1] == kind and hlmask[j] == h: j += 1
                rp = (code if code is not None else plain) if kind == 'c' else plain
                if kind == 'c' and code is None and code_rpr_from is not None:
                    rp = code_rpr_from
                out.append(self.wrap('ins', [self.mk_run(rp, newtext[k:j], highlight=h)]))
                k = j
        keepmap = {}
        for tag, a0, a1, b0, b1 in ops:
            ca0, ca1, cb0, cb1 = oa[a0], oa[a1], ob[b0], ob[b1]
            if tag == 'equal':
                for d in range(ca1 - ca0): keepmap[ca0 + d] = cb0 + d
                emit_old(ca0, ca1, False)
            elif tag == 'delete':
                emit_old(ca0, ca1, True)
            elif tag == 'insert':
                emit_new(cb0, cb1)
            else:
                emit_old(ca0, ca1, True); emit_new(cb0, cb1)
        # materializar 'keep' con resaltado si corresponde
        final = []
        for item in out:
            if isinstance(item, tuple):
                _, rp, txt, k0 = item
                # dividir por máscara de resaltado
                s = 0
                while s < len(txt):
                    h = hlmask[keepmap[k0 + s]]; e = s
                    while e < len(txt) and hlmask[keepmap[k0 + e]] == h: e += 1
                    r = self.mk_run(rp, txt[s:e])
                    if h:
                        rpn = r.find(q('rPr'))
                        if rpn is None:
                            rpn = etree.Element(q('rPr')); r.insert(0, rpn)
                        self._set_hl(rpn)
                        ch = etree.SubElement(rpn, q('rPrChange'))
                        ch.set(q('id'), self.id()); ch.set(q('author'), AUTHOR); ch.set(q('date'), DATE)
                        orig = copy.deepcopy(rp) if rp is not None else etree.Element(q('rPr'))
                        for x in orig.findall(q('rPrChange')): orig.remove(x)
                        ch.append(orig)
                    final.append(r); s = e
            else:
                final.append(item)
        # sustituir runs
        first = runs[0]
        parent = first.getparent()
        pos = parent.index(first)
        for r in runs:
            r.getparent().remove(r)
        for k, el in enumerate(final):
            parent.insert(pos + k, el)
        self.texts[i] = self.ptext(p)
        self.log.append(('replace', prefix[:60]))

    def delete_para(self, prefix, after=None, nth=0):
        i = self.find(prefix, after, nth)
        p = self.paras[i]
        for r in self.text_runs(p):
            parent = r.getparent(); pos = parent.index(r)
            for t in r.findall(q('t')): t.tag = q('delText')
            parent.remove(r)
            parent.insert(pos, self.wrap('del', [r]))
        ppr = p.find(q('pPr'))
        if ppr is None:
            ppr = etree.Element(q('pPr')); p.insert(0, ppr)
        rpr = ppr.find(q('rPr'))
        if rpr is None:
            rpr = etree.SubElement(ppr, q('rPr'))
        d = etree.Element(q('del')); d.set(q('id'), self.id()); d.set(q('author'), AUTHOR); d.set(q('date'), DATE)
        rpr.insert(0, d)
        self.texts[i] = ''
        self.log.append(('delete', prefix[:60]))

    def insert_after(self, prefix, texts, after=None, nth=0, style_from=None, style_after=None, exact=False):
        """inserta párrafos nuevos después del párrafo indicado; copia pPr de style_from"""
        i = self.find(prefix, after, nth, exact=exact)
        anchor = self.paras[i]
        src = self.paras[self.find(style_from, style_after)] if style_from else anchor
        sppr = src.find(q('pPr'))
        srun = self.text_runs(src)
        rpr = srun[0].find(q('rPr')) if srun else None
        ref = anchor
        for txt in texts:
            np_ = etree.Element(q('p'))
            if sppr is not None:
                ppr = copy.deepcopy(sppr)
                for x in ppr.findall(q('rPr')): ppr.remove(x)
                for x in ppr.findall(q('sectPr')): ppr.remove(x)
            else:
                ppr = etree.Element(q('pPr'))
            np_.append(ppr)
            mr = etree.SubElement(ppr, q('rPr'))
            insm = etree.SubElement(mr, q('ins')); insm.set(q('id'), self.id()); insm.set(q('author'), AUTHOR); insm.set(q('date'), DATE)
            # soporta `codigo` y ⟦resaltado⟧
            parts = re.split(r'(`[^`]+`|⟦[^⟧]+⟧)', txt)
            runs = []
            for part in parts:
                if not part: continue
                if part.startswith('`'):
                    rr = self.mk_run(self.code_rpr(), part[1:-1])
                elif part.startswith('⟦'):
                    rr = self.mk_run(rpr, part[1:-1], highlight=True)
                else:
                    rr = self.mk_run(rpr, part)
                runs.append(rr)
            np_.append(self.wrap('ins', runs))
            ref.addnext(np_)
            ref = np_
        self.refresh()
        self.log.append(('insert', prefix[:60]))

    def code_rpr(self):
        if not hasattr(self, '_code'):
            for r in self.root.iter(q('r')):
                rp = r.find(q('rPr'))
                if rp is not None and rp.find(q('rStyle')) is not None and rp.find(q('rStyle')).get(q('val')) == 'CodeChar':
                    self._code = rp; break
        return self._code

    def delete_row_containing(self, prefix, after=None, nth=0):
        i = self.find(prefix, after, nth)
        p = self.paras[i]
        tr = p
        while tr is not None and tr.tag != q('tr'): tr = tr.getparent()
        trpr = tr.find(q('trPr'))
        if trpr is None:
            trpr = etree.Element(q('trPr'))
            # trPr va después de tblPrEx si existe
            idx = 1 if tr.find(q('tblPrEx')) is not None else 0
            tr.insert(idx, trpr)
        d = etree.SubElement(trpr, q('del')); d.set(q('id'), self.id()); d.set(q('author'), AUTHOR); d.set(q('date'), DATE)
        for cp in tr.iter(q('p')):
            for r in self.text_runs(cp):
                parent = r.getparent(); pos = parent.index(r)
                for t in r.findall(q('t')): t.tag = q('delText')
                parent.remove(r); parent.insert(pos, self.wrap('del', [r]))
        self.refresh()
        self.log.append(('delrow', prefix[:60]))


    # ---------- imágenes ----------
    def insert_images_after(self, prefix, images, caption, after=None, nth=0, exact=False,
                            max_w_cm=15.0, max_h_cm=11.0, gap_cm=0.3):
        """Inserta (como cambio controlado) un párrafo centrado con una o varias imágenes
        lado a lado y, debajo, la leyenda 'Ilustración N. <caption>' con campo SEQ,
        igual que las ilustraciones existentes. Devuelve el número asignado."""
        import os, shutil
        from PIL import Image
        pkg = os.path.dirname(os.path.dirname(self.path))  # carpeta del docx desempaquetado
        R_NS = 'http://schemas.openxmlformats.org/officeDocument/2006/relationships'
        rels_path = os.path.join(pkg, 'word', '_rels', 'document.xml.rels')
        rels = etree.parse(rels_path)
        rroot = rels.getroot()
        used = {r.get('Id') for r in rroot}
        os.makedirs(os.path.join(pkg, 'word', 'media'), exist_ok=True)
        # tamaños: misma altura para todas, ajustadas al ancho disponible
        dims = [Image.open(p).size for p in images]
        n = len(images)
        avail_w = max_w_cm - gap_cm * (n - 1)
        h = max_h_cm
        widths = [h * w / hh for (w, hh) in dims]
        if sum(widths) > avail_w:
            h = h * avail_w / sum(widths)
            widths = [h * w / hh for (w, hh) in dims]
        EMU = 360000
        A = 'http://schemas.openxmlformats.org/drawingml/2006/main'
        WP = 'http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing'
        PIC = 'http://schemas.openxmlformats.org/drawingml/2006/picture'
        maxdoc = max([int(x.get('id')) for x in self.root.iter('{%s}docPr' % WP)] + [1000])
        runs = []
        for k, p in enumerate(images):
            ext = os.path.splitext(p)[1].lower().lstrip('.')
            if ext == 'jpeg': ext = 'jpg'
            if ext not in ('png', 'jpg'):
                raise ValueError('formato no soportado (usa png o jpg): ' + p)
            base = 'evidencia_%d.%s' % (self.nid + k + 1, ext)
            shutil.copy(p, os.path.join(pkg, 'word', 'media', base))
            rid = 'rIdEv%d' % (self.nid + k + 1)
            while rid in used: rid += 'x'
            used.add(rid)
            rel = etree.SubElement(rroot, '{http://schemas.openxmlformats.org/package/2006/relationships}Relationship')
            rel.set('Id', rid)
            rel.set('Type', R_NS + '/image')
            rel.set('Target', 'media/' + base)
            cx, cy = int(widths[k] * EMU), int(h * EMU)
            did = maxdoc + k + 1
            xml = ('<w:r xmlns:w="%s" xmlns:wp="%s" xmlns:a="%s" xmlns:pic="%s" xmlns:r="%s">'
                   '<w:rPr><w:noProof/></w:rPr><w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">'
                   '<wp:extent cx="%d" cy="%d"/><wp:docPr id="%d" name="Evidencia %d"/>'
                   '<wp:cNvGraphicFramePr><a:graphicFrameLocks noChangeAspect="1"/></wp:cNvGraphicFramePr>'
                   '<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">'
                   '<pic:pic><pic:nvPicPr><pic:cNvPr id="%d" name="%s"/><pic:cNvPicPr/></pic:nvPicPr>'
                   '<pic:blipFill><a:blip r:embed="%s"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>'
                   '<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="%d" cy="%d"/></a:xfrm>'
                   '<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic></a:graphicData></a:graphic>'
                   '</wp:inline></w:drawing></w:r>') % (W, WP, A, PIC, R_NS, cx, cy, did, did, did, base, rid, cx, cy)
            runs.append(etree.fromstring(xml))
            if k < n - 1:
                runs.append(self.mk_run(None, '  '))
        rels.write(rels_path, xml_declaration=True, encoding='UTF-8', standalone=True)
        self.nid += n
        # número de ilustración = SEQ Ilustración anteriores + 1
        i = self.find(prefix, after, nth, exact=exact)
        anchor = self.paras[i]
        count = 0
        for el in self.root.iter(q('p'), q('instrText')):
            if el is anchor: break
            if el.tag == q('instrText') and 'SEQ Ilustraci' in (el.text or ''):
                # las copias en mc:Fallback (cuadros de texto) no cuentan
                if not any(etree.QName(a).localname == 'Fallback' for a in el.iterancestors()):
                    count += 1
        # el bucle anterior se detiene al llegar al ancla (iter recorre en orden de documento)
        num = count + 1
        def para(ppr_children):
            p = etree.Element(q('p')); ppr = etree.SubElement(p, q('pPr'))
            for c in ppr_children: ppr.append(c)
            mr = etree.SubElement(ppr, q('rPr'))
            m = etree.SubElement(mr, q('ins')); m.set(q('id'), self.id()); m.set(q('author'), AUTHOR); m.set(q('date'), DATE)
            return p
        def el(tag, **attrs):
            e = etree.Element(q(tag))
            for k2, v in attrs.items(): e.set(q(k2), v)
            return e
        pimg = para([el('keepNext'), el('jc', val='center')])
        pimg.append(self.wrap('ins', runs))
        pcap = para([el('pStyle', val='Descripcin'), el('jc', val='center')])
        def crun(text=None, fld=None, instr=None, italic_off=False):
            r = etree.Element(q('r')); rp = etree.SubElement(r, q('rPr'))
            if italic_off: rp.append(el('i', val='0'))
            rp.append(el('iCs', val='0')); rp.append(el('color', val='auto'))
            if fld: r.append(el('fldChar', fldCharType=fld))
            if instr:
                t = etree.SubElement(r, q('instrText')); t.text = instr; t.set('{%s}space' % XML, 'preserve')
            if text is not None:
                t = etree.SubElement(r, q('t')); t.text = text; t.set('{%s}space' % XML, 'preserve')
            return r
        crs = [crun('Ilustración '), crun(fld='begin', italic_off=True), crun(instr=' SEQ Ilustración \\* ARABIC ', italic_off=True),
               crun(fld='separate', italic_off=True), crun(str(num), italic_off=True), crun(fld='end', italic_off=True),
               crun('. ' + caption)]
        pcap.append(self.wrap('ins', crs))
        anchor.addnext(pimg); pimg.addnext(pcap)
        self.refresh()
        self.log.append(('image', prefix[:60], num))
        return num

    def save(self, path=None):
        self.tree.write(path or self.path, xml_declaration=True, encoding='UTF-8', standalone=True)
