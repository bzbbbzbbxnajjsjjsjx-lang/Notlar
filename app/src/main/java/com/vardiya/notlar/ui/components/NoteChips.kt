package com.vardiya.notlar.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.ReminderRepeat
import java.time.Instant
import java.time.ZoneId

private const val CHIP_TINT = 0.12f
private const val PAST_ALPHA = 0.6f

/** The note text styles grown or shrunk by the text size setting. */
fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale == 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

/**
 * The reminder and labels of a note as small pills, on a card or under the title in the editor.
 * A single reminder whose time has passed stays, struck through, until it is changed or removed.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteChips(
    note: Note,
    modifier: Modifier = Modifier,
    nowMillis: Long = System.currentTimeMillis(),
) {
    val today = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val at = note.reminderAt
    if (at == null && note.labels.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (at != null) {
            val past = at < nowMillis && note.reminderRepeat == ReminderRepeat.NONE
            val text = formatReminder(at, ZoneId.systemDefault(), today)
            Pill(
                text = text,
                icon = { Icon(Icons.Outlined.Alarm, contentDescription = null, Modifier.size(14.dp)) },
                past = past,
                trailing = if (note.reminderRepeat != ReminderRepeat.NONE) {
                    { Icon(Icons.Outlined.Repeat, contentDescription = null, Modifier.size(14.dp)) }
                } else {
                    null
                },
                modifier = Modifier.semantics {
                    contentDescription = buildString {
                        append("Hatırlatıcı: $text")
                        if (note.reminderRepeat != ReminderRepeat.NONE) append(", ${note.reminderRepeat.label.lowercase()}")
                        if (past) append(", geçti")
                    }
                },
            )
        }
        note.labels.forEach { name ->
            Pill(
                text = name,
                icon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, Modifier.size(14.dp)) },
                modifier = Modifier.semantics { contentDescription = "Etiket: $name" },
            )
        }
    }
}

@Composable
private fun Pill(
    text: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    past: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val content = LocalContentColor.current.let { if (past) it.copy(alpha = PAST_ALPHA) else it }
    Surface(shape = CircleShape, color = content.copy(alpha = CHIP_TINT), contentColor = content, modifier = modifier) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon()
            Text(
                text,
                style = MaterialTheme.typography.labelMedium,
                textDecoration = if (past) TextDecoration.LineThrough else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            trailing?.invoke()
        }
    }
}
