#!/usr/bin/env sh
set -eu

# ==============================================================================
# deploy-authorized-devices.sh
# Compila el APK de Debug, valida la lista local de dispositivos autorizados
# (authorized-devices.local.conf, ignorado por Git) e instala/lanza la app
# únicamente en los dispositivos conectados que estén explícitamente autorizados.
# Compatible con POSIX sh, bash 3.2+ (macOS) y zsh.
# ==============================================================================

PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONFIG_FILE="${PROJECT_ROOT}/authorized-devices.local.conf"
EXAMPLE_FILE="${PROJECT_ROOT}/authorized-devices.example.conf"
APP_ID="com.laralbir.japantraveltools"
MAIN_ACTIVITY="${APP_ID}/.MainActivity"

# Evitar crash de mDNS auto-connect y fallo de libusb en platform-tools 37.0.0 en macOS arm64
export ADB_MDNS=0
export ADB_MDNS_AUTO_CONNECT=0
export ADB_LIBUSB=0

# Localizar binario adb
if command -v adb >/dev/null 2>&1; then
  ADB_BIN="$(command -v adb)"
elif [ -x "${HOME}/Library/Android/sdk/platform-tools/adb" ]; then
  ADB_BIN="${HOME}/Library/Android/sdk/platform-tools/adb"
else
  echo "❌ Error: No se encontró 'adb' en el PATH ni en ~/Library/Android/sdk/platform-tools/adb" >&2
  exit 1
fi

if [ ! -f "${CONFIG_FILE}" ]; then
  echo "⚠️  No existe el archivo local de dispositivos autorizados: ${CONFIG_FILE}"
  echo "👉 Créalo a partir de la plantilla (este archivo está en .gitignore y nunca se sube al repo):"
  echo "   cp ${EXAMPLE_FILE} ${CONFIG_FILE}"
  echo ""
  echo "📱 Dispositivos detectados actualmente por ADB:"
  "${ADB_BIN}" devices -l
  exit 1
fi

# Función auxiliar para limpiar espacios al principio y al final
trim_string() {
  echo "$1" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//'
}

# Auto-descubrimiento mDNS (Wireless Debugging) para dispositivos autorizados
MDNS_DISCOVERY_SCRIPT="${PROJECT_ROOT}/scripts/discover-adb-mdns.py"
MDNS_CACHE_FILE="/tmp/japan_travel_tools_mdns_devices.tmp"
rm -f "${MDNS_CACHE_FILE}"

if [ -x "${MDNS_DISCOVERY_SCRIPT}" ] && command -v python3 >/dev/null 2>&1; then
  python3 "${MDNS_DISCOVERY_SCRIPT}" > "${MDNS_CACHE_FILE}" 2>/dev/null || true
  if [ -s "${MDNS_CACHE_FILE}" ]; then
    while IFS= read -r mdns_line || [ -n "$mdns_line" ]; do
      disc_serial="$(echo "$mdns_line" | cut -d'|' -f1)"
      disc_endpoint="$(echo "$mdns_line" | cut -d'|' -f2)"
      disc_name="$(echo "$mdns_line" | cut -d'|' -f3)"
      disc_service="$(echo "$mdns_line" | cut -d'|' -f4)"

      # Comprobar si el dispositivo descubierto está en la lista de autorizados
      is_authorized=0
      while IFS= read -r conf_line || [ -n "$conf_line" ]; do
        trimmed="$(trim_string "$conf_line")"
        case "$trimmed" in ""|\#*) continue ;; esac
        auth_id="$(trim_string "${trimmed%%|*}")"
        if [ "$auth_id" = "$disc_serial" ] || [ "$auth_id" = "$disc_service" ] || [ "$auth_id" = "$disc_endpoint" ]; then
          is_authorized=1
          break
        fi
      done < "${CONFIG_FILE}"

      if [ "$is_authorized" -eq 1 ]; then
        echo "📡 [mDNS] Dispositivo autorizado detectado en red Wi-Fi: ${disc_name:-Android} (${disc_serial}) en ${disc_endpoint}"
        "${ADB_BIN}" connect "${disc_endpoint}" >/dev/null 2>&1 || true
      fi
    done < "${MDNS_CACHE_FILE}"
  fi
fi

# Verificar que exista al menos un serial válido en CONFIG_FILE y conectar entradas Wi-Fi estáticas (IP:puerto)
AUTH_COUNT=0
while IFS= read -r line || [ -n "$line" ]; do
  trimmed="$(trim_string "$line")"
  case "$trimmed" in
    ""|\#*) continue ;;
  esac
  raw_serial="${trimmed%%|*}"
  serial="$(trim_string "$raw_serial")"
  raw_alias="${trimmed#*|}"
  alias_name="$(trim_string "$raw_alias")"
  if [ -n "$serial" ]; then
    AUTH_COUNT=$((AUTH_COUNT + 1))
    case "$serial" in
      *:*)
        echo "📡 Conectando por ADB Wi-Fi a ${alias_name:-$serial} (${serial})..."
        "${ADB_BIN}" connect "${serial}" >/dev/null 2>&1 || true
        ;;
    esac
  fi
done < "${CONFIG_FILE}"

