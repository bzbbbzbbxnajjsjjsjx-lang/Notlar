# Foundations: theme, color, motion, shape, typography

Values below are read from the material3 1.5.0-beta01 sources (`tokens/`, `MotionScheme.kt`,
`MaterialTheme.kt`, `Shapes.kt`, `Typography.kt`, `MaterialShapes.kt`). Full tables are in
`tokens.md`, signatures in `api/`.

## Theme

```kotlin
@Composable
fun MaterialExpressiveTheme(
    colorScheme: ColorScheme? = null,
    motionScheme: MotionScheme? = null,
    shapes: Shapes? = null,
    typography: Typography? = null,
    content: @Composable () -> Unit,
)
```

- Outermost use: anything left `null` falls back to the Expressive default —
  `expressiveLightColorScheme()`, `MotionScheme.expressive()`, `Shapes()`, `Typography()`.
- Nested use: `null` means "inherit from the enclosing theme".
- It is just `MaterialTheme` with Expressive defaults; components do not switch behaviour on it.
  Passing `motionScheme = MotionScheme.expressive()` to a plain `MaterialTheme` gives the same
  motion.
- `expressiveLightColorScheme()` is `lightColorScheme()` with the `on*Container` roles moved from
  tone 10 to tone 30 (`onPrimaryContainer = Primary30`, etc.) for softer contrast inside
  containers. There is no `expressiveDarkColorScheme`; dark stays `darkColorScheme()`.
- With a custom brand scheme (as in this app), keep your scheme and pass only the motion scheme.

Sample: `samples/ThemeSamples.kt` → `MaterialExpressiveThemeSample`.

## Color

The color system is unchanged in roles; Expressive leans harder on them:

- Containers carry emphasis: `primaryContainer`, `secondaryContainer`, `tertiaryContainer` for
  hero surfaces, toggles and selected states; the `surfaceContainer*` ladder
  (`Lowest`, `Low`, base, `High`, `Highest`) for layering instead of elevation shadows.
- Fixed roles (`primaryFixed`, `primaryFixedDim`, `onPrimaryFixed`, `onPrimaryFixedVariant`, and
  the secondary/tertiary equivalents) stay the same in light and dark; use them for brand
  moments that must not flip with the theme.
- "Vibrant" variants exist for menus (`MenuDefaults.itemVibrantColors()`, vibrant menu
  containers) and floating toolbars (`FloatingToolbarDefaults.vibrantFloatingToolbarColors()`):
  they use `tertiaryContainer`/`primaryContainer` instead of surface colors.
- Contrast: a progress track or slider track that contrasts below 3:1 with what is behind it
  needs the stop indicator (`stopSize`, `drawStopIndicator`).

## Motion

`MotionScheme` exposes six spring specs; components ask the theme for them so the whole app moves
consistently.

| Spec | Use for | Standard (damping / stiffness) | Expressive (damping / stiffness) |
| --- | --- | --- | --- |
| `fastSpatialSpec()` | small components: switches, buttons, chips | 0.9 / 1400 | 0.6 / 800 |
| `defaultSpatialSpec()` | partial-screen: sheets, drawers, FAB menu | 0.9 / 700 | 0.8 / 380 |
| `slowSpatialSpec()` | full-screen transitions | 0.9 / 300 | 0.8 / 200 |
| `fastEffectsSpec()` | small color/alpha changes | 1.0 / 3800 | 1.0 / 3800 |
| `defaultEffectsSpec()` | medium color/alpha changes | 1.0 / 1600 | 1.0 / 1600 |
| `slowEffectsSpec()` | full-screen fades | 1.0 / 800 | 1.0 / 800 |

Rules that follow from the tokens:

- **Spatial** (position, size, shape, rotation) uses springs that may overshoot; Expressive
  overshoots visibly (damping 0.6–0.8). **Effects** (color, alpha) never overshoot (damping 1.0).
- Read specs from the theme instead of hard-coding:

  ```kotlin
  val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
  val scale by animateFloatAsState(if (pressed) 0.9f else 1f, spatial)
  val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Color>()
  val color by animateColorAsState(if (selected) cs.primaryContainer else cs.surface, effects)
  ```
- Springs have no fixed duration; don't pair them with `tween` durations for the same property.
  Linear tweens are only for continuous loops (wave phase, indeterminate rotation).

## Shape

### Corner scale (`MaterialTheme.shapes`, `ShapeDefaults`)

| Role | Corner |
| --- | --- |
| `extraSmall` | 4dp |
| `small` | 8dp |
| `medium` | 12dp |
| `large` | 16dp |
| `largeIncreased` (new) | 20dp |
| `extraLarge` | 28dp |
| `extraLargeIncreased` (new) | 32dp |
| `extraExtraLarge` (new) | 48dp |
| full | `CircleShape` |

### Shape morphing in components

Many components take a shapes object with per-state shapes and animate between them with the
motion scheme's spatial spec:

- `ButtonDefaults.shapes()` / `ButtonShapes(shape, pressedShape)` — round when idle, squarer when
  pressed. Per size: `ButtonDefaults.shapesFor(buttonHeight)`.
