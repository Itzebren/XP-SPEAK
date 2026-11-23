"""
Inserta capturas de pantalla en el TT XP-SPEAK como cambios controlados (autor "Claude")
y genera dos archivos: uno con control de cambios y otro limpio (cambios aceptados).

Uso:
    python insertar_evidencias.py BASE.docx SALIDA_PREFIJO

    BASE.docx       -> normalmente TT_B162_corregido_limpio.docx (o la versión más reciente
                       que el equipo haya guardado en Word).
    SALIDA_PREFIJO  -> p. ej. TT_B162_evidencias  ->  TT_B162_evidencias_con_cambios.docx
                                                     TT_B162_evidencias_limpio.docx

Edita el diccionario EVIDENCIAS antes de ejecutar. Requiere: lxml, Pillow.
"""
import os, sys, shutil, tempfile, zipfile, subprocess
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tc import Doc

# ------------------------------------------------------------------
# Código de prueba -> (lista de imágenes, texto de la leyenda)
# Varias imágenes en la misma lista se colocan lado a lado (misma altura).
# La leyenda queda como "Ilustración N. <texto>" con campo SEQ.
# Deja fuera las pruebas que todavía no tienen captura.
# ------------------------------------------------------------------
CARPETA = 'capturas'   # carpeta con las imágenes de la app
EVIDENCIAS = {
    # 'PV1-R':  (['registro_formulario.png', 'verifica_correo.png'],
    #            'Evidencia de la prueba PV1-R: registro de cuenta'),
}

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

def main(base, prefijo):
    tmp = tempfile.mkdtemp()
    w = os.path.join(tmp, 'w')
    unpack(base, w)
    d = Doc(os.path.join(w, 'word', 'document.xml'))
    for code, (imgs, leyenda) in EVIDENCIAS.items():
        rutas = [os.path.join(CARPETA, i) for i in imgs]
        for r in rutas:
            if not os.path.exists(r):
                raise FileNotFoundError(r)
        marcador = 'Evidencia: [PENDIENTE: captura de pantalla de la prueba %s ' % code
        n = d.insert_images_after(marcador, rutas, leyenda)
        d.delete_para(marcador)
        print('%s -> Ilustración %d (%d imagen(es))' % (code, n, len(rutas)))
    d.save()
    con_cambios = prefijo + '_con_cambios.docx'
    pack(w, con_cambios)
    # versión limpia
    c = os.path.join(tmp, 'c')
    shutil.copytree(w, c)
    subprocess.run([sys.executable, os.path.join(os.path.dirname(os.path.abspath(__file__)), 'accept.py'),
                    os.path.join(w, 'word', 'document.xml'), os.path.join(c, 'word', 'document.xml')], check=True)
    pack(c, prefijo + '_limpio.docx')
    print('Listo:', con_cambios, '|', prefijo + '_limpio.docx')

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
