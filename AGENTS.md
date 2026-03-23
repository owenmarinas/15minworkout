# AGENTS.md — Machine-Readable Project Context

## Project Identity
- **Name**: 15-Minute Workout Timer
- **Package**: `com.workout.timer`
- **Type**: Android application (APK)
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34
- **Build tool**: Gradle 8.9 + AGP 8.7.0

## Repository Structure
```
.
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/workout/timer/
│       │   ├── MainActivity.kt        # Home screen
│       │   ├── SettingsActivity.kt    # Settings screen
│       │   ├── WorkoutActivity.kt     # Active workout + pause + complete
│       │   └── Prefs.kt               # SharedPreferences wrapper (single source of truth for all settings)
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── activity_settings.xml
│           │   └── activity_workout.xml
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           ├── drawable/              # Vector launcher assets + TV banner
│           └── mipmap-*/              # PNG launcher icons (PIL-generated)
├── apk/                               # Release APKs named YY-MM-DD.apk
├── build.gradle                       # Root build — plugin declarations only
├── settings.gradle                    # pluginManagement + dependencyResolutionManagement
├── gradle.properties                  # android.useAndroidX=true, Xmx2048m
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties      # distributionUrl = gradle-8.9-bin.zip
└── gradlew
```

## Architecture
- **No ViewModel, no LiveData, no Compose** — plain Activity + XML layouts.
- **No external dependencies** beyond AndroidX core, AppCompat, Material, ConstraintLayout.
- **No network calls** — fully offline.
- **State lives in `WorkoutActivity`** as private vars. No persistence of workout state across process death.
- **Settings persisted** via `Prefs.kt` (SharedPreferences key `"workout_prefs"`).

## Data Model — `Prefs.kt`
All fields have defaults matching the classic 7-min workout baseline.

| Property | Type | Key | Default | Range |
|---|---|---|---|---|
| `reps` | Int | `reps` | 12 | 1–30 |
| `activeSeconds` | Int | `active_seconds` | 30 | 10–180 |
| `restSeconds` | Int | `rest_seconds` | 10 | 5–180 |
| `announceStart` | Boolean | `announce_start` | true | — |
| `announceRest` | Boolean | `announce_rest` | true | — |
| `announceMilestones` | Boolean | `announce_milestones` | false | — |
| `warmupEnabled` | Boolean | `warmup_enabled` | true | — |
| `warmupSeconds` | Int | `warmup_seconds` | 10 | 10–59 |

Computed: `totalSeconds() = (reps * activeSeconds) + ((reps - 1) * restSeconds)`

## Workout State Machine — `WorkoutActivity.kt`
```
START
  └─► [warmupEnabled?] ──yes──► WARMUP phase (GET READY countdown, no pause allowed)
                │                    └─► onFinish → WORK phase
                no
                └──────────────────► WORK phase
                                         └─► onFinish → [last rep?]
                                                  yes → COMPLETE screen
                                                  no  → REST phase
                                                           └─► onFinish → WORK phase (rep++)

Any phase (except WARMUP) ──tap/D-pad──► PAUSED overlay
PAUSED ──tap/Continue──► resume timer with remainingMs
PAUSED ──Cancel──► AlertDialog confirmation ──yes──► finish()
COMPLETE ──Do Again──► reset all state → START
```

Key state variables:
- `currentRep: Int` — 1-indexed, increments after each REST phase
- `totalReps: Int` — copied from prefs at start
- `isWorkPhase: Boolean` — true=WORK, false=REST
- `isWarmup: Boolean` — true during warmup countdown only
- `isPaused: Boolean`
- `remainingMs: Long` — snapshot of timer when paused, used to resume
- `elapsedMs: Long` — cumulative ms of fully completed phases (for progress %)
- `totalWorkoutMs: Long` — prefs.totalSeconds() * 1000L (warmup excluded from progress)
- `milestonesAnnounced: MutableSet<Int>` — tracks which of {50,75,95} have fired

## UI Layout Strategy
- **Orientation**: landscape forced (`screenOrientation="landscape"`) on all activities.
- **activity_main.xml**: vertical LinearLayout, centered, two buttons.
- **activity_settings.xml**: horizontal LinearLayout (two columns). Left: timer controls. Right: voice toggles + Save/Reset buttons pinned to bottom via `layout_weight="1"` spacer View.
- **activity_workout.xml**: FrameLayout with three overlapping children:
  1. `layoutWorkout` (RelativeLayout) — active timer, always visible unless paused/complete
  2. `layoutPaused` (LinearLayout, `visibility="gone"`) — pause overlay, two-column: stats left, buttons right
  3. `layoutComplete` (LinearLayout, `visibility="gone"`) — completion screen

## TTS (Text-to-Speech)
- Initialized in `WorkoutActivity.onCreate`, `OnInitListener` sets `ttsReady=true`.
- Uses `TextToSpeech.QUEUE_FLUSH` — each new announcement cancels the previous.
- Announcements: "Get ready!" (warmup), "Go!" (work start), "Rest" (rest start), milestone phrases, "Workout complete! Great job!"
- Milestone thresholds: 50%, 75%, 95% of `totalWorkoutMs`.
- Graceful degradation: if `ttsReady=false`, speak() is a no-op.

## Build Commands
```bash
./gradlew assembleDebug          # debug APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease        # release APK (minified) → app/build/outputs/apk/release/app-release-unsigned.apk
```

Release APKs are manually copied to `apk/YY-MM-DD.apk` after each build.

## Device Targets
- Primary: Amazon Fire TV (D-pad navigation, `LEANBACK_LAUNCHER` intent filter)
- Secondary: Android phones/tablets (touch)
- D-pad handling in `WorkoutActivity.onKeyDown`: DPAD_CENTER/ENTER → pause/resume, BACK → pause or confirm cancel

## Known Constraints
- APK is unsigned (no keystore configured). Sideload only — not Play Store ready.
- Icons are PIL-generated PNGs (solid color circle). Replace before production.
- `tv_banner.xml` is a plain dark rectangle. Replace with proper 320×180px banner for Fire TV store.
- No instrumented tests exist.
- ProGuard enabled for release; only TTS classes explicitly kept (`proguard-rules.pro`).

## Dependencies (app/build.gradle)
```
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
com.google.android.material:material:1.11.0
androidx.constraintlayout:constraintlayout:2.1.4
```
No Firebase, no ads SDK, no analytics, no network permissions.

## Permissions (AndroidManifest.xml)
- `WAKE_LOCK` — keeps screen on during workout
- No internet, no storage, no camera, no location
