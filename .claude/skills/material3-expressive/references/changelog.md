# material3 release notes, 1.4.0-alpha01 → 1.5.0-beta01

Copied from https://developer.android.com/jetpack/androidx/releases/compose-material3 (fetched
2026-10-16). Newest first. This is where Material 3 Expressive landed in Compose: the 1.4.0-alpha
line introduced it, 1.4.0 stable shipped without most of it (only `ShortNavigationBar` and
`WideNavigationRail` made it in), and the 1.5.0 line graduated most of it to stable. Search for a component name to see how its API moved.


## 1.5.0-beta01

_October 07, 2026_


**API Changes**

- Restored TopAppBarDefaults.enterAlwaysScrollBehavior overload taking reverseLayout as deprecated with warning to maintain compatibility with 1.4. (I4a87a)
- Restored DraggableState on SliderState and retained Slider, VerticalSlider, and RangeSlider overloads with nullable onValueChange as hidden deprecated APIs to maintain binary compatibility. (Ie4aee)
- Updated PolygonShape APIs: moved CornerRounding to top-level with dp() and fraction() factories, replaced transformed() and scaledToFit() extensions with in-place mutable PolygonShape.transform(PolygonShapeTransformScope.() -> Unit) and PolygonShape.copy(). (If5970)
- HorizontalCenteredHeroCarousel's maxItemWidth parameter has been renamed to preferredItemWidth. (Ie7883)
- Added CarouselDefaults.ItemSpacing and CarouselDefaults.ContentPadding, which hold the default item spacing and content padding used by the carousel composables. (I2f36a)

## 1.5.0-alpha29

_September 23, 2026_


**API Changes**

- Source breaking change: onValueChange is now a required parameter in Slider and RangeSlider and its order in the parameter list has changed

**Bug Fixes**

- DatePicker now resolves in 1 frame by computing month navigation availability directly from the displayed month index. (I08db7)
- TimeInput now internally uses the new TextFieldState text field APIs. (Ia7492, b/353692863)
- DateInput now internally uses the new TextFieldState text field APIs. (I5ad3d, b/353692863)
- Slider and RangeSlider now handle onValueChange and onValueChangeFinished directly in their implementations instead of passing them into SliderState and RangeSliderState. (I959ac)

## 1.5.0-alpha28

_September 09, 2026_


**API Changes**

- Renamed and simplified the multi-aspect carousel API to a
- global carouselParallaxScrollEffect modifier (I46b17)
- Removed deprecated ToggleButton overloads. Added utility functions (contentPaddingFor, shapesFor) and default constants (including size) to ToggleButtonDefaults, ElevatedToggleButtonDefaults, FilledTonalToggleButtonDefaults, and OutlinedToggleButtonDefaults. (I998cf)
- Deprecated stateless Slider and RangeSlider overloads in favor of their stateful versions. Renamed activeRangeStart and activeRangeEnd in RangeSliderState to startValue and endValue, deprecating older unpromoted and experimental Slider APIs with DeprecationLevel.HIDDEN. (If8995)
- To migrate Slider, from:
- kotlin
- var value by remember { mutableFloatStateOf(0.5f) }
- Slider(value = value, onValueChange = { value = it })
- To:
- kotlin
- val state = remember { SliderState(value = 0.5f) }
- Slider(state = state)
- To migrate RangeSlider, from:
- kotlin
- var range by remember { mutableStateOf(0.2f..0.8f) }
- RangeSlider(value = range, onValueChange = { range = it })
- To:
- kotlin
- val state = remember { RangeSliderState(startValue = 0.2f, endValue = 0.8f) }
- RangeSlider(state = state)
- Add enabled boolean and disabled color params to ScrollField (Iab612)

**Bug Fixes**

- Add focus ring support for ScrollField. (If21d6)
- Make design feedback adjustments for expressive TimePicker in dial mode. (I8795e)
- Make padding adjustments for expressive TimePicker in scroll and input mode. (Iddb33)
- Updates toggle height/layout calculation based on item count for expressive TimePicker.kt in uncontained mode. (I30700)
- Fixed an issue where AM/PM controls were truncated in TimePicker when large text or font scaling was enabled. (Iaca60)
- Updated SecureTextField and OutlinedSecureTextField components (as well as deprecated BasicSecureTextField overloads) to use TextObfuscationMode.System as the default obfuscation mode instead of TextObfuscationMode.RevealLastTyped. (I76c7e)

## 1.5.0-alpha27

_August 26, 2026_


**API Changes**

- Renamed RichTimePickerDialog to VibrantTimePickerDialog, renamed richColors to vibrantColors in TimePickerDefaults and TimeInputDefaults, renamed richContainerColor and richShape to vibrantContainerColor and vibrantShape in TimePickerDialogDefaults, and renamed unselected color properties to periodSelectorContainerColor, periodSelectorContentColor, timeSelectorContainerColor, timeSelectorContentColor, and clockDialContentColor. (Iea5aa)
- Update the Rich TimePicker API to allow for an uncontained scroll/input dialog. (I2b3b6)
- Introduced SelectableDropdownMenuItem and CheckableDropdownMenuItem for selectable and checkable expressive menu items. Updated DropdownMenuItem with shape to use MenuItemColors, and added MenuDefaults.itemVibrantColors() to support vibrant MenuItemColors. (I160ca)
- Added opinionated expressive ToggleButton, ElevatedToggleButton, FilledTonalToggleButton, and OutlinedToggleButton overloads with ButtonSize and icon slot. Simplified ToggleButtonDefaults, ElevatedToggleButtonDefaults, FilledTonalToggleButtonDefaults, and OutlinedToggleButtonDefaults method names to colors() and border(). (Iab484)
- Removed deprecated overloads of pinnedScrollBehavior and enterAlwaysScrollBehavior from TopAppBarDefaults. Use the overloads that take ScrollableState instead. (Idef9d)
- Removed deprecated experimental API LocalMotionScheme from MaterialTheme. Use MaterialTheme.motionScheme instead. (I4b61d)
- Added TimeInputDefaults and TimeInputColors for TimeInput. (I7534e)

**Bug Fixes**

- Fixed an issue where ScrollDisplayModeToggle announced 'Switch to scroll mode' while in scroll mode. (I2d588)
- Fixed an issue where DropdownMenuItem without a leading icon did not align trailing icons to the end of the menu item. (Ia24ff,  b/542873381)

## 1.5.0-alpha26

_August 12, 2026_


**API Changes**

- Added ElevatedToggleButtonDefaults, FilledTonalToggleButtonDefaults, and OutlinedToggleButtonDefaults, and cleaned up non-semantic shape properties in ToggleButtonDefaults. (Ia0a85)
- Fixed a11y contrast issue for the horizontal NavigationItem's label in high contrast themes. The label's color is now specified via selectedTextColorTopIconPosition (vertical item's label color, which has not changed) and selectedTextColorStartIconPosition (horizontal item's label color, now set to match selectedIconColor). To revert this update, pass in ShortNavigationBarItemDefaults/WideNavigationRailItemDefaults.colors().copy(selectedTextColorStartIconPosition = MaterialTheme.colorScheme.secondary) (I85855, b/490910896)
- Scroll offset variables have been removed from SearchBarScrollBehavior and should now be accessed via the new class SearchBarScrollState. (Ib24e4)
- Promoted BottomAppBar and its associated methods to stable. These APIs no longer require the @ExperimentalMaterial3Api opt-in. (I42c61)
- Saved active selection mode in TimePickerState across state restorations and added initialSelection parameter to TimePickerState factory function. (Iad905)
- ExposedDropdownMenu is now an extension function on ExposedDropdownMenuBoxScope instead of a member. You may need to update your code with a new import. (Ie8a65, b/356452026)

**Bug Fixes**

- Fixed an issue where partial color customization in MenuDefaults.itemColors overwrote unprovided colors with Color.Unspecified. (Iea847)
- Fixed TalkBack announcements to read formatted time values in the scrollable TimePicker. (Ice981)
- Fixed a crash in ExposedDropdownMenu when screen height is smaller than menu height. (I6adf1)
- Fixed wrong content padding of wide navigation rail. Bottom padding is now 0. To revert this update you can set contentPadding = PaddingValues(0.dp,44.dp,0.dp,44.dp). (I572bb)
- Updated the public API surface to align with recent API review feedback. (I71aff, b/532657001)

**External Contribution**

- Fixed Material3 Slider Label not showing while dragging the slider by its thumb with a mouse. (I97a6d, b/533483487)

## 1.5.0-alpha25

_July 29, 2026_


**API Changes**

- Renamed trailingIcon parameter to trailingContent in the shape, checked, and selected DropdownMenuItem overloads while maintaining binary compatibility. (I2ecbd)
- Added new selection and container color properties to MenuItemColors, and renamed trailingIcon to trailingContent in the new colors constructor while maintaining binary compatibility. (I20b92)
- Improved ScrollField accessibility and keyboard navigation support. (If7477)
- Remove ComponentOverride APIs (I3784b)
- Deprecated 2-parameter SliderState.Saver and RangeSliderState.Saver in favor of new overloads taking steps. SliderState no longer publicly implements DraggableState. (I7c91b)
- Graduate BasicAlertDialog API from Experimental (If157c)
- Changed ButtonGroupScope in ButtonGroup to a sealed interface. Split Modifier.animateWidth into two overloads to allow omitting compressionLimit and dynamically resolving the layout direction, and changed the compressionLimit parameter type from PaddingValues to Dp. (I8ef39)
- Renamed TonalToggleButton to FilledTonalToggleButton. Added smooth border stroke animations to OutlinedToggleButton. Deprecated ToggleButtonDefaults.shapes in favor of shapesFor. (Icb433)
- Promote LinearTrackStopIndicatorSize,
- LinearIndicatorTrackGapSize, and CircularIndicatorTrackGapSize to
- stable. (I794d0)
- Deprecated SplitButtonLayout Api to use SplitButton instead (Ic9840)

