# Sludge EQ

Single-module Android app (`:app`, package `com.vardiya.sludge`): a parametric equalizer for stoner, doom, noise and
drone listeners. Everything is manual: the person picks a genre preset or edits the filters. Fully offline: no AI, no
Last.fm, no notification access, no INTERNET permission. Jetpack Compose + Material 3 Expressive, no DI
framework, no navigation library. Same stack and conventions as the Notlar app in the parent folder; this folder is
its own Gradle project (run Gradle from here).

## Commands

```bash
./gradlew testDebugUnitTest      # JVM unit tests (domain logic) and screenshot tests
./gradlew recordRoborazziDebug   # re-record screenshots after an intended UI change (run it alone)
./gradlew verifyRoborazziDebug   # compare screens with app/src/test/screenshots/ (CI runs this)
./gradlew detekt                 # static analysis, incl. Compose rules
./gradlew lintDebug              # Android lint
./gradlew assembleDebug assembleRelease
```

CI is `../.github/workflows/sludge.yml` (it only runs for changes under `sludge/`; a `sludge-v*` tag publishes the
release APK). In a cloud session pass `-I .claude/gradle/maven-central-mirror.gradle` and set `ANDROID_HOME`
(the SDK platform is `android-37.0`; symlink it as `android-37`).

## Architecture

Dependencies point one way: `ui` → `app` → `domain`, with `data` and `audio` as adapters around Android. `domain`
imports nothing from Android and holds every rule, so every rule has a unit test.

- `domain/` — pure Kotlin.
  - `Models.kt` (`EqSettings`, `TuneSource`), `Genre.kt` (each preset is a list of parametric filters plus
    bass/virtualizer/loudness), `Tuning.kt` (`applying(genre)`), `CurveMath.kt` (millibels and loudness headroom).
  - `Parametric.kt` — `ParametricFilter` (peak, low and high shelf), `Biquad` (RBJ cookbook magnitude response), `Parametric`
    (summed response, `ENGINE_HZ` 31 third-octave bands), `FilterScale` (slider ↔ value maps).
  - `Status.kt` — the line the home screen leads with.
  - `Headphones.kt` — headphone correction profiles (published parametric filters, boost cap). `OutputRoute.kt` — where the sound goes
    (speaker, wired, Bluetooth with names) and whether a profile applies to it. `Shaping.kt` — `curveAt(route, hz)`: the curve the
    sound really gets (the person's filters plus the bass shelf plus the headphone correction, minus the automatic headroom),
    the bass knob as a low shelf filter (`Bass`) and the limiter gain.
- `data/` — `SettingsRepository` (everything in one Preferences DataStore; it wipes the keys, listening log and notes older versions stored).
- `audio/` — Android adapters: `EffectEngine` (per audio session: DynamicsProcessing first, with 31 bands read from `curveAt` and a limiter whose
  make-up gain is the loudness boost; the classic Equalizer and LoudnessEnhancer when a phone refuses it; the Virtualizer
  separately; BassBoost is not used),
  `AudioSessionReceiver` (players
  announce their audio session through the audio-effect broadcast), `OutputMonitor` (which output devices are connected).
- `app/` — `SludgeRuntime` is the composition root and the one object the UI and the Android entry points talk to.
  `EqController` keeps the effects in step with the settings and runs audio sessions through one queue (a close can
  never overtake its open).
- `ui/` — `SludgeViewModel` (state in, `SludgeActions` out), `SystemSettings` (the system screens it opens),
  `SludgeApp` (tabs: Ekolayzır, Kurulum), `screens/`, `components/` (parametric editor and response chart, effect cards,
  common pieces), `theme/Theme.kt`.

Rules that keep it sound: a new rule goes in `domain/` with a test, not in a composable or a service; Android types
stay out of `domain/`; the UI reads `UiState` and calls `SludgeActions` only (it may name an Android component, as
`SystemSettings` does for the system screens).

## Product logic

- The home screen leads with the real state (`statusOf`): Kapalı, Çalar bekleniyor, Ses işleniyor.
  Effects only exist on audio sessions a player has announced; without one the equalizer cannot change the sound, and
  the screen says so.
- Nothing is detected or applied on its own: the app does not know what is playing (that needs notification access, which
  the product deliberately does not ask for). A preset changes filters, bass, virtualizer and loudness together; any edit
  after it makes the source Elle.
- Headphone correction: a `HeadphoneProfile` is the filter set a measurement tool published (the 520BT's are AutoEq's
  ten ParametricEQ filters, checked in a test against AutoEq's own curve). `curveAt` adds the profile's correction on top of
  the person's filters, but only while that headphone is the output (matched by its Bluetooth name, spelling-insensitive; a
  Bluetooth name that cannot be read counts as a match; speaker, wired and other Bluetooth devices never get it). "Her
  çıkışta uygula" forces it. The correction's own boost is capped (+3 dB; the +6.4 dB at 20 Hz is where this headphone
  measures -6 dB and cannot play). Another headphone needs its own published filters, never guessed ones. The reasoning
  and sources are in `docs/eq-research.md`; the genre presets are engineering judgement, not measurements, and the doc says so.
- Headroom is automatic and global (`headroomDb`): the sound is turned down by the highest point of the whole curve from 40 Hz up
  (filters, bass shelf and headphone correction together) less a 3 dB slack for the limiter, never below zero. The bottom
  octave is left out because little music lives there and a small headphone cannot play it; peak-normalising to it cost
  11 dB on a bass-heavy preset. Presets thus stay at a comparable level. There is no strength slider; the loudness setting (the limiter's make-up gain) is what brings the level back.
- Parametric EQ only: the sound is shaped by `EqSettings.filters` (up to ten); the engine reads `curveAt` on 31 third-octave
  bands. Presets set the filters. A shelf reaches its gain well below its corner (half at the corner), so the presets
  put their shelves higher than the band they are meant for.
- Bass and loudness: Android's BassBoost and LoudnessEnhancer are optional and on many phones do nothing audible, so the
  bass knob is a +4.5 dB low shelf filter (`Bass.filter`) and the loudness setting is the limiter's make-up gain, which
  boosts without clipping. The Kurulum screen shows, per audio session, which path is working. That the DynamicsProcessing
  path sounds right has been checked on no real device from here; if a phone refuses it, the classic path takes over.
- Presets never boost more than +8 dB anywhere (tested); harsh noise keeps the treble down and the loudness gain off.

## Conventions

- Code, comments, commit messages: English. User-facing text (UI strings, `README.md`, `release-notes.md`): Turkish.
  UI strings are inline in Kotlin.
- Dates and number formatting use the Turkish locale (`tr-TR`).
- The app has no INTERNET permission, no notification access and no network code; adding any is a change to the
  product's privacy promise.
- Detekt config is in `config/detekt/detekt.yml`; fix findings, there is no baseline.
- Screenshot tests (`ui/ScreenshotTest.kt`, Roborazzi + Robolectric SDK 34) must be re-recorded and committed after a UI
  change; look at the PNGs first. Record and verify in separate Gradle runs.
