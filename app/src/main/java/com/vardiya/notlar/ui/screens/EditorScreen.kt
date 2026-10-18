package com.vardiya.notlar.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AlarmAdd
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.ChecklistItem
import com.vardiya.notlar.domain.EditHistory
import com.vardiya.notlar.domain.Labels
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteColor
import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.OutsideEdits
import com.vardiya.notlar.domain.ReminderRepeat
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.ui.components.AnimatedIcon
import com.vardiya.notlar.ui.components.IconMotion
import com.vardiya.notlar.ui.components.formatEdited
import com.vardiya.notlar.ui.components.scaled
import com.vardiya.notlar.ui.theme.container
import com.vardiya.notlar.ui.theme.label
import com.vardiya.notlar.ui.theme.notePalette
import com.vardiya.notlar.ui.theme.onContainer
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private const val SAVE_DELAY_MS = 500L
private const val HALF_TURN = 180f
private const val CHECK_POP = 1.3f
private const val DONE_ALPHA = 0.6f

/**
 * Writes and edits one note. Changes are saved half a second after typing stops and again on the
 * way out, so nothing is lost; a note left blank is not kept.
 */
@Composable
fun EditorScreen(
    initial: Note,
    onSave: (Note) -> Unit,
    onClose: () -> Unit,
    onShare: (Note) -> Unit,
    onArchive: (Note) -> Unit,
    onTrash: (Note) -> Unit,
    onDuplicate: (Note) -> Unit,
    modifier: Modifier = Modifier,
    onShareMarkdown: (Note) -> Unit = {},
    onCopy: (Note) -> Unit = {},
    settings: Settings = Settings(),
    onSettingsChange: (Settings) -> Unit = {},
    onCreateLabel: (String) -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    today: LocalDate = LocalDate.now(),
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    // The stored note as the draft last saw it; what changes outside (widget, reminder) is merged in.
    var seen by remember(initial.id) { mutableStateOf(initial) }
    LaunchedEffect(initial) {
        if (initial != seen) {
            draft = OutsideEdits.merge(draft, seen, initial)
            seen = initial
        }
    }
    val history = remember(initial.id) { EditHistory(initial) }
    // Bumped after every recorded change so the undo and redo buttons recompose.
    var historyVersion by remember(initial.id) { mutableIntStateOf(0) }
    LaunchedEffect(initial.id) {
        snapshotFlow { draft }.collect {
            if (it != history.current) {
                history.record(it, System.currentTimeMillis())
                historyVersion++
            }
        }
    }
    // Reading historyVersion makes these follow the history, which is not snapshot state itself.
    val canUndo = historyVersion >= 0 && history.canUndo
    val canRedo = historyVersion >= 0 && history.canRedo
    fun step(to: Note?) {
        to ?: return
        draft = to
        historyVersion++
    }
    val currentSave by rememberUpdatedState(onSave)
    LaunchedEffect(initial.id) {
        snapshotFlow { draft }.collectLatest {
            delay(SAVE_DELAY_MS)
            currentSave(it)
        }
    }
    val close = {
        onSave(draft)
        onClose()
    }
    BackHandler(onBack = close)

    var sheet by rememberSaveable { mutableStateOf<EditorSheet?>(null) }
    val askNotifications = rememberNotificationPermission()
    val scale = settings.textSize.scale

    val palette = notePalette
    val background = palette.container(draft.color)
    val content = palette.onContainer(draft.color)

    Scaffold(
        modifier = modifier,
        containerColor = background,
        contentColor = content,
        // Lifted above the floating toolbar.
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.padding(bottom = 80.dp)) },
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = close, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    // Undo and redo show up after the first change.
                    AnimatedVisibility(visible = canUndo || canRedo) {
                        Row {
                            IconButton(onClick = { step(history.undo()) }, enabled = canUndo, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Geri al")
                            }
                            IconButton(onClick = { step(history.redo()) }, enabled = canRedo, shapes = IconButtonDefaults.shapes()) {
                                Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Yinele")
                            }
                        }
                    }
                    IconButton(onClick = { sheet = EditorSheet.REMINDER }, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            if (draft.reminderAt != null) Icons.Filled.Alarm else Icons.Outlined.AlarmAdd,
                            contentDescription = if (draft.reminderAt != null) "Hatırlatıcıyı değiştir" else "Hatırlatıcı kur",
                        )
                    }
                    if (!draft.archived) {
                        IconToggleButton(
                            checked = draft.pinned,
                            onCheckedChange = { draft = draft.copy(pinned = it) },
                            shapes = IconButtonDefaults.toggleableShapes(),
                        ) {
                            AnimatedIcon(
                                if (draft.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = if (draft.pinned) "Sabitlemeyi kaldır" else "Sabitle",
                                play = draft.pinned,
                                motion = IconMotion.TWIST,
                            )
                        }
                    }
                    MoreMenu(
                        note = draft,
                        onDuplicate = {
                            onSave(draft)
                            onDuplicate(draft)
                        },
                        onUncheckAll = { draft = NoteEdits.uncheckAll(draft) },
                        onClearCompletions = { draft = NoteEdits.removeChecked(draft) },
                        onLabels = { sheet = EditorSheet.LABELS },
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = background,
                    scrolledContainerColor = background,
                    navigationIconContentColor = content,
                    actionIconContentColor = content,
                ),
            )
        },
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding()),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp),
            ) {
                PlainField(
                    value = draft.title,
                    onValueChange = { draft = draft.copy(title = it) },
                    placeholder = "Başlık",
                    style = MaterialTheme.typography.headlineSmallEmphasized.scaled(scale),
                    imeAction = ImeAction.Next,
                    modifier = Modifier.semantics { heading() },
                )
                EditorChips(
                    note = draft,
                    today = today,
                    onReminder = { sheet = EditorSheet.REMINDER },
                    onLabels = { sheet = EditorSheet.LABELS },
                )
                if (draft.isChecklist) {
                    ChecklistEditor(items = draft.items, textScale = scale, onItemsChange = { draft = draft.copy(items = it) })
                } else {
                    PlainField(
                        value = draft.body,
                        onValueChange = { draft = draft.copy(body = it) },
                        placeholder = "Not",
                        style = MaterialTheme.typography.bodyLarge.scaled(scale),
                        singleLine = false,
                    )
                }
                Text(
                    "Düzenlendi: ${formatEdited(draft.updatedAt, today = today)} · ${NoteEdits.wordCount(draft)} kelime",
                    style = MaterialTheme.typography.labelMedium,
                    color = content.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
                // Keeps the last lines clear of the floating toolbar.
                Spacer(Modifier.height(112.dp))
            }

            EditorToolbar(
                note = draft,
                onToggleChecklist = {
                    draft = if (draft.isChecklist) {
                        NoteRules.toText(draft)
                    } else {
                        var next = System.currentTimeMillis()
                        NoteRules.toChecklist(draft) { next++ }.let { converted ->
                            if (converted.items.isEmpty()) converted.copy(items = listOf(ChecklistItem(next))) else converted
                        }
                    }
                },
                onColor = { draft = draft.copy(color = it) },
                onTextSize = { sheet = EditorSheet.TEXT_SIZE },
                onArchive = { onArchive(draft) },
                onShare = { onShare(draft) },
                onShareMarkdown = { onShareMarkdown(draft) },
                onCopy = { onCopy(draft) },
                onTrash = { onTrash(draft) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp),
            )
        }
    }

    when (sheet) {
        EditorSheet.REMINDER -> ReminderSheet(
            reminderAt = draft.reminderAt,
            repeat = draft.reminderRepeat,
            actions = ReminderActions(
                onSet = { at, repeat ->
                    draft = draft.copy(reminderAt = at, reminderRepeat = repeat)
                    askNotifications()
                },
                onRemove = { draft = draft.copy(reminderAt = null, reminderRepeat = ReminderRepeat.NONE) },
            ),
            onDismissRequest = { sheet = null },
        )
        EditorSheet.LABELS -> LabelPickerSheet(
            labels = Labels.all(settings.labels, listOf(draft)),
            noteLabels = draft.labels,
            onToggle = { draft = Labels.toggle(draft, it) },
            onCreate = { name ->
                onCreateLabel(name)
                if (draft.labels.none { Labels.same(it, name) }) draft = Labels.toggle(draft, name)
            },
            onDismissRequest = { sheet = null },
        )
        EditorSheet.TEXT_SIZE -> TextSizeSheet(settings, onSettingsChange, onDismissRequest = { sheet = null })
        null -> Unit
    }
}

