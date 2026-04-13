# Cerebrum

## User Preferences

- Prefers minimal UI — no typing, minimal buttons, tap-only interaction
- Wants me to pick the tech stack when no preference is expressed
- Android-only, personal use, no Play Store distribution

## Key Learnings

- Stack choice: Kotlin + Jetpack Compose (native Android for reliable exact alarm scheduling)
- 7-day notification interval is the core feature — reliability is paramount

## Do-Not-Repeat

*(none yet)*

## Decision Log

- 2026-04-13: Chose Kotlin + Jetpack Compose over React Native/Expo/Flutter — native gives most reliable control over Android exact alarm scheduling (SCHEDULE_EXACT_ALARM), which is critical for 7-day reminders
