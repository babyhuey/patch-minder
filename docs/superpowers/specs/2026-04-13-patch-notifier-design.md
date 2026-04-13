# Patch Notifier — Design Spec

Personal Android app for tracking estrogen patch placement and scheduling replacements.

## Overview

Single-screen app with zero typing. Tap to log which patches you replaced, get notified in 7 days to replace them. Nags until you confirm.

## User Profile

- Solo user, personal device, no Play Store distribution
- 3 estrogen patches worn simultaneously, replaced on a 7-day cycle
- 4 placement locations on front thighs: Left Upper, Left Lower, Right Upper, Right Lower
- Sometimes replaces fewer than 3 (1 or 2), so patches can have independent timers

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3
- **Notifications:** AlarmManager (exact alarms via SCHEDULE_EXACT_ALARM) + NotificationManager
- **Persistence:** Room database (single table) or DataStore for simplicity
- **Min SDK:** 31 (Android 12) — simplifies exact alarm permissions
- **Build:** Gradle with Kotlin DSL, installed via USB/ADB sideload

## Data Model

```
Patch {
  id: Int (1-4, maps to location)
  location: String ("Left Upper" | "Left Lower" | "Right Upper" | "Right Lower")
  appliedAt: Long? (epoch millis, null = not currently applied)
  dueAt: Long? (epoch millis, null = no active timer)
}
```

No history table. Only current state matters. When a patch is replaced, `appliedAt` and `dueAt` are overwritten.

## Main Screen

Single screen, no navigation, no menus.

### Layout (top to bottom)

1. **Status header** — shows earliest due date across all active patches in human-readable form ("Thursday, Apr 17 · 3 patches · 4 days from now")
2. **Location grid** — 2x2 grid (Left/Right columns, Upper/Lower rows), each cell is a large tappable button
   - **Selected (blue, solid border):** this patch will be replaced on confirm
   - **Unselected (dark, dashed border):** this patch stays as-is
   - On open, the app pre-selects the locations it suggests based on rotation logic
   - User taps to toggle any location on/off
3. **Helper text** — "3 suggested · tap to toggle" (count updates dynamically)
4. **Confirm button** — "REPLACED N PATCHES" (N = number of selected locations). Disabled if 0 selected.

### Confirm Action

When tapped:
- For each selected location: set `appliedAt = now`, `dueAt = now + 7 days`
- Schedule exact alarm for each patch's `dueAt`
- Cancel any existing nag alarms for those patches
- Button briefly shows checkmark, then returns to normal state
- Status header updates to reflect new due dates

## Rotation Logic

The app suggests which 3 locations to use next. Algorithm:

1. Sort all 4 locations by `appliedAt` ascending (oldest first, null = never used = oldest)
2. Suggest the 3 with the oldest `appliedAt` — this naturally rotates the rest spot
3. The location most recently applied gets rested

This produces a round-robin pattern: each location gets 1 week of rest every 4 weeks.

User can always override by toggling locations manually.

## Notifications

### 7-Day Alert

- Fires at exact `dueAt` time for the earliest-due patch
- Groups all patches due at the same time into one notification
- Title: "Patch Replacement Due"
- Body: "Time to replace: Left Upper, Left Lower, Right Upper" (lists specific locations)
- Tapping the notification opens the app with those locations pre-selected

### Nag Repeats

- If the user doesn't open the app and confirm within 1 hour, a second notification fires
- After that, nags repeat every 2 hours
- Nag notifications use the same channel but with escalating priority (first nag = default, subsequent = high)
- Nags stop when the user opens the app and confirms replacement
- Implementation: each nag schedules the next one via AlarmManager. Confirming cancels pending nag alarms.

### Notification Channel

- Single channel: "Patch Reminders"
- Default importance: HIGH (shows heads-up notification)
- Sound: system default

## Permissions

- `SCHEDULE_EXACT_ALARM` — required for reliable 7-day and nag alarms
- `POST_NOTIFICATIONS` — required on Android 13+ for notification display
- No network, location, camera, or storage permissions needed

## Edge Cases

- **App killed / phone rebooted:** AlarmManager exact alarms survive reboot (with BOOT_COMPLETED receiver). App registers a BootReceiver that re-schedules all pending alarms from the database.
- **All 4 locations selected:** allowed — user might want to replace everything at once
- **0 locations selected:** confirm button disabled
- **Patches with different due dates:** status header shows the earliest. Each patch has its own timer and notification. If patches are due on different days, separate notifications fire.
- **First launch:** all patches show as unselected (no history), all 4 suggested. User picks their initial 3.

## Visual Design

- Dark theme only (OLED-friendly, matches mockup)
- Color palette: deep navy background (#1a1a2e), blue accent (#3d5afe), muted text (#8b8ba7)
- Large touch targets (minimum 48dp, buttons are ~56dp tall)
- No icons needed beyond checkmark on selected state
- No app bar / top bar — just the status header
- Rounded corners throughout (12-16dp radius)
