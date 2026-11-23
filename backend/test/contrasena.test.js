const test = require('node:test');
const assert = require('node:assert');
const { contraseñaEsValida } = require('../api/verify-reset-code');

// Mismas reglas que PasswordValidator (app): 8 caracteres, una letra y un número.
test('contraseña: acepta 8 caracteres con letra y número', () => {
  assert.equal(contraseñaEsValida('ingles2026'), true);
  assert.equal(contraseñaEsValida('Speak2024!'), true);
});

test('contraseña: la ñ y las letras con acento cuentan como letra, igual que en la app', () => {
  assert.equal(contraseñaEsValida('ñññ12345'), true);
  assert.equal(contraseñaEsValida('áéíóú123'), true);
});

test('contraseña: rechaza si falta longitud, letra o número', () => {
  assert.equal(contraseñaEsValida('abc123'), false);
  assert.equal(contraseñaEsValida('12345678'), false);
  assert.equal(contraseñaEsValida('password'), false);
  assert.equal(contraseñaEsValida('!!!!!!!!'), false);
  assert.equal(contraseñaEsValida(undefined), false);
});
