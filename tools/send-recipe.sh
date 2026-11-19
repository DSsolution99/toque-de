#!/bin/sh
# Sends one or more recipes in Toque format (JSON) to the device in the home network.
#   tools/send-recipe.sh rezept.json [device-address]
#   cat rezept.json | tools/send-recipe.sh - 192.168.1.50
# The device address can also come from TOQUE_HOST. Format: app/src/main/assets/web/prompt.txt
# Nothing starts on the device: every machine setting must be confirmed there before it runs.
set -eu
file="${1:-}"
host="${2:-${TOQUE_HOST:-}}"
if [ -z "$file" ] || [ -z "$host" ]; then
  echo "Usage: $0 <recipe.json|-> <device-address>   (or set TOQUE_HOST)" >&2
  echo "The address is shown on the device under Settings > Phone (web interface)." >&2
  exit 2
fi
case "$host" in http://*) base="$host" ;; *:*) base="http://$host" ;; *) base="http://$host:8080" ;; esac
if [ "$file" = "-" ]; then data="@-"; else [ -f "$file" ] || { echo "File not found: $file" >&2; exit 2; }; data="@$file"; fi
out=$(curl -sS --max-time 60 -w '\n%{http_code}' -X POST -H 'X-Toque: 1' -H 'Content-Type: application/json' \
  --data-binary "$data" "$base/api/import")
code=$(printf '%s\n' "$out" | tail -n 1)
body=$(printf '%s\n' "$out" | sed '$d')
printf '%s\n' "$body"
case "$code" in
  200) echo "OK: saved on the device ($base)." ;;
  409) echo "The device is busy with a program. Try again when it is finished." >&2; exit 1 ;;
  *) echo "Not saved (HTTP $code)." >&2; exit 1 ;;
esac
