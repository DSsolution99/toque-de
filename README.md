# toque-de

🇩🇪 **[Deutsche Anleitung (Mac): Installation, Funktionen, Handy](docs/INSTALLATION.de.md)** ·
🇫🇷 [Version française (original project)](README.fr.md)

**Toque** is a fast, modern replacement interface for the Lidl **Monsieur Cuisine Connect**
kitchen robot (SKMC 1200). It runs on the robot's own tablet instead of the stock app and uses
**the official Lidl recipes** from monsieur-cuisine.com.

**toque-de** is an independent version of Toque, based on
[Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect) (the Toque app) and
[flexbox/MrCuisineVibeConnect](https://github.com/flexbox/MrCuisineVibeConnect) (the macOS install
tools). It adds a German interface, German recipes, a phone web app and more (see below), and it
updates from **this repository's releases**.

> Independent project, not affiliated with Lidl, SilverCrest or Hoyer. Motor control is based on
> a public analysis of the serial protocol; the original Toque has been running daily on a robot
> (SKMC 1200, Android 6) since October 2026 and on a second one since November 2026, but has not
> been tried on others. Use at your own risk and follow the first-run procedure.

## What's new in toque-de

Compared with the original Toque:

- **German interface.** Every screen is translated (`app/src/main/assets/lang/de.json`), next to
  English and French; *Settings › Langue · Language*, or automatic from the Android language.
- **German recipes, and many more of them.** The recipe language follows the interface: in German
  Toque downloads the German Lidl recipes. It now takes the recipes of **every Monsieur Cuisine
  model and the community recipes**, not only the official Connect ones: about **2,700 German
  recipes instead of about 1,000**, with **no duplicates**. When the same recipe exists several
  times (one per model, several community versions, title words in another order), one is kept:
  official before community, Connect before other models, then the most rated. Recipes made for
  another model are marked "check every setting"; steps for programs the Connect does not have are
  shown as "do it by hand"; text-only recipes of older models become readable steps.
- **Whisk detection in German recipes.** The whisk is called *Rühraufsatz* in German recipes; the
  original only recognised the French *fouet*. With the whisk, Toque limits the speed to 4 and
  refuses Turbo, as intended.
- **Nutrition, filters and sorting.** Calories, carbohydrates, fat and protein on every recipe.
  Filters: all categories (a "Vegan" recipe is found even when Vegan is not its first category),
  favourites, 30 min max, and calories per serving in steps (200 / 300 / 400 / 500 / 600 / 800
  kcal; recipes given per piece or per jar are not counted). Sorting: relevance, popular, newest,
  top rated, fewest calories. Recipes from the last 14 days are marked "New".
- **Servings.** Change the number of servings on a recipe: ingredient amounts are recalculated
  (times, weighing and machine settings stay those of the original amount, with a notice).
- **Shopping list.** Add a recipe's ingredients (at the chosen servings) with one tap. The same
  ingredient from several recipes is added up (same unit), grouped by aisle, ticked off by touch.
- **Phone web app.** From any phone on the same Wi-Fi, open the address shown in *Settings ›
  Phone (web interface)* (for example `http://192.168.178.42:8080`): search and filter recipes,
  read them with servings and nutrition, **open a recipe on the robot's screen**, manage favourites
  and the shopping list, and see the running program (time left, temperature). Nothing can be
  started from the phone, and nothing opens while a program runs. Home network only.
- **Your own recipes, written by an assistant.** The phone page has a **+** tab: copy the
  instructions, send them with any recipe (text, link, cookbook photo, an idea) to an assistant
  like Claude, and paste the answer back. The robot only checks and stores the finished recipe
  (format `toque-recettes-1`, guide in [`app/src/main/assets/web/prompt.txt`](app/src/main/assets/web/prompt.txt)),
  so it costs almost nothing on the device. From a computer: `tools/send-recipe.sh recipe.json <robot IP>`.
  Own recipes get a *My recipes* filter, can be deleted from the phone, and are never removed by
  the Lidl sync. Every imported machine setting must be confirmed on the robot before it runs;
  settings outside the machine limits are flagged and stay blocked. Imports are refused while a
  program runs.