- `ToggleButtonDefaults.shapesFor(buttonHeight)` or `shapesFor(ToggleButtonSize)` /
  `ToggleButtonShapes(shape, pressedShape, checkedShape)` — round → square when checked.
- `IconButtonDefaults.shapes()`, `IconButtonDefaults.toggleableShapes()` — same for icon buttons.
- `SplitButtonDefaults` leading/trailing shapes; the trailing button becomes a circle when
  checked.
- `ButtonGroupDefaults.connectedLeadingButtonShapes()`, `connectedMiddleButtonShapes()`,
  `connectedTrailingButtonShapes()` for connected groups.

### MaterialShapes (35 expressive polygons, still `@ExperimentalMaterial3ExpressiveApi`)

`Circle`, `Square`, `Slanted`, `Arch`, `Fan`, `Arrow`, `SemiCircle`, `Oval`, `Pill`, `Triangle`,
`Diamond`, `ClamShell`, `Pentagon`, `Gem`, `Sunny`, `VerySunny`, `Cookie4Sided`, `Cookie6Sided`,
`Cookie7Sided`, `Cookie9Sided`, `Cookie12Sided`, `Ghostish`, `Clover4Leaf`, `Clover8Leaf`,
`Burst`, `SoftBurst`, `Boom`, `SoftBoom`, `Flower`, `Puffy`, `PuffyDiamond`, `PixelCircle`,
`PixelTriangle`, `Bun`, `Heart`.

They are normalized (fit 0..1) `androidx.graphics.shapes.RoundedPolygon`s; graphics-shapes comes
with material3 as an `api` dependency. `toShape()` and `RoundedPolygon.toPath()` are
`@Composable` (remembered); `Morph.toPath()` is not.

```kotlin
// Static clip / background
Box(Modifier.size(96.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primaryContainer))

// Morph between two shapes
val morph = remember { Morph(MaterialShapes.Circle, MaterialShapes.Cookie9Sided) }
val progress by animateFloatAsState(if (selected) 1f else 0f, MaterialTheme.motionScheme.defaultSpatialSpec())
val path = remember { Path() }
Canvas(Modifier.size(96.dp)) {
    path.rewind()
    morph.toPath(progress, path)          // unit-size path, centered at (0.5, 0.5)
    scale(size.minDimension, pivot = Offset.Zero) { drawPath(path, color) }
}
```

Helpers: `RoundedPolygon.toShape(startAngle = 0)`, `RoundedPolygon.toPath(startAngle = 0)`,
`Morph.toPath(progress, path, startAngle = 0)`. 1.5.0-beta01 also reworked `PolygonShape`
(`CornerRounding` top level with `dp()`/`fraction()` factories, in-place
`PolygonShape.transform { }` and `copy()`); see `api/` for the exact API.

Sample: `samples/MaterialShapesSamples.kt` (`AllShapes`).

## Typography

Every role has an **Emphasized** twin with a heavier weight and the same size, for hero numbers,
selected states and headlines that must stand out (`MaterialTheme.typography.displayLargeEmphasized`
etc.).

| Role | Size / line height | Weight | Emphasized weight |
| --- | --- | --- | --- |
| displayLarge | 57 / 64sp | Normal | Medium |
| displayMedium | 45 / 52sp | Normal | Medium |
| displaySmall | 36 / 44sp | Normal | Medium |
| headlineLarge | 32 / 40sp | Normal | Medium |
| headlineMedium | 28 / 36sp | Normal | Medium |
| headlineSmall | 24 / 32sp | Normal | Medium |
| titleLarge | 22 / 28sp | Normal | Medium |
| titleMedium | 16 / 24sp | Medium | Bold |
| titleSmall | 14 / 20sp | Medium | Bold |
| bodyLarge | 16 / 24sp | Normal | Medium |
| bodyMedium | 14 / 20sp | Normal | Medium |
| bodySmall | 12 / 16sp | Normal | Medium |
| labelLarge | 14 / 20sp | Medium | Bold |
| labelMedium | 12 / 16sp | Medium | Bold |
| labelSmall | 11 / 16sp | Medium | Bold |

`Typography(fontFamily = …)` (new in 1.5) applies one font family to every style,
emphasized ones included. Prefer an emphasized role over `fontWeight = FontWeight.Bold` on a
regular role.

`Text` supports `autoSize` (`TextAutoSize.StepBased(...)`) for numbers that must fit a container.

## Component sizes (from tokens)

| Component | XSmall | Small | Medium | Large | XLarge |
| --- | --- | --- | --- | --- | --- |
| Button height | 32dp | 40dp | 56dp | 96dp | 136dp |
| Icon button container | 32dp | 40dp | 56dp | 96dp | 136dp |

Check `tokens.md` (`Button*Tokens`, `*IconButtonTokens`, `Fab*Tokens`, `ExtendedFab*Tokens`,
`SplitButton*Tokens`) for icon sizes, paddings and pressed shapes per size; the `Defaults`
objects expose them (`ButtonDefaults.MediumContainerHeight`, `ButtonDefaults.contentPaddingFor`,
`ButtonDefaults.iconSizeFor`, `ButtonDefaults.textStyleFor`, …).
