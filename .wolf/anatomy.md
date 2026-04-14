# anatomy.md

> Auto-maintained by OpenWolf. Last scanned: 2026-04-14T01:42:59.477Z
> Files: 44 tracked | Anatomy hits: 0 | Misses: 0

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

- `build.gradle.kts` (~617 tok)

## app/src/main/

- `AndroidManifest.xml` (~509 tok)

## app/src/main/java/com/patch/notifier/

- `MainActivity.kt` — MainActivity: onCreate, onNewIntent (~2336 tok)
- `PatchApp.kt` — PatchApp: onCreate (~256 tok)

## app/src/main/java/com/patch/notifier/alarm/

- `AlarmReceiver.kt` — Returns true if the patch was recently replaced and nag should be suppressed. (~1423 tok)
- `AlarmScheduler.kt` — Adjusts a raw dueAt timestamp to fire at the preferred notification time on that day. (~1260 tok)

## app/src/main/java/com/patch/notifier/boot/

- `BootReceiver.kt` — BootReceiver: onReceive (~441 tok)

## app/src/main/java/com/patch/notifier/data/

- `Patch.kt` — Data class: Patch (~165 tok)
- `PatchDao.kt` — observeAll, getAll, upsert, upsertAll, getById (~188 tok)
- `PatchDatabase.kt` — PatchDatabase: patchDao, getInstance, onCreate (~476 tok)
- `UserPreferences.kt` — Data class: PatchPreferences (~414 tok)

## app/src/main/java/com/patch/notifier/ui/

- `DisplayUtils.kt` — Data class: StatusInfo (~312 tok)
- `PatchScreen.kt` — PatchScreen, StatusHeader, LocationGrid, ThighColumn, LocationButton (~4419 tok)
- `Theme.kt` — PatchTheme (~242 tok)

## app/src/main/java/com/patch/notifier/widget/

- `PatchWidget.kt` — PatchWidget: provideGlance, loadState, PatchWidgetContent (~959 tok)
- `PatchWidgetReceiver.kt` — PatchWidgetReceiver: onUpdate, updateAllWidgets, loadState, updateWidget (~965 tok)
- `PatchWidgetState.kt` — Data class: WidgetUrgency (~372 tok)

## app/src/main/res/drawable/

- `ic_launcher_background.xml` (~88 tok)
- `ic_launcher_foreground.xml` (~471 tok)
- `widget_background.xml` (~59 tok)

## app/src/main/res/layout/

- `widget_loading.xml` (~138 tok)
- `widget_patch.xml` (~301 tok)

## app/src/main/res/mipmap-anydpi-v26/

- `ic_launcher_round.xml` (~73 tok)
- `ic_launcher.xml` (~73 tok)

## app/src/main/res/values/

- `strings.xml` (~55 tok)
- `themes.xml` (~43 tok)

## app/src/main/res/xml/

- `patch_widget_info.xml` (~135 tok)

## app/src/test/java/com/patch/notifier/

- `AlarmReceiverTest.kt` — Declares AlarmReceiverTest (~1015 tok)
- `AlarmSchedulerTest.kt` — AlarmSchedulerTest: fixedDueAt (~1583 tok)
- `DisplayUtilsTest.kt` — Declares DisplayUtilsTest (~924 tok)
- `PatchWidgetStateTest.kt` — Declares PatchWidgetStateTest (~1043 tok)
- `RotationLogicTest.kt` — Declares RotationLogicTest (~1134 tok)

## docs/superpowers/plans/

- `2026-04-13-patch-notifier.md` — Patch Notifier Implementation Plan (~10910 tok)

## docs/superpowers/specs/

- `2026-04-13-patch-notifier-design.md` — Patch Notifier — Design Spec (~1275 tok)

## gradle/wrapper/

- `gradle-wrapper.properties` (~54 tok)
