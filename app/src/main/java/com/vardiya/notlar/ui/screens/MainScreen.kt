package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteFilter
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.ui.components.NoteCard

private val Section.title: String
    get() = when (this) {
        Section.NOTES -> "Notlar"
        Section.ARCHIVE -> "Arşiv"
        Section.TRASH -> "Çöp kutusu"
    }

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

private val NoteFilter.label: String
    get() = when (this) {
        NoteFilter.ALL -> "Tümü"
        NoteFilter.TEXT -> "Notlar"
        NoteFilter.CHECKLIST -> "Yapılacaklar"
    }

/** The three lists behind the bottom bar: notes, archive and trash. */
@Composable
fun MainScreen(
    notes: List<Note>,
    loaded: Boolean,
    section: Section,
    onSectionChange: (Section) -> Unit,
    onOpen: (Note) -> Unit,
    onNewNote: () -> Unit,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyStaggeredGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(section.title) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = {
            ShortNavigationBar {
                Section.entries.forEach { item ->
                    val selected = item == section
                    ShortNavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                            onSectionChange(item)
                        },
                        icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                        label = { Text(item.title) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (section == Section.NOTES) {
                ExtendedFloatingActionButton(
                    text = { Text("Not") },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                        onNewNote()
                    },
                    expanded = fabExpanded,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (section) {
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
                section = section,
                gridState = gridState,
                onOpen = onOpen,
                contentPadding = padding,
            )
        }
    }
}

@Composable
private fun NoteGrid(
    notes: List<Note>,
    loaded: Boolean,
    section: Section,
    gridState: LazyStaggeredGridState,
    onOpen: (Note) -> Unit,
    contentPadding: PaddingValues,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(NoteFilter.ALL) }
    val visible = NoteRules.visible(notes, section, query, if (section == Section.NOTES) filter else NoteFilter.ALL)
    val pinned = visible.filter { it.pinned }
    val others = visible.filterNot { it.pinned }

    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
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
        item(span = StaggeredGridItemSpan.FullLine) {
            SearchField(query = query, onQueryChange = { query = it }, modifier = Modifier.padding(bottom = 4.dp))
        }
        if (section == Section.NOTES) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteFilter.entries.forEach { item ->
                        FilterChip(
                            selected = filter == item,
                            onClick = { filter = item },
                            label = { Text(item.label) },
                            leadingIcon = if (filter == item) {
                                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
        if (pinned.isNotEmpty() && others.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { GroupHeader("Sabitlenenler") }
        }
        items(pinned, key = { it.id }) { note -> NoteCard(note, onClick = { onOpen(note) }, Modifier.animateItem()) }
        if (pinned.isNotEmpty() && others.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) { GroupHeader("Diğerleri") }
        }
        items(others, key = { it.id }) { note -> NoteCard(note, onClick = { onOpen(note) }, Modifier.animateItem()) }
        if (loaded && visible.isEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine) {
                EmptyState(
                    icon = section.icon,
                    text = when {
                        query.isNotBlank() -> "\"${query.trim()}\" ile eşleşen not yok."
                        section == Section.ARCHIVE -> "Arşivlenen notlar burada görünür."
                        else -> "Henüz not yok.\nSağ alttaki Not ile ilk notunu yaz."
                    },
                )
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Notlarda ara") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Close, contentDescription = "Aramayı temizle")
                }
            }
        } else {
            null
        },
        singleLine = true,
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
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
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(icon, contentDescription = null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text(
                text,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
