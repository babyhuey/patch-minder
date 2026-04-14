# Patch Notifier — Session Notes (2026-04-13)

## What Was Built

Personal Android app for tracking estrogen patch replacement. Kotlin + Jetpack Compose, dark theme only, sideloaded via ADB/Nextcloud.

## Features

- **Single screen** — 4 thigh locations (Left/Right Upper/Lower) in a 2x2 grid
- **Auto-rotation** — suggests which 3 locations to use, rests 1 each cycle
- **Tap to toggle** — select/deselect locations, confirm with one button
- **7-day alarms** — exact alarms per patch, fires at preferred notification time
- **Nag mode** — repeats every 2 hours (up to 48 nags / 4 days) until confirmed
- **Boot recovery** — re-schedules alarms after phone reboot
- **1x1 home screen widget** — shows days until next replacement with color urgency
- **Settings** — adjustable patch count (1-4), notification time (hour stepper)
- **Reset** — two-tap reset in settings to clear all patches
- **Haptic feedback** on all tappable elements

## Tech Stack

- Kotlin, Jetpack Compose, Material 3, Room, DataStore Preferences
- AlarmManager (exact + inexact fallback), NotificationManager
- RemoteViews widget (not Glance — better Samsung compatibility)
- Docker-based builds (`./docker-build.sh ./gradlew assembleDebug --no-daemon`)
- Min SDK 31, Target SDK 35, tested on API 35 emulator

## Key Files

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Activity, Compose wiring, confirm/reset/settings handlers |
| `ui/PatchScreen.kt` | Main UI — status header, location grid, settings section |
| `ui/DisplayUtils.kt` | Pure functions: formatStatusInfo, formatDaysLeft |
| `ui/Theme.kt` | Dark theme colors |
| `data/Patch.kt` | Room entity, PATCH_DURATION_MS, suggestLocations() |
| `data/PatchDao.kt` | Room DAO — observeAll, getById, upsert, getActivePatchesByDueDate |
| `data/PatchDatabase.kt` | Room DB singleton, seeds 4 rows via raw SQL in onCreate |
| `data/UserPreferences.kt` | DataStore — patch count, notify hour/minute |
| `alarm/AlarmScheduler.kt` | Schedule/cancel exact alarms, adjustToNotifyTime |
| `alarm/AlarmReceiver.kt` | BroadcastReceiver — shows notification, chains nags, checks DB |
| `boot/BootReceiver.kt` | Re-schedules alarms after reboot |
| `widget/PatchWidgetReceiver.kt` | RemoteViews 1x1 widget — days remaining with color |
| `widget/PatchWidgetState.kt` | Pure widget state logic — urgency, display text |

## Tests (87 total, 5 files)

| File | Count | Covers |
|------|-------|--------|
| `RotationLogicTest.kt` | 11 | suggestLocations edge cases, PATCH_DURATION_MS |
| `AlarmSchedulerTest.kt` | 18 | Request codes, nag delays, adjustToNotifyTime |
| `AlarmReceiverTest.kt` | 22 | Nag suppression, scheduling limits, notification content |
| `DisplayUtilsTest.kt` | 14 | Status header text, days-left, overdue boundary |
| `PatchWidgetStateTest.kt` | 22 | Widget urgency, display text, subtitle |

## Build & Deploy

```bash
# Build
./docker-build.sh ./gradlew assembleDebug --no-daemon

# Run tests
./docker-build.sh ./gradlew test --no-daemon

# APK location
app/build/outputs/apk/debug/app-debug.apk

# Serve to phone (start python HTTP server)
mkdir -p /tmp/apk-serve
cp app/build/outputs/apk/debug/app-debug.apk /tmp/apk-serve/patch-notifier.apk
cd /tmp/apk-serve && python3 -m http.server 8888 --bind 0.0.0.0
# Then on phone: http://<LAN_IP>:8888/patch-notifier.apk

# Docker emulator for testing (API 35)
docker run -d --name android-emu --device /dev/kvm \
  -v $(pwd):/project --entrypoint bash patch-notifier-builder -c '...'
```

## Signing

Uses `debug.keystore` in project root (checked into git). Same key across builds so updates install over existing without uninstall.

## Bugs Fixed During Session

1. `collectAsStateWithLifecycle` crash — switched to `collectAsState`
2. Room DB seed race — use raw SQL in onCreate callback
3. `selectedIds` race on confirm — snapshot before clearing
4. `SCHEDULE_EXACT_ALARM` SecurityException on API 35 — fallback to inexact
5. Orphan coroutines — switched to `lifecycleScope`
6. Overdue display used truncated days — fixed with millisecond delta
7. Nag chain infinite — capped at 48, checks DB before nagging
8. `onNewIntent` not handled — notification tap now pre-selects patches
9. `adjustToNotifyTime` compared wrong value — fixed to compare against dueAtMs
10. Silent failures in receivers — added catch blocks with fallback notification
11. Widget blank on Samsung — switched from Glance to RemoteViews
12. Widget `onUpdate` process kill — added `goAsync()`
13. Reset confirm never expired — auto-dismisses after 3 seconds

## Code Review History

Passed 6 review rounds (code quality, test coverage, silent failure analysis). Final review: no issues found.

## GitHub

Private repo: https://github.com/babyhuey/patch-notifier
