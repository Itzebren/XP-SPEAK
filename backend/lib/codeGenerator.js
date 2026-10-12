// Sin caracteres ambiguos (0/O, 1/I/L) para que el usuario lo transcriba
// del correo sin confundirse.
const ALFABETO = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

function generarCodigo(longitud = 6) {
  let codigo = '';
  for (let i = 0; i < longitud; i++) {
    codigo += ALFABETO[Math.floor(Math.random() * ALFABETO.length)];
  }
  return codigo;
}

module.exports = { generarCodigo };