**Bug Fixes**

- Update focus switching in case of error for TimePickerTextField input in TimePicker.kt. (Ieceec)
- Fixed shape of segmented list item when the list only has a single item. (I2ea1c, b/479721696)
- Update KDocs for TimePickerDialog.kt. (Ia5fb8)

## 1.5.0-alpha24

_July 15, 2026_

- Features
- Add new scroll variant for expressive TimePicker (I46f6b)
- Added a new material3-ripple library (I1b3a3), which adds support for drawing ripples that visually display focus using inset focus rings instead of an opacity layer. This new library acts as a replacement for material-ripple, and expected usage is similar:
- If you are using material3 itself, use the built-in material3 ripple configuration that integrates with the theme and uses material3-ripple internally as an implementation detail.
- If you were using material-ripple directly or want to support inset focus rings without depending on material3, use the new material3-ripple library directly.

**API Changes**

- Changed scrollbar ScrollIndicatorState parameter to be non-nullable. (Iab7c7, b/529445739)
- Re-add @ExperimentalMaterial3Api annotation to AppBarWithSearch APIs. (I0b61a)
- Promoted SearchBarState and slot-based SearchBar APIs to stable. Deprecated older SearchBar APIs using expanded/onExpandedChange. (Ia3793)

**Bug Fixes**

- Fixed a rendering delay issue where the RichTooltip and PlainTooltip carets would appear late during the entrance animation. (I874b6, b/395753083)
- Update margin values for Input/Scroll variant of TimePicker. (I83f00)
- SplitButton measure trailing button first to guarantee space. (Id3a1e)
- Fixed an accessibility issue where Switch Access users could not activate editable text fields inside ExposedDropdownMenuBox. (I0ea17)
- Fixed talkback announcement order in TextField and
- OutlinedTextField when a prefix or suffix is present. (Ie79e7)
- Update TimePicker margins and colon placement (I6bfeb)
- Fix focus order in Talkback for ScrollField. (I4ca9f, b/526870141)
- Updated Compose dependencies in Material 3 to 1.12.0-beta01. (Ie9417)
- Tooltips are now clamped to the window boundaries to prevent them from being cut off on small screens or under large font configurations. (I421ff)
- Update ScrollField with more expressive number transitions and adjust sizing/typography (Ic1614)

## 1.5.0-alpha23

_July 01, 2026_

- Features
- - expressive timepicker component  (Iaa45f)
- - Added new non-interactive variants of standard list item and segmented list item which follow Material Expressive specifications. The non-expressive version has been deprecated. (Ide4de, b/491994186)
- Behavioral Change
- - Fix issue where minimum height of expressive list items did not change based on lines of content. The feature flag isExpressiveListItemHeightBasedOnTextLinesFixEnabled has been introduced to aid migration. (Ibbb91, b/484693233)

**API Changes**

- Expressive list item APIs are no longer experimental. (I2836a)
- Remove ComponentOverride APIs (I820c4)
- Remove unused offset parameter from DropdownMenuPopup. Added the default menu properties into the menu defaults. Updated the naming for groups label from MenuDefaults.Label to MenuDefaults.DropdownMenuGroupLabel. Added disabledContainerColor into MenuDefaults.selectableItemColors and MenuDefaults.selectableItemVibrantColors. Added Modifier parameter to composable functions in MenuDefaults that return a layout. (I3d2b1)
- Added a public flingAnimationSpec getter to TopAppBarDefaults to expose the default fling animation spec. (Ic1daa)
- Graduate TopAppBar, MediumFlexibleTopAppBar,LargeTopAppBar,LargeFlexibleTopAppBar,TwoRowsTopAppBar,FlexibleBottomAppBar,FlexibleContentPadding,FlexibleBottomAppBarHeight,FlexibleHorizontalArrangement,FlexibleFixedHorizontalArrangement to non-experimental. These APIs and variables no longer require the @ExperimentalMaterial3ExpressiveApi opt-in. (Ia4af6)
- Graduate ExpandedDockedSearchBarWithGap and ExpandedFullScreenContainedSearchBar to non-experimental (Ie152d)

**Bug Fixes**

- Cleans up API information (Ia8066)
- Fix Scaffold's keyboard navigation order. (I77194, b/462577176)
- Remove requirement for compileSdk 37 (If78b4)
- Add a11y support for ScrollField component. More specifically, ScrollField now implements selectable and animates to the selected item on click with selected/unselected announcements in Talkback. (I7bdb7)
- Fix bug where there is extra bottom padding if the nav suite scaffold's content has ime padding set. (I86d9d, b/389467593)

## 1.5.0-alpha22

_June 17, 2026_


**API Changes**

- Added a public snapAnimationSpec getter to TopAppBarDefaults to expose the default snap animation spec. (Ic7b37, b/519193808)
- Add shapes to internal and private methods of TimePicker.kt API (I9d09e, b/441573791)
- Support shapes in AnimatedPane (Ie7c60, b/470517507)
- Fix contrast of AM/PM toggle buttons for TimePicker (Ifc0b7, b/339079853)
- Graduate Expressive FloatingToolbar APIs to non-experimental. (I5a4d8, b/497887216)
- Remove deprecated transformOriginState from DropdownMenuPopupPositionProvider. (I0d5b0, b/505481611)
- Updated pinnedScrollBehavior and enterAlwaysScrollBehavior to accept a ScrollableState, which automatically handles edge cases like reversed layouts and pre-scrolled content. Legacy overloads are deprecated. Migrate usages of isScrollingContentAtStart to the new overloads with scrollableState param. Promoted TopAppBarScrollBehavior and related APIs to stable. (Ib83cf, b/519193808)
- Updated pinnedScrollBehavior and enterAlwaysScrollBehavior to accept a ScrollableState, which automatically handles edge cases like reversed layouts and pre-scrolled content. Promoted TopAppBarScrollBehavior and related APIs to stable. (Ieda4b, b/405129274)
- Update DropdownMenuPositionProvider to remove MutableState from transformOrigin. Collapse MenuAnchorPosition from a sealed interface into a single class. Introduces MenuAnchorPositionScope that contains information that can be used to create custom x and y candidates. (I21f2b, b/505481611)
- Promote ButtonGroup APIs to stable. Remove deprecated experimental APIs that were introduced in a 1.5.0-alpha. (Idaf96, b/497876828)

**Bug Fixes**

- Fixed a bug where ButtonGroup's compression animation did not correctly handle asymmetric button paddings or RTL layout directions, and could crash with IllegalArgumentException under certain display densities. (I35074, b/516743181)
- Fix TimePicker text input mode error handling mechanism (If4541, b/405054265)

## 1.5.0-alpha21

_June 03, 2026_


**API Changes**

- Add shapes to timepicker component API (I57f28, b/441573791)
- Added compressionLimit to animateWidth that specifies the padding of the item being compressed which indicates the maximum compression that the item can be squished by. Deprecating the animateWidth API without this compressionLimit parameter. (I4a725, b/418822334, b/403281052)
- BottomSheet and ModalBottomSheet PartiallyExpanded anchor is no longer programmatically removed based on layout conditions. Users now explicitly control this anchor via rememberBottomSheetState. Legacy behavior for these components can be enabled via disabling isBottomSheetPartiallyExpandedDeterministicEnabled feature flag or by leveraging deprecated rememberModalBottomSheetState and rememberStandardBottomSheetState functions. isAnchoredDraggableComponentsAnchorRecoveryEnabled has now been removed from feature flags. (Ia4167, b/478210200, b/512076811)
- Reintroduces experimental tag for PullToRefreshDefaults.loadingIndicatorColor and PullToRefreshDefaults.loadingIndicatorContainerColor (Ib9d15, b/513225663)
- Updated the SelectableChipColors parameters to be public (I559e0, b/512576750)
- Add horizontalArrangement parameter to MenuItems (Ie8088, b/497891817)
- Re-mark Button's contentPaddingFor function as experimental. (I07d3e, b/500355872)
- Introduced roundedShape and tonalColors() in both
- TextFieldDefaults and OutlinedTextFieldDefaults, which
- are the visual configurations for expressive style. (Id9185, b/448728288)
- Deprecated TextFieldLabelPosition.Attached, and introduced
- Inside and Cutout subtypes, which allows more
- customizability like an OutlinedTextField with inside label
- placement. Also deprecated
- OutlinedTextFieldDefaults.contentPadding(), and introduced
- OutlinedTextFieldDefaults.contentPaddingWithLabel() and
- OutlinedTextFieldDefaults.contentPaddingWithoutLabel() to
- support both label positions. (I40f62, b/448728288)

**Bug Fixes**