if [ "${AUTH_COUNT}" -eq 0 ]; then
  echo "⚠️  El archivo ${CONFIG_FILE} no tiene ningún serial configurado."
  echo "📱 Dispositivos conectados actualmente:"
  "${ADB_BIN}" devices -l
  exit 1
fi

echo "🔨 Compilando APK Debug (assembleDebug)..."
(cd "${PROJECT_ROOT}" && ./gradlew assembleDebug)

APK_PATH="${PROJECT_ROOT}/app/build/outputs/apk/debug/app-debug.apk"
if [ ! -f "${APK_PATH}" ]; then
  echo "❌ No se encontró el APK compilado en ${APK_PATH}" >&2
  exit 1
fi

# Mostrar estado de dispositivos ADB (incluyendo 'unauthorized' si falta aceptar en la pantalla del móvil)
UNAUTHORIZED_SERIALS="$("${ADB_BIN}" devices | awk 'NR>1 && $2=="unauthorized" {print $1}')"
if [ -n "${UNAUTHORIZED_SERIALS}" ]; then
  echo "⚠️  Dispositivo(s) pendiente(s) de aceptar '¿Permitir depuración por USB?' en la pantalla del móvil:"
  echo "${UNAUTHORIZED_SERIALS}"
fi

CONNECTED_SERIALS="$("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')"
if [ -z "${CONNECTED_SERIALS}" ]; then
  # Si el demonio ADB previo se inició con libusb=1 por Android Studio, reiniciarlo con ADB_LIBUSB=0
  "${ADB_BIN}" kill-server >/dev/null 2>&1 || true
  "${ADB_BIN}" start-server >/dev/null 2>&1 || true
  CONNECTED_SERIALS="$("${ADB_BIN}" devices | awk 'NR>1 && $2=="device" {print $1}')"
fi

if [ -z "${CONNECTED_SERIALS}" ]; then
  echo "⚠️  No hay ningún dispositivo Android conectado en estado 'device' por ADB en este momento."
  echo "👉 Asegúrate de tener activada la 'Depuración por USB' en Opciones para desarrolladores y aceptar el diálogo RSA en el teléfono."
  exit 1
fi

# Función para buscar si un serial está autorizado en CONFIG_FILE y devolver su alias
find_authorized_alias() {
  target_serial="$1"

  # Si el target es IP:puerto, consultar su número de serie de hardware real
  hw_serial=""
  case "$target_serial" in
    *:*)
      hw_serial="$("${ADB_BIN}" -s "${target_serial}" shell getprop ro.serialno 2>/dev/null | tr -d '\r\n' || true)"
      ;;
  esac

  # Comprobar también en la caché mDNS si target_serial coincide con algún endpoint
  mdns_serial=""
  if [ -s "${MDNS_CACHE_FILE}" ]; then
    while IFS= read -r mdns_line || [ -n "$mdns_line" ]; do
      endpoint="$(echo "$mdns_line" | cut -d'|' -f2)"
      if [ "$endpoint" = "$target_serial" ]; then
        mdns_serial="$(echo "$mdns_line" | cut -d'|' -f1)"
        break
      fi
    done < "${MDNS_CACHE_FILE}"
  fi

  while IFS= read -r line || [ -n "$line" ]; do
    trimmed="$(trim_string "$line")"
    case "$trimmed" in
      ""|\#*) continue ;;
    esac
    raw_serial="${trimmed%%|*}"
    serial="$(trim_string "$raw_serial")"
    raw_alias="${trimmed#*|}"
    alias_name="$(trim_string "$raw_alias")"
    if [ "$serial" = "$target_serial" ] || \
       { [ -n "$hw_serial" ] && [ "$serial" = "$hw_serial" ]; } || \
       { [ -n "$mdns_serial" ] && [ "$serial" = "$mdns_serial" ]; }; then
      echo "${alias_name:-$serial}"
      return 0
    fi
  done < "${CONFIG_FILE}"
  return 1
}

INSTALLED_COUNT=0
for serial in ${CONNECTED_SERIALS}; do
  if device_label="$(find_authorized_alias "$serial")"; then
    echo "📲 Instalando y lanzando en dispositivo autorizado: ${device_label} (${serial})..."
    if command -v android >/dev/null 2>&1; then
      android install --device="${serial}" --apks="${APK_PATH}" || "${ADB_BIN}" -s "${serial}" install -r -t "${APK_PATH}"
    else
      "${ADB_BIN}" -s "${serial}" install -r -t "${APK_PATH}"
    fi
    "${ADB_BIN}" -s "${serial}" shell am force-stop "${APP_ID}" >/dev/null 2>&1 || true
    "${ADB_BIN}" -s "${serial}" shell am start -S -n "${MAIN_ACTIVITY}"
    INSTALLED_COUNT=$((INSTALLED_COUNT + 1))
  else
    echo "🔒 Ignorando dispositivo conectado NO incluido en authorized-devices.local.conf: ${serial}"
  fi
done

if [ "${INSTALLED_COUNT}" -eq 0 ]; then
  echo "⚠️  Ninguno de los dispositivos conectados está en tu lista autorizada (${CONFIG_FILE})."
  exit 1
fi

echo "✅ Despliegue Debug completado en ${INSTALLED_COUNT} dispositivo(s) autorizado(s)."
