#!/usr/bin/env bash
# Spielt die deutsche Sprachdatei (de.json) auf den Monsieur Cuisine und startet Toque neu.
# Toque muss schon installiert sein. Die Datei liegt im App-Ordner auf dem Gerät und bleibt
# bei Toque-Updates erhalten.
#
#   bash tools/install-deutsch.sh                 # per USB-Kabel
#   bash tools/install-deutsch.sh --ip 192.168.1.50   # per WLAN (nach dem Schritt "wifi")
set -euo pipefail

here="$(cd "$(dirname "$0")/.." && pwd)"
file="$here/app/src/main/assets/lang/de.json"
adb="${ADB:-$HOME/Toque/platform-tools/adb}"
dir="/sdcard/Android/data/app.toque/files/lang"
ip=""

while [ $# -gt 0 ]; do
  case "$1" in
    --ip) ip="${2:?--ip braucht eine Adresse}"; shift 2 ;;
    *) echo "Unbekannte Option: $1" >&2; exit 1 ;;
  esac
done

[ -x "$adb" ] || { echo "STOP: adb nicht gefunden ($adb). Zuerst: bash tools/setup/toque-setup.sh prepare" >&2; exit 2; }
[ -f "$file" ] || { echo "STOP: $file fehlt" >&2; exit 2; }

if [ -n "$ip" ]; then
  "$adb" connect "$ip:5555" >/dev/null
  export ANDROID_SERIAL="$ip:5555"
fi

"$adb" get-state >/dev/null 2>&1 || { echo "STOP: Gerät nicht erreichbar (Kabel steckt? Gerät an?)" >&2; exit 2; }
"$adb" shell pm path app.toque 2>/dev/null | grep -q package: || { echo "STOP: Toque ist noch nicht installiert (Schritt install)" >&2; exit 2; }

"$adb" shell mkdir -p "$dir"
"$adb" push "$file" "$dir/de.json" >/dev/null
"$adb" shell ls -l "$dir/de.json" | grep -q de.json || { echo "STOP: Kopieren fehlgeschlagen" >&2; exit 2; }
echo "OK: de.json kopiert"

echo "Toque wird neu gestartet (nur machen, wenn gerade kein Programm läuft)…"
"$adb" shell am force-stop app.toque
"$adb" shell am start -n app.toque/.ui.MainActivity >/dev/null
echo "OK: fertig. Auf dem Gerät: Réglages › Langue · Language › Deutsch."
echo "    Danach: Einstellungen › Lidl-Rezepte › Synchronisieren (lädt die deutschen Rezepte)."
