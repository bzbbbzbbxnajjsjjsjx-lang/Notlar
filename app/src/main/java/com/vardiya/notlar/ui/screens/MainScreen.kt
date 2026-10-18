package com.vardiya.notlar.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.runtime.rememberCoroutineScope
import com.vardiya.notlar.ui.components.hapticClick
import com.vardiya.notlar.domain.NoteSort
import com.vardiya.notlar.ui.components.Haptics
import com.vardiya.notlar.ui.components.rememberHaptics
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridItemScope
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Labels
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteFilter
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.ui.components.AnimatedIcon
import com.vardiya.notlar.ui.components.IconMotion
import com.vardiya.notlar.ui.components.NewNoteFabMenu
import com.vardiya.notlar.ui.components.NoteCard
import com.vardiya.notlar.ui.components.SwipeAction
import com.vardiya.notlar.ui.components.SwipeActions
import com.vardiya.notlar.ui.components.entrance
import com.vardiya.notlar.ui.components.rememberMotionEnabled
import com.vardiya.notlar.ui.components.sharedNoteBounds
import kotlinx.coroutines.delay

// Lists slide by a sixth of their width when the section changes.
private const val SLIDE_FRACTION = 6
private const val INTRO_MS = 700L
private const val EMPTY_TURN = -40f
private const val SECTION_SCALE = 0.94f
private const val SCRIM_ALPHA = 0.32f

private val Section.title: String
    get() = when (this) {
        Section.NOTES -> "Notlar"
        Section.ARCHIVE -> "Arşiv"
        Section.TRASH -> "Çöp kutusu"
    }

private val NAV_ICON_SIZE = 28.dp

private val Section.selectedIcon: ImageVector
    get() = when (this) {
        Section.NOTES -> Icons.AutoMirrored.Filled.Notes
        Section.ARCHIVE -> Icons.Filled.Archive
        Section.TRASH -> Icons.Filled.Delete
    }

private val Section.icon: ImageVector
    get() = when (this) {
        Section.NOTES -> Icons.AutoMirrored.Outlined.Notes
        Section.ARCHIVE -> Icons.Outlined.Archive
        Section.TRASH -> Icons.Outlined.Delete
    }

private val Section.motion: IconMotion
    get() = when (this) {
        Section.NOTES -> IconMotion.BOUNCE
        Section.ARCHIVE -> IconMotion.DROP
        Section.TRASH -> IconMotion.WIGGLE
    }

internal val NoteFilter.label: String
    get() = when (this) {
        NoteFilter.ALL -> "Tümü"
        NoteFilter.TEXT -> "Notlar"
        NoteFilter.CHECKLIST -> "Yapılacaklar"
    }

/**
 * The three lists behind the bottom bar: notes, archive and trash. A long press on a note starts
 * selection; while notes are [selection] the top bar acts on all of them and the bottom bar hides.
 */
