# Remindly — Personal Reminder App

A beautiful, **offline-first** native Android reminder assistant built from the
complete PRD/TRD in [`Personal Reminder App.pdf`](./Personal%20Reminder%20App.pdf).

> Remember what matters.

## Features (MVP per PRD §76)

- ✅ **Reliable local reminders** — `AlarmManager` exact alarms, boot/timezone
  rescheduling, never depends on the network to fire (PRD §45)
- ✅ **Recurrence engine** — Once · Daily · Weekdays · Weekly · Monthly ·
  Yearly · Custom interval · End date (PRD §37)
- ✅ **Multiple alerts per reminder** (PRD §24)
- ✅ **Snooze / Done** notification actions + deep link to detail (PRD §35–36)
- ✅ **Tasks** — Today / Upcoming / Completed, swipe to complete/delete,
  Morning/Afternoon/Evening grouping (PRD §30–31)
- ✅ **Birthdays & People** — countdown card, “5 days before · daily · 10 PM”
  alert windows, birthday-day UI (PRD §25–28)
- ✅ **Calendar** — floating month grid + day agenda (PRD §29)
- ✅ **Home dashboard** — greeting, progress bar, NEXT card, date selector
  (PRD §18–21)
- ✅ **Quick add sheet** + **Advanced create/edit** (PRD §22–23)
- ✅ **Splash · Onboarding (3) · Login (Guest/Google/Email) · Profile ·
  Permissions** (PRD §10–17)
- ✅ **Design system** — purple/lavender glass theme, dark mode, Manrope/Inter,
  radius & spacing tokens (PRD §1–8, §60–61)
- ✅ **Home-screen widget** (PRD §34)
- ✅ **Search**, empty/error states, accessibility semantics (PRD §37, §63–65)

## Tech stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose · Material 3 |
| Database | Room (SQLite) · local-first |
| Settings | DataStore |
| Scheduling | AlarmManager + BroadcastReceivers |
| Architecture | MVVM + Repository + pure domain engine |
| Tests | JUnit (includes mandatory PRD §72 QA case) |

> The PDF suggests Flutter for cross-platform; this delivery is the
> **native Android** equivalent of that architecture (see
> [docs/DEVELOPMENT_PLAN.md](./docs/DEVELOPMENT_PLAN.md)).

## Build & run

```bash
# Open in Android Studio (Hedgehog or newer) and press Run, or:
./gradlew :app:assembleDebug          # → app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # recurrence engine QA (PRD §71–72)
```

**Requirements:** JDK 17 · Android SDK Platform 34 · Gradle 8.7 (wrapper included)

## Project structure

```
app/src/main/java/com/remindly/app/
├── core/
│   ├── data/          # Room entities, DAOs, repositories, DataStore
│   ├── domain/
│   │   ├── model/     # Reminder, RepeatRule, Person, Occurrence
│   │   ├── recurrence/# Occurrence calculator (pure Kotlin, fully tested)
│   │   └── scheduler/ # AlarmManager scheduler + receivers
│   ├── notifications/ # Channels, rich notifications, actions
│   └── di/            # AppContainer
├── ui/
│   ├── theme/         # Colors, type, spacing, radius, motion tokens
│   ├── components/    # GlassCard, pills, cards, empty states…
│   └── navigation/    # Routes, bottom bar + floating +
└── features/          # splash-gate, onboarding, auth, home, tasks,
                       # reminders, calendar, people, more(+widget)
```

## Documentation

- [Development plan & requirement traceability](./docs/DEVELOPMENT_PLAN.md)
- Full requirements: `Personal Reminder App.pdf` (63 pages)

## License

All rights reserved — © Remindly.
