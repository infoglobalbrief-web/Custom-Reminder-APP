# Development Plan — Personal Reminder App (Remindly)

> Source of truth: `Personal Reminder App.pdf` (63 pages — PRD + TRD + UI/UX +
> Architecture + Development Plan). Every requirement below is mapped to the
> implementation in this repository.

## 1. Requirement scan summary (from PDF)

| PDF § | Requirement area | Key points |
|------:|------------------|------------|
| Product concept | Offline-first personal reminder assistant | Tasks, birthdays, important dates, recurring activities; **no internet needed to fire a reminder** |
| §1–8 | Design direction | Soft Glass + modern skeuomorphism, purple/lavender palette, pills, rounded cards |
| §2 | Design tokens | Light `#F4F1FF` / primary `#6658E8`; dark `#11101B` / primary `#8B7CFF` |
| §3–4 | Typography | Manrope headings, Inter body, compact scale (hero 26–32 … caption 10–11) |
| §5 | Radius system | XS 8 · SM 12 · MD 16 · LG 22 · XL 28 · PILL 999 |
| §6–7 | Glassmorphism + micro-skeuomorphism | Glass for floating UI; solid for forms/dialogs; raised selected dates |
| §8 | Pill UI | Filters, segmented controls, date/time pills |
| §9 | Navigation | Home · Tasks · **+** · Calendar · People (+ More), central elevated FAB |
| §10–11 | Splash + 3-screen onboarding | REMINDLY wordmark; Get Started CTA |
| §12–16 | Authentication | Guest · Google · Apple · Email; minimal first-login profile |
| §17 | Permission onboarding | Notifications, exact alarms, battery — with rationale |
| §18–21 | Home | Greeting, TODAY progress, NEXT card, date selector, swipe task cards |
| §22 | Quick add sheet | Title, Today/Tomorrow, time, More options › |
| §23 | Advanced create | WHEN · REPEAT · ALERTS · ALARM · ORGANIZE |
| §24 | Multiple alerts | One reminder → many alerts (stored as alert rules) |
| §25–28 | Birthdays/People | Countdown card, add-birthday rules, birthday-day UI |
| §29 | Calendar | Floating month grid + day agenda |
| §30–31 | Tasks + gestures | Today/Upcoming/Completed, swipe/long-press/tap |
| §32 | Reminder detail | Full metadata + Edit |
| §33 | More tab | Profile, appearance, notifications, widgets, privacy, about |
| §34 | Widgets | Small NEXT REMINDER widget |
| §35–36 | Notifications | Rich copy + DONE / SNOOZE actions + deep link |
| §37 | Functional requirements | CRUD, duplicate, complete, snooze, skip, archive, search, filter, recurrence set, birthdays, calendar, org |
| §38 | Non-functional | <2s launch, 60fps, survive reboot/timezone/permission changes, 100% local core |
| §39–43 | Architecture | Clean layers, repository, single state approach, design-system first |
| §44–46 | Reminder engine | Rule → occurrence calculator → next → local scheduler → OS |
| §45 | **Critical rule** | Never depend on backend to fire reminders |
| §47–52 | Auth + schema | UUID identity; reminders / alerts / people / birthdays tables |
| §53–54 | Sync readiness | Change tracking fields (`updatedAt`, `deletedAt`, `syncVersion`, `dirty`) |
| §55–56 | Scheduler API | schedule/reschedule/cancel/rebuild; schedule only next occurrence |
| §57 | Lifecycle | Launch repair, boot reschedule |
| §58–59 | Permission failure UX + timezone | Honest disabled state; follow-device vs keep-original TZ |
| §60–61 | Component library + tokens | GlassCard, pills, cards, empty states; AppColors/Type/Spacing/Radius |
| §62–64 | Motion / empty / error states | Subtle 200–350ms; friendly error copy |
| §65–68 | Accessibility, security, guest linking | TalkBack labels, secure flags, guest → signed-in merge |
| §69 | Analytics (minimal) | Product events only — not reminder content |
| §70 | Phases 0–9 | MVP = UI + reliable reminders (see boundary below) |
| §71–72 | Testing | Time/recurrence/device matrix; **mandatory Father-Birthday QA test** |
| §73 | Store readiness | Permissions, privacy, signing checklists |
| §74–75 | Final architecture + ~50 screens | Implemented MVP screens + extension points |
| §76–77 | MVP boundary + unified Reminder Rule | Single rule engine powers tasks, birthdays, events |

