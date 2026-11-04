"""Driver mínimo de UI por adb. Uso: python u.py 'expr; expr'"""
import re, subprocess, sys, time, os
from xml.etree import ElementTree as ET

ADB = "/Users/teddyarellano/Library/Android/sdk/platform-tools/adb"
S = os.path.dirname(os.path.abspath(__file__))
SHOTS = os.path.join(S, "shots")
VOZ = os.path.join(S, "voz")
os.makedirs(SHOTS, exist_ok=True); os.makedirs(VOZ, exist_ok=True)
APP = "com.xpspeak.app"


def adb(*a, binary=False):
    r = subprocess.run([ADB, *a], capture_output=True, text=not binary)
    return r.stdout


def nodos():
    for _ in range(4):
        adb("shell", "rm", "-f", "/sdcard/ui.xml")
        adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
        x = adb("shell", "cat", "/sdcard/ui.xml")
        if x.strip().startswith("<?xml"):
            break
        time.sleep(0.7)
    out = []
    for n in ET.fromstring(x).iter("node"):
        b = list(map(int, re.findall(r"\d+", n.get("bounds"))))
        out.append(dict(t=n.get("text") or "", d=n.get("content-desc") or "", cls=n.get("class"),
                        click=n.get("clickable") == "true", b=b, cx=(b[0] + b[2]) // 2, cy=(b[1] + b[3]) // 2))
    return out


def buscar(txt, ns=None, exacto=False):
    ns = ns or nodos()
    f = (lambda s: s == txt) if exacto else (lambda s: txt in s)
    return [n for n in ns if f(n["t"]) or f(n["d"])]


def esperar(txt, t=30, exacto=False):
    fin = time.time() + t
    while time.time() < fin:
        r = buscar(txt, exacto=exacto)
        if r:
            return r
        time.sleep(0.8)
    raise RuntimeError(f"no aparece {txt!r}")


def hay(txt):
    return bool(buscar(txt))


def xy(x, y):
    adb("shell", "input", "tap", str(x), str(y))


def tap(txt, i=0, t=15, exacto=False):
    n = esperar(txt, t, exacto)[i]
    xy(n["cx"], n["cy"])
    time.sleep(0.6)
    return n


def escribir(texto):
    adb("shell", "input", "text", texto.replace(" ", "%s").replace("'", "\\'"))


def fill(etiqueta, texto, i=0):
    ns = nodos()
    edits = [n for n in ns if n["cls"].endswith("EditText")]
    lab = buscar(etiqueta, ns)[i]
    campo = min(edits, key=lambda e: 0 if (e["b"][0] <= lab["cx"] <= e["b"][2] and e["b"][1] <= lab["cy"] <= e["b"][3]) else abs(e["cy"] - lab["cy"]) + 1)
    xy(campo["cx"], campo["cy"]); time.sleep(0.5)
    adb("shell", "input", "keycombination", "113", "29"); adb("shell", "input", "keyevent", "67")
    escribir(texto); time.sleep(0.4)


def back():
    adb("shell", "input", "keyevent", "4"); time.sleep(0.6)


def ocultar_teclado():
    if "mInputShown=true" in adb("shell", "dumpsys", "input_method"):
        back()


def scroll(n=1, dy=900, y0=1700):
    for _ in range(n):
        adb("shell", "input", "swipe", "540", str(y0), "540", str(y0 - dy), "600"); time.sleep(0.8)


def scroll_hasta(txt, maxn=12, dy=700):
    for _ in range(maxn):
        if hay(txt):
            return
        scroll(1, dy)
    raise RuntimeError(f"no encontré {txt!r}")


def arriba(n=1, dy=900):
    for _ in range(n):
        adb("shell", "input", "swipe", "540", "700", "540", str(700 + dy), "600"); time.sleep(0.6)


def shot(nombre):
    open(os.path.join(SHOTS, nombre + ".png"), "wb").write(adb("exec-out", "screencap", "-p", binary=True))
    print("captura", nombre)


def ls():
    for n in nodos():
        if n["t"] or n["d"] or n["cls"].endswith("EditText"):
            print(f'{n["cx"]:5},{n["cy"]:5} {"*" if n["click"] else " "} {n["t"][:70]!r} {n["d"][:50]!r} {n["cls"].split(".")[-1]}')


def nav(tab):
    xy({"Chat": 99, "Lecciones": 320, "Inicio": 540, "Progreso": 760, "Juegos": 980}[tab], 2298); time.sleep(2)


def abrir_app():
    adb("shell", "monkey", "-p", APP, "-c", "android.intent.category.LAUNCHER", "1"); time.sleep(2)


def tts(nombre, texto, voz="Samantha", rate=175):
    aiff = os.path.join(VOZ, nombre + ".aiff"); wav = os.path.join(VOZ, nombre + ".wav")
    if not os.path.exists(wav):
        subprocess.run(["say", "-v", voz, "-r", str(rate), "-o", aiff, texto], check=True)
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", aiff, "-ac", "1", "-ar", "16000", "-sample_fmt", "s16", wav], check=True)
    return wav


def hablar(nombre, espera=True):
    wav = os.path.join(VOZ, nombre + ".wav")
    adb("push", wav, "/data/local/tmp/voz.wav")
    adb("shell", "cat /data/local/tmp/voz.wav | run-as com.xpspeak.app sh -c 'mkdir -p files; cat > files/tmp.wav; mv files/tmp.wav files/xpspeak_voz.wav'")
    if espera:
        time.sleep(os.path.getsize(wav) / 32000 + 0.5)


def status_demo():
    for c in ["enter", "clock -e hhmm 1030", "battery -e level 100 -e plugged false",
              "network -e wifi show -e level 4 -e fully true", "network -e mobile hide", "notifications -e visible false"]:
        adb("shell", "am", "broadcast", "-a", "com.android.systemui.demo", "-e", "command", *c.split(" "))




def editar(i, texto):
    ed = [n for n in nodos() if n["cls"].endswith("EditText")]
    e = ed[i]
    xy(e["cx"], e["cy"]); time.sleep(0.6)
    adb("shell", "input", "keycombination", "113", "29"); adb("shell", "input", "keyevent", "67")
    escribir(texto); time.sleep(0.4); ocultar_teclado(); time.sleep(0.5)


if __name__ == "__main__":
    exec(sys.argv[1])