/** The sheets the editor opens over the note. */
private enum class EditorSheet { REMINDER, LABELS, TEXT_SIZE }

/** Overflow actions: copy the note, and the bulk to-do actions when there are done items. */
@Composable
private fun MoreMenu(
    note: Note,
    onDuplicate: () -> Unit,
    onUncheckAll: () -> Unit,
    onClearCompletions: () -> Unit,
    onLabels: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Diğer seçenekler")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("Etiketler") },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null) },
                onClick = {
                    open = false
                    onLabels()
                },
            )
            DropdownMenuItem(
                text = { Text("Kopyasını oluştur") },
                leadingIcon = { Icon(Icons.Outlined.ContentCopy, contentDescription = null) },
                onClick = {
                    open = false
                    onDuplicate()
                },
            )
            if (note.isChecklist && note.items.any { it.checked }) {
                DropdownMenuItem(
                    text = { Text("Tüm işaretleri kaldır") },
                    leadingIcon = { Icon(Icons.Outlined.CheckBoxOutlineBlank, contentDescription = null) },
                    onClick = {
                        open = false
                        onUncheckAll()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Tamamlananları sil") },
                    leadingIcon = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                    onClick = {
                        open = false
                        onClearCompletions()
                    },
                )
            }
        }
    }
}

@Composable
private fun EditorToolbar(
    note: Note,
    onToggleChecklist: () -> Unit,
    onColor: (NoteColor) -> Unit,
    onTextSize: () -> Unit,
    onArchive: () -> Unit,
    onShare: () -> Unit,
    onShareMarkdown: () -> Unit,
    onCopy: () -> Unit,
    onTrash: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var colorMenu by remember { mutableStateOf(false) }
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        // The vibrant toolbar is primaryContainer, the same as a yellow note; use the standard one there.
        colors = if (note.color == NoteColor.YELLOW) {
            FloatingToolbarDefaults.standardFloatingToolbarColors()
        } else {
            FloatingToolbarDefaults.vibrantFloatingToolbarColors()
        },
    ) {
        ToolbarButton(
            icon = if (note.isChecklist) Icons.AutoMirrored.Filled.Notes else Icons.Filled.Checklist,
            description = if (note.isChecklist) "Düz metne çevir" else "Yapılacak listesine çevir",
            onClick = onToggleChecklist,
        )
        Box {
            ToolbarButton(icon = Icons.Outlined.Palette, description = "Renk", onClick = { colorMenu = true })
            DropdownMenu(expanded = colorMenu, onDismissRequest = { colorMenu = false }) {
                NoteColor.entries.forEach { color ->
                    DropdownMenuItem(
                        text = { Text(color.label) },
                        leadingIcon = { ColorDot(color) },
                        trailingIcon = if (note.color == color) {
                            { Icon(Icons.Filled.Check, contentDescription = "Seçili") }
                        } else {
                            null
                        },
                        onClick = {
                            onColor(color)
                            colorMenu = false
                        },
                    )
                }
            }
        }
        ToolbarButton(icon = Icons.Outlined.FormatSize, description = "Yazı boyutu", onClick = onTextSize)
        ToolbarButton(
            icon = if (note.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            description = if (note.archived) "Arşivden çıkar" else "Arşivle",
            onClick = onArchive,
        )
        ShareSplitButton(onShare = onShare, onShareMarkdown = onShareMarkdown, onCopy = onCopy)
        ToolbarButton(icon = Icons.Outlined.Delete, description = "Çöp kutusuna taşı", onClick = onTrash)
    }
}

