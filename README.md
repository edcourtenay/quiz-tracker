# Quiz Tracker

A native Android app (Kotlin + Jetpack Compose) that turns the **Coopers Quiz**
spreadsheet into an offline quiz-night scorer.

The workbook in this repo (`Coopers Quiz.xlsx`) is a pub-quiz scorekeeping
workbook. Its numbers and team names are placeholders; the *rules* are what
matter, and the app reimplements them.

---

## 1. What the spreadsheet does

Four sheets, four tables, three saved workbook views.

### `Scores` — the master data-entry sheet

| Col | Header | Formula / meaning |
|-----|--------|-------------------|
| A | Team Name | team identifier |
| B | Players | headcount |
| C | Amount | `=if(B2, 2*B2, "")` → **entry fee = £2 × players** |
| D | Rounds 1-4 | combined score for rounds 1–4 |
| E | Rounds 1-4 Rank | `=If(D2, Rank(D2,D:D), "")` |
| F | Rounds 5-6 | combined score for rounds 5–6 |
| G | Subtotal | `=IF(F2, D2+F2, "")` |
| H | Rounds 5-6 Rank | `=if(G2, RANK(G2,G:G), "")` |
| I | Wipeout | the final "Wipeout" round score |
| J | Total | `=if(I2<>"", I2+G2, "")` |
| K | Final Position | `=if(J2, Rank(J2,J:J), "")` |
| L | Column 1 | `=max(D2,F2,J2)` helper that drives the leader-row highlight |
| N/O | Total Players / Total Funds | `=Sum(B:B)` / `=sum(C:C)` |

Rules encoded in the sheet:

- Frozen panes at `B2`; the range is an Excel table with an auto-filter.
- **Conditional formatting**: the best value in D, F and I turns **blue**
  (`#6D9EEB`); the whole leading row turns **green** (`#B7E1CD`); a **Wipeout of
  0 turns red** (`#EA9999`).
- Three saved workbook views — *Final Position*, *Rounds 1-4 Rank*,
  *Rounds 5-6 Rank* — used to flip the on-screen leaderboard.

### `Rounds 1-4`, `Rounds 5-6`, `Wipeout` — live ranked views

Each is a `SORT()` of the `Scores` table:

- **Rounds 1-4** → sorted by *Rounds 1-4 Rank*, highlights the best D.
- **Rounds 5-6** → sorted by *Rounds 5-6 Rank*, highlights the best F.
- **Wipeout** → sorted by *Final Position*, highlights the best I (flags zeros).

### The quiz-night flow

1. Teams register; fee collected at **£2/head** (funds tracked).
2. Rounds 1–4 scores entered → ranked.
3. Rounds 5–6 scores entered → subtotal + ranked.
4. Wipeout round adds to the subtotal → **Total** → **Final Position**.
5. Ranked leaderboards are shown for the room.

---

## 2. The app

| Choice | Decision |
|--------|----------|
| Language / UI | Kotlin + Jetpack Compose (Material 3) |
| Data | Local only, offline (Room / SQLite) |
| App name | Quiz Tracker |
| Package | `com.coopers.quiztracker` |
| minSdk / targetSdk | 31 (Android 12) / 36 |

### Screens

- **Scores** — add teams, set player counts (with a stepper), enter Rounds 1-4,
  Rounds 5-6 and Wipeout. Each card shows the live Subtotal, Total and Position,
  plus the per-team fee. A summary strip shows **Teams / Players / Funds**.
- **Board** — switch between the four leaderboards (Rounds 1-4, Rounds 5-6,
  Wipeout, Final). The leader row is green, the best value is highlighted, and a
  Wipeout of 0 is shown in red — mirroring the sheet's conditional formatting.
- **Settings** — quiz name, entry fee per player, share results as CSV, clear all
  scores (keeps teams), delete all teams, and an About panel.

### Score engine

`domain/Scoring.kt` is a faithful port of the sheet formulas:

- `Amount = fee × players`
- `Subtotal = rounds1to4 + rounds5to6` (blank until rounds 5-6 are entered)
- `Total = subtotal + wipeout` (blank until wipeout is entered)
- Ranks use **Excel `RANK.EQ`** semantics: ties share a rank and the next rank is
  skipped (1, 2, 2, 4). Blank scores are unranked.

An empty field means "not entered" (a blank cell), which is deliberately
different from an entered score of `0` — the Wipeout red-flag depends on it.

### Export

`export/ResultsExporter.kt` writes a CSV with the same columns as the `Scores`
sheet (plus generated timestamp and totals) and shares it via the system share
sheet (through a `FileProvider`).

---

## 3. Project layout

```
app/src/main/java/com/coopers/quiztracker/
├── MainActivity.kt            # Compose entry point
├── QuizTrackerApp.kt          # Application: database + repository
├── data/                      # Room entities, DAO, database, repository
├── domain/Scoring.kt          # The score/rank engine (ported from the sheet)
├── export/ResultsExporter.kt  # CSV export + share intent
└── ui/                        # ViewModel, root nav, screens, theme, components
app/src/debug/java/com/coopers/quiztracker/ui/previews/   # @Preview-only code
app/src/test/java/.../domain/ScoringTest.kt   # Sheet-parity unit tests
```

### Compose previews

Every screen is split into a **stateful** wrapper and a **stateless** `…Content`
composable that takes plain state plus callbacks. The stateless ones are what the
`@Preview` functions render, so previews need no ViewModel or database:

| Stateful (used by the app) | Stateless (previewable) |
|---|---|
| `TeamsScreen(state, viewModel)` | `TeamsScreenContent(state, …)` |
| `SettingsScreen(state, viewModel)` | `SettingsScreenContent(state, …)` |
| `LeaderboardScreen(state)` | same composable, `initialBoardIndex = 0..3` |

The previews live in `app/src/debug/` so they are compiled into the debug variant
only and never ship in release (verified: zero preview classes in the release
build). Sample data is centralised in `PreviewData`, so adding a new preview is
just a matter of pointing it at `PreviewData.state()`.

Previewed today: Scores (empty / populated / dark), each of the four boards,
Settings, and the summary and team cards as individual components. Open any file
under `app/src/debug/.../ui/previews/` and use the **Split** or **Design** view in
Android Studio.

---

## 4. Build & run

Requires **JDK 17 or 21** and the Android SDK (compileSdk 36).

> **Note on the Gradle JVM.** This project uses Gradle 8.14.3 (required by AGP
> 8.13.2), which can only run on Java 1.8–24 — Java 25 needs Gradle 9.1+.
> Android Studio's *embedded* JetBrains Runtime is Java 25, so by default the
> IDE reports *"Gradle version 8.14.3 is incompatible with the Gradle JVM
> version 25"*. To avoid that, `gradle.properties` pins the daemon:
> `org.gradle.java.home=/Library/Java/JavaVirtualMachines/openjdk-21.jdk/Contents/Home`.
> Adjust or remove that line if your JDK lives elsewhere, or set
> **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**
> to a JDK 17/21 instead of the embedded JDK.

```bash
# Point Gradle at your SDK (or set ANDROID_HOME)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

./gradlew :app:assembleDebug      # build the debug APK
./gradlew :app:testDebugUnitTest  # run the scoring unit tests
./gradlew :app:installDebug       # install on a connected device/emulator
```

The debug APK is written to
`app/build/outputs/apk/debug/app-debug.apk`.

---

## 5. Notes / possible next steps

- Multiple quiz nights with history (the sheet models a single night).
- A dedicated fullscreen presentation mode for projecting the leaderboard.
- PDF/image export in addition to CSV.