- Fixed an accessibility issue in TimePicker where keyboard focus was lost when switching from hours to minutes on the dial. Improved accessibility of keyboard focus. (I7dd81, b/498361169)

**External Contribution**

- Improved BottomSheet dismiss gesture physics for a smoother experience. (I21df5)

## 1.5.0-alpha20

_May 19, 2026_


**API Changes**

- Removed shouldUsePrecisionPointerComponentSizing function (I882c3, b/498698039)
- Fixed an issue where the tooltip caret would appear on the wrong side when the tooltip flipped due to screen edges. (Idc314, b/479224210)
- Introduced rememberBottomSheetState as the unified API for BottomSheet state. Deprecated rememberModalBottomSheetState and rememberStandardBottomSheetState in favor of the new unified API. (I2724e, b/500005697)
- Remove deprecated experimental WideNavigationRail APIs (Iaadd6, b/497891040)
- Graduate SplitButton APIs to non-experimental. (I4d395, b/497876691)
- Deprecate the DropdownMenuItem APIs with checked and selected params that have supportingText as a trailing lambda. Moved supportingText to be a parameter right after trailingIcon. (Icaf79, b/503047115)

**Bug Fixes**

- Update Navigation Drawer predictive back handler to allow going back while drawer is opening instead of only when fully opened (I714ae, b/418556231)

## 1.5.0-alpha19

_May 06, 2026_


**New Features**

- Typography now supports a default font family that will be merged with provided text styles if they don't have a font family explicitly set. (I2e305, b/500356360)

**API Changes**

- Removes supportingText as a trailing lambda for DropdownMenuItem. Instead move it to right after the trailingIcon. (I5694b, b/503047115)
- Promote ToggleButtons to stable. (I8a771, b/497876827)
- Promote expressive menu APIs. Remove DropdownMenuItem since it is a deprecated experimental expressive API that was a part of a 1.5.0-alpha. (I1d556, b/497885285)
- Graduate FAB and FAB Menu APIs from Experimental Expressive (Ie509c, b/497892373)
- Promote expressive button APIs. Remove the deprecated experimental API SmallButtonContentPaddingwhich was introduced in a 1.5.0-alpha. (Iee31e, b/497873833)
- Move scaffold order APIs back to experimental (I01492, b/489424245)
- Add another Typography consturctor overload (I8b3b3, b/500356360)
- Revert MaterialShapes and LoadingIndicator promotions to stable. (I30e69, b/497876695, b/497877850)

**Bug Fixes**

- Fix bug in slider's implementation that caused padding from the inset focus rings to be applied to the track and thumb when the component is not in focus. (I16315, b/506158497)
- Fixed an issue in ModalBottomSheet where imePadding was applied unconditionally, preventing control over IME behavior via contentWindowInsets. (Ied801, b/289824811)
- [FAB] Fix crash in Modifier.animateFloatingActionButton caused by density int vs float rounding (I2d25f, b/489769219)
- Fix focus getting trapped inside the date range picker. Tabbing moves focus in and out the dates, and arrow keys move focus through the dates. (I7f3ea, b/498332749)
- Revert medium and large buttons changes for precision pointer mode, and dialog's default icon size also for precision pointer mode (I58d8c, b/496938250, b/500356640)
- Fixed keyboard focus getting trapped in non dialog date picker. Also fixed focus order of dismiss and confirm buttons not following their visual order. (Ibbe2e, b/500454457)

## 1.5.0-alpha18

_April 22, 2026_


**New Features**

- Add FilterChip, ElevatedFilterChip, and InputChip overloads with shape morphing. Add new shape, spacing, and color defaults for these overloads. (I84717, b/442678355)
- Adds inset focus ring support via an opt-in API, using the new LocalRippleThemeConfiguration composition local. (I0551a, b/282184440).

**API Changes**

- Renamed rememberWithGapSearchBarState to rememberSearchBarWithGapState. (I5f54b, b/498697243)
- RippleThemeConfiguration and LocalRippleThemeConfiguration APIs are graduated to stable, adding the ability to configure ripples to be drawn with an inset focus ring appearance, instead of the opacity-based focus indication. APIs using the material-ripple configuration APIs are deprecated. (Ide5d0, b/485893129)
- Promote WavyProgressIndicator apis (Id72a3, b/497877853)
- Promote materialExpressTheme, expressiveLightColorScheme (I40eab, b/497876844)
- BottomSheet composable has been moved to BottomSheet.kt (If7e33, b/500091309)
- Add Material3ExpressiveApi annotation that doesn't require OptIn. (Ib9f2e, b/499994043)
- Adds DropdownMenuPopupPositionProviders to DropdownMenuPopup, so developers can now configure the position of the menu is placed relative to the anchor. This allows for submenu support within Dropdown Menus. You can now create cascading submenus using new anchor-relative positionings. Pass these new positioning options into rememberDropdownMenuPopupPositionProvider to configure your menu. For a complete implementation, refer to the catalog menu sample. (Ic1ace, b/476161294)

**Bug Fixes**

- Update material3 components to support inset focus ring indications (I88006, b/498610244, b/467984300, b/498281359)
- Resolved an issue in TimePicker samples where TalkBack focus moved, allowing invalid time entry (I51690, b/498364606)

## 1.5.0-alpha17

_April 08, 2026_


**API Changes**

- Promoted TopAppBarScrollBehavior and its associated methods to stable. These APIs no longer require the @ExperimentalMaterial3Api opt-in. (Ieb2d1, b/496918628)

## 1.5.0-alpha16

_March 25, 2026_


**New Features**

- Promote Typography constructors and attrs, Added a Typography constructor that accepts a default FontFamily to be applied to all TextStyles in the scale. (I3f6f1, b/236358112)
- Promote experimental APIs for sliders to stable. (I5efb3, b/304811984)

**API Changes**

- isAnchoredDraggableComponentsAnchorRecoveryEnabled feature flag introduced for draggable components experiencing ambiguous target errors. Components include ModalBottomSheet, BottomSheetScaffold, SwipeToDismissBox and WideNavigationRail (I5b0e5, b/487941042, b/478210200, b/477038695)
- Added DropdownMenuItemTrailingLabel default function and pointer optimized UI paddings for segmented menus (I28977, b/485937388)
- Added paddings for dialogs for when ComposeMaterial3Flags.isPrecisionPointerComponentSizingEnabled is true, and added AlertDialogDefaults.IconSize for the icon. (Ib9652, b/485932651)
- Optimized BasicTextField's internal min size calculations. These are controlled by ComposeFoundationFlags#isBasicTextFieldMinSizeOptimizationEnabled, should you need to disable them. (I70ade)
- Updated parameter order in SearchBarDefaults.InputField to maintain source code compatibility. (I20e3b, b/486964336)
- Remove deprecated FilterChip and AssistChip api that have a horizontalSpacing parameter (Iec623, b/483762893)
- The isAtTop parameter in TopAppBarDefaults.pinnedScrollBehavior and TopAppBarDefaults.enterAlwaysScrollBehavior has been renamed to isAtStart (If7323, b/405129274)

**Bug Fixes**

- BottomSheet components now respect MaterialTheme.motionScheme during nested scroll and drag gestures. (I5fe22, b/452071842, b/384959324)
- Fixed icon-label padding being applied when the item has a selectedLeadingIcon that is not currently displayed (Iba820)

## 1.5.0-alpha15

_February 25, 2026_


**New Features**

- Scrim component introduced for use alongside Modal components. (I29555, b/455862212, b/372395682, b/375853129)
- Introduces standalone static sheet component. (I0ef95, b/455862212, b/350779977, b/444176963, b/336349080)

**API Changes**

- MaterialTheme has been refactored to use a single CompositionLocal for theme data. Composition locals can now be accessed through MaterialTheme.LocalMaterialTheme.current, or currentValueOf(MaterialTheme.LocalMaterialTheme) in CompositionLocalAccessorScope (Idee2e, b/483676974)
- Update default content padding of the expressive text button to correct specs. Also remove deprecation of TextButtonContentPadding and TextButtonWithIconContentPadding. (I67283)
- Introduce parameter to disable backhandler from BottomSheet (Iff81e, b/483763626)
- Graduate motion scheme from experimental (If822f, b/484003769)
- Added 'enabled' and 'threshold' parameters to PullToRefreshBox in Material3. (I5610b, b/369044003)
- Replace horizontalSpacing with horizontalArrangement in FilterChip and ElevatedFilterChip. Add horizontalArrangement defaults to FilterChipDefaults. (If3d6a, b/304853782)
- Improved KDoc for TopAppBarDefaults scroll behaviors to better explain usage with reversed and pre-scrolled content. (I247dd, b/405129274)
- Replace horizontalSpacing parameter with horizontalArrangmenet parameter. Add horizontalArrangement defaults to AssistChipDefaults. (I15cec, b/304853782)
- Add getter for shouldUsePrecisionPointerComponentSizing (I234ce)
- Added overloads to TopAppBarDefaults.enterAlwaysScrollBehavior and TopAppBarDefaults.pinnedScrollBehavior that accept an isAtTopState parameter. This allows for custom at top state detection when using the scroll behaviors. (I785b9, b/405129274)
- Add contentPadding and horizontalArrangement parameters to InputChip and ElevatedInputChip. Add horizontalArrangement and contentPadding defaults to InputChipDefaults. (Iae39c, b/304853782)
- Added precision pointer sizing and paddings for Buttons and deprecated obsolete TextButtonContentPadding val. (I6d7b6, b/479879803)
- Added rememberWithGapSearchBarState to be used with ExpandedDockedSearchBarWithGap. (Ia3361)

