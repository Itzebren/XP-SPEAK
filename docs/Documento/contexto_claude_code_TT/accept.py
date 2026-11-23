import sys
from lxml import etree
W='http://schemas.openxmlformats.org/wordprocessingml/2006/main'
q=lambda t:'{%s}%s'%(W,t)
tree=etree.parse(sys.argv[1]); root=tree.getroot()
# filas borradas
for tr in list(root.iter(q('tr'))):
    trpr=tr.find(q('trPr'))
    if trpr is not None and trpr.find(q('del')) is not None:
        tr.getparent().remove(tr)
for el in list(root.iter(q('del'))):
    par=el.getparent()
    if par.tag==q('rPr'):
        continue
    par.remove(el)
# párrafos con marca de párrafo borrada: fusionar con el siguiente (aquí están vacíos -> eliminar)
for p in list(root.iter(q('p'))):
    ppr=p.find(q('pPr'))
    if ppr is not None and ppr.find(q('rPr')) is not None and ppr.find(q('rPr')).find(q('del')) is not None:
        if not ''.join(t.text or '' for t in p.iter(q('t'))).strip():
            p.getparent().remove(p)
for el in list(root.iter(q('ins'))):
    par=el.getparent()
    if par.tag==q('rPr'):
        par.remove(el); continue
    i=par.index(el)
    for ch in list(el): par.insert(i,ch); i+=1
    par.remove(el)
for el in list(root.iter(q('rPrChange'))): el.getparent().remove(el)
for rpr in list(root.iter(q('rPr'))):
    if len(rpr)==0 and rpr.getparent().tag==q('pPr'): rpr.getparent().remove(rpr)
tree.write(sys.argv[2],xml_declaration=True,encoding='UTF-8',standalone=True)
