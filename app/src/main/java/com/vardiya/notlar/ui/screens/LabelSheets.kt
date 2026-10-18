package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.ui.components.hapticClick
import com.vardiya.notlar.domain.Labels

/** Puts labels on one note: tick the ones it should carry, or make a new one. */
@Composable
fun LabelPickerSheet(
    labels: List<String>,
    noteLabels: List<String>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        LabelPickerContent(labels, noteLabels, onToggle, onCreate, Modifier.verticalScroll(rememberScrollState()))
    }
}

/** The picker's content; separate so screenshot tests can render it without a window. */
@Composable
internal fun LabelPickerContent(
    labels: List<String>,
    noteLabels: List<String>,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetColumn("Etiketler", modifier) {
        NewLabelField(onCreate)
        if (labels.isEmpty()) {
            Hint("Henüz etiket yok. Yukarıya bir ad yazıp ekle; örneğin İş, Ev ya da Alışveriş.")
        } else {
            SettingsGroup(
                "Bu notun etiketleri",
                labels.map { name ->
                    {
                        val checked = noteLabels.any { Labels.same(it, name) }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(name) })
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RowText(Icons.AutoMirrored.Outlined.Label, name, subtitle = null, Modifier.weight(1f))
                            Checkbox(checked = checked, onCheckedChange = null)
                        }
                    }
                },
            )
        }
    }
}

/** Every label in one place: add, rename, delete. Deleting a label keeps the notes that carried it. */
@Composable
fun LabelManagerSheet(
    labels: List<String>,
    onCreate: (String) -> Unit,
    onRename: (old: String, new: String) -> Unit,
    onDelete: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        LabelManagerContent(labels, onCreate, onRename = { renaming = it }, onDelete, Modifier.verticalScroll(rememberScrollState()))
    }
    renaming?.let { old ->
        RenameLabelDialog(
            old = old,
            onRename = { new ->
                renaming = null
                onRename(old, new)
            },
            onDismiss = { renaming = null },
        )
    }
}

@Composable
internal fun LabelManagerContent(
    labels: List<String>,
    onCreate: (String) -> Unit,
    onRename: (String) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetColumn("Etiketleri düzenle", modifier) {
        NewLabelField(onCreate)
        if (labels.isEmpty()) {
            Hint("Henüz etiket yok.")
        } else {
            SettingsGroup(
                "Etiketler",
                labels.map { name ->
                    {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .padding(start = 16.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RowText(Icons.AutoMirrored.Outlined.Label, name, subtitle = null, Modifier.weight(1f))
                            IconButton(onClick = hapticClick { onRename(name) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Edit, contentDescription = "$name etiketini yeniden adlandır")
                            }
                            IconButton(onClick = hapticClick { onDelete(name) }, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.Outlined.Delete, contentDescription = "$name etiketini sil")
                            }
                        }
                    }
                },
            )
            Hint("Bir etiketi silmek notları silmez; etiket yalnızca notlardan kalkar.")
        }
    }
}

@Composable
private fun SheetColumn(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
    )
}

/** A name field with an add button; Enter adds too. */
@Composable
private fun NewLabelField(onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val add = {
        if (Labels.clean(name).isNotEmpty()) {
            onCreate(Labels.clean(name))
            name = ""
        }
    }
    OutlinedTextField(
        value = name,
        onValueChange = { name = it.take(Labels.MAX_LENGTH) },
        label = { Text("Yeni etiket") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { add() }),
        trailingIcon = {
            IconButton(onClick = add, enabled = Labels.clean(name).isNotEmpty(), shapes = IconButtonDefaults.shapes()) {
                Icon(Icons.Filled.Add, contentDescription = "Etiketi ekle")
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun RenameLabelDialog(old: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    var name by rememberSaveable { mutableStateOf(old) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Etiketi yeniden adlandır") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(Labels.MAX_LENGTH) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (Labels.clean(name).isNotEmpty()) onRename(name) }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name) }, enabled = Labels.clean(name).isNotEmpty()) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    )
}