**Bug Fixes**

- Fixes the issue shadows are not showing for levitated panes (Ie6108, b/470517507)
- Update TextButtons to have correct material3 paddings specs. In order to opt out and revert to the old behavior you should set isTextButtonContentPaddingFixEnabled to false in your application. (I66c8e)

## 1.5.0-alpha14

_February 11, 2026_

- Workaround
- Added isAnchoredDraggableComponentsStrictOffsetCheckEnabled. This flag controls whether BottomSheetScaffold, ModalBottomSheet, SwipeToDismissBox and WideNavigation rail strictly require their internal offsets to be initialized. When disabled, the components won't place their content until the offset is initialized. This flag can help temporarily work around a bug where these components throw an exception due to the offset not being initialized. (I36870, b/477038695, b/478210200, b/471818801, b/475249572, b/475598146)

**API Changes**

- Add isTextButtonContentPaddingFixEnabled that when true makes TextButton have correct paddings specs. (Ib986e)
- Added indicatorPadding param for WideNavigationRailItem. (I3d5dc)
- Add contentPadding and horizontalArrangement parameters to SuggestionChip and ElevatedSuggestionChip. Add horizontalArrangement and ContentPadding defaults to SuggestionChipDefaults. Create ChipArrangement class. (Ida874, b/304853782)
- Update the selectable menu items to contain a parameter for supporting text. Deprecate the previous APIs that didn't include the supporting text parameter. Also add the recommended default icon sizes for the leading and trailing icons. (I89e4b, b/417731599)
- Updated the Snackbar layout to improve multi-line text alignment. To minimize UI disruption, the isSnackbarStylingFixEnabled flag has been introduced to assist with migration. Please enable this flag manually in your application, it will be removed in a future release. (I37d63, b/322866101)
- Fixed a bug in BottomSheetScaffold, ModalBottomSheet, SwipeToDismissBox and WideNavigationRail where the anchors were not recalculated in some cases. This fix is behind a feature flag, ComposeMaterial3Flags#isAnchoredDraggableComponentsInvalidationFixEnabled. (I9acb1, b/478210200)
- Add content padding param to the WideNavigationRail and ModalWideNavigationRail so that the default paddings are customizable. (I49106)

**Bug Fixes**

- Setting BottomSheetScaffold sheetPeekHeight to 0 disables partiallyExpanded anchor. PartiallyExpanded anchor is preserved in its first layout pass to allow for layout calculation. (Ia33a4, b/465158677)
- SheetState#targetValue prefers it's current anchor if the current offset is valid. This prevents initialValue from immediately updating. (Ied2c4, b/477279704)
- Fix WideNavigationRailItem's icon not being vertically centered if item height changes. (Ib8c83)

## 1.5.0-alpha13

_January 28, 2026_


**API Changes**

- Added support for search bar animationSpecForContentExpand and animationSpecForContentCollapse. (I033a5)

**Bug Fixes**

- Fixed an issue where the content of an expanded SearchBar did not use a theme-aware color (I878e0, b/379441904)
- Fixed a bug where BottomSheetScaffold would invoke SheetState's confirmValueChange callback with incorrect values when passing a drag handle to BottomSheetScaffold. Please note that confirmValueChange should only be used to veto state changes. Use a snapshotFlow to observe state changes. (Ice9ee, b/465824174, b/477031833)

## 1.5.0-alpha12

_January 14, 2026_


**API Changes**

- Updates TopAppBarDefaults (enterAlways and pinned behaviors) to support lazyListState, scrollState, and reverseScrolling. This fixes layout direction issues and correctly handles initially scrolled content. (I9d5c2, b/262234750)
- Add contentPadding and horizontalSpacing parameters to AssistChip and ElevatedAssistChip. Add HorizontalSpacing and ContentPadding defaults to AssistChipDefaults. (I2ac90, b/304853782)
- The DatePicker APIs using Java Time classes are no longer tagged as experimental. (I5039c, b/457537971)

**Bug Fixes**

- Fix a DatePicker date-formatting crash on API 23 (I67a94, b/452713222)
- Fixed visual alignment bug in the fancy animated indicator sample when used with scrollable tab rows. (Iae0f3, b/466790304)
- Fixed an issue where the TimePicker's AM/PM selector did not use text style defined by Material Design specification. (Ie908a, b/469788786)
- Fixed a crash in HorizontalFloatingToolbar that could occur in landscape mode when the on-screen keyboard was displayed. (Ia13c1, b/466692323)
- Fixed an issue where the TopAppBar title could overlap with its actions when no navigationIcon was provided. The title is now correctly constrained to its available space. (I2ba97, b/428697836)
- Support RTL with pane expansion anchors (I0770b, b/467775639)

## 1.5.0-alpha11

_December 17, 2025_


**New Features**

- Added ExpandedFullScreenContainedSearchBar. (Ie472d,b/454658635)
- Added support to create multi-aspect carousels using lazy grids. (I2b109, b/462137656)
- Material expressive list items are now available, supporting interactions and segmented styling. Additional color fields have been added to ListItemColors. (I54057, b/441569230)
- Multi-browse and uncontained carousel APIs are now stable. (I7a558, b/401537465)
- Add contentPadding and horizontalSpacing parameters to FilterChip and ElevatedFilterChip. Add HorizontalSpacing and ContentPadding defaults to FilterChipDefaults. (Iec6e3, b/455596578)

**Bug Fixes**

- Setting BottomSheetScaffold sheetPeekHeight to 0 disables partiallyExpanded anchor. (I52dc9, b/465158677)

## 1.5.0-alpha10

_December 03, 2025_


**New Features**

- Added multi-aspect carousel (I15247, b/411232854)

**API Changes**

- Feature flag isCheckboxStylingFixEnabled is now provided through ComposeMaterial3Flags object. (I97baf, b/457504316)
- The disabledCheckmarkColor parameter in CheckboxColors has been moved to the end of the parameter list to ensure binary compatibility. (I734d8, b/457504316)

**Bug Fixes**

- Revert all arrow keys changing slider values to fix focus getting trapped in certain devices without a tab key. (I154dd, b/460912699)

## 1.5.0-alpha09

_November 19, 2025_


**New Features**

- Adding expressive menu updates. This includes a new toggleable menu item, selectable menu item, menu groups, and menu popup. It also includes new expressive menu default values in MenuDefaults. (I5cdd4, b/417731597, b/448646125, b/448646896, b/448646891)
- The Modifier.minimumInteractiveComponentSize now provides two new public AlignmentLines: MinimumInteractiveTopAlignmentLine and MinimumInteractiveLeftAlignmentLine. These lines mark the visual edges of a component before extra space is added to meet minimum touch target requirements. (I7f485, b/458124197)
- Added ExpandedDockedSearchBarWithGap. (Idb7f8)

**API Changes**

- Add checks to enable precision pointer component sizing. (I8108d)
- Add Modifier.align to ButtonGroupScope (I03890, b/416590906)
- Remove deprecated experimental ModalBottomSheet APIs that have been in at least one stable release. (Ifbe1d, b/449757604)
- New ButtonGroup overload with verticalAlignment parameter. (I23a37, b/416590906)
- Removed deprecated hidden experimental APIs that have been in at least one stable release. (I4f68d, b/449754465, b/449749933, b/401311419, b/449749928, b/449756019)

**Bug Fixes**

- Add tooltip to floating toolbar's default overflow button. (Ife952, b/422781172)
- Fix WideNavigationRail's indicator being cut if its set to a larger width via a larger label size. (I9d740, b/444728723)
- Update split button trailing buttons to center the icon optically depending on the given shape even in RTL. (Icab82)

## 1.5.0-alpha08

_November 05, 2025_


**API Changes**

- hourInput and minuteInput properties added to the TimePickerState interface. These properties are intended to hold the raw, unvalidated input from the user. The existing hour and minute properties remain and represent the last known valid time. (I09d74, b/394612017)

**Bug Fixes**

- A bug where Switch could not be used with ReusableContent without animating has been fixed. (I61093, b/455909150)

## 1.5.0-alpha07

_October 22, 2025_


**API Changes**

- Enhanced SearchBar.InputField with a keyboardOptions parameter, allowing for optimized text input by specifying keyboard options and lineLimits parameter allowing to specify text wrapping scrolling (Id08a4, b/416991049)

**Bug Fixes**

- Fixed a bug in Slider where press interactions were not emitted on touch down. This change ensures that a PressInteraction.Press is emitted immediately on awaitFirstDown, providing instant visual feedback. (If9e25, b/308501482)
- Fixed an issue with the Tooltip caret not changing directions when scrolling. The TooltipBox now triggers a recomposition whenever the tooltip popup switches sides, ensuring the caret's direction is updated correctly. (I5ad1e, b/438875827)

## 1.5.0-alpha06

_October 08, 2025_


**API Changes**

