# Habivance
A private, offline-first habit tracker for Android. No account, no cloud, no Google dependencies. Your data stays on your device.

https://img.shields.io/badge/Kotlin-2.2.10-purple
https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue
https://img.shields.io/badge/Room-2.8.1-green
https://img.shields.io/badge/minSdk-24-orange

## What it is
Habivance is a habit tracker built for people who don't want to hand their daily routines to a cloud service. Every completion, every streak, every reminder is stored locally in a Room database. The app never makes a network request, never phones home, and works entirely on-device.

## Features
- **Habit tracking** — daily or weekly habits with custom name, emoji, color, and priority
- **Streaks** — current and best streak calculated per habit
- **Heatmap history** — GitHub-style 6-month completion view on each habit's detail screen
- **Exact-time reminders** — AlarmManager-based, self-rescheduling, with notification actions (Mark done, +1h, +2h, Tomorrow)
- **Boot persistence** — reminders restored after device reboot via BootReceiver
- **App lock** — PIN (SHA-256 hashed) + biometric unlock, with 30-second background grace
- **Backup & restore** — JSON export/import via Android Storage Access Framework
- **Drag to reorder** — long-press any habit on the main list to reorder
- **Themes** — Light / Dark / System, with amber accent throughout
- **No Google Play Services** — runs on Huawei and any Android device without GMS

## Tech Stack
| Layer | Tech |
| :--- | :--- |
| **Language** | Kotlin 2.2.10 |
| **UI** | Jetpack Compose, Material 3 |
| **Persistence** | Room 2.8.1, DataStore 1.1.7 |
| **Reminders** | AlarmManager (exact) + Notification actions |
| **Background** | BootReceiver, AlarmManager rescheduling |
| **Security** | SHA-256 hashed PIN (DataStore), BiometricPrompt |
| **Backup** | SAF + kotlinx.serialization (JSON) |
| **Images** | Coil 3.x |

## Requirements
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 37
- minSdk 24, targetSdk 37

## Build
1. Open the project in Android Studio
2. Create `keystore.properties` at `../keystores/habivance/` (outside the repo — never committed)
3. `./gradlew assembleDebug` for debug, or `./gradlew assembleRelease` for signed release

## Privacy
Habivance collects nothing. No analytics, no crash reporting, no network requests, no user accounts.

Privacy policy: https://maeskanaex-code.github.io/habivance-privacy/

## License
All rights reserved. Source code is published for portfolio and reference purposes only.