## 2. Tech stack decision

PDF recommends Flutter for cross-platform. The delivery brief requires a
**native Android** app, so the equivalent native stack is used while keeping
the PDF's architecture intact:

| Concern | Choice |
|---------|--------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 (design system from §2–8) |
| Architecture | MVVM + Repository + domain engine (PDF §41–42 layers) |
| Local DB | Room / SQLite (PDF §41) — 100% local-first |
| Settings | DataStore Preferences |
| Scheduling | `AlarmManager` exact alarms + `BroadcastReceiver`s (PDF §46) |
| Notifications | NotificationManager channels + actions DONE/SNOOZE (PDF §35–36) |
| Boot / TZ recovery | `BootCompletedReceiver` (PDF §38, §57) |
| Widget | `AppWidgetProvider` + RemoteViews (PDF §34) |
| Auth | Guest + Email local; `AuthRepository` seam for Google Credential Manager (PDF §13, §47) |
| DI | Lightweight `AppContainer` (manual, no reflection) |
| Tests | JUnit recurrence tests incl. §72 mandatory case |

Cloud sync (Supabase, PDF §40/§53) is **V2**: entities already carry
`updatedAt / deletedAt / syncVersion / dirty` so the sync engine can attach
without schema rewrites.

## 3. Build map (phase → code)

| PDF Phase | Status | Where |
|-----------|--------|-------|
| 0 Design system | ✅ | `ui/theme/*`, `ui/components/*` |
| 1 Foundation | ✅ | Gradle project, `RemindlyApp`, nav, DataStore, Room, theme, guest |
| 2 Authentication | ✅ MVP (Guest/Email/Google seam; Apple for iOS target) | `features/auth/*`, `SettingsRepository` |
| 3 Reminder engine | ✅ | `core/domain/recurrence`, `core/domain/scheduler/*` |
| 4 Tasks | ✅ | `features/tasks`, categories/priority on reminder model |
| 5 Birthdays/People | ✅ | `features/people`, countdown + window alerts |
| 6 Calendar | ✅ month + agenda (week/day segmented ready) | `features/calendar` |
| 7 Widgets | ✅ small widget | `features/more/widget/*` |
| 8 Cloud sync | 🧭 schema-ready, engine in V2 | `dirty/syncVersion/deletedAt` fields |
| 9 Smart features | 🧭 V2 | — |

## 4. Mandatory QA (PDF §72) — automated

`app/src/test/java/com/remindly/app/RecurrenceEngineTest.kt`

- Father birthday **30 Oct 2026**, start **25 Oct**, **22:00**, daily
- expects 25→30 Oct @ 22:00, **no** 31 Oct, next cycle reopens **25 Oct 2027**
  for the 30 Oct 2027 birthday
- plus §71 matrix: weekdays, weekly BYDAY, monthly clamp, leap year, until-date,
  custom interval, midnight/month/year edges, serialization round-trip

Run: `./gradlew :app:testDebugUnitTest`

## 5. How to build the APK

```bash
# Android Studio → Open → this folder → Run ▶
# or CLI:
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Requirements: JDK 17, Android SDK 34, Gradle 8.7 (wrapper included).

## 6. MVP boundary (PDF §76) — delivered

Reminder + Recurrence + Alarm + Snooze + Tasks + Calendar + Categories +
Birthday countdown/daily alerts + Local scheduler + Notifications + Widget.

V2: Supabase sync, widgets medium/large, voice/NLU, templates, AI messages.