- Added scrolled colors for AppBarWithSearch. In addition, the container color of a search bar input field is now transparent by default. The default container color of the search bar itself remains unchanged. (I4fe32)
- Added a default overflow indicator composable to FloatingToolbar. (I6a6f8, b/415833723)
- Update isExpanded to isShowing for menu APIs in button group. (I86309, b/412419514)
- Updated initialIsExpanded to initialIsShowing and added a default overflow indicator composable in ButtonGroupDefaults. (I6e67c, b/412419514)
- Remove the drag-to-resize feature from the public API surface (Ic85ba, b/437953743, b/442636084)

**Bug Fixes**

- Fix focus order of keys keyboard navigation, and also fix it for RTL. (Ibba27, b/422220597)
- Fix date picker's year selection grid keyboard navigation. (I02363, b/422425720, b/446814683)
- Fixed keyboard navigation for date selection grid in date pickers. (I594ef, b/422220597, b/422223115)
- Fixed offscreen toolbar receiving keyboard focus. (I01a73, b/422786812)
- Fixed RangeSlider and Slider keyboard navigation. (Ib6bcf, b/424845268, b/422942624)

## 1.5.0-alpha04

_September 10, 2025_


**New Features**

- Added Text composable overloads accepting a ColorProducer lambda that enables efficient color changes without triggering a full recomposition. (I9ff25, b/407055128)

**API Changes**

- Rename Scrim() to LevitatedPaneScrim() and hide properties of Levitated and Reflowed classes. (I090e1, b/427953101)
- Hide calculatePosture() API as internal (Ie7227, b/424442112)
- Expose PaneScaffoldHorizontalOrder as a sealed public API (Ia4ebe)
- Mark window size and posture related APIs as experimental. (I4ee96)

**Bug Fixes**

- Fixed keyboard navigation order so that focus goes from the fab button to the first item at the top. (Icaaa1, b/422762939)
- Improve Navigation Drawer keyboard a11y: drawer is no longer focusable when dismissed, it can be closed via esc key, and its content receives focus when opened. (Idb995, b/422793544, b/422793651, b/422797424)
- Time picker's clock face now responds to keyboard navigation/input. (I9d5d9, b/425710631)
- Use new initial anchor when pane expansion anchor list changes (I91cd1, b/438829477)

## 1.5.0-alpha03

_August 27, 2025_

- Behavior Change
- The size of the checkbox and indicator colors has been adjusted to align with the specification. To minimize UI disruption, the flag isCheckboxStylingFixEnabled has been added to help migration. Please enable it manually in your apps. This flag will eventually be removed in a future version. (I5bcd3, b/304300693)

**API Changes**

- Added contentPadding parameter for TopAppBar (Ia5fea)
- Add component override for nav suite scaffold (I85312)

**Bug Fixes**

- The containerColor parameter in TimePickerDialog is now correctly applied, allowing customization of the dialog's background color. (I47f89, b/403183883)
- Modal navigation rail no longer loses focus when collapsing, and now closes in response to the ESC key. (4255257)
- Snackbar now has correct keyboard focus order for action and dismiss buttons, dismiss button also supports displaying a tooltip. (11fa13d)

## 1.5.0-alpha02

_August 13, 2025_


**API Changes**

- In PullToRefreshDefaults, renamed shape to indicatorShape and containerColor to indicatorContainerColor and added indicatorMaxDistance for indicator use. (Ib6cbe)
- Remove deprecation tag from PullToRefreshDefaults.indicatorColor (Iaaee2)
- Added AppBarWithSearch, replacing TopSearchBar and supporting navigation/action icons. (I213a5)

**Bug Fixes**

- Fix bug where backpress incorrectly updates drawer offset. (I85624, b/427778135)
- SwipeToDismissBox now falls back to a settled targetValue if no other anchors exist. BottomSheetScaffold now falls back to an Expanded targetValue if no other anchors exist. (I73d5e, b/428856426)
- Fixed button padding in AlertDialog when stacked vertically. (Ia2118)
- [FAB] Fix bug where FAB is still clickable after Modifier.animateFloatingActionButton hides it (I8ea6c, b/430336834)
- [Slider] Fixed keyboard navigation for Slider (I3a405)

## 1.5.0-alpha01

_July 30, 2025_


**New Features**

- Add more position provider for tooltips so now developers can control if tooltip is placed above, below, left, or right of the anchor. Add an API that takes in a Shape for carets, so more custom shapes can be provided. (Ie513c)

**API Changes**

- TextFieldColors methods to calculate component colors based on state are now public. (I03165)
- Suspend annotation has been removed from onDismiss callback. (Ie3166)
- DatePickerState.getDisplayedMonth(): YearMonth? and DateRangePickerState.getDisplayedMonth(): YearMonth? were updated to return non nullable value. (Ice09c, b/427952972)
- Remove ModalWideNavigationRailDefaults, move its contents to WideNavigationRailDefaults and rename its shape related names. (Ic5e61)
- WideNavigationRailItem’s colors and copy deprecated functions should be level Hidden. (Id7e82)
- Change level of deprecated WideNavigationRail apis to Warning and make them experimental. (I89085)

**Bug Fixes**

- Ensures DatePicker respects its own locale for number formatting. Previously, if DatePicker was configured for an Arabic locale, it could incorrectly render Latin digits if the device's system locale used a different numbering system. (Iccf76, b/432616196)
- Fix a LinearProgressIndicator issue that did not render a stop indicator correctly in RTL layouts. (I0734c)
- PrimaryScrollableTabRow and SecondaryScrollableTabRow divider now extends to the full screen size, even when tab content does not extend to the end of the screen. (Ic1e9c, b/261741384)

## 1.4.0

_September 24, 2025_


**Important changes since 1.3.0**


**Library change announcements**

- The androidx.compose.material.icons library is no longer recommended for displaying Material Icons in Compose, as Material Symbols are the new way forward. We have stopped publishing updates to this library and it has been removed from the latest Material 3 library release, you can still manually reference it if you cannot migrate yet.
- Instead, we recommend downloading a Vector Drawable XML file from the Android tab of https://fonts.google.com/icons to get access to the latest styled icons: Material Symbols.
- Why are we not recommending the library anymore? The icons library ("Material Icons") have been superseded by the newer look of Material Symbols and we've seen that the library can increase the build time of your apps significantly as it includes all the various icons that may not be needed.
- For more information: https://developer.android.com/develop/ui/compose/graphics/images/material

**Behavior Changes**

- This library no longer adds a dependency to material-icons-core so if your project relied on that, you will have to explicitly add that dependency in your build.gradle[.kts] files. (I735ff, b/349894318)
- NavigationBarItem and NavigationRailItem's active label color change from onSurface to secondary in order to improve usability, color contrast and improve coherence within the system (Ibc297), to revert to the previous behavior, copy the default colors and change the selectedTextColor to MaterialTheme.colorScheme.onSurface.
- Material 3 components are now using the new MotionScheme to define their motion. (Ie0f93)
- Indeterminate circular Progress Indicator motion changes (I3c07e)

**New Material Design 3 Components**

- HorizontalCenteredHeroCarousel
- VerticalDragHandle
- Secure text fields for password entry fields
- SecureTextField
- OutlinedSecureTextField

**Enhancements to existing Material Design 3 components**

- Text now supports autoSize
- Text with string
- Text with annotatedString
- Added Material 3 decorators for BasicTextField2
- Textfield
- OutlineTextfield
- Added an TimePickerDialog API that can be used for TimePicker, TimeInput or to have a switchable version.
- New search bar APIs:
- Collapsed search bars and expanded search "views" are now separate composables:
- SearchBar represents a search bar in the collapsed state.
- ExpandedFullScreenSearchBar and ExpandedDockedSearchBar represent the search bar in the expanded state. These open in a new window.
- SearchBarState to control the state of the search bar

**Promoted experimental APIs to stable!**


**Performance improvements**


## 1.4.0-rc01

_September 10, 2025_


## 1.4.0-beta03

_August 27, 2025_


## 1.4.0-beta02

_August 13, 2025_

- Dependency Changes
- Remove graphics-shapes dependency (I219e0, b/436230765)

**API Changes**

- In PullToRefreshDefaults, renamed shape to indicatorShape and containerColor to indicatorContainerColor and added indicatorMaxDistance for indicator use. (Ib6cbe)
- Remove deprecation tag from PullToRefreshDefaults.indicatorColor (Iaaee2)
- BasicAlertDialogOverrideScope was accidentally promoted to stable and had its experimental annotation removed in aosp/3701846. Marking it as internal. It will stay as public experimental in 1.5.0-alpha (I9182a)

**Bug Fixes**

- Fix bug where backpress incorrectly updates drawer offset. (I85624, b/427778135)

## 1.4.0-beta01

_July 30, 2025_

- Breaking changes
- All public APIs tagged with ExperimentalMaterial3ExpressiveApi or ExperimentalMaterial3ComponentOverrideApi have been removed, please switch to 1.5.0-alpha to continue enjoying these features. (Ie4ae0)
- Please expect the following stabilized pullToRefresh APIs to be renamed in the next release. (Ib6cbe)

**New Features**

- Tooltip now supports custom caret shape and custom caret position (above, below, left, or right of the anchor). (Ie513c)

**API Changes**

- Suspend annotation has been removed from onDismiss callback. (Ie3166)
- DatePickerState.getDisplayedMonth(): YearMonth? and DateRangePickerState.getDisplayedMonth(): YearMonth? were updated to return non nullable value. (Ice09c, b/427952972)
- Remove ModalWideNavigationRailDefaults, move its contents to WideNavigationRailDefaults and rename its shape related names. (Ic5e61)