/**
 * Expressive split button: the left half shares right away, the arrow opens the other ways to
 * send the note out. Filled, so it stands out on both toolbar colors.
 */
@Composable
private fun ShareSplitButton(onShare: () -> Unit, onShareMarkdown: () -> Unit, onCopy: () -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (menu) HALF_TURN else 0f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "shareArrow",
    )
    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(onClick = onShare) {
                    Icon(
                        Icons.Filled.Share,
                        contentDescription = "Paylaş",
                        modifier = Modifier.size(SplitButtonDefaults.LeadingIconSize),
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = menu,
                    onCheckedChange = { menu = it },
                    modifier = Modifier.semantics {
                        stateDescription = if (menu) "Açık" else "Kapalı"
                    },
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        contentDescription = "Diğer paylaşma seçenekleri",
                        modifier = Modifier
                            .size(SplitButtonDefaults.TrailingIconSize)
                            .graphicsLayer { rotationZ = rotation },
                    )
                }
            },
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            ShareMenuItem("Metin olarak paylaş", Icons.Filled.Share) {
                menu = false
                onShare()
            }
            ShareMenuItem("Markdown olarak paylaş", Icons.Outlined.Code) {
                menu = false
                onShareMarkdown()
            }
            ShareMenuItem("Panoya kopyala", Icons.Outlined.ContentCopy) {
                menu = false
                onCopy()
            }
        }
    }
}

@Composable
private fun ShareMenuItem(text: String, icon: ImageVector, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
    )
}

@Composable
private fun ToolbarButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, shapes = IconButtonDefaults.shapes()) {
        Icon(icon, contentDescription = description)
    }
}