- **Own releases.** Updates (*Settings › Updates*) and the install script use the releases of
  [DSsolution99/toque-de](https://github.com/DSsolution99/toque-de/releases). The APK is signed
  with the same public project key as the original, so it installs over an existing Toque.

## Install

**English, step by step: [docs/INSTALLATION.md](docs/INSTALLATION.md)** ·
**Deutsch: [docs/INSTALLATION.de.md](docs/INSTALLATION.de.md)**. Both are for a Mac. On Windows,
follow the [original repository's guide](https://github.com/Wuki60/MrCuisineVibeConnect/blob/main/docs/INSTALLATION.md)
and install the APK from this repository's releases.

In short: you need a Mac, a **USB-A male ↔ USB-A male** cable (plus a USB-C → USB-A adapter on a
Mac with only USB-C ports), a small Torx screwdriver and 1 to 2 hours. You prepare the Mac
(Homebrew, Python, libusb, then a copy of this repository), then either let an AI agent such as
[Claude Code](https://code.claude.com/docs/en/quickstart) do the install by following
[AGENTS.md](AGENTS.md), or run [`tools/setup/toque-setup.sh`](tools/setup/toque-setup.sh) yourself,
one step at a time. It backs up the robot first, roots it with Magisk (only the robot: nothing is
rooted or changed on the Mac), installs Toque and sets up ADB over Wi-Fi. You only handle the
cover, the cable, the power switch and a tap on the robot's screen.

## Features

- **Dark interface** designed for the robot's 7" screen and rotary knob, and smooth on its
  original tablet (1 GB RAM, Android 6): no heavy libraries, downsampled images, recycled views.
- **Lidl recipes, straight from monsieur-cuisine.com**, in the interface language, with each
  step's machine settings, downloaded from the site's public API nightly or on demand: no account,
  no server to run, no need for the stock app. Requests are spaced out and only new or changed
  recipes are downloaded again. The recipes already in the stock app can still be imported instead.
- **Author, ratings and reviews**: each recipe shows who published it and its rating on the site,
  and the recipe page loads the users' comments (with Monsieur Cuisine's replies).
- **Search that works**: ignores accents, plurals and typos ("bechamle", "risoto"), and common
  French and German filler words. Search by ingredients (e.g. "chicken, curry"), filters, favourites.
- **Guided cooking**: each step starts with one tap, the next step shows up by itself, built-in
  weighing, resume after a power cut. Settings that follow each other with nothing to do in
  between (rinsing, blending at several speeds…) start together with a single tap: the blade goes
  straight from one speed and temperature to the next without stopping.
- **Manual cooking**, **Knead / Steam / Sauté** programs, **Turbo**, **scale**.
- **Offline**: everything works without a network (no more "server unreachable" at startup).
- **Always the right time**: the time zone comes from the Internet connection and the time from
  the network (the robot has no clock battery); can be set by hand in *Settings › Date and time*.
- **Made for a robot without Android buttons**: Toque opens at startup, and a **Back to Toque**
  button stays on top of the stock app when you open it.
- **Updates from GitHub**: *Settings › Updates* checks this repository's
  [Releases](https://github.com/DSsolution99/toque-de/releases) and installs the new version
  (silently on a rooted robot, then Toque restarts). Refused while a program is running.

## Network and privacy

Toque only goes online when you ask for it, with one exception (the time):

| Connection | When | To |
|---|---|---|
| Recipe sync, photos | *Settings › Lidl recipes › Sync*, or nightly only if *Automatic sync* is on | mc-api.tecpal.com, Lidl photo servers |
| Reviews of a recipe | Only after tapping *Load reviews* | mc-api.tecpal.com |
| Update check and download | Only after tapping *Check for an update* | api.github.com, github.com |
| Time zone | **Once**, if set to *automatic*; then it is kept (choose *automatic* again to re-detect) | ipwho.is, geojs.io or ipapi.co (HTTPS) |
| Clock | Android's own network time (NTP); the robot has no clock battery | Android's time server |

Inbound, from the home network only:

- **Phone web app** (port 8080, can be turned off): only answers when the address is the robot's own
  (IP or a local name such as `….fritz.box`), which blocks DNS-rebinding attacks from web pages;
  changes need the `X-Toque` header and a matching origin; nothing can start the motor or heater.
- **ADB over Wi-Fi** (port 5555): protected by key (only computers whose key was accepted on the
  robot), and can be switched off in *Settings › Remote maintenance (ADB)*.

On Android 6 the Let's Encrypt roots (ISRG Root X1/X2) are added to the system ones so that
GitHub downloads work; certificates are still fully checked.

## Safety: what changes compared to the stock app

All original limits are kept (speed 3 max when heating and in reverse, maximum durations,
Turbo conditions…). Several flaws found while analysing the protocol are fixed:

| Stock app flaw | Toque |
|---|---|
| Start was requested before checking the lid | Lid, errors and data freshness are checked **before** any start |
| No detection of a silent motor board | No reply for 1.5 s: everything stops and an alert is shown |
| Serial write errors ignored | Stop and alert |
| A STOP followed by a START could never send the stop | Any restart or direction change waits for a sample confirming **motor stopped** |
| Serial reading without resynchronisation | Fragmented, merged or corrupted frames are handled |
| The blade sometimes turned in steam mode | Steam at speed 0 |

Also added: confirmation (and a gradual ramp-up) before blending contents at 60 °C or more, a
speed limit with the whisk, alerts that stay latched until acknowledged, nothing restarts on its
own after a pause or an alert. When cold, the blade goes straight to the requested speed. Motor control runs in **its own process**: if the interface crashes, a cooking run is not
cut off and finishes normally.

## Project layout

```
core/   Pure Kotlin logic, tested without Android
        protocol/  serial frames (15 bytes sent, 27 received, 4800 baud)
        machine/   safety rules, controller, 100 ms loop, links (root serial, TCP, demo)
        recipes/   model, Lidl API import (all models, no duplicates), stock database import,
                   search, filters and sorting, servings (Quantity), shopping list, phone data
app/    Android app (minSdk 23), no AndroidX
        machine/   motor control service (":machine" process) and IPC
        recipes/   local catalogue, Lidl sync, shopping list storage
        ui/        screens
        web/       phone web app server (port 8080, home network only)
        assets/    lang/ (fr in the code, en.json, de.json), web/ (phone page: HTML, CSS, JS)
tools/  setup/toque-setup.sh (full install from a Mac, can be driven by an AI agent), robot/
        (scripts run on the robot: root, flash from TWRP, restore), install, restore and release
        scripts (.sh for macOS, the original .ps1 for Windows), twin.py (digital twin for testing)
docs/   INSTALLATION.md, INSTALLATION.de.md, INSTALLATION.fr.md
AGENTS.md  install procedure for an AI agent
```

## Build

Open the folder in **Android Studio**, wait for the Gradle sync, then
**Build › Build App Bundle(s) / APK(s) › Build APK(s)**. The APK is in
`app/build/outputs/apk/debug/`.

Without a local JDK, push a branch other than `main`: the [check workflow](.github/workflows/check.yml)
runs the tests and builds the app on GitHub, without publishing anything.

From the command line: `./gradlew assembleDebug` (and `./gradlew :core:test` for the tests). Gradle
needs JDK 17 or 21; with a newer default JDK the build stops with just a version number as error.
Android Studio's JDK works:
`export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"`.

## Try it without the robot

1. **Demo mode** (default): install the APK on any Android device (or the Android Studio
   emulator, in landscape). The motor board is simulated; *Settings › Tools › Motor base
   diagnostics* lets you open the lid or trigger an error to see the safety
   features. *Settings › Lidl recipes › Sync* downloads the recipes on any device too.
2. **Digital twin**: the twin from the [Monsieur Cuisine Developer SDK](https://github.com/antrea/monsieur-cuisine-developer-sdk)
   reproduces the real protocol independently of Toque.
   Run `python3 tools/twin.py --sdk <SDK folder>`, then in Toque choose *Settings › Machine ›
   Twin (PC)* (emulator: host `10.0.2.2`; real device: `adb reverse tcp:8765 tcp:8765` and host
   `127.0.0.1`). `python3 tools/twin.py --send "lid 1"` opens the lid, etc.

## Add a language

The app is written in French; each other language is one file in
[`app/src/main/assets/lang/`](app/src/main/assets/lang) (`en.json` for English, `de.json` for German).
The phone web app uses the same files (its texts are listed in `app/.../web/WebTexts.kt`).

1. Copy `en.json` to `<code>.json` (for example `it.json`), set `"_name"` (for example `"Italiano"`)
   and translate the values. Keys are the French texts; keep the `%s` / `%d` placeholders.
   Missing texts stay in French.
2. Rebuild the app, or, without rebuilding, copy the file to `Android/data/app.toque/files/lang/`
   on the device and restart Toque.

The language then appears in *Settings › Langue · Language*. `./gradlew :core:test` checks
every language file.

## Publish a new version

1. Increase `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit and push to `main`.

The [release workflow](.github/workflows/release.yml) runs the tests, builds the APK and publishes
the `vX.Y.Z` release with it. Robots then find it in *Settings › Updates*.

All builds are signed with the project key in `app/signature/` (public on purpose), so an APK built
on any PC or by GitHub installs over the previous version. Only install APKs from this repository's
releases.

Without GitHub Actions: `./gradlew assembleRelease`, then
`bash tools/publish-release.sh --apk app/build/outputs/apk/release/app-release.apk [--repo owner/name]`
creates the `vX.Y.Z` release and attaches the APK (needs `gh`; GitHub token: `GH_TOKEN`, `gh auth login`,
or the credentials already stored for git; `--dry-run` only shows what it would do).

## Install on the robot

**[docs/INSTALLATION.md](docs/INSTALLATION.md)** (see [Install](#install)): preparing the Mac, root,
install with Claude Code or by hand, first run with an empty bowl, recipe sync, going back to the
stock app. From Windows, use the original repository's guide.

## Credits

- Protocol: [Monsieur Cuisine Developer SDK](https://github.com/antrea/monsieur-cuisine-developer-sdk) (MIT).
- Robot access: [Monsieur-Cuisine-Connect-Hack](https://github.com/EliasKotlyar/Monsieur-Cuisine-Connect-Hack).
- Toque app: [Wuki60/MrCuisineVibeConnect](https://github.com/Wuki60/MrCuisineVibeConnect); macOS install tools: [flexbox/MrCuisineVibeConnect](https://github.com/flexbox/MrCuisineVibeConnect).
- MediaTek download mode on macOS: [mtkclient](https://github.com/bkerler/mtkclient) (GPLv3, downloaded by `prepare`, not included; `prepare` applies the macOS serial fixes in `tools/setup/mtkclient-macos-serial.patch`).

MIT License. This repository contains no Lidl recipes, images or code: each robot downloads the
recipes itself from monsieur-cuisine.com, for its owner's personal use.
