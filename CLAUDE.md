# Notlar

Single-module Android app (`:app`, package `com.vardiya.notlar`) for notes and to-do lists.
Jetpack Compose + Material 3 Expressive, no DI framework, no navigation library. Built on the same
stack and conventions as the Vardiya app (bzbbbzbbxnajjsjjsjx-lang/Androidharnwss).

## Commands

```bash
./gradlew testDebugUnitTest      # JVM unit tests (domain logic) and screenshot tests
./gradlew recordRoborazziDebug   # re-record screenshots after an intended UI change
./gradlew verifyRoborazziDebug   # compare screens with app/src/test/screenshots/ (CI runs this)
./gradlew detekt                 # static analysis, incl. Compose rules
./gradlew lintDebug              # Android lint
./gradlew assembleDebug assembleRelease
```

CI (`.github/workflows/android.yml`) runs detekt, lint, unit and screenshot tests and both APK
builds on every push. Cloud sessions can build locally if the Android SDK (platform 37) is
installed and `ANDROID_HOME` points at it; Maven Central rate-limits the session proxy (HTTP 429),
so pass `-I .claude/gradle/maven-central-mirror.gradle`. Still read the GitHub Actions result
before calling a change done.

## Layout

- `domain/` — pure Kotlin, unit-tested. `Note.kt` (`Note`, `ChecklistItem`, `NoteColor`,
  `Section`, `NoteFilter`, `NoteSort` incl. MANUAL, `ReminderRepeat`, `TextSize`), `NoteRules.kt` (list order, search,
  filters, 30-day trash, text ↔ checklist conversion, share text), `Markdown.kt` (the marks the
  editor writes: **bold**, *italic*, "# ", "- "; bodies are stored as Markdown), `Reminders.kt` (quick times,
  repeats), `Labels.kt` (label names, rename/merge/delete).
- `data/NoteRepository.kt` — all notes as one JSON list in Preferences DataStore. `ImageStore.kt`
  keeps note pictures as JPEGs in `filesDir/images` (notes hold file names; unused files are deleted
  at start; backups carry them base64 in `Note.embeddedImages`, never in stored notes). `AutoBackup.kt` writes the daily JSON backup into a SAF folder.
- `ui/NotesViewModel.kt` — single `AndroidViewModel`; saving a blank note removes it. It keeps
  reminder alarms in step with the stored notes and refreshes the widgets.
- `tile/NewNoteTileService.kt` — Quick Settings tile that opens a new note. A note's `locked` flag asks for
  the app PIN in `NotlarApp.openAskingPin`; cards hide its content and search skips it.
- `reminders/` — `ReminderScheduler` (one AlarmManager alarm per note), `ReminderReceiver` (rings,
  re-sets alarms after boot/update/clock change), `ReminderNotifier` (hides content while locked).
- `widget/` — Jetpack Glance widgets: `ChecklistWidget` (a chosen to-do list, checkable; hidden
  while the app lock is on) and `QuickNoteWidget`.
- `ui/NotlarApp.kt` — switches between the lists and the editor, snackbars with undo, sharing.
- `ui/screens/` — `MainScreen` (notes / archive / trash behind a `ShortNavigationBar`, large
  flexible app bar, staggered grid), `TrashList`, `EditorScreen` (autosave, floating toolbar,
  checklist editor).
- `ui/components/` — `NoteCard`, `Format.kt` (Turkish dates).
- `ui/theme/Theme.kt` — amber brand scheme (seed #7C5800) with `MaterialExpressiveTheme`; note
  colors map to container roles so they follow light and dark. `Type.kt` — Google Sans Flex
  (`res/font`, a Latin subset of the variable font with wdth 100–120, wght 300–900, opsz, ROND);
  emphasized display/headline roles are wide, rounded and heavy.

## Design

Screens were sketched first in M3E Canvas; `docs/m3e-canvas.json` is the document (open it with
*Open project* on https://lnkiai.github.io/m3e-canvas/). Sketch new screens there before building
them when the layout is not obvious.

## Conventions

- Code, comments, commit messages: English. User-facing text (UI strings, `README.md`,
  `.github/release-notes.md`): Turkish. UI strings are inline in Kotlin, not in `strings.xml`.
- Search and dates use the Turkish locale (`tr-TR`), never the default one.
- Accessibility: icon-only buttons have a `contentDescription`, headings are marked, touch
  targets ≥ 48dp.
- Build: AGP 9 (built-in Kotlin), Gradle 9, compileSdk 37, targetSdk 36, material3
  `1.5.0-beta01` ahead of the Compose BOM. Expressive opt-ins are set in `app/build.gradle.kts`.
  Icons come from `material-icons-extended` 1.7.8 (R8 strips unused ones).
- Detekt config is in `config/detekt/detekt.yml`; fix findings, there is no baseline.
- Screenshot tests (`ui/ScreenshotTest.kt`, Roborazzi + Robolectric SDK 34) must be re-recorded
  and committed after a UI change; look at the PNGs first.

## Releasing

Bump `versionCode` and `versionName` in `app/build.gradle.kts` and add a "Yenilikler (x.y.z)"
section at the top of `.github/release-notes.md`; also add `fastlane/metadata/android/tr-TR/changelogs/<versionCode>.txt`
(F-Droid, max 500 bytes). Pushing a `v*` tag, or running the workflow
with `release_tag`, publishes the release APK as a GitHub release.

## Agents

`.claude/agents/` holds three subagents; use them, in this order, for every change:

- `dal-bekcisi` - at the start of a task: confirms the branch contains the latest release.
- `tasarimci` - for any new screen or vague UI request: sketches it in M3E Canvas and returns a
  link; get the user's approval before writing code.
- `kontrolcu` - after the change and before every push or release: detekt, lint, tests, release
  build, and a look at every changed screenshot against the request.

## Skills

`.claude/skills/` holds Compose UI, performance audit, accessibility and testing guidance, plus
`material3-expressive`: the full Material 3 Expressive reference for material3 1.5.0-beta01.
Load the matching skill before working in that area.