@Composable
fun MainScreen(
    notes: List<Note>,
    loaded: Boolean,
    section: Section,
    settings: Settings,
    selection: Set<Long>,
    onSelectionChange: (Set<Long>) -> Unit,
    onSectionChange: (Section) -> Unit,
    onOpen: (Note) -> Unit,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onPasteNote: () -> Unit,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    onToggleListView: () -> Unit,
    onOpenSettings: () -> Unit,
    onPinSelection: (pinned: Boolean) -> Unit,
    onArchiveSelection: () -> Unit,
    onTrashSelection: () -> Unit,
    onSwipeArchive: (Note) -> Unit,
    onSwipeTrash: (Note) -> Unit,
    onRememberSearch: (String) -> Unit,
    onForgetSearch: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    onManageLabels: () -> Unit = {},
    onReorder: (shownIds: List<Long>, from: Int, to: Int) -> Unit = { _, _, _ -> },
    nowMillis: Long = System.currentTimeMillis(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyStaggeredGridState()
    // Notes and archive slide past each other while switching, so each grid keeps its own state.
    val archiveGridState = rememberLazyStaggeredGridState()
    val haptic = LocalHapticFeedback.current
    val motion = MaterialTheme.motionScheme
    val selecting = selection.isNotEmpty()
    var fabMenuOpen by rememberSaveable { mutableStateOf(false) }
    val query = rememberTextFieldState()
    val queryText = query.text.toString()
    var filter by rememberSaveable { mutableStateOf(NoteFilter.ALL) }
    val labels = Labels.all(settings.labels, notes)
    // The label chosen above the list; forgotten once the label is renamed or deleted.
    var chosenLabel by rememberSaveable { mutableStateOf<String?>(null) }
    val label = chosenLabel?.takeIf { chosen -> labels.any { Labels.same(it, chosen) } }
    val searchState = rememberSearchBarState()
    val searchScroll = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior()
    // Each list starts unsearched; a rotation keeps the search.
    var searchedSection by rememberSaveable { mutableStateOf(section) }
    LaunchedEffect(section) {
        if (section != searchedSection) {
            query.clearText()
            searchedSection = section
        }
    }
    BackHandler(enabled = selecting) { onSelectionChange(emptySet()) }

    Scaffold(
        // Only the bar on screen reacts to scrolling: the trash's large title, or the search bar.
        // An unmeasured large title would otherwise swallow every scroll of the notes grid.
        modifier = modifier.nestedScroll(
            if (section == Section.TRASH) scrollBehavior.nestedScrollConnection else searchScroll.nestedScrollConnection,
        ),
        topBar = {
            AnimatedContent(
                targetState = selecting,
                transitionSpec = { fadeIn(motion.defaultEffectsSpec()) togetherWith fadeOut(motion.fastEffectsSpec()) },
                label = "topBar",
            ) { selectingBar ->
                if (selectingBar) {
                    val allPinned = notes.filter { it.id in selection }.all { it.pinned }
                    // A small bar: the list below already starts with the section's big title.
                    TopAppBar(
                        title = { Text("${selection.size} seçildi") },
                        navigationIcon = {
                            IconButton(onClick = hapticClick { onSelectionChange(emptySet()) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Filled.Close, contentDescription = "Seçimi kapat")
                            }
                        },
                        actions = {
                            if (section == Section.NOTES) {
                                BarAction(
                                    icon = if (allPinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                                    description = if (allPinned) "Sabitlemeyi kaldır" else "Sabitle",
                                    onClick = { onPinSelection(!allPinned) },
                                )
                            }
                            BarAction(
                                icon = if (section == Section.ARCHIVE) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                                description = if (section == Section.ARCHIVE) "Arşivden çıkar" else "Arşivle",
                                onClick = onArchiveSelection,
                            )
                            BarAction(icon = Icons.Outlined.Delete, description = "Çöp kutusuna taşı", onClick = onTrashSelection)
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                    )
                } else if (section == Section.TRASH) {
                    LargeFlexibleTopAppBar(
                        // Heavy like the Notlar and Arşiv titles; the bar keeps sizing it as it collapses.
                        title = { Text(section.title, fontWeight = FontWeight.ExtraBold) },
                        actions = { BarAction(icon = Icons.Outlined.Settings, description = "Ayarlar", onClick = onOpenSettings) },
                        scrollBehavior = scrollBehavior,
                    )
                } else {
                    SearchTopBar(
                        section = section,
                        notes = notes,
                        query = query,
                        searchState = searchState,
                        filter = filter,
                        onFilterChange = { filter = it },
                        settings = settings,
                        onToggleListView = onToggleListView,
                        onOpenSettings = onOpenSettings,
                        onOpen = onOpen,
                        onRememberSearch = onRememberSearch,
                        onForgetSearch = onForgetSearch,
                        scrollBehavior = searchScroll,
                    )
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = !selecting,
                enter = slideInVertically(motion.defaultSpatialSpec()) { it } + fadeIn(motion.defaultEffectsSpec()),
                exit = slideOutVertically(motion.fastSpatialSpec()) { it } + fadeOut(motion.fastEffectsSpec()),
                label = "bottomBar",
            ) {
                SectionBar(section = section, onSectionChange = onSectionChange)
            }
        },
        floatingActionButton = {
            NewNoteFabMenu(
                visible = section == Section.NOTES && !selecting,
                expanded = fabMenuOpen,
                onExpandedChange = { fabMenuOpen = it },
                onNewNote = onNewNote,
                onNewChecklist = onNewChecklist,
                onPaste = onPasteNote,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        // The new list slides in from the side of the tapped destination.
        AnimatedContent(
            targetState = section,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (
                    slideInHorizontally(motion.defaultSpatialSpec()) { it / SLIDE_FRACTION * direction } +
                        scaleIn(motion.defaultSpatialSpec(), SECTION_SCALE) +
                        fadeIn(motion.defaultEffectsSpec())
                    )
                    .togetherWith(
                        slideOutHorizontally(motion.fastSpatialSpec()) { -it / SLIDE_FRACTION * direction } +
                            scaleOut(motion.fastSpatialSpec(), SECTION_SCALE) +
                            fadeOut(motion.fastEffectsSpec()),
                    )
            },
            label = "section",
        ) { shown ->
            when (shown) {
                Section.TRASH -> TrashList(
                    notes = NoteRules.visible(notes, Section.TRASH),
                    nowMillis = nowMillis,
                    onRestore = onRestore,
                    onDeleteForever = onDeleteForever,
                    onEmptyTrash = onEmptyTrash,
                    contentPadding = padding,
                )
                else -> NoteGrid(
                    notes = notes,
                    loaded = loaded,
                    section = shown,
                    settings = settings,
                    gridState = if (shown == Section.ARCHIVE) archiveGridState else gridState,
                    selection = selection,
                    onOpen = { note ->
                        when {
                            !selecting -> onOpen(note)
                            note.id in selection -> onSelectionChange(selection - note.id)
                            else -> onSelectionChange(selection + note.id)
                        }
                    },
                    onSelect = { note ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelectionChange(if (note.id in selection) selection - note.id else selection + note.id)
                    },
                    onSwipeArchive = onSwipeArchive,
                    onSwipeTrash = onSwipeTrash,
                    query = queryText,
                    filter = filter,
                    onFilterChange = { filter = it },
                    labels = LabelFilter(labels, label, onLabelChange = { chosenLabel = it }, onManage = onManageLabels),
                    nowMillis = nowMillis,
                    contentPadding = padding,
                    onReorder = onReorder,
                )
            }
        }
        // While the FAB menu is open, the list dims and a tap outside closes the menu.
        AnimatedVisibility(
            visible = fabMenuOpen,
            enter = fadeIn(motion.defaultEffectsSpec()),
            exit = fadeOut(motion.fastEffectsSpec()),
            label = "fabScrim",
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = SCRIM_ALPHA))
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClickLabel = "Menüyü kapat",
                    ) { fabMenuOpen = false },
            )
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = hapticClick(onClick), shapes = IconButtonDefaults.shapes()) {
        Icon(icon, contentDescription = description)
    }
}

/** The tall bottom bar: icon in a wide pill with the label under it. */
@Composable
private fun SectionBar(section: Section, onSectionChange: (Section) -> Unit) {
    val haptic = LocalHapticFeedback.current
    NavigationBar {
        Section.entries.forEach { item ->
            val selected = item == section
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onSectionChange(item)
                },
                icon = {
                    AnimatedIcon(
                        if (selected) item.selectedIcon else item.icon,
                        contentDescription = null,
                        play = selected,
                        motion = item.motion,
                        modifier = Modifier.size(NAV_ICON_SIZE),
                    )
                },
                label = { Text(item.title) },
            )
        }
    }
}

