# Remove Single Patch — Design Spec

**Date:** 2026-04-25
**Status:** Approved

## Problem

When the user replaces a patch but places the new one at a *different* location than the old one, the old slot keeps its countdown running. The only existing escape hatch is "Reset all patches", which wipes every active slot. There is no way to clear a single slot individually.

## Solution

Long-press a location button to enter a per-slot "pending removal" state. A second tap on that same slot within 3 seconds clears its `dueAt`. Auto-cancels otherwise.

## UX

| Slot state | Long-press behavior |
|---|---|
| Active patch (dueAt > 0) | Strong haptic; slot turns red with text `TAP TO REMOVE`; awaits confirm tap |
| Active patch, currently selected for replacement | Clears selection, then enters pending-removal state |
| Empty (dueAt null) | No-op, weak haptic |

- Pending-removal auto-cancels after 3 seconds (mirrors existing "Reset all patches" pattern).
- Only one slot can be in pending-removal at a time; long-pressing another switches it.
- Confirm tap fires a long-press haptic and clears the slot.

## Implementation

### `ui/PatchScreen.kt`
- Add hoisted state `pendingRemovalId: Int?` in `PatchScreen`.
- Change `LocationButton` to use `combinedClickable` with `onClick` and `onLongClick`.
- When `pendingRemovalId == patch.id`, render the slot with red background, red border, and text `TAP TO REMOVE`. Tap calls `onRemove(id)` and clears `pendingRemovalId`.
- `LaunchedEffect(pendingRemovalId)` resets it to null after 3 seconds.
- Long-press on a selected slot: call `onToggle(id)` to deselect, then set `pendingRemovalId`.
- Long-press on empty slot: weak haptic only.

### `MainActivity.kt`
- Add `onRemove(id: Int)` callback wired to `PatchScreen`.
- Implementation: load patch, set `dueAt = null` and `replacedAt = null`, `dao.upsert(...)`, call `AlarmScheduler` to cancel/reschedule for that patch, trigger widget update.

### Data model
- No changes. `Patch.dueAt` is already nullable.

### Tests
- `RotationLogicTest`: add case verifying that clearing one patch's `dueAt` does not affect others' due dates or notification scheduling.

## Out of scope
- Undo / snackbar.
- Bulk-remove via multi-select.
- Long-press on empty slots doing anything beyond a weak haptic.