@Composable
private fun ColorDot(color: NoteColor) {
    val palette = notePalette
    Box(
        Modifier
            .size(24.dp)
            .background(palette.container(color), CircleShape)
            .border(1.dp, palette.outline, CircleShape),
    )
}

/** To-do rows: open items first, then the done ones under their own heading. */
@Composable
private fun ChecklistEditor(items: List<ChecklistItem>, textScale: Float, onItemsChange: (List<ChecklistItem>) -> Unit) {
    // The row just added asks for focus once it is on screen.
    var focusId by remember { mutableStateOf<Long?>(null) }

    fun addAfter(afterId: Long?) {
        val id = (items.maxOfOrNull { it.id } ?: 0L) + 1
        val index = afterId?.let { target -> items.indexOfFirst { it.id == target } + 1 } ?: items.size
        onItemsChange(items.toMutableList().apply { add(index.coerceIn(0, size), ChecklistItem(id)) })
        focusId = id
    }

    val open = items.filterNot { it.checked }
    val done = items.filter { it.checked }

    // Rows added, removed or moved under "Tamamlananlar" resize the list smoothly.
    Column(Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec())) {
        open.forEach { item ->
            ChecklistRow(
                item = item,
                textScale = textScale,
                requestFocus = focusId == item.id,
                onFocus = { focusId = null },
                onChange = { changed -> onItemsChange(items.map { if (it.id == changed.id) changed else it }) },
                onRemove = { onItemsChange(items.filterNot { it.id == item.id }) },
                onNext = { addAfter(item.id) },
            )
        }
        TextButton(onClick = { addAfter(open.lastOrNull()?.id) }, modifier = Modifier.padding(start = 8.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Öğe ekle")
        }
        if (done.isNotEmpty()) {
            Text(
                "Tamamlananlar (${done.size})",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                    .semantics { heading() },
            )
            done.forEach { item ->
                ChecklistRow(
                    item = item,
                    textScale = textScale,
                    requestFocus = focusId == item.id,
                    onFocus = { focusId = null },
                    onChange = { changed -> onItemsChange(items.map { if (it.id == changed.id) changed else it }) },
                    onRemove = { onItemsChange(items.filterNot { it.id == item.id }) },
                    onNext = { addAfter(open.lastOrNull()?.id) },
                )
            }
        }
    }
}

@Composable
private fun ChecklistRow(
    item: ChecklistItem,
    textScale: Float,
    requestFocus: Boolean,
    onFocus: () -> Unit,
    onChange: (ChecklistItem) -> Unit,
    onRemove: () -> Unit,
    onNext: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnFocus by rememberUpdatedState(onFocus)
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focusRequester.requestFocus()
            currentOnFocus()
        }
    }
    // Ticking an item pops its box; a finished item's text softens.
    val pop = remember { Animatable(1f) }
    val spatial = MaterialTheme.motionScheme.fastSpatialSpec<Float>()
    var shown by remember { mutableStateOf(item.checked) }
    LaunchedEffect(item.checked) {
        if (item.checked && !shown) {
            pop.snapTo(CHECK_POP)
            pop.animateTo(1f, spatial)
        }
        shown = item.checked
    }
    val textAlpha by animateFloatAsState(
        targetValue = if (item.checked) DONE_ALPHA else 1f,
        animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
        label = "itemAlpha",
    )
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onChange(item.copy(checked = it)) },
            modifier = Modifier.graphicsLayer {
                scaleX = pop.value
                scaleY = pop.value
            },
        )
        PlainField(
            value = item.text,
            onValueChange = { onChange(item.copy(text = it.replace("\n", ""))) },
            placeholder = "Yapılacak",
            style = MaterialTheme.typography.bodyLarge.scaled(textScale).copy(
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
            ),
            imeAction = ImeAction.Next,
            onImeAction = onNext,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer { alpha = textAlpha }
                .focusRequester(focusRequester),
        )
        IconButton(onClick = onRemove, shapes = IconButtonDefaults.shapes()) {
            Icon(Icons.Filled.Close, contentDescription = "Öğeyi sil")
        }
    }
}

/** A borderless text field on the note's own background. */
@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onImeAction: (() -> Unit)? = null,
) {
    val content = LocalContentColor.current
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        textStyle = style,
        placeholder = { Text(placeholder, style = style, color = content.copy(alpha = 0.6f)) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
        keyboardActions = KeyboardActions(onAny = { onImeAction?.invoke() ?: defaultKeyboardAction(imeAction) }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = content,
            unfocusedTextColor = content,
        ),
    )
}
