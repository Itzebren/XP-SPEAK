/*
 * Parche para el micrófono del emulador de Android en macOS (lo carga emulador.sh).
 *
 * El emulador registra un aviso de «cambió el formato de audio» cada vez que abre el micrófono
 * de la Mac (AudioObjectAddPropertyListener) y nunca lo quita al cerrarlo. En la siguiente
 * apertura macOS rechaza el registro repetido con kAudioHardwareIllegalOperationError, el
 * emulador lo toma como fallo («coreaudio: Could not set audio format change listener») y desde
 * entonces el micrófono virtual solo entrega silencio: la primera grabación funciona y las
 * demás no, hasta reiniciar el emulador.
 *
 * Aquí se intercepta esa llamada: si macOS dice que el aviso ya estaba registrado, se quita el
 * viejo y se vuelve a registrar, así que la reapertura sale bien. No toca nada más.
 */
#include <CoreAudio/CoreAudio.h>

static OSStatus registrar(AudioObjectID objeto, const AudioObjectPropertyAddress *direccion,
                          AudioObjectPropertyListenerProc aviso, void *datos) {
    OSStatus estado = AudioObjectAddPropertyListener(objeto, direccion, aviso, datos);
    if (estado == kAudioHardwareIllegalOperationError) {
        AudioObjectRemovePropertyListener(objeto, direccion, aviso, datos);
        estado = AudioObjectAddPropertyListener(objeto, direccion, aviso, datos);
    }
    return estado;
}

__attribute__((used, section("__DATA,__interpose")))
static const struct { const void *nueva; const void *original; } intercepcion = {
    (const void *)registrar, (const void *)AudioObjectAddPropertyListener
};
