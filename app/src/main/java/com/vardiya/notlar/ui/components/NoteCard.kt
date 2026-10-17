package com.vardiya.notlar.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteColor
import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.ui.theme.container
import com.vardiya.notlar.ui.theme.notePalette
import com.vardiya.notlar.ui.theme.onContainer

private const val PREVIEW_ITEMS = 6
private const val PREVIEW_LINES = 10

/**
 * A note in the grid or list: title, then the start of its text or its first to-do items.
 * A long press starts selection; a [selected] card gets a thick outline and a check badge.
 */
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val palette = notePalette
    val shape = MaterialTheme.shapes.large
    val interactions = remember { MutableInteractionSource() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactions)
            .clip(shape)
            .semantics { this.selected = selected }
            .combinedClickable(
                interactionSource = interactions,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = "Seç",
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = palette.container(note.color),
            contentColor = palette.onContainer(note.color),
        ),
        border = when {
            selected -> BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            note.color == NoteColor.DEFAULT -> BorderStroke(1.dp, palette.outlineVariant)
            else -> null
        },
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (note.title.isNotBlank() || note.pinned || selected) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        note.title,
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // While selected, the check badge takes the pin's place.
                    if (selected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Seçildi",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    } else if (note.pinned) {
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteEdits.progress(note)?.let { (done, total) ->
            val content = LocalContentColor.current
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val progress by animateFloatAsState(
                    targetValue = done.toFloat() / total,
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                    label = "progress",
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.weight(1f),
                    color = content,
                    trackColor = content.copy(alpha = 0.2f),
                    drawStopIndicator = {},
                )
                Text(
                    "$done/$total",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.semantics { contentDescription = "$total öğeden $done tamamlandı" },
                )
            }
        }
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
}
