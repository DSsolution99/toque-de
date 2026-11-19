# Toque auf dem Monsieur Cuisine Connect installieren – auf Deutsch, vom Mac aus

Diese Anleitung geht von einem ganz normalen Mac (Apple-Chip oder Intel) und einem
Monsieur Cuisine Connect im Originalzustand aus. Am Ende läuft Toque mit **deutscher
Oberfläche** und **deutschen Lidl-Rezepten** (rund 2.700 Rezepte von monsieur-cuisine.com, ohne Doppel).

**Einmal ganz durchlesen, bevor du anfängst.** Dauer: 1 bis 2 Stunden, meistens Warten.

## Was am Mac passiert – und was nicht

- Am Mac wird **nichts gerootet**, keine Sicherheitseinstellung geändert und kein Treiber installiert.
- Installiert werden nur normale Programme: **Homebrew**, **Python** und **libusb**. Das Skript lädt alles Weitere in den Ordner `~/Toque` und prüft jede Datei (SHA-256).
- Gerootet wird nur **der Monsieur Cuisine**. Ohne Root kann Toque den Motor nicht steuern,
  das geht leider nicht anders. Vorher wird eine **Sicherung** gemacht, damit du jederzeit zurückkannst.

## 1. Bevor du anfängst

### Dein Gerät

- Auf dem Typenschild muss **SKMC 1200** (Monsieur Cuisine **Connect**) stehen.
- Bei einem Monsieur Cuisine **Smart** (SKMS 1200) oder einem anderen Modell: **hier aufhören**.

### Einkaufsliste

| | Was | Hinweis |
|---|---|---|
| ☐ | **USB-A-Stecker ↔ USB-A-Stecker-Kabel** (Datenkabel) | Beide Enden der große rechteckige USB-Stecker. Ein Kabel USB-C ↔ USB-A funktioniert **nicht**. Ca. 5–10 € |
| ☐ | **Adapter USB-C (Stecker) → USB-A (Buchse)** | Nur nötig, wenn der Mac keine USB-A-Buchse hat (z. B. neuere MacBooks). Ein USB-Hub mit USB-A geht auch |
| ☐ | **Kleiner Torx-Schraubendreher** | Für die Wartungsklappe unter dem Gerät |
| ☐ | **WLAN-Passwort** | Für den Schluss |
| ☐ | Gerät **neben dem Mac**, am Strom, **Mixbehälter leer** | Bleibt die ganze Zeit am Strom |

### Risiken

- Die Garantie des Geräts erlischt.
- Ein Fehler kann das Tablet im Gerät unbrauchbar machen. Das Skript prüft vor jedem Schreiben,
  und die **Sicherung** am Anfang ist dein Weg zurück: nie überspringen, eine Kopie aufheben.
- Toque läuft bisher täglich auf zwei Geräten, eins davon wurde am Mac (Apple silicon) installiert.
  Das ist nicht viel: auf eigenes Risiko.

### Die 3 Regeln

Unter dem Gerät, hinter der Klappe, sind zwei USB-Anschlüsse:

<img src="https://raw.githubusercontent.com/EliasKotlyar/Monsieur-Cuisine-Connect-Hack/master/informations/usbports.jpg" alt="Die zwei USB-Anschlüsse unter der Klappe: links „Power PCB USB“, rechts „Android USB“" width="360">

1. **Immer nur der rechte Anschluss „Android USB“.** Nie „Power PCB USB“: Der kann die Platine
   zerstören.
2. **Gerät mit abgezogenem Kabel einschalten**, Kabel erst einstecken, wenn der Bildschirm an ist.
   Mit eingestecktem Kabel bleibt es auf einem Akku-Bild hängen: aus, Kabel ab, an, Kabel rein.
   **Einzige Ausnahme**: die Schritte `backup` und `flash-twrp`. Da steckt das Kabel bei
   ausgeschaltetem Gerät, und du schaltest es ein, wenn der Mac darauf wartet.
3. **Während ein Schritt läuft, nie ausschalten oder das Kabel ziehen.** Warte auf die Zeile
   `OK: ... done`.

