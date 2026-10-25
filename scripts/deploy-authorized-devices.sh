#!/usr/bin/env zsh
set -euo pipefail

# ==============================================================================
# deploy-authorized-devices.sh
# Compila el APK de Debug, valida la lista local de dispositivos autorizados
# (authorized-devices.local.conf, ignorado por Git) e instala/lanza la app
# únicamente en los dispositivos conectados que estén explícitamente autorizados.
# ==============================================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="${PROJECT_ROOT}/authorized-devices.local.conf"
EXAMPLE_FILE="${PROJECT_ROOT}/authorized-devices.example.conf"
APP_ID="com.laralbir.japantraveltools"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# Localizar binario adb
if command -v adb >/dev/null 2>&1; then
  ADB_BIN="$(command -v adb)"
elif [[ -x "${HOME}/Library/Android/sdk/platform-tools/adb" ]]; then
  ADB_BIN="${HOME}/Library/Android/sdk/platform-tools/adb"
else
  echo "❌ Error: No se encontró 'adb' en el PATH ni en ~/Library/Android/sdk/platform-tools/adb" >&2
  exit 1
fi

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "⚠️  No existe el archivo local de dispositivos autorizados: ${CONFIG_FILE}"
  echo "👉 Créalo a partir de la plantilla (este archivo está en .gitignore y nunca se sube al repo):"
  echo "   cp ${EXAMPLE_FILE} ${CONFIG_FILE}"
  echo ""
  echo "📱 Dispositivos detectados actualmente por ADB:"
  "${ADB_BIN}" devices -l
  exit 1
fi

# Leer seriales autorizados (ignorando comentarios y líneas vacías)
typeset -A AUTHORIZED_DEVICES
while IFS= read -r line || [[ -n "$line" ]]; do
  trimmed="${line#"${line%%[![:space:]]*}"}"
  [[ -z "$trimmed" || "$trimmed" == \#* ]] && continue
  serial="${trimmed%%|*}"
  serial="${serial%"${serial##*[![:space:]]}"}"
  alias_name="${trimmed#*|}"
  alias_name="${alias_name#"${alias_name%%[![:space:]]*}"}"
  if [[ -n "$serial" ]]; then
    AUTHORIZED_DEVICES["$serial"]="${alias_name:-$serial}"
  fi
done < "${CONFIG_FILE}"

if [[ ${#AUTHORIZED_DEVICES[@]} -eq 0 ]]; then
  echo "⚠️  El archivo ${CONFIG_FILE} no tiene ningún serial configurado."
  echo "📱 Dispositivos conectados actualmente:"
  "${ADB_BIN}" devices -l
  exit 1
fi

echo "🔨 Compilando APK Debug (assembleDebug)..."
(cd "${PROJECT_ROOT}" && ./gradlew assembleDebug)

APK_PATH="${PROJECT_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
if [[ ! -f "${APK_PATH}" ]]; then
  echo "❌ No se encontró el APK compilado en ${APK_PATH}" >&2
  exit 1
fi

# Obtener dispositivos conectados en estado 'device'
CONNECTED_SERIALS=($("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}'))

if [[ ${#CONNECTED_SERIALS[@]} -eq 0 ]]; then
  echo "⚠️  No hay ningún dispositivo Android conectado por ADB en este momento."
  exit 1
fi

INSTALLED_COUNT=0
for serial in "${CONNECTED_SERIALS[@]}"; do
  if [[ -n "${AUTHORIZED_DEVICES[$serial]+x}" ]]; then
    device_label="${AUTHORIZED_DEVICES[$serial]}"
    echo "📲 Instalando y lanzando en dispositivo autorizado: ${device_label} (${serial})..."
    if command -v android >/dev/null 2>&1; then
      android install --device="${serial}" --apks="${APK_PATH}" || "${ADB_BIN}" -s "${serial}" install -r -t "${APK_PATH}"
    else
      "${ADB_BIN}" -s "${serial}" install -r -t "${APK_PATH}"
    fi
    "${ADB_BIN}" -s "${serial}" shell am start -n "${MAIN_ACTIVITY}"
    ((INSTALLED_COUNT++))
  else
    echo "🔒 Ignorando dispositivo conectado NO incluido en authorized-devices.local.conf: ${serial}"
  fi
done

if [[ ${INSTALLED_COUNT} -eq 0 ]]; then
  echo "⚠️  Ninguno de los dispositivos conectados está en tu lista autorizada (${CONFIG_FILE})."
  exit 1
fi

echo "✅ Despliegue Debug completado en ${INSTALLED_COUNT} dispositivo(s) autorizado(s)."
