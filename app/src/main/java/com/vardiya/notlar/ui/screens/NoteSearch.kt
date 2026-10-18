package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarScrollBehavior
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.ui.components.hapticClick
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteFilter
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.ui.components.NoteCard
import kotlinx.coroutines.launch

/**
 * The Expressive search app bar of the notes and archive lists. Tapping it opens a full-screen
 * search with the recent searches; typing shows the matching notes there.
 */
@Composable
fun SearchTopBar(
    section: Section,
    notes: List<Note>,
    query: TextFieldState,
    searchState: SearchBarState,
    filter: NoteFilter,
    onFilterChange: (NoteFilter) -> Unit,
    settings: Settings,
    onToggleListView: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpen: (Note) -> Unit,
    onRememberSearch: (String) -> Unit,
    onForgetSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    scrollBehavior: SearchBarScrollBehavior? = null,
) {
    val scope = rememberCoroutineScope()
    val expanded = searchState.currentValue == SearchBarValue.Expanded
    fun collapse() {
        scope.launch { searchState.animateToCollapsed() }
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            textFieldState = query,
            searchBarState = searchState,
            onSearch = {
                onRememberSearch(it)
                collapse()
            },
            placeholder = {
                Text(if (section == Section.ARCHIVE) "Arşivde ara" else "Notlarda ara", Modifier.clearAndSetSemantics {})
            },
            leadingIcon = {
                if (expanded) {
                    IconButton(onClick = hapticClick(::collapse), shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Aramayı kapat")
                    }
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null)
                }
            },
            trailingIcon = if (query.text.isNotEmpty()) {
                {
                    IconButton(onClick = hapticClick { query.clearText() }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.Close, contentDescription = "Aramayı temizle")
                    }
                }
            } else {
                null
            },
        )
    }

    AppBarWithSearch(
        state = searchState,
        inputField = inputField,
        modifier = modifier,
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = hapticClick(onToggleListView), shapes = IconButtonDefaults.shapes()) {
                Icon(
                    if (settings.listView) Icons.Outlined.GridView else Icons.AutoMirrored.Outlined.ViewList,
                    contentDescription = if (settings.listView) "Izgara görünümü" else "Liste görünümü",
                )
            }
            IconButton(onClick = hapticClick(onOpenSettings), shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Outlined.Settings, contentDescription = "Ayarlar")
            }
        },
    )

    ExpandedFullScreenSearchBar(state = searchState, inputField = inputField) {
        val text = query.text.toString()
        if (section == Section.NOTES) {
            FilterChips(filter, onFilterChange, Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
        if (text.isBlank()) {
            RecentSearches(
                recent = settings.recentSearches,
                onPick = { query.setTextAndPlaceCursorAtEnd(it) },
                onForget = onForgetSearch,
            )
        } else {
            val results = NoteRules.visible(notes, section, text, if (section == Section.NOTES) filter else NoteFilter.ALL, settings.sort)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (results.isEmpty()) {
                    item { Text("\"${text.trim()}\" ile eşleşen not yok.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(results, key = { it.id }) { note ->
                    NoteCard(
                        note,
                        onClick = {
                            onRememberSearch(text)
                            collapse()
                            onOpen(note)
                        },
                        highlight = text,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentSearches(recent: List<String>, onPick: (String) -> Unit, onForget: (String) -> Unit) {
    if (recent.isEmpty()) {
        Text(
            "Aradıkların burada görünür.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        return
    }
    LazyColumn {
        item {
            Text(
                "Son aramalar",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    .semantics { heading() },
            )
        }
        items(recent, key = { it }) { entry ->
            ListItem(
                headlineContent = { Text(entry) },
                leadingContent = { Icon(Icons.Outlined.History, contentDescription = null) },
                trailingContent = {
                    IconButton(onClick = { onForget(entry) }, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Filled.Close, contentDescription = "\"$entry\" aramasını sil")
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                modifier = Modifier
                    .animateItem()
                    .clickable { onPick(entry) },
            )
        }
    }
}

/** The kind chips shared by the list and the full-screen search. */
@Composable
internal fun FilterChips(filter: NoteFilter, onFilterChange: (NoteFilter) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteFilter.entries.forEach { item ->
            FilterChip(
                selected = filter == item,
                onClick = hapticClick { onFilterChange(item) },
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
