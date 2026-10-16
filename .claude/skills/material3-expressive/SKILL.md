---
name: material3-expressive
description: Complete Material 3 Expressive reference for Jetpack Compose (androidx material3 1.5.0-beta01) — every public API with KDoc, all design tokens, the official samples, theme/motion/shape/typography foundations, component guides, release notes since 1.4.0-alpha01 and the notes on this app's upgrade from 1.4.0-alpha14. Use for any work on visual components, shapes, motion, typography, the Summary screen ring, or a material3 upgrade.
---

# Material 3 Expressive

Material 3 Expressive is the 2025 evolution of Material 3: springy, physics-based motion; a
richer shape system with morphing between states; emphasized typography; bigger, bolder
component sizes; and new components (button groups, split buttons, floating toolbars, FAB menu,
wavy progress, loading indicator, short navigation bar, wide navigation rail, flexible app bars,
segmented lists, expressive menus).

## Versions — read first

| | material3 | Expressive status |
| --- | --- | --- |
| 1.4.0-alpha14 | May 2025 | what this app used before 1.6.0; several APIs since renamed |
| 1.4.0 stable | Sep 2025 | shipped without Expressive (only `ShortNavigationBar`, `WideNavigationRail`) |
| **This app and reference** | `1.5.0-beta01` (Oct 2026) | stable, except `LoadingIndicator`, `ContainedLoadingIndicator`, `MaterialShapes` and some menu/slider/pull-to-refresh bits (`@ExperimentalMaterial3ExpressiveApi`) |

Before writing code against an API from this reference, check it exists in the version the app
actually builds with (`gradle/libs.versions.toml`). `references/migration.md` lists what was
renamed or removed after alpha14 and how upgrading changes this app's code.

## Reference map

| Need | Read |
| --- | --- |
| Theme, color roles, motion springs, corner scale, MaterialShapes and morphing, emphasized type scale, component sizes | `references/foundations.md` |
| Which component to use and how; expressive knobs; sample names | `references/components.md` |
| Exact signature, parameters and KDoc of any public API | `references/api/INDEX.md` → `references/api/<File>.md` |
| Exact spec values (dp, shapes, color roles, type roles, motion) | `references/tokens.md` (search `## <Component>Tokens`) |
| Working example code (official androidx samples) | `references/samples/<File>Samples.kt` — KDoc `@sample …samples.Foo` points to `fun Foo` |
| When/why an API changed | `references/changelog.md` |
| What the alpha14 → 1.5 upgrade changed | `references/migration.md` |

Search rather than read whole files: `api/` is ~1.5 MB, `tokens.md` ~130 KB.

```bash
grep -rn "fun ButtonGroup" .claude/skills/material3-expressive/references/api/
grep -n -A12 "^## SplitButtonMediumTokens" .claude/skills/material3-expressive/references/tokens.md
grep -rln "@material3expressive" .claude/skills/material3-expressive/references/api/   # APIs tagged Expressive
```

## Principles to apply

1. **Library first.** Use the Material component if one exists; it brings theming, motion,
   semantics and touch targets. Draw your own only when the component can't be configured to
   fit (the Summary ring turned out to be configurable), and take its proportions from `tokens.md`.
2. **Motion from the theme.** Get specs from `MaterialTheme.motionScheme`
   (`fastSpatialSpec()`, `defaultSpatialSpec()`, `slowSpatialSpec()`, `…EffectsSpec()`).
   Spatial properties (size, position, shape) use springs that may overshoot; color and alpha use
   the non-overshooting effects specs. No hand-picked `tween` durations except continuous loops.
3. **Shape carries state.** Pressed, checked and selected states morph shape (round ↔ square)
   through the components' `shapes` parameters (`ButtonDefaults.shapes()`,
   `ToggleButtonDefaults.shapesFor(size)`, `IconButtonDefaults.shapes()`, `ListItemDefaults.shapes()`).
   Use `MaterialShapes` for decorative or hero shapes, not for touch targets.
4. **Emphasis through roles.** Bigger sizes (`Medium`/`Large`/`ExtraLarge` buttons and FABs),
   `…Emphasized` type styles and container colors (`primaryContainer`, vibrant variants) mark the
   primary action. One emphasized hero per screen.
5. **Colors from the scheme.** Never hard-code colors in screens; this app's teal scheme lives in
   `ui/theme/Theme.kt` with dynamic color off.
6. **Accessibility holds.** Touch targets ≥ 48dp; morphing animates the drawn shape, not the
   layout bounds; information shown by shape, color or waves is also in text or semantics; tracks
   below 3:1 contrast need the stop indicator.

## This app

- Opt-ins `ExperimentalMaterial3Api` and `ExperimentalMaterial3ExpressiveApi` are set for the
  whole module in `app/build.gradle.kts`; don't add `@OptIn` at call sites.
- Lists use `LargeFlexibleTopAppBar` with `exitUntilCollapsedScrollBehavior`, a
  `ShortNavigationBar` and an `ExtendedFloatingActionButton` that collapses on scroll.
- The editor's actions sit in a `HorizontalFloatingToolbar` (vibrant colors, standard on yellow
  notes where vibrant would match the background); icon buttons use `IconButtonDefaults.shapes()`
  and the pin uses `IconToggleButton(shapes = IconButtonDefaults.toggleableShapes())`.
- Note colors are scheme container roles (`ui/theme/Theme.kt`), never hard-coded colors.
- Transitions take specs from `MaterialTheme.motionScheme`; the theme is `MaterialExpressiveTheme`.

## Keeping the reference current

`scripts/refresh.sh <androidx commit> <version>` regenerates `api/`, `tokens.md` and `samples/`
from the androidx GitHub mirror. The commit is the end of the `+log/A..B/compose/material3` range
for that version on the material3 release notes page. Then update `foundations.md`,
`components.md`, `migration.md` and `changelog.md` by hand, and the version table above.

Sources: AOSP androidx (`compose/material3/material3` at commit
`19991f2c0858aadfcb442ce02b2c8e4743c96898`, 1.5.0-beta01) and the material3 release notes;
Apache License 2.0, see `references/NOTICE`.
