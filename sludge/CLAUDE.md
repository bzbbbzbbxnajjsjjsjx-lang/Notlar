# Sludge EQ

Single-module Android app (`:app`, package `com.vardiya.sludge`): a parametric equalizer for stoner, doom, noise and
drone listeners that picks a genre preset from the track that is playing. Fully offline: no AI, no Last.fm, no INTERNET permission. Jetpack Compose + Material 3 Expressive, no DI
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
  - `Models.kt` (`TrackInfo`, `EqSettings`, `Tuning`, `TuneSource`), `Genre.kt` (each preset is a list of parametric filters plus
    bass/virtualizer/loudness; its `bands` are the filters read on the ten graphic bands), `GenreDetector.kt` (player genre tag,
    then artist list, then album/title words), `CurveMath.kt` (log-frequency mapping onto the device's bands, headroom).
  - `Tuning.kt` — `localTuning(track)` (detected preset plus the sentence that says why) and `applying`.
  - `Parametric.kt` — `ParametricFilter` (peak, low and high shelf), `Biquad` (RBJ cookbook magnitude response), `Parametric`
    (summed response, `ENGINE_HZ` 31 third-octave bands), `FilterScale` (slider ↔ value maps).
  - `NowPlaying.kt` — which media session to follow (`pickCurrent`). `Status.kt` — the line the home screen leads with.
  - `ListenStats.kt` — the listening log and the charts' numbers.
  - `Headphones.kt` — headphone correction profiles (ten band gains and a preamp). `OutputRoute.kt` — where the sound goes
    (speaker, wired, Bluetooth with names) and whether a profile applies to it. `Shaping.kt` — `curveAt(route, hz)`: the curve the
    sound really gets (graphic bands or parametric filters, plus headphone correction and the bass shelf, never above +12 dB),
    the bass knob as a low shelf (`Bass`), the band edges and the limiter gain.
- `data/` — `SettingsRepository` (everything in one Preferences DataStore; it wipes the keys older versions stored).
- `audio/` — Android adapters: `EffectEngine` (per audio session: DynamicsProcessing first, with 31 bands read from `curveAt` and a limiter whose
  make-up gain is the loudness boost; the classic Equalizer and LoudnessEnhancer when a phone refuses it; the Virtualizer
  separately; BassBoost is not used),
  `SludgeListenerService` (notification access, only used to read media sessions), `AudioSessionReceiver` (players
  announce their audio session through the audio-effect broadcast), `OutputMonitor` (which output devices are connected).
- `app/` — `SludgeRuntime` is the composition root and the one object the UI and the Android entry points talk to.
  `EqController` keeps the effects in step with the settings and runs audio sessions through one queue (a close can
  never overtake its open). `TuneCoordinator` follows the player through one pipeline (a newer request replaces an
  older one; the genre preset is applied and the listen logged), behind the `TuneEnvironment` port so `TuneCoordinatorTest` runs it on virtual time with fakes.
- `ui/` — `SludgeViewModel` (state in, `SludgeActions` out), `SystemSettings` (the system screens it opens),
  `SludgeApp` (tabs: Ekolayzır, Dinleme, Kurulum), `screens/`, `components/` (band columns, effect cards,
  common pieces), `theme/Theme.kt`.

Rules that keep it sound: a new rule goes in `domain/` with a test, not in a composable or a service; Android types
stay out of `domain/`; the UI reads `UiState` and calls `SludgeActions` only (it may name an Android component, as
`SystemSettings` does for the system screens); a paused track is shown but is neither tuned nor logged; a listen is
counted even when a newer track cancels its tuning; an automatic result never overwrites a slider the person moved
while it was being worked out; a manual edit stays until the next track's preset replaces it. Tests that use
`backgroundScope` must move the virtual clock with `advanceTimeBy` (`advanceUntilIdle` does not wait for it).

## Product logic

- The home screen leads with the real state (`statusOf`): Kapalı, Kurulum gerekli, Çalar bekleniyor, Ses işleniyor.
  Effects only exist on audio sessions a player has announced; without one the equalizer cannot change the sound, and
  the screen says so.
- Genre evidence, best first: the player's own genre tag (the first tag that names a genre decides; a tag that also names
  another scene such as pop or house is skipped; a bare "noise" or "ambient" tag counts only as a whole tag), then the artist
  list, then words in the album and title. A track nothing recognises gets the flat preset, and the screen says so. The
  listening log files each play under the detected genre.
- Headphone correction: the curve the person or the AI chose is kept; the profile's correction and preamp are added on top
  when the equalizer is applied (`effectiveBands(route)`), but only while that headphone is the output (matched by its
  Bluetooth name, spelling-insensitive; a Bluetooth name that cannot be read counts as a match; speaker, wired and other
  Bluetooth devices never get it). "Her çıkışta uygula" forces it. Boosts in a profile are capped (+3 dB; the end bands
  are where a small headphone cannot deliver and where a boost eats the most headroom) and the headroom is the boost that
  is really applied, so the correction costs about 3 dB, not the tool's 6.1; "Düzeltme gücü" scales it down further. The 520BT numbers are AutoEq's fixed-band result (Harman over-ear 2018
  target, Rtings B&K 5128 measurement); another headphone needs its own measured profile, never a guessed one.
- Parametric EQ only: the sound is shaped by `EqSettings.filters` (up to ten); the engine reads `curveAt` on 31 third-octave
  bands. Presets set the filters (and `bands`, the same curve on ten bands, which the headphone correction still builds on).
  Manual edits keep the source MANUAL. A shelf reaches its gain well below its corner (half at the corner), so the presets
  put their shelves higher than the band they are meant for.
- Bass and loudness: Android's BassBoost and LoudnessEnhancer are optional and on many phones do nothing audible, so the
  bass knob is a +4 dB low shelf on the bands (`Bass.shelf`) and the loudness setting is the limiter's make-up gain, which
  boosts without clipping. The setup screen shows, per audio session, which path is working. That the DynamicsProcessing
  path sounds right has been checked on no real device from here; if a phone refuses it, the classic path takes over.
- Tuning: the detected genre's preset, applied once per track when auto-tune is on. Harsh noise keeps the treble down and
  the loudness gain off. Presets never boost more than +8 dB anywhere (tested).

## Conventions

- Code, comments, commit messages: English. User-facing text (UI strings, `README.md`, `release-notes.md`): Turkish.
  UI strings are inline in Kotlin.
- Dates and number formatting use the Turkish locale (`tr-TR`).
- The app has no INTERNET permission and no network code; adding any is a change to the product's privacy promise.
- Detekt config is in `config/detekt/detekt.yml`; fix findings, there is no baseline.
- Screenshot tests (`ui/ScreenshotTest.kt`, Roborazzi + Robolectric SDK 34) must be re-recorded and committed after a UI
  change; look at the PNGs first. Record and verify in separate Gradle runs.