@Composable
private fun NoteGrid(
    notes: List<Note>,
    loaded: Boolean,
    section: Section,
    settings: Settings,
    gridState: LazyStaggeredGridState,
    selection: Set<Long>,
    onOpen: (Note) -> Unit,
    onSelect: (Note) -> Unit,
    onSwipeArchive: (Note) -> Unit,
    onSwipeTrash: (Note) -> Unit,
    query: String,
    filter: NoteFilter,
    onFilterChange: (NoteFilter) -> Unit,
    labels: LabelFilter,
    nowMillis: Long,
    contentPadding: PaddingValues,
    onReorder: (shownIds: List<Long>, from: Int, to: Int) -> Unit,
) {
    val visible = NoteRules.visible(
        notes,
        section,
        query,
        if (section == Section.NOTES) filter else NoteFilter.ALL,
        settings.sort,
        label = if (section == Section.NOTES) labels.selected else null,
        manualOrder = settings.manualOrder,
    )
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val reorder = remember(gridState) { ReorderState(gridState, onReorder) }
    // Dragging reorders only the plain notes list; the archive and searches keep their own order.
    val canReorder = section == Section.NOTES && settings.sort == NoteSort.MANUAL && query.isBlank()
    val shown = reorder.arrange(visible)
    val pinned = shown.filter { it.pinned }
    val others = shown.filterNot { it.pinned }
    var hintDismissed by rememberSaveable { mutableStateOf(false) }

    // The first moments after a list opens its cards cascade in; cards that appear later (scrolling,
    // a new note) just show up.
    var intro by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(INTRO_MS)
        intro = false
    }
    val order = remember(visible) { visible.withIndex().associate { it.value.id to it.index } }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(if (settings.listView) 1 else 2),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            // Room for the extended FAB above the navigation bar.
            bottom = contentPadding.calculateBottomPadding() + 96.dp,
        ),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // The search bar sits in the app bar; the section's big title opens the list.
        item(span = StaggeredGridItemSpan.FullLine) {
            Text(
                section.title,
                style = MaterialTheme.typography.displaySmallEmphasized,
                modifier = Modifier
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
                    .semantics { heading() },
            )
        }
        if (section == Section.NOTES) {
            item(span = StaggeredGridItemSpan.FullLine) { FilterChips(filter, onFilterChange) }
            if (labels.all.isNotEmpty()) {
                item(span = StaggeredGridItemSpan.FullLine) { LabelChips(labels) }
            }
        }
        if (canReorder && !hintDismissed && visible.size > 1) {
            item(span = StaggeredGridItemSpan.FullLine) {
                ReorderHint(onDismiss = {
                    haptics.tick()
                    hintDismissed = true
                })
            }
        }
        if (pinned.isNotEmpty() && others.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { GroupHeader("Sabitlenenler") }
        }
        val swipe = SwipeSetup(
            section,
            query,
            swipeEnabled = selection.isEmpty(),
            onSwipeArchive,
            onSwipeTrash,
            textScale = if (settings.textSizeOnCards) settings.textSize.scale else 1f,
            nowMillis = nowMillis,
            reorder = ReorderSetup(canReorder, reorder, shown, haptics) { scope.launch { gridState.scrollBy(it) } },
        )
        items(pinned, key = { it.id }) { note -> GridCard(note, note.id in selection, onOpen, onSelect, swipe, intro, order[note.id] ?: 0) }
        if (pinned.isNotEmpty() && others.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { GroupHeader("Diğerleri") }
        }
        items(others, key = { it.id }) { note -> GridCard(note, note.id in selection, onOpen, onSelect, swipe, intro, order[note.id] ?: 0) }
        if (!loaded) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Box(Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
        if (loaded && visible.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyState(
                    icon = section.icon,
                    text = when {
                        query.isNotBlank() -> "\"${query.trim()}\" ile eşleşen not yok."
                        labels.selected != null && section == Section.NOTES -> "\"${labels.selected}\" etiketli not yok."
                        section == Section.ARCHIVE -> "Arşivlenen notlar burada görünür."
                        else -> "Henüz not yok.\nSağ alttaki + ile ilk notunu yaz."
                    },
                )
            }
        }
    }
}