**Bug Fixes**

- Ensures DatePicker respects its own locale for number formatting. Previously, if DatePicker was configured for an Arabic locale, it could incorrectly render Latin digits if the device's system locale used a different numbering system. (Iccf76, b/432616196)
- Fix a LinearProgressIndicator issue that did not render a stop indicator correctly in RTL layouts. (I0734c)
- PrimaryScrollableTabRow and SecondaryScrollableTabRow divider now extends to the full screen size, even when tab content does not extend to the end of the screen. (Ic1e9c, b/261741384)

## 1.4.0-alpha18

_July 16, 2025_


**New Features**

- ModalBottomSheetProperties now provides the option to disable dismissRequest from a scrim click. (I8e715)
- Add trackCornerSize support to RangeSlider's Track. (Iec529)

**API Changes**

- Made railExpanded a required param for WideNavigationRailItem, added modalContentColor for WideNavigationRailColors and moved WideNavigationRailDefaults.modalContainerShape to ModalWideNavigationRailDefaults.containerShape. (Id60c5)
- Move WideNavigationRail’s new colors function param to be the last one, and deprecate the old function. (Iac7f7)
- PullToRefresh is now an stable API (I18537)
- Updates to the DatePickers API. We removed the default null end date for DateRangePicker and made it mandatory to pass in. We also clarified the midnight UTC usage of the date representation in the API documentation, and marked the latest getters and setters that use the java.time APIs as experimental for now. (I121b3, b/427952972)
- Graduate the Icon with tint: ColorProducer param API to non-experimental. (Ia0133)
- Removed the experimental annotations from DatePicker, DateRangePicker, and their supporting states, classes, and types. (I0e4e0, b/391848485)
- Changed default focusable value to false to fix a11y focus, and added hasAction param to TooltipBox. (I62998)
- Graduate experimental WideNavigationRail, ShortNavigationBar and NavigationItem APIs. (I3ca3c)
- Remove the @ExperimentalMaterial3Api annotations from some of the top app bar functions, supporting classes, and objects. (I0a9b7)

## 1.4.0-alpha17

_July 2, 2025_


**New Features**

- Update button group's animation to animate to 75% of the animation when clicked instead of pressed. (593942a, b/423596967)

**Bug Fixes**

- Fixed an issue where a carousel item could, in some cases, exceed the large element size during layout calculations, that cut the whole widget. (I1c3d7, b/397489534)
- Fixed jumping past beforeContentPadding when scrolling carousels with start and end shift offsets that overlapped. (c3d5f3a, b/420618979)
- Update SwipeToDismiss’ enabled behavior to observe settled state instead of current. Restores existing behavior where anchor behavior is only disabled when the component is settled at a dismissed state. (3844e07, b/425006844)
- ExposedDropdownMenu’s popup menu can now be opened via keyboard input. Also fix the menu is now reachable via keyboard for the editable variation. (46ead03)

## 1.4.0-alpha16

_June 18, 2025_


**New Features**

- Added a center aligned hero carousel component (I6f6d3)

**API Changes**

- Implement XR overrides vertical toolbar (Ia1604)
- Added programmatic scroll functions to CarouselState (I12f8e)
- Add ComponentOverride for ModalWideNavigationRail (I4f440)
- Add ComponentOverride for WideNavigationRail (I6354f)
- Create ComponentOverride for HorizontalFloatingToolbar (I51116)
- Create ComponentOverride for ShortNavigationBar (I30e24)
- SwipeToDismissBoxState references to confirmValueChange have been marked deprecated. Users should instead leverage SwipeToDismissBox API onDismissed callback. (Iee780)
- Added userScrollEnabled parameter to Carousel composables. (I1d4d2)
- Carousel's currentItem can now be observed from CarouselState. (Ie87e9)

**Bug Fixes**

- Fix an issue where an arbitrary shape on a FAB that is passed to a FloatingToolbar that was not applying its shadow correctly. (Icdcc9, b/423336922)
- All Carousels now use the Carousel semantic role by default. (I7af12)
- Fixed incorrect thumb movement when the slider state was updated via LaunchedEffect (Id9f31, b/302774166)

## 1.4.0-alpha15

_May 20, 2025_


**API Changes**

- Added MotionTheme.LocalMotionScheme, composition local. This allows access to the theme motion scheme from CompositionLocalConsumerModifierNodes, with currentValueOf(MotionTheme.LocalMotionScheme). (I014b1)
- ColorScheme constructor without Fixed color roles is now deprecated. Please migrate to constructor that includes fixed roles. (Iad0ee)
- M3 Text component now supports TextAutoSize. (I7f524)
- Add api for constructing split button size variants and add samples. (Ice30b)

**Bug Fixes**

- Fixed an issue where Snackbar and FloatingActionButton were incorrectly positioned when edge-to-edge was enabled. Scaffold now correctly applies horizontal insets to these components. (Ib7c30, b/244400727)
- Fix the BottomSheet motion from fully expanded to partially expanded. The BottomSheet now uses the MotionScheme's fast-effect when hiding or collapsing, and default-spatial when expanding. (Ifa46f, b/416063171)
- ColorScheme constructor without surface container roles has been marked hidden and no longer recommended for use. (Ia7237)
- Removed a CircularWavyProgressIndicator Size.minDimension > 0 requirement, which used to throw an exception, to allow more flexibility. (Ic9418, b/377531195)
- Hyperlinks in Text(AnnotatedString) now have Material styling by default. (I78288, b/339843816)
- Fix an issue where the toShape() function in the RoundedPolygon is caching a Path and causing an issue when multiple createOutline calls are made with different sizes. (I4026d)

## 1.4.0-alpha14

_May 7, 2025_


**API Changes**

- Added a new AppBarColumn composable working in the same fashion as AppBarRow but for content laid out in a column. (Iaf6bd)
- Added max items to AppBarRow, this is necessary to correctly implement the material spec for top app bars. (I92ce4)
- Added a method in TooltipScope to obtain the layoutCoordinates of the anchor. Deprecating the drawCaret method because developers can use this new method to obtain the anchor bounds layoutCoordinates and create an appropriate shape that contains a caret. (Ia2e12, b/329470609)
- Added java.time support for Date Pickers: Introduced rememberDatePickerState/rememberDateRangePickerState overloads that accept java.time objects (e.g., initial LocalDate, YearMonth). Also added extension functions on the state objects to get/set values using types like LocalDate and YearMonth. Requires API 26+ or desugaring. (I70f29, b/266202516, b/281859606)
- Add CenteredTrack composable allowing using a Slider with a track that starts from the center. (I5b1d6)

**Bug Fixes**

- Enhanced the performance of Wavy Progress Indicators: Linear types now load ~8.5% faster with ~11% fewer allocations, while Circular types see a substantial ~47% speed boost and ~39% reduced allocations. (I595d8)

## 1.4.0-alpha13

_April 23, 2025_


**API Changes**

- isAppearanceLightStatusBars and isAppearanceLightNavigationBars reintroduced to ModalBottomSheetProperties as android only params. (Id4bc0)
- Add an AppBarRow composable, that handles overflow of items that would fit outside its bounds. (I742bd)
- Update the the DatePicker and DateRangePicker API for requesting a focus when switching to a date-input mode. We've replaced the requestFocus boolean parameter with an optional focusRequester param that takes a FocusRequester. (I14b69)
- Remove modifier and interaction source from the default clickable and toggleable items. Have it as an implementation detail instead. (I95ed6)
- Updated ButtonGroup to overflow into a dropdown menu when there are too many buttons to fit on the screen. (I7b88b)

**Bug Fixes**

- Bottomsheet now consumes top insets when smaller than current offset. This allows users to provide top insets for expanded behavior. BottomSheetDefaults.windowInsets now includes WindowInsets.safeDrawing.Top. (I0ab67, b/321877275, b/336962418, b/342093067)
- Enabled enter/exit animations (fade/slide) for Chip leading icons/avatars and trailing icons when they are added or removed. This primarily benefits selectable Chips (Filter, Input) but applies generally. (I9af21)
- Fix the broken RTL Slider behavior caused by LookaheadScope. (Ieb152, b/408118041)
- Fix the Bottom AppBar crash related to scrolling with a hidden system UI. (Ic6140, b/405996228)

## 1.4.0-alpha12

_April 9, 2025_


**API Changes**

- Removed android specific parameters from ModalBottomSheetProperties (Iab97f, b/362539765)
- Introduce overloading functions for drag handle size defaults (I0ed0d)
- Add new NavigationSuiteScaffold and NavigationSuiteScaffoldLayout functions to support new layout types and add support for an optional primary action content. (Ib262a, b/353144478)
- Deprecate Modifier.weight with fill parameter in ButtonGroup. Adding a version without fill. (Id32bb)
- Rename xSmall- and xLarge- IconButton component defaults to spell out extra. (Ib6e0f)
- Add new NavigationSuite, NavigationSuiteItem and NavigationSuiteColors functions to support new layout types. (I203d6)
- Add new NavigationSuiteTypes and add new navigationSuiteType function that include those layout choices (If68f9)

**Bug Fixes**

