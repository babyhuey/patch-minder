# anatomy.md

> Auto-maintained by OpenWolf. Last scanned: 2026-04-13T23:06:33.667Z
> Files: 28 tracked | Anatomy hits: 0 | Misses: 0

## ./

- `.gitignore` — Git ignore rules (~23 tok)
- `build.gradle.kts` (~76 tok)
- `docker-build.sh` (~112 tok)
- `Dockerfile` — Docker container definition (~81 tok)
- `gradle.properties` (~37 tok)
- `settings.gradle.kts` (~89 tok)

## .superpowers/brainstorm/45504-1776115036/content/

- `app-design.html` (~1942 tok)

## .superpowers/brainstorm/74395-1776115377/content/

- `app-design.html` (~1942 tok)
- `waiting.html` (~39 tok)

## Files


## app/

- `build.gradle.kts` (~507 tok)

## app/src/main/

- `AndroidManifest.xml` (~353 tok)

## app/src/main/java/com/patch/notifier/

- `MainActivity.kt` — MainActivity: onCreate (~1204 tok)
- `PatchApp.kt` — PatchApp: onCreate (~256 tok)

## app/src/main/java/com/patch/notifier/alarm/

- `AlarmReceiver.kt` — AlarmReceiver: onReceive, showNotification (~654 tok)
- `AlarmScheduler.kt` — patchAlarmRequestCode, nagAlarmRequestCode, nagDelayMs, schedulePatchAlarm, scheduleNagAlarm (~904 tok)

## app/src/main/java/com/patch/notifier/boot/

- `BootReceiver.kt` — BootReceiver: onReceive (~397 tok)

## app/src/main/java/com/patch/notifier/data/

- `Patch.kt` — Data class: Patch (~114 tok)
- `PatchDao.kt` — observeAll, getAll, upsert, upsertAll, getActivePatchesByDueDate (~163 tok)
- `PatchDatabase.kt` — PatchDatabase: patchDao, getInstance, buildDatabase, onCreate (~483 tok)

## app/src/main/java/com/patch/notifier/ui/

- `PatchScreen.kt` — PatchScreen, StatusHeader, LocationGrid, ThighColumn, LocationButton (~2079 tok)
- `Theme.kt` — PatchTheme (~242 tok)

## app/src/main/res/values/

- `strings.xml` (~31 tok)
- `themes.xml` (~43 tok)

## app/src/test/java/com/patch/notifier/

- `AlarmSchedulerTest.kt` — Declares AlarmSchedulerTest (~232 tok)
- `RotationLogicTest.kt` — Declares RotationLogicTest (~576 tok)

## docs/superpowers/plans/

- `2026-04-13-patch-notifier.md` — Patch Notifier Implementation Plan (~10910 tok)

## docs/superpowers/specs/

- `2026-04-13-patch-notifier-design.md` — Patch Notifier — Design Spec (~1275 tok)

## gradle/wrapper/

- `gradle-wrapper.properties` (~54 tok)
