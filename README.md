# Pileo — Pill reminders (Android 11+, minSdk 30)

Jetpack Compose app to set pill reminders by days / hours.

## Features
- Pills list with name, dosage, times, days, on/off switch
- Add / edit: name*, dosage, notes, multiple times per day (time picker),
  days Mon–Sun + shortcuts Daily / Weekdays / Weekend, active toggle
- Ring mode: exact alarms (`AlarmManager.setAlarmClock`, weekly cadence)
  + foreground service (alarm-stream sound + vibration)
  + full-screen alarm UI (validate button + swipe-up to snooze)
  + high-priority notification with **J'ai pris** and **Rappeler** actions
- Silent mode (global switch in Settings): no sound/vibration/full-screen UI —
  persistent (ongoing) notification until the dose is validated or snoozed
- Pause: switch OFF pauses only the next ring (yellow moon slider),
  following cycle resumes normally; tap again for full disable
- Snooze delay configurable in Settings (5/10/15/20/30/60 min, default 15)
- Re-scheduled on reboot (`BOOT_COMPLETED`, package replaced, time/timezone changes)
  + battery-optimization exemption prompt
- Runtime permission handling: `POST_NOTIFICATIONS` (13+), exact-alarm settings redirect
- Room persistence (`pileo.db`, v3: `pausedUntil`, `silent`)

## Project
- `com.pileo.debug` (debug) / `com.pileo` (release)
- compileSdk 34, minSdk 30, targetSdk 34
- Kotlin 1.9.24, AGP 8.5.2, Compose BOM 2024.06.00, Room 2.6.1
- Build JDK: `C:\Program Files\Zulu\zulu-17` (Java 17, via `org.gradle.java.home`
  dans `gradle.properties` + `gradleJvm="zulu-17"` dans `.idea/gradle.xml`)

## Build & install (adb)
```powershell
.\gradlew.bat assembleDebug
adb devices
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell am start -n com.pileo.debug/com.pileo.MainActivity
```
Le JDK est fixé par `org.gradle.java.home` dans `gradle.properties`,
pas besoin de définir `JAVA_HOME`.