/** What every card in one list shares: the search to highlight, what a swipe does and the text size. */
private class SwipeSetup(
    val section: Section,
    val query: String,
    val swipeEnabled: Boolean,
    val onArchive: (Note) -> Unit,
    val onTrash: (Note) -> Unit,
    val textScale: Float,
    val nowMillis: Long,
    val reorder: ReorderSetup,
)

/** What a card needs to be dragged: whether dragging is on, its state and the list it moves in. */
private class ReorderSetup(
    val enabled: Boolean,
    val state: ReorderState,
    val shown: List<Note>,
    val haptics: Haptics,
    val scrollBy: (Float) -> Unit,
)

@Composable
private fun LazyStaggeredGridItemScope.GridCard(
    note: Note,
    selected: Boolean,
    onOpen: (Note) -> Unit,
    onSelect: (Note) -> Unit,
    swipe: SwipeSetup,
    intro: Boolean,
    index: Int,
) {
    val scheme = MaterialTheme.colorScheme
    val inArchive = swipe.section == Section.ARCHIVE
    // Right: archive (or bring back from the archive); left: trash.
    SwipeActions(
        startAction = SwipeAction(
            icon = if (inArchive) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            label = if (inArchive) "Arşivden çıkar" else "Arşivle",
            containerColor = scheme.tertiaryContainer,
            contentColor = scheme.onTertiaryContainer,
            onSwipe = { swipe.onArchive(note) },
        ),
        endAction = SwipeAction(
            icon = Icons.Outlined.Delete,
            label = "Çöp kutusuna taşı",
            containerColor = scheme.errorContainer,
            contentColor = scheme.onErrorContainer,
            onSwipe = { swipe.onTrash(note) },
        ),
        enabled = swipe.swipeEnabled && swipe.reorder.state.dragId == null,
        modifier = Modifier
            .animateItem()
            .entrance(play = intro, index = index)
            .reorderable(
                id = note.id,
                enabled = swipe.reorder.enabled && swipe.swipeEnabled,
                state = swipe.reorder.state,
                shown = swipe.reorder.shown,
                haptics = swipe.reorder.haptics,
                scrollBy = swipe.reorder.scrollBy,
                onTap = { onSelect(note) },
            ),
    ) {
        NoteCard(
            note,
            onClick = { onOpen(note) },
            // A long press drags the card in the manual order, and selects it if it is not moved.
            onLongClick = if (swipe.reorder.enabled) null else ({ onSelect(note) }),
            selected = selected,
            highlight = swipe.query,
            textScale = swipe.textScale,
            nowMillis = swipe.nowMillis,
            modifier = Modifier.sharedNoteBounds(note.id),
        )
    }
}

