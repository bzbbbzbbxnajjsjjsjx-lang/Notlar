# Upgrading Vardiya from material3 1.4.0-alpha14 to 1.5.0-beta01

**Done in app 1.6.0.** Kept as a record of what changed. The app used
`material3 = "1.4.0-alpha14"` (May 2025). Everything in this skill documents
1.5.0-beta01 (October 2026), where most Expressive APIs are stable. This page lists what changes
for this codebase and what was removed or renamed in between. Changelog details:
`changelog.md`.

## Build changes

- **Toolchain (not in the release notes, found by building):** material3 1.5.0-beta01 and Compose
  1.12 AARs require `compileSdk` 37 and Android Gradle Plugin 9.1 or newer. The app moved to AGP
  9.4.1 and Gradle 9.8.1 and dropped the `org.jetbrains.kotlin.android` plugin (AGP 9 compiles
  Kotlin itself); `targetSdk` stays 36.
- `gradle/libs.versions.toml`: `material3 = "1.5.0-beta01"`.
- material3 1.5.0-beta01 depends on Compose `1.12.0` (`foundation`, `ui`, `runtime`,
  `animation-core`) and `androidx.graphics:graphics-shapes:1.0.1` (as `api`). The current
  `composeBom = "2025.05.00"` pins older Compose artifacts. Gradle resolves to the higher version
  anyway, but bump the BOM to a release that maps to Compose 1.12 so `ui-tooling` and friends
  match.
- `material-icons-core` is no longer pulled in by material3 (since 1.4.0-alpha01). The app already
  declares it. Google no longer updates that library; new icons should come as Material Symbols
  vector drawables from fonts.google.com/icons.
- Opt-ins: `ExperimentalMaterial3ExpressiveApi` is still needed for `LoadingIndicator`,
  `ContainedLoadingIndicator`, `MaterialShapes` and the `toShape()`/`toPath()` helpers. Keep the
  module-wide opt-in in `app/build.gradle.kts`.
- Verified locally (detekt, lint, unit tests, debug and release APKs) and in CI.

## Code in this app that the upgrade touches

| Where | Today | With 1.5.0-beta01 |
| --- | --- | --- |
| `ui/theme/Theme.kt` | `MaterialTheme(colorScheme)` | `MaterialTheme(colorScheme, motionScheme = MotionScheme.expressive())`, or `MaterialExpressiveTheme(colorScheme = …)`. Keep the teal scheme. |
| `ui/VardiyaApp.kt` opening animation | `LoadingIndicator(Modifier.size(96.dp))` | Same API, still experimental. |
| `ui/VardiyaApp.kt` bottom bar | `NavigationBar` + `NavigationBarItem` | `ShortNavigationBar` + `ShortNavigationBarItem` is the Expressive bar (64dp, pill indicator). |
| `ui/screens/SummaryScreen.kt` `ExpressiveButton` | hand-animated `RoundedCornerShape(percent)` | `Button(onClick, shapes = ButtonDefaults.shapes(), …)` plus `ButtonDefaults.MediumContainerHeight` (56dp) or `LargeContainerHeight` (96dp); the size helpers give padding, icon size and text style. Delete `ExpressiveButton`. |
| `ui/components/WavyRing.kt` | custom Canvas ring | Replaced in 1.6.1 by `CircularWavyProgressIndicator` with `stroke`, `trackStroke`, `gapSize` and `wavelength` scaled to 10dp; the custom wave was uneven. |
| Text styles | `fontWeight = FontWeight.SemiBold/Bold` on regular roles | `MaterialTheme.typography.displaySmallEmphasized`, `headlineMediumEmphasized`, etc. |

## Removed or renamed between alpha14 and beta01

From a diff of the two `api/current.txt` files.

- **Component overrides are gone**: `ExperimentalMaterial3ComponentOverrideApi`,
  `NavigationBarOverride`, `NavigationRailOverride`, `SingleRowTopAppBarOverride`,
  `TwoRowsTopAppBarOverride`, `BasicAlertDialogOverride` and their `Local…Override` and
  `Default…Override` symbols.
- `ExitAlwaysFloatingToolbarScrollBehavior` → use
  `FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection)`.
- `ModalWideNavigationRailDefaults` removed; `WideNavigationRailDefaults.containerShape` /
  `modalContainerShape` → `shape`, `modalCollapsedShape`, `modalExpandedShape`.
- `ButtonDefaults.SmallButtonContentPadding` removed → `ButtonDefaults.contentPaddingFor(height)`.
- `TonalToggleButton` → `FilledTonalToggleButton`. `ToggleButtonDefaults` lost its per-size shape
  vals (`mediumSquareShape`, `largePressedShape`, …), `shapes()`, `roundShape`, `squareShape`,
  `ContentPadding` and the `…ToggleButtonColors()` functions; use `shapesFor(size)`,
  `contentPaddingFor(size, hasStartIcon)`, `colors()` and the variant `…ToggleButtonDefaults`
  objects.
- `SplitButtonDefaults.leadingButtonShapes()` / `trailingButtonShapes()` (hidden) →
  `leadingButtonShapesFor(height)` / `trailingButtonShapesFor(height)`; `InnerCornerSize`,
  `LeadingButtonContentPadding`, `TrailingButtonContentPadding` removed.
- `IconButtonDefaults.SmallSelectedSquareShape` → `smallSelectedSquareShape`.
- `AppBarMenuState.isExpanded`, `ButtonGroupMenuState.isExpanded` → `isShowing`
  (see `api/AppBarDsl.md`, `api/ButtonGroup.md`).
- Sliders: `SliderState`/`RangeSliderState` no longer hold `onValueChange`,
  `onValueChangeFinished`, `valueRange`; `activeRangeStart`/`activeRangeEnd` → `startValue`/
  `endValue`; `onValueChange` is a required `Slider` parameter.
- `TooltipScope.drawCaret` removed → `obtainAnchorBounds()` and draw your own caret shape.
- `SearchBarScrollBehavior.scrollOffset`/`scrollOffsetLimit` and `SearchBarState.collapsedCoords`
  removed.
- `TimePickerColors` unselected-color properties removed (see `api/TimePicker.md`).
- `TopAppBarDefaults.pinnedScrollBehavior` / `enterAlwaysScrollBehavior` now take a
  `ScrollableState`; the old overloads are deprecated.

None of these are used in the app today.
