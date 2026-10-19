# Sludge EQ

Single-module Android app (`:app`, package `com.vardiya.sludge`): an equalizer for stoner, doom, noise and drone
listeners that tunes itself from the track that is playing. Jetpack Compose + Material 3 Expressive, no DI
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
  - `Models.kt` (`TrackInfo`, `EqSettings`, `AiConfig`, `AiProfile`, `TuneSource`), `Genre.kt` (presets as five
    anchors spread over ten bands), `GenreDetector.kt` (offline guess), `CurveMath.kt` (log-frequency mapping onto the
    device's bands, headroom), `UrlRules.kt` (plain HTTP only for private addresses).
  - `AiProtocol.kt` — prompt, request and response for Claude, OpenAI and OpenRouter, with `HttpRequestSpec` so no
    network type leaks in. `TuneCache` — remembered answers.
  - `AutoTuner.kt` — the tuning policy (remembered answer, then AI, then offline guess) behind two ports,
    `TuneStore` and `ProfileSource`; tests use fakes.
  - `NowPlaying.kt` — which media session to follow (`pickCurrent`). `Status.kt` — the line the home screen leads with.
  - `ListenStats.kt` — the listening log and the charts' numbers.
  - `Headphones.kt` — headphone correction profiles (ten band gains and a preamp) and `effectiveBands()`, the curve the
    equalizer is really set to. `OutputRoute.kt` — where the sound goes (speaker, wired, Bluetooth with names) and whether a
    profile applies to it. `LastFm.kt` — Last.fm requests, answers and tag filtering, the `TagSource` port, `withTags`.
- `data/` — `SettingsRepository` (everything in one Preferences DataStore; the API key goes through `KeyCipher`,
  an Android Keystore AES-GCM key), `AiClient.kt` (`Transport`, `AiTuner` implements `ProfileSource`),
  `RepoTuneStore` implements `TuneStore`, `LastFmClient` and `LastFmTagSource` (tags cached on disk, nothing asked without a key).
- `audio/` — Android adapters: `EffectEngine` (Equalizer, BassBoost, Virtualizer, LoudnessEnhancer per audio session),
  `SludgeListenerService` (notification access, only used to read media sessions), `AudioSessionReceiver` (players
  announce their audio session through the audio-effect broadcast), `OutputMonitor` (which output devices are connected).
- `app/` — `SludgeRuntime` is the composition root and the one object the UI and the Android entry points talk to.
  `EqController` keeps the effects in step with the settings and runs audio sessions through one queue (a close can
  never overtake its open). `TuneCoordinator` follows the player through one pipeline (a newer request replaces an
  older one), behind the `TuneEnvironment` port so `TuneCoordinatorTest` runs it on virtual time with fakes.
- `ui/` — `SludgeViewModel` (state in, `SludgeActions` out), `SystemSettings` (the system screens it opens),
  `SludgeApp` (tabs: Ekolayzır, Dinleme, Yapay Zekâ, Kurulum), `screens/`, `components/` (band columns, effect cards,
  common pieces), `theme/Theme.kt`.

Rules that keep it sound: a new rule goes in `domain/` with a test, not in a composable or a service; Android types
stay out of `domain/`; the UI reads `UiState` and calls `SludgeActions` only (it may name an Android component, as
`SystemSettings` does for the system screens); a paused track is shown but is neither tuned nor logged; a listen is
counted even when a newer track cancels its tuning; an automatic result never overwrites a slider the person moved
while it was being worked out; the AI button always asks the AI again, even for a remembered track. Tests that use
`backgroundScope` must move the virtual clock with `advanceTimeBy` (`advanceUntilIdle` does not wait for it).

## Product logic

- The home screen leads with the real state (`statusOf`): Kapalı, Kurulum gerekli, Çalar bekleniyor, Ses işleniyor.
  Effects only exist on audio sessions a player has announced; without one the equalizer cannot change the sound, and
  the screen says so.
- Genre evidence, best first: the tags (the player's own, then Last.fm's, most popular first; the first tag that names a
  genre decides; a tag that also names another scene such as pop or house is skipped; a bare "noise" or "ambient" tag counts
  only as a whole tag), then the artist list, then words in the album and title. Last.fm tags are short plain names only
  (no sentences or markup), fetched with a timeout, and never hold the tuning back.
- Headphone correction: the curve the person or the AI chose is kept; the profile's correction and preamp are added on top
  when the equalizer is applied (`effectiveBands(route)`), but only while that headphone is the output (matched by its
  Bluetooth name, spelling-insensitive; a Bluetooth name that cannot be read counts as a match; speaker, wired and other
  Bluetooth devices never get it). "Her çıkışta uygula" forces it. The 520BT numbers are AutoEq's fixed-band result (Harman over-ear 2018
  target, Rtings B&K 5128 measurement); another headphone needs its own measured profile, never a guessed one.
- Tuning: AI when a key is set and the request works, the offline genre guess otherwise. Harsh noise keeps the treble
  down and the loudness gain off; AI answers are clamped to the same limits.

## Conventions

- Code, comments, commit messages: English. User-facing text (UI strings, `README.md`, `release-notes.md`): Turkish.
  UI strings are inline in Kotlin.
- Dates and number formatting use the Turkish locale (`tr-TR`).
- The AI and Last.fm keys are never logged, never part of a `toString` (a Last.fm request carries its key in the address,
  so `HttpRequestSpec` masks it), never in a saved-state bundle, and only sent over HTTPS (the AI key may also go to a
  private-network address).
- Detekt config is in `config/detekt/detekt.yml`; fix findings, there is no baseline.
- Screenshot tests (`ui/ScreenshotTest.kt`, Roborazzi + Robolectric SDK 34) must be re-recorded and committed after a UI
  change; look at the PNGs first. Record and verify in separate Gradle runs.
