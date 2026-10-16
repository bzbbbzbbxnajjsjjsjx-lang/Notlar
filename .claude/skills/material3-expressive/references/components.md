# Expressive components (material3 1.5.0-beta01)

One section per component family: what it is for, the entry points, the knobs that make it
"expressive", and where the official samples are. Exact signatures: `api/<File>.md`. Samples:
`samples/<File>Samples.kt` (function names listed). Stability column: **stable** = no opt-in,
**exp** = `@ExperimentalMaterial3ExpressiveApi`.

## Buttons — `api/Button.md`, `samples/ButtonSamples.kt` (stable)

`Button`, `ElevatedButton`, `FilledTonalButton`, `OutlinedButton`, `TextButton`. Each has an
expressive overload taking `shapes: ButtonShapes` instead of `shape`:

```kotlin
Button(
    onClick = onStart,
    shapes = ButtonDefaults.shapes(),                      // round → squarer while pressed
    modifier = Modifier.heightIn(ButtonDefaults.MediumContainerHeight),
    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
) {
    Icon(icon, null, Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)))
    Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
    Text("Başlat", style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
}
```

- Five sizes: `ExtraSmallContainerHeight` 32dp, `MinHeight` (small) 40dp, `MediumContainerHeight`
  56dp, `LargeContainerHeight` 96dp, `ExtraLargeContainerHeight` 136dp. The `…For(height)`
  helpers (`shapesFor`, `contentPaddingFor`, `iconSizeFor`, `iconSpacingFor`, `textStyleFor`)
  give the spec values for any height.
- Square variant: `ButtonShapes(shape = ButtonDefaults.squareShape, pressedShape = …)` or
  `ButtonDefaults.shapes(shape = …)`.
- Samples: `ButtonWithAnimatedShapeSample`, `SquareButtonSample`, `XSmallButtonWithIconSample`,
  `MediumButtonWithIconSample`, `LargeButtonWithIconSample`, `XLargeButtonWithIconSample`, and
  the `…WithAnimatedShapeSample` of every variant.

## Toggle buttons — `api/ToggleButton.md`, `samples/ToggleButtonSamples.kt` (stable)

`ToggleButton`, `ElevatedToggleButton`, `FilledTonalToggleButton`, `OutlinedToggleButton`: a
button with `checked`/`onCheckedChange` that morphs round → square when checked.

- Opinionated overload: `buttonSize: ToggleButtonSize` (`ExtraSmall`, `Small`, `Medium`, `Large`,
  `ExtraLarge`) plus an `icon` slot configures height, shapes, padding and icon size together.
- `ToggleButtonShapes(shape, pressedShape, checkedShape)`; defaults via
  `ToggleButtonDefaults.shapesFor(buttonSize)`.
- Samples: `ToggleButtonSample`, `ToggleButtonWithIconSample`, `ToggleButtonWithButtonSizeSample`,
  size samples `XSmall…`/`Medium…`/`Large…`/`XLarge…ToggleButtonWithIconSample`.

## Icon buttons — `api/IconButton.md`, `api/IconButtonDefaults.md`, `samples/IconButtonSamples.kt` (stable)

Standard, Filled, FilledTonal, Outlined, each as button and toggle button, each with an
expressive overload taking `shapes: IconButtonShapes` / `IconToggleButtonShapes`.

- Sizes: `IconButtonDefaults.extraSmallContainerSize()`, `smallContainerSize()`,
  `mediumContainerSize()`, `largeContainerSize()`, `extraLargeContainerSize()` — each takes a
  `widthOption` (`IconButtonWidthOption.Narrow`, `Uniform`, `Wide`). Matching icon sizes:
  `extraSmallIconSize` … `extraLargeIconSize`.
- Shapes per size: `…RoundShape`, `…SquareShape`, `…PressedShape`, `…SelectedRoundShape`,
  `…SelectedSquareShape`; or `IconButtonDefaults.shapes()` / `toggleableShapes()`.
- Vibrant colors (for use on colored containers / toolbars): `iconButtonVibrantColors()`,
  `iconToggleButtonVibrantColors()`, `outlinedIconButtonVibrantColors()`.
