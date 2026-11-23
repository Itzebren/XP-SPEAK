#!/bin/zsh
# Arranca el emulador de Android con el micrófono de la Mac funcionando de forma estable.
#
#   scripts/emulador/emulador.sh              arranca (o reutiliza) el emulador y lo deja listo
#   scripts/emulador/emulador.sh --probar     además graba 5 veces seguidas y dice si llegó audio
#   scripts/emulador/emulador.sh --reiniciar  cierra el emulador abierto y lo vuelve a arrancar
#   AVD=QA_API26 scripts/emulador/emulador.sh usa otro dispositivo virtual (por omisión Pixel_10a)
#
# Por qué hace falta: el emulador (visto en 37.1.11) falla en macOS al REABRIR el micrófono, así
# que la primera grabación funciona y las siguientes llegan en silencio. microfono_mac.c lo
# corrige y este script lo carga al arrancar. Si el emulador se abre desde Android Studio el
# parche no entra: hay que arrancarlo con este script (Android Studio lo detecta ya abierto y
# se puede dar Run sobre él con normalidad).
set -e
AQUI="${0:A:h}"
RAIZ="${AQUI:h:h}"
SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
ADB="$SDK/platform-tools/adb"
AVD="${AVD:-Pixel_10a}"
SALIDA="$RAIZ/build/emulador"
PARCHE="$SALIDA/libmicrofono_mac.dylib"
mkdir -p "$SALIDA"

motor() { pgrep -f "qemu-system.*-avd $AVD" | head -1 }
con_parche() { vmmap "$1" 2>/dev/null | grep -q libmicrofono_mac }

if [[ ! -f "$PARCHE" || "$AQUI/microfono_mac.c" -nt "$PARCHE" ]]; then
  echo "Compilando el parche del micrófono…"
  clang -dynamiclib -O2 -o "$PARCHE" "$AQUI/microfono_mac.c" -framework CoreAudio
fi

PID="$(motor)"
if [[ -n "$PID" ]] && { [[ "$*" == *--reiniciar* ]] || ! con_parche "$PID" }; then
  if [[ "$*" != *--reiniciar* ]]; then
    echo "El emulador $AVD está abierto SIN el parche del micrófono (¿se abrió desde Android Studio?)."
    echo "Ciérralo o vuelve a correr esto con --reiniciar."
    exit 1
  fi
  echo "Cerrando el emulador abierto…"
  "$ADB" emu kill >/dev/null 2>&1 || kill "$PID"
  while [[ -n "$(motor)" ]]; do sleep 1; done
  PID=""
fi

if [[ -z "$PID" ]]; then
  echo "Arrancando $AVD (arranque en frío, tarda ~1 min)…"
  # OJO: sin `nohup` ni `env`; son binarios protegidos de macOS y borran DYLD_INSERT_LIBRARIES.
  # -allow-host-audio: sin esto el emulador manda silencio en vez del micrófono de la Mac.
  # -no-snapshot: los arranques rápidos restauran el audio a medias; siempre en frío.
  (
    trap '' HUP
    export DYLD_INSERT_LIBRARIES="$PARCHE"
    "$SDK/emulator/emulator" -avd "$AVD" -allow-host-audio -no-snapshot > "$SALIDA/emulador.log" 2>&1 &
  )
fi

"$ADB" wait-for-device
until [[ "$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == 1 ]]; do sleep 2; done
PID="$(motor)"
con_parche "$PID" || { echo "✗ El emulador arrancó sin el parche del micrófono (revisa $SALIDA/emulador.log)"; exit 1 }
"$ADB" emu avd hostmicon >/dev/null 2>&1 || true
# El build debug habla con el backend local por localhost (ver app/build.gradle.kts).
"$ADB" reverse tcp:3000 tcp:3000 >/dev/null
echo "✓ Emulador listo: micrófono de la Mac conectado y con el parche cargado."

if [[ "$*" == *--probar* ]]; then
  echo "Probando el micrófono: la Mac va a hablar unos segundos (sube el volumen si está en cero)…"
  export JAVA_HOME="${JAVA_HOME:-/Applications/Android Studio.app/Contents/jbr/Contents/Home}"
  (cd "$RAIZ" && sh ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest --console=plain -q)
  "$ADB" install -r "$RAIZ/app/build/outputs/apk/debug/app-debug.apk" >/dev/null
  "$ADB" install -r "$RAIZ/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk" >/dev/null
  "$ADB" logcat -c
  ( repeat 5 say -v Samantha "Testing the microphone, one two three four five" ) &
  RESULTADO="$("$ADB" shell am instrument -w -e class com.xpspeak.app.MicrofonoEmuladorTest \
    com.xpspeak.app.test/androidx.test.runner.AndroidJUnitRunner 2>&1 || true)"
  kill %1 2>/dev/null || true; killall say 2>/dev/null || true
  "$ADB" logcat -d -s MicrofonoEmulador:I | sed -n 's/.*\(ronda=.*\)/  \1/p'
  if [[ "$RESULTADO" == *"OK (1 test)"* ]]; then
    echo "✓ El micrófono entregó audio en las 5 grabaciones seguidas."
  else
    echo "✗ Alguna grabación llegó en silencio:"
    echo "$RESULTADO" | grep -E "AssertionError|Error" | head -3
    grep -E "coreaudio" "$SALIDA/emulador.log" | sort | uniq -c
    echo "  Revisa también: Ajustes del Sistema → Privacidad → Micrófono (la terminal debe tener permiso)."
    exit 1
  fi
fi
