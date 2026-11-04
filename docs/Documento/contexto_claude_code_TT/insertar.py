import os, sys, shutil, tempfile, zipfile, subprocess
CTX = '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/contexto_claude_code_TT'
sys.path.insert(0, CTX)
from tc import Doc

S = os.path.dirname(os.path.abspath(__file__))
EV = os.path.join(S, 'ev')
BASE = '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/TT_B162_corregido_limpio.docx'
OUT = '/Users/teddyarellano/Developer/XP-SPEAK/docs/Documento/TT_B162_evidencias'

def ev(*n):
    return [os.path.join(EV, x if '.' in x else x + '.jpg') for x in n]

PV = [
    ('PV1-R', [(ev('pv1_a', 'pv1_b', 'pv1_c'), 'Evidencia de la prueba PV1-R: registro de cuenta')]),
    ('PV2-IS', [(ev('pv2_a', 'pv2_b', 'pv2_c'), 'Evidencia de la prueba PV2-IS: inicio de sesión')]),
    ('PV3-RA', [(ev('pv3_a', 'pv3_b', 'pv3_c'), 'Evidencia de la prueba PV3-RA: recuperación de acceso')]),
    ('PV4-NP', [(ev('pv4_a', 'pv4_b', 'pv4_c'), 'Evidencia de la prueba PV4-NP: nivel y perfil')]),
    ('PV5-CH', [(ev('pv5_a', 'pv5_b'), 'Evidencia de la prueba PV5-CH: chat con inteligencia artificial')]),
    ('PV6-PR', [(ev('pv6_a', 'pv6_b', 'pv6_c'), 'Evidencia de la prueba PV6-PR: evaluación de pronunciación')]),
    ('PV7-LE', [(ev('pv7_a', 'pv7_b', 'pv7_c'), 'Evidencia de la prueba PV7-LE: lecciones y evaluaciones (1 de 2)'),
                (ev('pv7_d', 'pv7_e', 'pv7_f'), 'Evidencia de la prueba PV7-LE: lecciones y evaluaciones (2 de 2)')]),
    ('PV8-MJ', [(ev('pv8_a', 'pv8_b', 'pv8_c'), 'Evidencia de la prueba PV8-MJ: minijuegos')]),
    ('PV9-PG', [(ev('pv9_a', 'pv9_b', 'pv9_c'), 'Evidencia de la prueba PV9-PG: progreso y logros')]),
    ('PV10-NT', [(ev('pv10_a', 'pv10_b', 'pv10_c'), 'Evidencia de la prueba PV10-NT: notificaciones')]),
    ('PV11-EC', [(ev('pv11_a', 'pv11_b'), 'Evidencia de la prueba PV11-EC: cuenta y eliminación de datos')]),
]

ANEXO = [
    ('Ejecución de las 121 pruebas unitarias de la aplicación',
     'Ejecución de las 144 pruebas unitarias de la aplicación (./gradlew :app:testDebugUnitTest o el informe de Android Studio): Ilustración {n}.',
     [(ev('anexo_gradle_test.png'), 'Ejecución de las pruebas unitarias de la aplicación (3 de noviembre de 2026)')]),
    ('Ejecución de las 154 pruebas del servidor',
     'Ejecución de las 166 pruebas del servidor (npm test en la carpeta backend): Ilustración {n}.',
     [(ev('anexo_npm_test.png'), 'Ejecución de las pruebas unitarias del servidor (3 de noviembre de 2026)')]),
    ('Ejecución de las pruebas con emuladores',
     'Ejecución de las pruebas con emuladores (npm run test:emulador): Ilustración {n}.',
     [(ev('anexo_test_emulador.png'), 'Ejecución de las pruebas de integración con los emuladores de Firebase')]),
    ('Registro de tiempos de la etiqueta XPS-TIEMPOS',
     'Registro de tiempos de la etiqueta XPS-TIEMPOS en Logcat: Ilustración {n}.',
     [(ev('anexo_xps_tiempos.png'), 'Registro de tiempos de la etiqueta XPS-TIEMPOS durante las pruebas de validación')]),
    ('Capturas de las pruebas de validación PV1 a PV11',
     'Capturas de los flujos de las pruebas de aceptación PA-F1 a PA-F4, ejecutados en el emulador: Ilustraciones {n} a {m}.',
     [(ev('paf1_a', 'paf1_b', 'paf1_c'), 'Flujo PA-F1: verificación del correo, personalización e inicio de sesión con la nueva contraseña'),
      (ev('paf2_a', 'paf2_b'), 'Flujo PA-F2: conversación en vivo con corrección y práctica de pronunciación'),
      (ev('paf3_a', 'paf3_b', 'paf3_c'), 'Flujo PA-F3: resultado del repaso, catálogo actualizado y progreso'),
      (ev('paf4_a', 'paf4_b'), 'Flujo PA-F4: eliminación de la cuenta e intento de inicio de sesión posterior')]),
]


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


def insertar(d, ancla, figuras):
    from tc import q
    nums = []
    es_leyenda = False
    for imgs, ley in figuras:
        for r in imgs:
            assert os.path.exists(r), r
        n = d.insert_images_after(ancla, imgs, ley, exact=False)
        if es_leyenda:
            # tc.py no cuenta el campo SEQ del propio párrafo ancla cuando éste es una leyenda
            i = d.find(ancla)
            cap = d.paras[i + 2]
            ts = [t for t in cap.iter(q('t'))]
            k = [j for j, t in enumerate(ts) if t.text == str(n)][0]
            n += 1
            ts[k].text = str(n)
            d.refresh()
        nums.append(n)
        ancla = 'Ilustración %d. %s' % (n, ley)
        es_leyenda = True
    return nums


def main():
    tmp = tempfile.mkdtemp()
    w = os.path.join(tmp, 'w')
    unpack(BASE, w)
    ct = open(os.path.join(w, '[Content_Types].xml')).read()
    if 'Extension="jpg"' not in ct:
        ct = ct.replace('<Default ', '<Default Extension="jpg" ContentType="image/jpeg"/><Default ', 1)
        open(os.path.join(w, '[Content_Types].xml'), 'w').write(ct)
    d = Doc(os.path.join(w, 'word', 'document.xml'))
    for code, figs in PV:
        marcador = 'Evidencia: [PENDIENTE: captura de pantalla de la prueba %s ' % code
        nums = insertar(d, marcador, figs)
        d.delete_para(marcador)
        print(code, '->', nums)
    for prefijo, nuevo, figs in ANEXO:
        nums = insertar(d, prefijo, figs)
        nuevo_t = nuevo.format(n=nums[0], m=nums[-1])
        d.replace(prefijo, nuevo_t)
        from tc import q
        par = d.paras[d.find(nuevo_t.split(' (')[0][:45])]
        for h in list(par.iter(q('highlight'))):
            h.getparent().remove(h)
        d.refresh()
        print(prefijo[:40], '->', nums)
    d.save()
    pack(w, OUT + '_con_cambios.docx')
    c = os.path.join(tmp, 'c')
    shutil.copytree(w, c)
    subprocess.run([sys.executable, os.path.join(CTX, 'accept.py'),
                    os.path.join(w, 'word', 'document.xml'), os.path.join(c, 'word', 'document.xml')], check=True)
    pack(c, OUT + '_limpio.docx')
    print('Listo')


main()