- Samples: `IconButtonWithAnimatedShapeSample`, `ExtraSmallNarrowSquareIconButtonsSample`,
  `MediumRoundWideIconButtonSample`, `LargeRoundUniformOutlinedIconButtonSample`,
  `…ToggleButtonWithAnimatedShapeSample` variants.

## Button groups — `api/ButtonGroup.md`, `samples/ButtonGroupSamples.kt` (stable)

`ButtonGroup(overflowIndicator, …) { clickableItem(…); toggleableItem(…); customItem(…) }` — a
row whose pressed item widens (`expandedRatio`, default `ButtonGroupDefaults.ExpandedRatio`)
while neighbours compress, and which overflows into a menu.

- Custom children opt in with `Modifier.animateWidth(interactionSource)`.
- Connected (segmented-style selection) groups: plain `Row` +
  `ButtonGroupDefaults.ConnectedSpaceBetween` + `ToggleButton`s using
  `ButtonGroupDefaults.connectedLeadingButtonShapes()`, `connectedMiddleButtonShapes()`,
  `connectedTrailingButtonShapes()`.
- Samples: `ButtonGroupSample`, `ButtonGroupWithCustomItemSample`,
  `SingleSelectConnectedButtonGroupSample`, `MultiSelectConnectedButtonGroupSample`,
  `VerticalButtonGroupSample`.

## Split buttons — `api/SplitButton.md`, `samples/SplitButtonSamples.kt` (stable)

`SplitButtonLayout(leadingButton, trailingButton)` with `SplitButtonDefaults.LeadingButton`,
`TrailingButton`, and `Tonal…`/`Elevated…`/`Outlined…` variants. The trailing button is usually
checkable and opens a menu; it morphs to a circle when checked and its icon rotates.

- Per-size shapes: `SplitButtonDefaults.leadingButtonShapesFor(height)`,
  `trailingButtonShapesFor(height)`.
- Samples: `FilledSplitButtonSample`, `SplitButtonWithDropdownMenuSample`,
  `TonalSplitButtonSample`, `ElevatedSplitButtonSample`, `OutlinedSplitButtonSample`, size samples
  `XSmall…`/`Medium…`/`Large…`/`ExtraLargeFilledSplitButtonSample`.

## FABs — `api/FloatingActionButton.md`, `samples/FloatingActionButtonSamples.kt` (stable)

- Sizes: `SmallFloatingActionButton`, `FloatingActionButton`, `MediumFloatingActionButton` (new),
  `LargeFloatingActionButton`.
- Extended FABs in three sizes: `SmallExtendedFloatingActionButton`,
  `MediumExtendedFloatingActionButton`, `LargeExtendedFloatingActionButton`, each with a
  `content` overload and a `text`/`icon`/`expanded` overload that animates collapsing to an icon.
- `Modifier.animateFloatingActionButton(visible, alignment)` scales/fades a FAB in and out
  (e.g. on scroll).
- Samples: `MediumFloatingActionButtonSample`, `AnimatedFloatingActionButtonSample`,
  `MediumAnimatedExtendedFloatingActionButtonSample`, etc.

## FAB menu — `api/FloatingActionButtonMenu.md`, `samples/FloatingActionButtonMenuSamples.kt` (stable)

```kotlin
FloatingActionButtonMenu(
    expanded = expanded,
    button = {
        ToggleFloatingActionButton(checked = expanded, onCheckedChange = { expanded = it }) {
            val icon by remember { derivedStateOf { if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add } }
            Icon(icon, null, Modifier.animateIcon({ checkedProgress }))
        }
    },
) {
    FloatingActionButtonMenuItem(onClick = { … }, icon = { Icon(…) }, text = { Text("Vardiya ekle") })
}
```

`ToggleFloatingActionButton` animates size, corner radius and color with `checkedProgress`; its
scope offers `Modifier.animateIcon`. Sample: `FloatingActionButtonMenuSample`.

## Floating toolbars — `api/FloatingToolbar.md`, `samples/FloatingToolbarSamples.kt` (stable)