Der Ein/Aus-Schalter ist hinten am Gerät.

## 2. Den Mac vorbereiten (einmalig, ca. 10 Minuten)

Alles passiert im **Terminal**: `Cmd + Leertaste`, `Terminal` tippen, `Return`.
Jeden Befehl kopieren, einfügen (`Cmd + V`), `Return`, warten, bis der Cursor wieder da ist.

1. **Homebrew installieren**, falls noch nicht vorhanden (`brew --version` zeigt eine Nummer, wenn
   es schon da ist):
   ```bash
   /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
   ```
   Es fragt nach dem Passwort des Macs (beim Tippen ist nichts zu sehen, das ist normal). Zeigt es am
   Ende **Next steps** mit Befehlen, diese ausführen und das Terminal neu öffnen.
2. **Python und libusb installieren**:
   ```bash
   brew install python libusb
   ```
3. **Dieses Projekt herunterladen** (in den Ordner `toque-de` in deinem Benutzerordner):
   ```bash
   git clone https://github.com/DSsolution99/toque-de.git ~/toque-de
   ```
4. **In den Projektordner wechseln**:
   ```bash
   cd ~/toque-de
   ```
   Jedes Mal, wenn du das Terminal neu öffnest, zuerst diesen Befehl.

Dann wählen: **3a** (Claude tippt die Befehle für dich, empfohlen) oder **3b** (du tippst selbst).

## 3a. Mit Claude installieren (empfohlen)

Öffne in der Claude-App eine Sitzung im Ordner `toque-de` und schreibe:

> Installiere Toque auf meinem Monsieur Cuisine Connect nach AGENTS.md. Sprich Deutsch mit mir.

- Claude erklärt die Risiken und fragt nach einem **Ja**, bevor es anfängt.
- Vor jedem Schritt, der auf das Gerät schreibt, sagt es, was passiert, und wartet auf dein **Ja**.
- Wenn deine Hände gebraucht werden (Klappe, Kabel, Schalter, Tippen auf dem Bildschirm), sagt es
  dir Bescheid: machen, dann „fertig“ antworten.

Lass diese Anleitung offen: Teil 3b zeigt, was bei jedem Schritt passiert.

## 3b. Selbst installieren, Schritt für Schritt

Ein Skript macht alles: `bash tools/setup/toque-setup.sh <schritt>`. Es gibt drei Arten von Zeilen aus:

- `OK:` erledigt und geprüft;
- `HUMAN:` du bist dran (Kabel, Schalter, Bildschirm). Machen; wenn der Schritt angehalten hat,
  denselben Befehl noch mal ausführen;
