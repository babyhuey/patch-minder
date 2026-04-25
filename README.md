# Patch Minder

A minimal Android app for tracking weekly patch replacements (estrogen,
nicotine, scopolamine — anything on a 7-day rotation). Built for one user,
one device, no cloud, no accounts.

## What it does

- Tracks four rotation slots (Left/Right × Upper/Lower thigh).
- Reminds you on the day each patch is due, at the time you choose.
- Suggests which slots to use next based on which were used least recently.
- Shows status at a glance via a home-screen widget.

## Features

- **Tap to select** a slot, then `REPLACED N PATCHES` to mark new patches placed.
- **Long-press a slot** to remove a single patch (e.g. when the new one didn't
  go on the same spot). Two-stage confirm with auto-cancel after 3 seconds.
- **Reset all patches** in settings (two-tap confirm).
- **Configurable** patches-per-change (1–4) and reminder time of day.
- **Home-screen widget** showing next-due slot and time remaining.
- **Exact alarm scheduling** via `AlarmManager`, surviving reboot.

## Build

Requires Docker. The project ships a `Dockerfile` with the Android SDK, so no
local SDK install is needed.

```bash
# Run unit tests
./docker-build.sh ./gradlew testDebugUnitTest

# Build a debug APK
./docker-build.sh ./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Install on device

Easiest path on a phone without a developer USB connection:

```bash
# From the project root, on the host:
cd app/build/outputs/apk/debug
python3 -m http.server 8000
```

Then on the phone, browse to `http://<host-lan-ip>:8000/app-debug.apk` and
install (you'll need to allow installs from your browser the first time).

## Project layout

```
app/src/main/java/com/patch/notifier/
├── MainActivity.kt          # Entry point + Compose root
├── PatchApp.kt              # Application class
├── alarm/                   # AlarmManager scheduling + receiver
├── boot/                    # BOOT_COMPLETED → reschedule alarms
├── data/                    # Room DB, Patch entity, preferences
├── ui/                      # Compose UI (PatchScreen, theme)
└── widget/                  # Home-screen widget (RemoteViews)
```

Specs and implementation plans live in `docs/superpowers/`.

## Testing

Unit tests cover rotation logic, alarm scheduling math, widget state, and
display formatting. They run on the JVM (no emulator required) and are
exercised in CI via the same Docker image used for local builds.

```bash
./docker-build.sh ./gradlew testDebugUnitTest
```

## License

MIT — see [LICENSE](LICENSE).