`HorizontalFloatingToolbar` / `VerticalFloatingToolbar`: a pill of actions floating over content,
optionally with an attached FAB (`floatingActionButton = { FloatingToolbarDefaults.VibrantFloatingActionButton(…) }`,
`floatingActionButtonPosition`). Collapses/expands via `expanded`, hides on scroll with
`FloatingToolbarDefaults.exitAlwaysScrollBehavior(exitDirection)`.

- Colors: `standardFloatingToolbarColors()` (surface) or `vibrantFloatingToolbarColors()`
  (primary container); use vibrant icon button colors inside the vibrant one.
- Samples: `ExpandableHorizontalFloatingToolbarSample`, `OverflowingHorizontalFloatingToolbarSample`,
  `ScrollableHorizontalFloatingToolbarSample`, `HorizontalFloatingToolbarWithFabSample`,
  `CenteredHorizontalFloatingToolbarWithFabSample`, `HorizontalFloatingToolbarAsScaffoldFabSample`,
  vertical equivalents.

## App bars — `api/AppBar.md`, `api/AppBarRow.md`, `api/AppBarColumn.md`, `samples/AppBarSamples.kt` (stable)

- Top: `TopAppBar` (now with `subtitle` and `titleHorizontalAlignment`), `MediumFlexibleTopAppBar`,
  `LargeFlexibleTopAppBar` (bigger expanded title, optional subtitle), `TwoRowsTopAppBar` for
  custom two-row bars. Scroll behaviours: `TopAppBarDefaults.pinnedScrollBehavior(scrollableState)`,
  `enterAlwaysScrollBehavior(scrollableState)`, `exitUntilCollapsedScrollBehavior()`.
- Bottom: `FlexibleBottomAppBar` with `BottomAppBarDefaults.FlexibleHorizontalArrangement` /
  `FlexibleFixedHorizontalArrangement`, `exitAlwaysScrollBehavior()`.
- `AppBarRow { clickableItem(…); toggleableItem(…); customItem(…) }` / `AppBarColumn`: action
  slots that overflow into a menu (`maxItemCount`).
- Samples: `SimpleTopAppBarWithSubtitle`, `ExitUntilCollapsedMediumFlexibleTopAppBar`,
  `LargeFlexibleTopAppBarWithSubtitleAndCenterAligned`, `CustomTwoRowsTopAppBar`,
  `ExitAlwaysBottomAppBarSpacedAround`, `ExitAlwaysBottomAppBarFixedVibrant`, `BottomAppBarWithOverflow`.

## Navigation — `api/ShortNavigationBar.md`, `api/WideNavigationRail.md`, `samples/NavigationBarSamples.kt`, `samples/NavigationRailSamples.kt` (stable)

- `ShortNavigationBar` + `ShortNavigationBarItem` replace `NavigationBar` in Expressive: shorter
  (64dp), pill indicator, `iconPosition = NavigationItemIconPosition.Top` on phones or `Start`
  (horizontal items) on medium widths; `arrangement` `EqualWeight` or `Centered`.
- `WideNavigationRail` (collapsed ↔ expanded via `rememberWideNavigationRailState()`:
  `expand()`, `collapse()`, `toggle()`), `WideNavigationRailItem(railExpanded = …)`,
  `ModalWideNavigationRail` for overlays.
- Both items use `NavigationItemColors`; selected label color is `secondary`.
- Samples: `ShortNavigationBarSample`, `ShortNavigationBarWithHorizontalItemsSample`,
  `WideNavigationRailResponsiveSample`, `ModalWideNavigationRailSample`,
  `DismissibleModalWideNavigationRailSample`.

## Progress — `api/WavyProgressIndicator.md`, `api/LoadingIndicator.md`, `samples/ProgressIndicatorSamples.kt`, `samples/LoadingIndicatorSamples.kt`

| Component | Stability | Use |
| --- | --- | --- |
| `LinearWavyProgressIndicator`, `CircularWavyProgressIndicator` | stable | determinate (`progress: () -> Float`) or indeterminate; wave via `amplitude`, `wavelength`, `waveSpeed` |
| `LoadingIndicator`, `ContainedLoadingIndicator` | **exp** | morphing-shape spinner for waits under ~5s or pull-to-refresh; determinate overload with `progress` |