- ColorScheme.contentColorFor now maps surfaceDim to onSurface. (I8891a)
- Updated DateInputTextField implementation to address an issue where the input field did not reflect programmatically set date. (I6c8d1, b/401143451)
- Fixed the talkback focus order for the navigation rail and wide navigation rail (I6cf6f, b/407048224)

## 1.4.0-alpha11

_March 26, 2025_


**API Changes**

- Update capitalization for composable properties in default objects for button and toggle button. Add methods in ToggleButtonDefaults and ButtonDefaults for recommended content padding, shapes, icon size, icon spacing, and text style depending on container height. (Iea69e)
- Removed deprecated experimental functions and constants from the FloatingToolbar. (I8f339)
- Elevation components now share a common interface and have public constructors and properties. (Ibb172)
- Adding animateWidth modifier in ButtonGroupScope that will need to be used with button group's children to correctly animate the children. (Ia3bb6)

**Bug Fixes**

- Fixed Floating Toolbar padding to ensure visual balance during collapse, and improved flexibility for larger content. (I06c00)

## 1.4.0-alpha10

_March 12, 2025_


**API Changes**

- TabRowDefaults.tabIndicatorOffset has been deprecated in favor of TabIndicatorScope.tabIndicatorOffset. (Ib36b3)
- Fix typo for small content padding in button defaults. (I19bfe)
- TabRow and ScrollableTabRow are deprecated in favor of Primary and Secondary variants of each. Primary and Secondary tab rows are more performant and accurate to spec. (I918e2)
- Added rememberSliderState and rememberRangeSliderState. (I8e384)
- Added shouldAutoSnap to SliderState to control the auto-snapping mechanism, disabling it may be useful for custom animations. (I07745)
- Scrollable tab row's minimum tab width is now a parameter. Primary and Secondary tab row variants are no longer experimental. (If6f15, b/226665301)
- Made the class ExitAlwaysFloatingToolbarScrollBehavior public allowing creation without composition. (Ibf31c)

## 1.4.0-alpha09

_February 26, 2025_


**API Changes**

- Rename DragHandle’s setting fields in the default state; separate pressed and dragged states; make the default sizes be public. (I787b9)
- Updates to the TowRowsTopAppBar API. Removed the expanded lambda parameter and added separate parameters for the expanded and collapsed heights. (Idd677, b/306697446, b/229134133, b/268068946)

**Bug Fixes**

- The bottom app bars now observe the touch exploration service (e.g., TalkBack) and keep them visible whenever the service is on. (I4b34d)

## 1.4.0-alpha08

_February 12, 2025_


**New Features**

- Added an TimePickerDialog API that can be used for TimePicker, TimeInput or to have a switchable version. (Id2d83)
- New search bar APIs:
- Collapsed search bars and expanded search "views" are now separate composables.
- SearchBar represents a search bar in the collapsed state.
- ExpandedFullScreenSearchBar and ExpandedDockedSearchBar represent the search bar in the expanded state. These open in a new window.
- SearchBarState to control the state of the search bar
- TopSearchBar to add insets handling and scroll behavior
- New overload of InputField which uses SearchBarState (Ie0723, b/261496232, b/283311462, b/350916229, b/352872248)

**API Changes**

- Support corner shape morphing of icon buttons on press. (I21843)
- Text field decorator/decoration box APIs are no longer experimental. (I31d95)
- Added samples and defaults for the connected variant for ButtonGroup. (I5c8ce)
- Make state param the last one in NavigationSuiteScaffold and NavigationSuiteScaffoldLayout. (I9cc7b)

**Bug Fixes**

- Added custom accessibility actions to the FloatingToolbars so accessibility services can now expand or collapse all FloatingToolbar variations. The FAB-equipped version applies this to the FAB, while the FAB-less version applies it to the main content. (I26420)
- The FloatingToolbars now observe the touch exploration service (e.g., TalkBack) and keep the toolbar expanded and visible whenever the service is on. (I02172)
- Fixed a crash at the progress and loading indicators in case a Float.NaN is passed in as a progress. (I4fa96, b/352364576)

## 1.4.0-alpha07

_January 29, 2025_


**New Features**

- Added more flexibility in customizing the appearance of Checkboxes with a new API. This API provides Stroke parameters that allow for greater control over how the checkmark and the checkbox-outline are rendered. (I65a88)
- Added TwoRowsTopAppBar API, a more customizable alternative to Medium and Large app bars for creating two-row top app bars. Developers now have control over the content of titles and subtitles in both collapsed and expanded states, with customizable heights for each state. (I0be3c, b/306697446, b/229134133, b/268068946)

**API Changes**

- Renamed an experimental BottomAppBar variation to FlexibleBottomAppBar, providing more control over content arrangement and height. (Iaa448)
- Update naming for properties in Button Group default object to be CapitalCase. Update the name of animateFraction to be more precise to the behavior that it's representing. (I545cd)
- Adding overloads for common buttons that allow for animated shapes on press interaction. Added cached button shapes. (I5ec20)
- You can now control how a FloatingToolbar with FAB reacts to scrolling by providing a FloatingToolbarScrollBehavior. For  toolbars positioned along a center edge (like top or bottom center), we recommend using a scroll behavior to hide the entire component on scroll for a cleaner look. This also prevents the FAB from becoming off-center, which could happen when using the expanded flag for collapsing. (I33f67)
- Updates the experimental TopAppBar APIs to use Alignment.Horizontal instead of a custom TopAppBarTitleAlignment when setting the alignment of the title and subtitle. (I70ca2)
- SliderState#onValueChange is now public to give more control to the user (I104eb)
- Introduce NavigationSuiteScaffoldState to allow for animation of the navigation component. Also introduce overloads of NavigationSuiteScaffold and NavigationSuiteScaffoldLayout that have a state param. (I6a8c9, b/328674235)

**Bug Fixes**

- Move IconButtonColors and IconToggleButtonColors classes from IconButtonDefaults.kt back to IconButton.kt. (I3c233)

**External Contribution**

- SliderState#isDragging is now public. (I8458a)

## 1.4.0-alpha06

_January 15, 2025_

- Behavior Change
- Added displayCutout to the group of insets that Material components take into account by default, to avoid content overlapping with the display cutout. This is a behavior change that will impact how inset-aware components behave around a display cutout. This includes the default value of the WindowInsets parameter for inset-aware Material 3 components, and the WindowInsets values provided in the component Defaults objects for both Material 2 and Material 3. If this change causes undesirable behavior, manually specify the WindowInsets parameter on a per-component basis. (I43ee9, b/362508045)

**API Changes**

- Updates to the TopAppBar API. Rename the medium and large top app bars with a subtitle to *FlexibleTopAppBar. Unified the color variations into a single TopAppBarDefault.topAppBarColors() function, and added a subtitle color parameter to be applied for a provided subtitle Composable (I41b65)
- Remove WideNavigationRailArrangement API in favor of Arrangement.Vertical (Id0341)
- Add a new VerticalSlider @Composable. (I2bfba)
- Add a new Track @Composable that allows specifying custom external track corners and track icons. (I436a4)
- Update the floating toolbar APIs to use the FloatingToolbarColors object instead of a single container color. (I9a054)
- Add and use new experimental annotation ExperimentalMaterial3ComponentOverrideApi (Ia1eaf)
- Modifier.indicatorLine now takes a shape for the text field to handle clipping. (I8c5f3, b/380704151)
- Rename the experimental FloatingAppBar functions to FloatingToolbar (I1dbf8)
- Added a new floatingToolbarVerticalNestedScroll that can be attached to a scrollable container to update the floating toolbar expansion state based on a scroll motion that crosses a threshold. (I6d65f)
- Introducing a new API for creating a floating toolbar with an attached Floating Action Button (FAB). The API provides flexible customization options, allowing you to arrange the toolbar horizontally or vertically and place the FAB at the start or end of the toolbar. (I9e350)

**Bug Fixes**

- Fix a Snackbar accessibility issue that caused it to announce itself on dismiss when TalkBack is on. (/I9db53)

**External Contribution**

- Commonized BasicTooltip in foundation and BasicTooltip/Tooltip in material3. (Ifc2e6)

## 1.4.0-alpha05

_December 12, 2024_


**API Changes**

- Replace type in WideNavigationRailState.current/target value for an enum for better readability (I2d6ab)
- SplitButton change Shape api from data class to class for binary compatibility. (I53812)
- Added a tooltip API that has a onDismissRequest parameter so makers can decide what happens when a user clicks outside of the tooltip. Deprecated the old API that doesn't contain this new parameter. (I99aef)
- Change naming of opticalCentering to horizontalCenterOptically. Making the modifier internal. Replace padding parameter with max start and end offset. (I0b904)
- Change WideNavigationRailState to have current/target value, remove enums in favor of boolean. (Idfa29, b/356039090)
- Add NavigationBarItemComponentOverride. (I3a06a)

**Bug Fixes**

- Fix the top and bottom app-bar behaviors to reliably change color when content is scrolled all the way. (Idc4e8, b/293665988)
- Scroll behavior functions for top and bottom app bars now return a remembered behavior to perform better across recompositions. (I0fdbe, b/207957336)
- Modifier pararemeter is now applied after internal modifiers such as dragging behavior and semantics, instead of before. This affects the ordering in which modifiers are applied. (I8d83f)
- Expanded bottom sheet remains expanded on size change. (I2870b, b/324934884)
- Correctly route status and navigation bar flags for windows properties. (Ie674d, b/362539765)
- Move modifier parameter to the scaffolds root, as documented. This aligns implementation with M2. (I0235e, b/372311595)

