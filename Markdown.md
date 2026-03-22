# 7-Minute Workout Timer APK - Project Specifications

## Project Overview
This is a **simple, offline, bodyweight-only HIIT timer app** inspired by the classic "7 Minute Workout" (12 high-intensity exercises, 30s work + 10s rest, ~7 minutes total per circuit).  
The app is designed for **Amazon Fire TV** (and Android phones/tablets as secondary target), runs directly on the device, and requires **no internet**, **no online registration/login**, **no ads**, and **no analytics**.

Key principles:
- One single user profile only (no multi-user support).
- Fully configurable timer settings for flexibility beyond the classic 7-minute format.
- Voice announcements for start/rest/milestones using Android's built-in Text-to-Speech (TTS) engine.
- Minimalist UI optimized for TV remote navigation (D-pad, OK button, minimal touch if on phone).
- No equipment needed — pure bodyweight exercises.
- No data collection or cloud sync.

## Core Features & Requirements

### 1. User Profile & Data Storage
- **Single profile only** — No login, no accounts.
- All settings saved locally using **SharedPreferences** or a simple Room/SQLite DB if needed (but SharedPreferences is sufficient for simplicity).
- On first launch: Show a quick welcome screen with "Get Started" button → immediately to main screen.
- No profile name/email/etc. — anonymous by design.

### 2. Main Screens & Navigation
- **Home Screen**:
  - Big "Start Workout" button.
  - "Settings" button (gear icon) to configure.
  - Optional: Quick info like "Classic 7-Min Workout" preset reminder.
- **Settings Screen**:
  - All configurable options listed below.
  - "Save" / "Apply" button + "Reset to Defaults" button.
  - Use simple switches, sliders, number pickers (avoid complex inputs on TV).
- **Workout Screen** (full-screen during session):
  - Large countdown timer for current phase (work/rest).
  - Visual indicator: Green for work, red/orange for rest.
  - Current exercise name (if implemented in future) or just "Work" / "Rest".
  - Progress: Circular progress bar or percentage for total workout completion.
  - Big pause/resume button + stop button.
  - Screen stays on during workout (disable keyguard / use WakeLock).

### 3. Configurable Settings (Possible Configurations)
All settings adjustable in the Settings screen. Save defaults as classic 7-min values.

| Setting                          | Description                                                                 | Default Value          | Input Type          | Range/Options                          |
|----------------------------------|-----------------------------------------------------------------------------|------------------------|---------------------|----------------------------------------|
| a. Total number of repetitions   | Number of work intervals (exercises/sets) per workout                       | 12                     | Number picker       | 1–30                                   |
| b. Active time during 1 set      | Duration of each work/high-intensity phase                                  | 30 seconds             | Slider / picker     | 10–120 seconds                         |
| c. Resting time                  | Duration of rest/transition between work sets                               | 10 seconds             | Slider / picker     | 5–60 seconds                           |
| d. Total time of exercise        | **Display only** — auto-calculated: (reps × active) + ((reps-1) × rest)    | ~7 minutes (read-only) | Text (calculated)   | N/A                                    |
| e. Loud "start" announcement     | Voice says "Start" (or "Go!" / "Exercise!") at beginning of each work phase | Enabled                | Switch              | On/Off                                 |
| f. Loud "rest" announcement      | Voice says "Rest" at beginning of each rest phase                           | Enabled                | Switch              | On/Off                                 |
| g. Milestone announcements       | If enabled: Voice announces at 50%, 75%, and 95% of total workout completion (e.g., "Halfway done!", "Almost there! 75%", "Just 5% left!") | Disabled               | Switch              | On/Off                                 |

- **Voice Announcements Details**:
  - Use Android **TextToSpeech** API (built-in, offline-capable on most devices).
  - Phrases:
    - Start: "Start" or "Begin exercising" or "Go!"
    - Rest: "Rest"
    - Milestones: "50% complete", "75% complete", "95% complete – keep going!"
  - Respect system TTS language/voice settings.
  - Volume: Use media volume stream (not notification).
  - If TTS not available/offline voices missing → fallback to silent or simple beep (but prefer voice).

### 4. Workout Flow Logic
1. User presses "Start Workout".
2. Calculate total duration from settings.
3. Loop for each repetition:
   - Announce "Start" (if enabled) → play work phase timer (b).
   - Count down visually + audibly (optional ticking sound).
   - When work ends → announce "Rest" (if enabled) → rest timer (c).
   - After last rest (no rest after final rep) → finish.
4. During workout:
   - Track overall progress %.
   - If milestone % reached (50/75/95) and option g enabled → speak once.
5. End screen: "Workout Complete!" + total time taken + "Do again?" button.

### 5. Technical & Platform Requirements
- **Target**: Android 7.0+ (API 24+) for broad Fire TV compatibility.
- **Build**: Android Studio + Kotlin (preferred) or Java.
- **Dependencies** (keep minimal):
  - AndroidX core.
  - Material Design components for UI.
  - TextToSpeech (built-in).
  - No external libs if possible (avoid Firebase, ads, etc.).
- **Permissions**:
  - None required ideally (TTS is permission-free on modern Android).
  - Optional: MODIFY_AUDIO_SETTINGS if custom volume needed.
- **TV Optimization**:
  - Leanback library or basic D-pad focus handling.
  - Large touch targets.
  - No small text/buttons.
- **Offline**: 100% — no network calls ever.

### 6. Future-Phase Ideas (Not in Scope Yet)
- Pre-defined exercise list (12 classic ones: Jumping Jacks, Wall Sit, Push-ups, etc.) with names shown during work phases.
- Simple images/animations per exercise.
- Multiple circuits/rounds.
- Sound effects (beeps) as backup.
- History log of completed workouts (dates, durations).

### 7. Constraints & Non-Features
- No user accounts / registration / cloud.
- No in-app purchases / ads.
- No sharing / social.
- No video playback (pure timer + voice).
- No Google Fit / health integration.
- Keep APK size small (<10MB ideal).