package com.vardiya.notlar.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteColor
import com.vardiya.notlar.ui.theme.container
import com.vardiya.notlar.ui.theme.onContainer

private const val PREVIEW_ITEMS = 6
private const val PREVIEW_LINES = 10

/** A note in the grid: title, then the start of its text or its first to-do items. */
@Composable
fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = scheme.container(note.color),
            contentColor = scheme.onContainer(note.color),
        ),
        border = if (note.color == NoteColor.DEFAULT) BorderStroke(1.dp, scheme.outlineVariant) else null,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (note.title.isNotBlank() || note.pinned) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (note.pinned) {
                        Icon(Icons.Filled.PushPin, contentDescription = "Sabitlendi", modifier = Modifier.size(18.dp))
                    }
                }
            }
            when {
                note.isChecklist -> ChecklistPreview(note)
                note.body.isNotBlank() -> Text(
                    note.body.trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = PREVIEW_LINES,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ChecklistPreview(note: Note) {
    val items = note.items.filter { it.text.isNotBlank() }.sortedBy { it.checked }
    items.take(PREVIEW_ITEMS).forEach { item ->
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (item.checked) "Tamamlandı" else "Yapılacak",
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(18.dp),
            )
            Text(
                item.text,
                style = MaterialTheme.typography.bodyMedium,
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
    if (items.size > PREVIEW_ITEMS) {
        Text("+${items.size - PREVIEW_ITEMS} öğe daha", style = MaterialTheme.typography.labelMedium)
    }
}