@Composable
private fun ReorderHint(onDismiss: () -> Unit) {
    AssistChip(
        onClick = onDismiss,
        label = { Text("Sıralamak için bir kartı basılı tutup sürükle") },
        leadingIcon = { Icon(Icons.Outlined.DragHandle, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "İpucunu kapat", Modifier.size(AssistChipDefaults.IconSize)) },
        modifier = Modifier.animateContentSize(),
    )
}

@Composable
private fun GroupHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(start = 4.dp, top = 8.dp)
            .semantics { heading() },
    )
}

@Composable
internal fun EmptyState(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        // The shape pops in with a spring and turns into place; the text follows it.
        val enabled = rememberMotionEnabled()
        val pop = remember { Animatable(if (enabled) 0f else 1f) }
        val spring = MaterialTheme.motionScheme.slowSpatialSpec<Float>()
        if (enabled) {
            LaunchedEffect(Unit) { pop.animateTo(1f, spring) }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // The icon sits on one of the Expressive shapes.
            Box(
                Modifier
                    .graphicsLayer {
                        val p = pop.value
                        scaleX = p
                        scaleY = p
                        rotationZ = (1f - p) * EMPTY_TURN
                    }
                    .size(112.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Text(
                text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer { alpha = pop.value.coerceIn(0f, 1f) },
            )
        }
    }
}

/** The labels above the notes list, the one chosen, and the way to the label editor. */
internal class LabelFilter(
    val all: List<String>,
    val selected: String?,
    val onLabelChange: (String?) -> Unit,
    val onManage: () -> Unit,
)

/** One scrollable row: "Tüm etiketler", then every label, then "Düzenle". */
@Composable
private fun LabelChips(labels: LabelFilter) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LabelChip("Tüm etiketler", selected = labels.selected == null, icon = null) { labels.onLabelChange(null) }
        labels.all.forEach { name ->
            val selected = labels.selected?.let { Labels.same(it, name) } == true
            LabelChip(name, selected, Icons.AutoMirrored.Outlined.Label) { labels.onLabelChange(if (selected) null else name) }
        }
        AssistChip(
            onClick = labels.onManage,
            label = { Text("Düzenle") },
            leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
            modifier = Modifier.semantics { contentDescription = "Etiketleri düzenle" },
        )
    }
}

@Composable
private fun LabelChip(text: String, selected: Boolean, icon: ImageVector?, onClick: () -> Unit) {
    val shown = if (selected) Icons.Filled.Check else icon
    FilterChip(
        selected = selected,
        onClick = hapticClick(onClick),
        label = { Text(text) },
        leadingIcon = shown?.let { { Icon(it, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) } },
    )
}