- `STOP:` ein Problem, **es wurde nichts Unumkehrbares gemacht**. Die Zeile sagt, was zu tun ist
  (siehe auch [Wenn etwas schiefgeht](#5-wenn-etwas-schiefgeht)).

Wenn du nicht weißt, wo du stehst:

```bash
bash tools/setup/toque-setup.sh status
```

Jeder Schritt kann gefahrlos wiederholt werden.

### Schritt 1 · Werkzeuge laden – `prepare`

Das Gerät ist noch nicht beteiligt.

```bash
bash tools/setup/toque-setup.sh prepare
```

Lädt adb, Magisk, TWRP und mtkclient nach `~/Toque` und prüft alles (die neueste Toque-Version
kommt in Schritt 6, `android`).
Steht da `MISSING:`, installiere das Genannte und führe den Schritt noch mal aus.

✅ Am Ende: `OK: prepare done. Next: backup`

### Schritt 2 · Gerät sichern – `backup`

Es wird nur gelesen, nichts geschrieben.

1. **Gerät ausschalten** (Schalter hinten).
2. Mixbehälter abnehmen, Gerät vorsichtig kippen und die **Wartungsklappe** unten abschrauben.
3. Kabel in den rechten Anschluss **„Android USB“** stecken, andere Seite über den Adapter in den Mac.
4. Starten:
   ```bash
   bash tools/setup/toque-setup.sh backup
   ```
5. Sobald im Terminal *Waiting for PreLoader VCOM* steht (danach alle 10 Sekunden *Handshake
   failed, retrying...*, das ist normal): **Gerät einschalten**, einmal. Der Bildschirm bleibt
   schwarz, auch das ist normal.
6. Fragt der Mac, ob er das **Zubehör verbinden** darf: **Erlauben** klicken, dann das Gerät aus-
   und wieder einschalten, während der Schritt noch wartet.

✅ Am Ende: `OK: backup done. Next: flash-twrp`

Im Ordner `~/Toque/backup` liegen jetzt deine zwei Sicherungen. **Kopiere den Ordner auf einen
USB-Stick oder in die Cloud**: Das ist dein einziger Weg zurück. Danach das **Gerät ausschalten**
(schwarzer Bildschirm: es ist noch im Download-Modus).

### Schritt 3 · TWRP aufspielen – `flash-twrp`

Schreibt auf das Gerät: TWRP, ein Wartungssystem, ersetzt vorübergehend den normalen Start.

1. Gerät **aus**, Kabel **steckt**.
2. Starten:
   ```bash
   bash tools/setup/toque-setup.sh flash-twrp
   ```
3. Wie bei der Sicherung: Gerät einschalten, wenn das Terminal auf das Gerät wartet.

✅ Am Ende: `OK: flash-twrp done`

4. Gerät **aus**, Kabel **ab**, Gerät **an**: Der **TWRP**-Bildschirm erscheint. Kabel wieder **rein**.

### Schritt 4 · Root vorbereiten – `root`

Gerät zeigt TWRP, Kabel steckt.

```bash
bash tools/setup/toque-setup.sh root
```

Baut aus deiner Sicherung die „gerootete“ Startdatei (mit Magisk) und schaltet die Verbindung
zum Mac in Android ein.

✅ Am Ende: `OK: Magisk boot image ready ... Next: flash-root`

### Schritt 5 · Root schreiben – `flash-root`

Schreibt auf das Gerät. Weiter in TWRP, Kabel steckt.

```bash
bash tools/setup/toque-setup.sh flash-root
```

**Gerät nicht anfassen**, bis der Schritt fertig ist. Das Skript prüft vorher, was auf dem Gerät
ist (und schreibt nichts, wenn es das nicht erkennt), schreibt und liest zur Kontrolle zurück.

✅ Am Ende: `OK: flash-root done. Next: android`

Dann Gerät **aus**, Kabel **ab**, Gerät **an**: Die normale Monsieur-Cuisine-App startet.
Kabel wieder **rein**.

### Schritt 6 · Root prüfen – `android`

```bash
bash tools/setup/toque-setup.sh android
```

- Fragt das Gerät nach **USB-Debugging erlauben**: *Immer erlauben* ankreuzen, **OK**, Schritt
  noch mal ausführen.
- Fragt Magisk nach Superuser-Rechten für *Shell*: **innerhalb von 10 Sekunden** auf **Grant**
  (Gewähren) tippen. Verpasst? Schritt noch mal ausführen.

✅ Am Ende: `OK: Root works (uid 0). Next: install`

### Schritt 7 · Toque installieren – `install`

```bash
bash tools/setup/toque-setup.sh install
```

✅ Am Ende: `OK: Toque installed`, und Toque öffnet sich auf dem Gerät. Installiert wird die
Version aus diesem Projekt (Releases von `DSsolution99/toque-de`): Deutsch ist eingebaut.

### Schritt 8 · Sprache prüfen

Ist Android auf dem Gerät auf Deutsch eingestellt, ist Toque schon deutsch. Sonst auf dem Gerät:
**Réglages** (Zahnrad) › **Langue · Language** › **Deutsch**.

### Schritt 9 · Erster Test – Mixbehälter LEER

1. In Toque: **Einstellungen › Gerät › Echtes Gerät › Übernehmen**. Toque trennt die Original-App
   vom Motor (umkehrbar) und öffnet die serielle Schnittstelle.
2. Oben rechts muss **„Bereit“** stehen. Wenn nicht: Einstellungen › Werkzeuge › Diagnose der
   Motorbasis, und das Protokoll.
3. Mixbehälter leer, Deckel zu, **Hand in der Nähe des Schalters**:
   - Manuell kochen: 10 s, Stufe 2 → der Motor dreht und stoppt von selbst.
   - Während eines Tests den Deckel öffnen → sofortiger Stopp, „Pause: Deckel offen“.
   - 30 s bei 37 °C, Stufe 1 → die angezeigte Temperatur steigt.
   - Waage: etwas mit bekanntem Gewicht drauflegen, Tara.
   - Diagnose: Mixbehälter abnehmen und „Mixbehälter erkannt“ beobachten. Wenn sich das richtig
     ändert, kannst du Einstellungen › Sicherheit › „Mixbehälter-Erkennung verlangen“ einschalten.
4. Beim kleinsten komischen Verhalten: **Hauptschalter aus**, dann zurück zur Original-App
   ([Teil 6](#6-rückgängig-machen)).

### Schritt 10 · Ohne Kabel – `wifi`

Das Gerät muss im WLAN sein. Wenn nicht: in Toque **5-mal schnell auf die Uhr** oben rechts tippen
(Android-Einstellungen), WLAN verbinden, mit der Schaltfläche **„Zurück zu Toque“** zurück.

```bash
bash tools/setup/toque-setup.sh wifi
```

✅ Am Ende: `OK: ADB over Wi-Fi: <IP-Adresse>:5555`. Kabel ab, **Klappe wieder anschrauben**. Fertig.

## 4. Deutsche Rezepte laden

**Einstellungen › Lidl-Rezepte › Synchronisieren** (Gerät im WLAN). Toque lädt die deutschen
Rezepte von monsieur-cuisine.com, ohne Konto: danach nur neue, jede Nacht um 3:30 Uhr.

Diese Version lädt rund **2.700 Rezepte statt gut 1.000** (die offizielle Toque-Version), ohne Doppel:

- alle offiziellen Rezepte für den Connect, dazu die Community-Rezepte;
- Rezepte für den Monsieur Cuisine Smart und ältere Modelle, wenn es sie nicht auch für den
  Connect gibt. Sie tragen den Hinweis „Rezept für ein anderes Monsieur-Cuisine-Modell“: jede
  Einstellung vor dem Start prüfen. Programme, die der Connect nicht hat (z. B. Fermentieren),
  erscheinen als „bitte von Hand machen“;
- Rezepte des Monsieur Cuisine Plus sind reiner Text (z. B. „6 Sekunden/Stufe 10“): die
  Einstellungen von Hand vornehmen;
- gibt es ein Rezept mehrfach (mehrere Modelle, mehrere Community-Versionen, Titel mit anderer
  Wortreihenfolge), bleibt nur eins: offiziell vor Community, Connect vor anderen Modellen, sonst
  das mit den meisten Bewertungen.

Die erste Synchronisierung dauert damit eher 30 bis 60 Minuten, und die Fotos brauchen etwa
200 MB mehr Speicher.

Die Rezeptsprache folgt der Sprache der Oberfläche. Stellst du später wieder um, wird beim
nächsten Synchronisieren alles in der neuen Sprache neu geladen.

**Rühraufsatz:** Diese Version erkennt den „Rühraufsatz“ in deutschen Rezepten und begrenzt dann
wie vorgesehen auf höchstens Stufe 4, ohne Turbo.

### Rezepte finden, Portionen, Einkaufsliste

- **Filter und Sortierung** in der Rezeptliste: die erste Taste wechselt die Reihenfolge
  (Relevanz, Beliebt, Neu, Am besten bewertet, Wenigste kcal); dazu Favoriten, „30 min max“,
  **Kalorien** in Stufen (max. 200 / 300 / 400 / 500 / 600 / 800 kcal pro Portion – jedes Antippen
  nimmt die nächste Stufe; Rezepte, die pro Stück oder Glas angegeben sind, zählen hier nicht mit)
  und alle Kategorien – „Vegan“ oder „Vegetarisch“ finden jetzt alle passenden Rezepte, auch wenn
  das nicht die erste Kategorie ist. Neue Rezepte (letzte 14 Tage) sind mit „Neu“ markiert.
- **Nährwerte** (kcal, Kohlenhydrate, Fett, Eiweiß) stehen auf der Rezeptseite.
- **Portionen:** mit − und + neben „Zutaten“. Die Mengen werden umgerechnet; Zeiten,
  Wiegeschritte und Geräteeinstellungen gelten weiter für die Originalmenge (Hinweis in Gelb).
- **Einkaufsliste:** Taste **+** auf der Rezeptseite. Gleiche Zutaten aus mehreren Rezepten
  werden zusammengezählt (bei gleicher Einheit) und nach Warengruppe sortiert. Die Liste ist auf
  der Startseite und am Handy erreichbar; antippen = abgehakt.

### Am Handy

Das Handy muss im selben WLAN wie das Gerät sein. In **Einstellungen › Handy (Weboberfläche)**
steht die Adresse, z. B. `http://192.168.178.42:8080`: im Handy-Browser öffnen (Tipp: „Zum
Home-Bildschirm“ hinzufügen, dann wirkt sie wie eine App).

- Rezepte suchen und ansehen, mit Filtern, Portionen und Nährwerten.
- **„Am Gerät öffnen“** zeigt das Rezept auf dem Bildschirm des Monsieur Cuisine. Gestartet wird
  immer am Gerät; während ein Programm läuft, öffnet sich nichts.
- Favoriten und Einkaufsliste – dieselben wie auf dem Gerät.
- Oben eine Statuszeile, solange ein Programm läuft (Restzeit, Temperatur).

Die Weboberfläche ist nur im Heimnetz erreichbar und kann in den Einstellungen ausgeschaltet
werden. Sie kann nichts starten, nur anzeigen, Rezepte öffnen und die Liste ändern.

### Eigene Rezepte mit Claude

Jedes Rezept (Link, Text, Foto aus dem Kochbuch, Thermomix-Rezept oder nur eine Idee) lässt sich
ins Gerät holen. Die Umwandlung macht ein Assistent wie Claude, das Gerät prüft und speichert nur.

**Am Handy:** Reiter **+** öffnen › **Anleitung kopieren** › in der Claude-App einfügen und dein
Rezept dazugeben › Claudes Antwort kopieren › im Feld einfügen › **Prüfen und speichern**.

**Am Mac mit Claude:** z. B. „Hol mir dieses Rezept auf meinen Monsieur Cuisine: <Link>. Die
Adresse des Geräts ist 192.168.178.42.“ Claude schreibt das Rezept und schickt es mit
`tools/send-recipe.sh`.

- Eigene Rezepte findest du über den Filter **Meine Rezepte** (Gerät und Handy) und kannst sie am
  Handy löschen. Der Lidl-Abgleich lässt sie in Ruhe.
- Jede Geräte-Einstellung aus einem eigenen Rezept musst du am Gerät **bestätigen**, bevor sie
  läuft. Einstellungen außerhalb der Gerätegrenzen werden angezeigt und bleiben gesperrt.
- Während ein Programm läuft, nimmt das Gerät keine Rezepte an.

### Im Alltag

- Toque öffnet sich beim Einschalten von selbst.
- Das Gerät hat keine Home- oder Zurück-Taste: Wenn eine andere App offen ist, bleibt die
  Schaltfläche **„Zurück zu Toque“** oben (verschiebbar).
- Android-Einstellungen: 5-mal schnell auf die Uhr oben rechts tippen.
- Updates: **Einstellungen › Updates** (neue Versionen dieses Projekts).
- Steht auf dem Gerät noch die offizielle Toque-Version ohne Deutsch, spielt
  `bash tools/install-deutsch.sh` (per WLAN: `--ip <IP-Adresse des Geräts>`) nur die Sprachdatei auf.

## 5. Wenn etwas schiefgeht

| Was du siehst | Warum | Was tun |
|---|---|---|
| Gerät zeigt ein **Akku-Bild** statt zu starten | Mit eingestecktem Kabel eingeschaltet | Aus, Kabel ab, an, Kabel rein, wenn der Bildschirm an ist |
| `backup` / `flash-twrp` warten ewig (*Waiting for device*) | Falsches Kabel, falscher Anschluss, kein Adapter, oder Gerät war schon an | A-A-Kabel, „Android USB“, USB-C→USB-A-Adapter. Gerät aus, Schritt neu starten, einschalten, wenn gefragt |
| *Handshake failed, retrying...* alle 10 s | Normal: der Mac wartet auf das Gerät | Gerät einschalten, einmal |
| *Sync error* oder *Failed to upload da* | Gerät war schon im Download-Modus, oder `prepare` fehlt | `prepare`, Gerät aus, Schritt noch mal |
| Bildschirm schwarz nach `backup` oder `flash-twrp` | Gerät bleibt im Download-Modus | Ausschalten |
| Mac fragt, ob er das **Zubehör erlauben** soll | Sicherheit bei Apple-silicon-Macs | **Erlauben**, dann Gerät aus- und wieder einschalten |
| `MISSING: Python` oder `MISSING: libusb` | Pakete fehlen | `brew install python libusb` |
| Mac sieht das Gerät in TWRP nicht | Kabel steckte, bevor TWRP da war | Kabel ab und wieder rein |
| Akku-Bild zwischen `root` und `flash-root` | Gerät hat mit Kabel neu gestartet | Aus, Kabel ab, an: TWRP kommt wieder. Kabel rein, weitermachen |
| `flash-root`: *write failed* oder *read-back differs* | Schreibfehler | **Gerät nicht ausschalten** (TWRP läuft noch). `flash-root` noch mal |
| `flash-root`: *holds something unexpected* | Unerwarteter Inhalt auf dem Gerät | Es wurde nichts geschrieben. Nicht weitermachen: `status`, Hilfe holen |
| `android`: **Grant** verpasst | Magisk-Abfrage schließt nach 10 s | `android` noch mal und schnell tippen. Für immer abgelehnt: Magisk-App › Superuser › Shell › erlauben |
| Toque zeigt „Keine Basis“ | busybox von Magisk fehlt | `install` noch mal |
| Meldungen, dass Toque Superuser-Rechte verweigert wurden | Magisk-Einstellung | `install` noch mal |
| `install-deutsch.sh`: *Gerät nicht erreichbar* | Kabel oder Gerät aus | Kabel prüfen, oder `--ip` nach dem Schritt `wifi` |
| In der Original-App festgesteckt | Normal, das Gerät hat keine Zurück-Taste | Schaltfläche **„Zurück zu Toque“** |

Immer noch fest? Die Datei `~/Toque/setup-log.txt` (und bei `backup` / `flash-twrp`
`~/Toque/mtk-<schritt>-out.txt`) an jemanden schicken, der hilft.

**Wenn der Mac das Gerät im Download-Modus nie erreicht:** Direkt nach dem Einschalten muss
`system_profiler SPUSBDataType` ein Gerät **MT65xx Preloader** zeigen. Wenn nicht, liegt es an
Kabel, Anschluss oder Adapter. Notlösung: nur `backup` und `flash-twrp` an einem Windows-PC mit der
[Original-Anleitung](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/docs/INSTALLATION.md)
machen, die zwei Sicherungen nach `~/Toque/backup/` auf den Mac kopieren, `backup` ausführen (prüft
sie dann nur) und am Mac mit `root` weitermachen.

## 6. Rückgängig machen

- **Zurück zur Original-App** (Root bleibt): in Toque Einstellungen › Werkzeuge › „Zurück zur
  Original-App“, oder am Mac `bash tools/restore-robot.sh --uninstall`.
- **Auch Root und TWRP entfernen** (nach dem Punkt oben): `bash tools/setup/toque-setup.sh unroot`
  spielt deine Sicherungen `boot-original.img` und `recovery-original.img` zurück. Startet das Gerät
  gar nicht mehr: `unroot --download-mode` (Gerät aus, Kabel rein, wie bei `backup`).