- Defaults: `WavyProgressIndicatorDefaults.indicatorAmplitude` (flattens near 0 and 1),
  `ProgressAnimationSpec` for animating progress, `circularIndicatorStroke`, `CircularWavelength`,
  `CircularIndicatorTrackGapSize`, `LinearTrackStopIndicatorSize`.
- Circular token values: 40dp size (48dp with wave), 4dp stroke, 1.6dp amplitude, 15dp wavelength,
  4dp gap. Thick variants: pass a bigger `Stroke` and `wavelength` (`CircularThickWavyProgressIndicatorSample`).
- `LoadingIndicatorDefaults.IndeterminateIndicatorPolygons` / `DeterminateIndicatorPolygons`
  are lists of `MaterialShapes`; pass your own list to change the morph sequence.
- Samples: `CircularWavyProgressIndicatorSample`, `IndeterminateCircularWavyProgressIndicatorSample`,
  `LinearThickWavyProgressIndicatorSample`, `LoadingIndicatorSample`,
  `ContainedLoadingIndicatorSample`, `DeterminateLoadingIndicatorSample`,
  `LoadingIndicatorPullToRefreshSample`.

## Lists — `api/ListItem.md`, `api/ListItemDefaults.md`, `samples/ListSamples.kt` (stable)

Expressive `ListItem` overloads: non-interactive, `onClick`, `selected`+`onClick`,
`checked`+`onCheckedChange`, each with `shapes: ListItemShapes` (shape morphs on press/selection),
`ListItemColors`, `ListItemElevation`. `SegmentedListItem` renders grouped rows with rounded
outer corners and small inner gaps — pass per-position shapes
(`ListItemDefaults.segmentedShapes(index, count)`).

Samples: `StandardListItems`, `SegmentedListItems`, `ClickableListItemSample`,
`SingleSelectionListItemSample`, `MultiSelectionListItemSample`,
`SingleSelectionSegmentedListItemSample`, `SegmentedListItemWithExpansionSample`.

## Menus — `api/Menu.md`, `api/MenuDefaults.md`, `samples/MenuSamples.kt` (stable, some parts exp)

Expressive menus are built from `DropdownMenuPopup` + `DropdownMenuGroup(shapes = MenuDefaults.groupShape(index, count))`
+ items: `DropdownMenuItem(shape = …)`, `CheckableDropdownMenuItem(checked, onCheckedChange, shapes = …)`,
`SelectableDropdownMenuItem(selected, onClick, shapes = …)`. Vibrant colors:
`MenuDefaults.itemVibrantColors()`, `groupVibrantContainerColor`. Samples: `GroupedMenuSample`,
`MenuWithCascadingMenusSample`, `MenuWithScrollStateSample`.

## Other expressive updates

- **Sliders** (`api/Slider.md`, `samples/SliderSamples.kt`): thicker tracks, inset icons
  (`SliderWithTrackIconsSample`), centered track (`CenteredSliderSample`), `VerticalSlider`.
- **Time pickers** (`api/TimePicker.md`, `samples/TimePickerSamples.kt`): vibrant and uncontained
  styles, `TimeScroll` scroll variant (`VibrantTimePickerScrollSample`, `UncontainedTimePickerSample`).
- **ScrollField** (`api/ScrollField.md`, `samples/ScrollFieldSamples.kt`): wheel-style number
  picker (`TimeScrollFieldSample`, `UnitScrollFieldSample`).
- **Search** (`api/SearchBar.md`): `AppBarWithSearch`, `ExpandedFullScreenContainedSearchBar`,
  `ExpandedDockedSearchBarWithGap`.
- **Carousel** (`api/carousel_Carousel.md`): `HorizontalMultiBrowseCarousel`,
  `HorizontalUncontainedCarousel`, `HorizontalCenteredHeroCarousel` (`preferredItemWidth`).
- **Chips** (`api/Chip.md`): expressive `FilterChip`/`InputChip` overloads with shape morphing.
- **Pull to refresh** (`api/pulltorefresh_PullToRefresh.md`): `PullToRefreshDefaults.LoadingIndicator`
  for the expressive indicator (exp).
- **Drag handle** (`api/DragHandle.md`): `VerticalDragHandle` for resizable panes.
