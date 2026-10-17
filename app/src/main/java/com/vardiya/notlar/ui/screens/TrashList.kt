package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.RestoreFromTrash
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.ui.components.SwipeAction
import com.vardiya.notlar.ui.components.SwipeActions

/** Trashed notes with restore and delete-for-good actions; they expire after 30 days. */
@Composable
internal fun TrashList(
    notes: List<Note>,
    nowMillis: Long,
    onRestore: (Note) -> Unit,
    onDeleteForever: (Note) -> Unit,
    onEmptyTrash: () -> Unit,
    contentPadding: PaddingValues,
) {
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding(),
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                "Çöp kutusundaki notlar ${NoteRules.TRASH_DAYS} gün sonra kalıcı olarak silinir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (notes.isEmpty()) {
            item { EmptyState(icon = Icons.Outlined.Delete, text = "Çöp kutusu boş.") }
        } else {
            item {
                val height = ButtonDefaults.MediumContainerHeight
                FilledTonalButton(
                    onClick = { confirmEmpty = true },
                    shapes = ButtonDefaults.shapesFor(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = null, Modifier)
                    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(height)))
                    Text("Çöpü boşalt", style = ButtonDefaults.textStyleFor(height))
                }
            }
        }
        items(notes, key = { it.id }) { note ->
            // Swipe right to bring a note back; deleting for good keeps its confirmation dialog.
            SwipeActions(
                startAction = SwipeAction(
                    icon = Icons.Filled.RestoreFromTrash,
                    label = "Geri yükle",
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    onSwipe = { onRestore(note) },
                ),
                endAction = null,
                modifier = Modifier.animateItem(),
            ) {
                OutlinedCard {
                    ListItem(
                        headlineContent = { Text(note.displayTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text("${NoteRules.daysLeft(note, nowMillis)} gün sonra silinecek") },
                        trailingContent = {
                            Row {
                                IconButton(onClick = { onRestore(note) }, shapes = IconButtonDefaults.shapes()) {
                                    Icon(Icons.Filled.RestoreFromTrash, contentDescription = "Geri yükle")
                                }
                                IconButton(onClick = { confirmDeleteId = note.id }, shapes = IconButtonDefaults.shapes()) {
                                    Icon(Icons.Filled.DeleteForever, contentDescription = "Kalıcı olarak sil")
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        ConfirmDialog(
            title = "Çöp kutusu boşaltılsın mı?",
            text = "${notes.size} not kalıcı olarak silinecek. Bu işlem geri alınamaz.",
            confirm = "Boşalt",
            onConfirm = onEmptyTrash,
            onDismiss = { confirmEmpty = false },
        )
    }
    confirmDeleteId?.let { id ->
        val note = notes.firstOrNull { it.id == id }
        if (note == null) {
            confirmDeleteId = null
        } else {
            ConfirmDialog(
                title = "Not kalıcı olarak silinsin mi?",
                text = "\"${note.displayTitle()}\" geri getirilemeyecek.",
                confirm = "Sil",
                onConfirm = { onDeleteForever(note) },
                onDismiss = { confirmDeleteId = null },
            )
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}

/** Title, else the first line of the content, else a placeholder. */
internal fun Note.displayTitle(): String =
    title.trim().ifEmpty {
        (if (isChecklist) items.firstOrNull { it.text.isNotBlank() }?.text else body.lineSequence().firstOrNull { it.isNotBlank() })
            ?.trim() ?: "Başlıksız not"
    }