**External Contribution**

- Commonized DatePickerDialog function. (I7dced)

## 1.4.0-alpha04

_November 13, 2024_


**Behavior Changes**

- Revert color changes introduced in aosp/3212478 to iconButtonColors, iconToggleButtonColors, outlinedIconButtonColors, outlinedIconToggleButtonColors and outlinedIconToggleButtonBorder. Moved the now-reverted behavior into a new set of functions called iconButtonVibrantColors and so forth to emphasize the high contrast colors being returned. (Iffd8d)

**New Features**

- Introduce VerticalDragHandle component (I6c770)

**API Changes**

- Rename DismissibleModalWideNavigationRailDefaults to ModalWideNavigationRailDefaults. (I8e877)
- Add NavigationBarComponentOverrides. (I8a3f3)
- Add NavigationRailComponentOverride. (I83e13)
- Introduce WideNavigationRailState to handle collapsing/expanding of the rail, allow ModalWideNavigationRail to be dismissible and delete DismissibleModalWideNavigationRail. (I88568)
- Add xSmall, medium, large, and xLarge size defaults into toggle button defaults. (Ie95d1)
- Add xSmall, medium, large, and xLarge size defaults into button defaults. (If8b6d)

**Bug Fixes**

- Filter chip trailing icon color has been updated from Primary to OnSurfaceContainer, per spec. All chip outline colors have been updated from Outline to Outline Variant, per spec. (I68bd4)

## 1.4.0-alpha03

_October 30, 2024_


**API Changes**

- SheetState constructor with density has been deprecated in favor of positional and velocity thresholds. (Ifd16e)
- Added rememberTooltipPositionProvider that contains an updated positioning logic. Deprecated rememberPlainTooltipPositionProvider and rememberRichTooltipPositionProvider. (Ie66e2)
- Additional ModalBottomSheetProperties allow for customization of status and navigation bar colors. By default, these update based on content color instead of system dark theme status. (Ib874e, b/362539765)
- Improved the input experience for date pickers: when in input mode, the date text field will be focused for immediate text entry. The change adds a requestFocus parameter to the DatePicker and the DateRangePicker. You may prevent a focus by passing false. (I12d09, b/286399710, b/340102743)
- Caching the shape defaults into the Shape object. Making the shape defaults @Composable for toggle button. Collapsing the shapes into one shape default since the variants point to the same token file. (Iaa014)

**Bug Fixes**

- Optimize Scaffold contentPadding behavior to avoid always recomposing the body content when the contentPadding changes. (I8c8e2, b/373904168)
- TextFieldLabelScope progress renamed to labelMinimizedProgress
- TextFieldLabelPosition Default renamed to Attached. Interface members have been removed. (If75c6)
- Fixed an issue with the DatePicker in input mode where validation errors could cause the component's height to change. (I2e229, b/280462363)
- Make the material Slider change its value when control keys are pressed. (I1c442)

## 1.4.0-alpha02

_October 16, 2024_


**API Changes**

- Updates to the date pickers to ensure consistent Locale usage throughout the DatePicker and DateRangePicker when setting a Locale directly through a DatePickerState or a DateRangePickerState. Note that when setting Locales directly to the state, it's up to you to ensure that the title and headline texts are localized accordingly, as their default text will still be applied according to the default platform Locale. (I37073, b/326490763, b/321657276)
- SheetState.isAnimationRunning is now exposed. (I9a3d7)
- DatePickerColors now correctly take precedence over any conflicting colors defined at the theme's Typography text styles. Also note that this update adjusts the color parameter's position in the date picker functions and introduces a contentColor parameter for customizing the header and title text colors. (I30d03, b/347031394)
- Rename SplitButton to SplitButtonLayout and remove SplitButton color variants APIs, variants will be achieve by re-using button variants provided by SplitButtonDefaults. (I44c36)
- ModalBottomSheet now has sheetGestureEnabled parameter (I856cb, b/288211587)
- Renamed the standardMotionScheme and the expressiveMotionScheme to standard and expressive. Both functions are now accessible through the MotionScheme companion object by calling MotionScheme.standard() and MotionScheme.expressive(). (Iceccf)
- Adding maxWidth parameter to plain and rich tooltips. It defaults to the design spec of 200 dp for plain tooltips and 320 dp for rich tooltips. (I30ce9)

**Bug Fixes**

- The DatePicker and DateRangePicker now correctly update the displayed month when set via their state's displayedMonthMillis. (If9e47, b/333414302)
- ModalBottomSheet content now moves content away from status bar. (I5114c, b/321877275, b/336962418, b/342093067)
- [Bottom Sheet] Change back callback priority to PRIORITY_DEFAULT to allow IME keyboard to dismiss first. (I447fb)
- Fix crashes in the DatePicker and DateRangePicker when the minimum selectable year is set to a future year. (I78656, b/319395747)
- Fix a date-picker issue to update the UI when an updated SelectableDates instance is applied. (Iad59a, b/290135807, b/339898760)

## 1.4.0-alpha01

_October 2, 2024_


**Behavior Changes**

- Library no longer adds a dependency to material-icons-core so if your project relied on that, you will have to explicitly add that dependency in your build.gradle[.kts] files. (I735ff, b/349894318)
- NavigationBarItem and NavigationRailItem’s active label color change from onSurface to secondary in order to improve usability, color contrast and improve coherence within the system (Ibc297), to revert to the previous behavior, copy the default colors and change the selectedTextColor to MaterialTheme.colorScheme.onSurface.
- Material 3 components are now using the new MotionScheme to define their motion. (Ie0f93)
- Indeterminate circular Progress Indicator motion changes (I3c07e)
- Update OutlinedIconButtonColors and OutlinedIconToggleButtonColors for better color contrast. (I2743d)
- Updated container and content color for FilledIconToggleButtonColors and FilledTonalIconToggleButtonColors. (Ic5d0f)
- Updated OutlinedButton border color from outline to outlineVariant. (057f00)

**API Changes**

- Added an optional reverseLayout parameter to the TopAppBar's enterAlwaysScrollBehavior function to better support content that was set with a reverseLayout. (I4e0e5)
- SegmentedButton now supports contentPadding argument. (I5ad91, b/358414376)
- PullToRefreshState does no longer have default implementation for isAnimating (I6a593)
- Added new overloads of Material TextField and OutlinedTextField that use TextFieldState. Added decoration box APIs that are compatible with TextFieldDecorator. (If13a1)
- Added parameter to control text field's labelPosition. Using alwaysMinimize allows the UI pattern of displaying a label and placeholder in a text field at the same time even when the field is unfocused. (I1ef2c)
- Added Material SecureTextField and OutlinedSecureTextField for password entry fields. (I7e22d)
- Added a scope to text field labels to query animation progress. (If5ec8)
- Adds support for MotionEvent.CLASSIFICATION_DEEP_PRESS to tooltips. (I62e6a)
- TimePickerState’s isAfternoon is now an extension val instead of a var in the interface, renamed to isPm (I89a97)
- Updating typography class to support emphasized type scales. (Ifa13c)
- Added ModalWideNavigationRail API and renamed ModalExpandedNavigationRail APIs to DismissibleModalWideNavigationRail. (Ic9118)
- Updated FloatingAppBarScrollBehavior to auto-calculate the screenOffset and not require a @Composable. (Idf349)
- Updates to the LoadingIndicator API to fix the naming at its defaults object. Added a LoadingIndicatorElevation constant at the PullToRefresh. (I1d72b)
- Updates to the LoadingIndicator API to fix the naming at its defaults object. Added a LoadingIndicatorElevation constant at the PullToRefresh. (I1d72b)
- Added an amplitude and a waveSpeed parameters to the indeterminate variations of the LinearWavyProgressIndicator and the CircularWavyProgressIndicator. (I2a0c5)
- Support changing an icon toggle button's shape based on its pressed or checked state. (Ibc781)
- Updating typography class to support emphasized type scales. (Ifa13c)
- SplitButton shape morphs based on default / pressed state. Removed AnimatedTrailingButton api because TrailingButton api can offer the same customizations (I95066)
- Add modifier to animate showing and hiding of FAB, e.g. when content scrolls. (I8338d)
- Adding connected button group shapes and spacing to ButtonGroupDefaults to be used in a sample. (I68e30)
- Split button add horizontal padding for trailing button and enable optical centering calculated from start and end corner differences (I122e2)
- Introducing a new Material MotionScheme to allow setting a scheme for the component's motion. The scheme is set through the MaterialTheme. (Id50c2)

**Bug Fixes**

- Apply the correct focus traversal index to Scaffold child Composables. The order is topBar, bottomBar, fab, content, snackbar. (I5936b)
- Fixed an issue at the DatePicker and DateRangePicker where in certain locales and format-skeletons some of the date elements (e.g. month names) did not start with a capital letter. (I1430f)
- Integrate FAB and FAB Menu component tokens (minor visual updates to paddings and text) (Ib57f3)
- ModalBottomSheet is now first in semantic traversal order, followed by the scrim. (I436f9, b/358594665)
- Fix bottom app bar not disappearing entirely when scrolling under the navigation pill in edge to edge mode (I3ee21)
