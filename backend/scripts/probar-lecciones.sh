#!/usr/bin/env bash
# Colección de pruebas manual de Lecciones (docs/lecciones-diseno.md §7):
# catálogo → contenido → intento reprobado → conceptos débiles → intento
# aprobado → desbloqueo → reintento idempotente → lección bloqueada.
#
#   BASE_URL=https://xp-speak-auth-backend.vercel.app TOKEN=$(node scripts/obtener-token.js …) \
#     ./scripts/probar-lecciones.sh
#
# Úsalo con un usuario de PRUEBA: escribe progreso real en Firestore.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:3000}"
: "${TOKEN:?Define TOKEN con un ID token de Firebase (scripts/obtener-token.js)}"
AUTH=(-H "Authorization: Bearer ${TOKEN}")
JSON=(-H "Content-Type: application/json")

paso() { printf '\n\033[1m== %s\033[0m\n' "$1"; }
uuid() { node -e 'console.log(require("crypto").randomUUID())'; }
pedir() { curl -sS -w '\n[HTTP %{http_code}]\n' "$@"; }

L1=a1-saludos-presentaciones
L2=a1-informacion-personal

APROBADAS='[{"id":"q1","valor":1},{"id":"q2","valor":"is"},{"id":"q3","valor":"am"},{"id":"q4","valor":1},{"id":"q5","valor":1},{"id":"q6","valor":[["Hello!","¡Hola!"],["Goodbye!","¡Adiós!"],["How are you?","¿Cómo estás?"],["See you later!","¡Nos vemos luego!"]]}]'
# 3/6 = 50%: falla q2 y q3 (to be) y q4 (pronombre).
REPROBADAS='[{"id":"q1","valor":1},{"id":"q2","valor":"are"},{"id":"q3","valor":"is"},{"id":"q4","valor":0},{"id":"q5","valor":1},{"id":"q6","valor":[["Hello!","¡Hola!"],["Goodbye!","¡Adiós!"],["How are you?","¿Cómo estás?"],["See you later!","¡Nos vemos luego!"]]}]'

paso "Manifiesto (público)"
pedir "${BASE_URL}/api/lessons/manifest"

paso "Catálogo A1"
pedir "${AUTH[@]}" "${BASE_URL}/api/lessons?level=A1"

paso "Contenido de ${L1}"
pedir "${AUTH[@]}" "${BASE_URL}/api/lessons/${L1}" | head -c 400; echo

paso "${L2} bloqueada (espera 403 si es un usuario nuevo)"
pedir "${AUTH[@]}" "${JSON[@]}" -X POST "${BASE_URL}/api/lessons/${L2}/attempt" \
  -d "{\"attempt_id\":\"$(uuid)\",\"respuestas\":[{\"id\":\"q1\",\"valor\":1}]}"

paso "Guardar avance de teoría"
pedir "${AUTH[@]}" "${JSON[@]}" -X POST "${BASE_URL}/api/lessons/${L1}/progress" -d '{"seccion_actual":2}'

paso "Intento reprobado"
pedir "${AUTH[@]}" "${JSON[@]}" -X POST "${BASE_URL}/api/lessons/${L1}/attempt" \
  -d "{\"attempt_id\":\"$(uuid)\",\"respuestas\":${REPROBADAS}}"

paso "Conceptos débiles (SRS)"
pedir "${AUTH[@]}" "${BASE_URL}/api/srs/review?level=A1"

INTENTO=$(uuid)
paso "Intento aprobado (attempt_id ${INTENTO})"
pedir "${AUTH[@]}" "${JSON[@]}" -X POST "${BASE_URL}/api/lessons/${L1}/attempt" \
  -d "{\"attempt_id\":\"${INTENTO}\",\"respuestas\":${APROBADAS}}"

paso "Reenvío del MISMO intento (idempotente: repetido=true, sin XP extra)"
pedir "${AUTH[@]}" "${JSON[@]}" -X POST "${BASE_URL}/api/lessons/${L1}/attempt" \
  -d "{\"attempt_id\":\"${INTENTO}\",\"respuestas\":${APROBADAS}}"

paso "Catálogo: ${L1} completada y ${L2} disponible"
pedir "${AUTH[@]}" "${BASE_URL}/api/lessons?level=A1"

paso "Sin token (espera 401)"
pedir "${BASE_URL}/api/lessons?level=A1"
