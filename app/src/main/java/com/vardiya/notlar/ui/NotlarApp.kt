package com.vardiya.notlar.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteEdits
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.ui.components.LocalSharedNoteScope
import com.vardiya.notlar.ui.components.SharedNoteScope
import com.vardiya.notlar.ui.components.sharedNoteBounds
import com.vardiya.notlar.domain.Security
import com.vardiya.notlar.ui.screens.EditorScreen
import com.vardiya.notlar.ui.screens.ExportBackupDialog
import com.vardiya.notlar.ui.screens.ImportPasswordDialog
import com.vardiya.notlar.ui.screens.PinSetupScreen
import com.vardiya.notlar.ui.screens.PrivacyActions
import com.vardiya.notlar.ui.screens.MainScreen
import com.vardiya.notlar.ui.screens.SettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private const val EDITOR_START_SCALE = 0.92f
private const val BACKUP_MIME = "application/json"

/** What the app was opened for from outside: a launcher shortcut or text shared by another app. */
sealed interface LaunchAction {
    data object NewNote : LaunchAction
    data object NewChecklist : LaunchAction
    data class Shared(val text: String, val subject: String?) : LaunchAction
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun NotlarApp(
    modifier: Modifier = Modifier,
    viewModel: NotesViewModel = viewModel(),
    lockViewModel: LockViewModel = viewModel(),
    biometricAvailable: Boolean = false,
    launchAction: LaunchAction? = null,
    onLaunchActionConsume: () -> Unit = {},
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.NOTES) }
    // Ids of the notes selected with a long press; a list so it survives configuration changes.
    var selection by rememberSaveable { mutableStateOf(emptyList<Long>()) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    // Id of the note open in the editor; a new note gets its id before it is first saved.
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val motion = MaterialTheme.motionScheme
    // A note opened before its first save lands in the stored list (a new list, a copy, a share).
    var pending by remember { mutableStateOf<Note?>(null) }

    fun open(note: Note) {
        pending = note
        openId = note.id
    }

    val currentOnConsume by rememberUpdatedState(onLaunchActionConsume)
    LaunchedEffect(launchAction) {
        val action = launchAction ?: return@LaunchedEffect
        open(
            when (action) {
                LaunchAction.NewNote -> viewModel.newNote()
                LaunchAction.NewChecklist -> viewModel.newChecklist()
                is LaunchAction.Shared -> viewModel.createFromShare(action.text, action.subject)
            },
        )
        currentOnConsume()
    }

    fun undoable(message: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "Geri al", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    fun message(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    /** Runs [send] with [text], or says the note is empty instead of sending nothing. */
    fun shareOrWarn(text: String, send: (String) -> Unit) {
        if (text.isBlank()) message("Not boş") else send(text)
    }

    // The password of the backup being exported; kept only in memory until the file is written.
    var exportPassword by remember { mutableStateOf<String?>(null) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var sealedImport by remember { mutableStateOf<String?>(null) }
    var importWrong by remember { mutableStateOf(false) }
    var pinSetup by rememberSaveable { mutableStateOf(false) }

    suspend fun import(raw: String) {
        val count = viewModel.importBackup(raw)
        message(if (count == null) "Bu dosya bir Notlar yedeği değil" else "$count not içe aktarıldı")
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME)) { uri ->
        val password = exportPassword
        exportPassword = null
        if (uri == null) return@rememberLauncherForActivityResult
        val backup = viewModel.exportBackup()
        scope.launch {
            val written = runCatching {
                // Sealing runs PBKDF2, which is slow on purpose; keep it off the main thread.
                val content = withContext(Dispatchers.Default) { password?.let { Security.sealBackup(backup, it) } ?: backup }
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) }
                }
            }.getOrNull() != null
            val sealedNote = if (password != null) " (şifreli)" else ""
            message(if (written) "Yedek kaydedildi$sealedNote" else "Yedek kaydedilemedi")
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val raw = runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
            }.getOrNull()
            when {
                raw == null -> message("Dosya okunamadı")
                Security.isSealed(raw) -> {
                    importWrong = false
                    sealedImport = raw
                }
                else -> import(raw)
            }
        }
    }

    if (showSettings) {
        SettingsSheet(
            settings = settings,
            onSettingsChange = { changed -> viewModel.updateSettings { changed } },
            privacy = PrivacyActions(
                onLockChange = { enabled ->
                    if (enabled) {
                        showSettings = false
                        pinSetup = true
                    } else {
                        lockViewModel.disableLock()
                        message("Uygulama kilidi kapatıldı")
                    }
                },
                onChangePin = {
                    showSettings = false
                    pinSetup = true
                },
                biometricAvailable = biometricAvailable,
            ),
            onExport = {
                showSettings = false
                showExport = true
            },
            onImport = {
                showSettings = false
                lockViewModel.stayOpenThroughNextTrip()
                importLauncher.launch(arrayOf(BACKUP_MIME, "text/plain", "application/octet-stream"))
            },
            onDismissRequest = { showSettings = false },
        )
    }
    if (showExport) {
        ExportBackupDialog(
            onExport = { password ->
                showExport = false
                exportPassword = password
                lockViewModel.stayOpenThroughNextTrip()
                exportLauncher.launch("notlar-yedek-${LocalDate.now()}.json")
            },
            onDismiss = { showExport = false },
        )
    }
    sealedImport?.let { raw ->
        ImportPasswordDialog(
            wrong = importWrong,
            onOpen = { password ->
                scope.launch {
                    val opened = withContext(Dispatchers.Default) { Security.openBackup(raw, password) }
                    if (opened == null) {
                        importWrong = true
                    } else {
                        sealedImport = null
                        import(opened)
                    }
                }
            },
            onDismiss = { sealedImport = null },
        )
    }
    if (pinSetup) {
        BackHandler { pinSetup = false }
        PinSetupScreen(
            onPinConfirm = { pin ->
                lockViewModel.setPin(pin)
                pinSetup = false
                message("Uygulama kilidi açık")
            },
            onCancel = { pinSetup = false },
        )
        return
    }

    SharedTransitionLayout(modifier) {
        AnimatedContent(
            targetState = openId,
            transitionSpec = {
                // A note with a card grows out of it (shared bounds); a new note zooms in from the middle.
                val fromCard = targetState?.let { target -> notes.any { it.id == target } } ?: true
                val enter = if (fromCard) {
                    fadeIn(motion.defaultEffectsSpec())
                } else {
                    fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), EDITOR_START_SCALE)
                }
                enter togetherWith fadeOut(motion.fastEffectsSpec())
            },
            label = "editor",
        ) { id ->
            CompositionLocalProvider(LocalSharedNoteScope provides SharedNoteScope(this@SharedTransitionLayout, this@AnimatedContent)) {
                if (id == null) {
                    MainScreen(
                        notes = notes,
                        loaded = loaded,
                        section = section,
                        settings = settings,
                        selection = selection.toSet(),
                        onSelectionChange = { selection = it.toList() },
                        onSectionChange = {
                            selection = emptyList()
                            section = it
                        },
                        onOpen = { openId = it.id },
                        onNewNote = { open(viewModel.newNote()) },
                        onNewChecklist = { open(viewModel.newChecklist()) },
                        onPasteNote = {
                            scope.launch {
                                // Only plain text: an image or file on the clipboard would otherwise become its content:// address.
                                val clip = clipboard.getClipEntry()?.clipData
                                val text = clip
                                    ?.takeIf { it.itemCount > 0 && it.description.hasMimeType("text/*") }
                                    ?.getItemAt(0)?.text?.toString()
                                if (text.isNullOrBlank()) {
                                    message("Panoda metin yok")
                                } else {
                                    open(viewModel.createFromShare(text, subject = null))
                                }
                            }
                        },
                        onRestore = { note ->
                            viewModel.restore(setOf(note.id))
                            undoable("Not geri yüklendi") { viewModel.moveToTrash(setOf(note.id)) }
                        },
                        onDeleteForever = { viewModel.deleteForever(it.id) },
                        onEmptyTrash = viewModel::emptyTrash,
                        onToggleListView = { viewModel.updateSettings { it.copy(listView = !it.listView) } },
                        onOpenSettings = { showSettings = true },
                        onPinSelection = { pinned ->
                            viewModel.setPinned(selection.toSet(), pinned)
                            selection = emptyList()
                        },
                        onArchiveSelection = {
                            val ids = selection.toSet()
                            val archive = section != Section.ARCHIVE
                            viewModel.setArchived(ids, archive)
                            selection = emptyList()
                            val text = if (archive) "${ids.size} not arşivlendi" else "${ids.size} not arşivden çıkarıldı"
                            undoable(text) { viewModel.setArchived(ids, !archive) }
                        },
                        onTrashSelection = {
                            val ids = selection.toSet()
                            viewModel.moveToTrash(ids)
                            selection = emptyList()
                            undoable("${ids.size} not çöp kutusuna taşındı") { viewModel.restore(ids) }
                        },
                        onRememberSearch = viewModel::rememberSearch,
                        onForgetSearch = viewModel::forgetSearch,
                        onSwipeArchive = { note ->
                            viewModel.setArchived(setOf(note.id), !note.archived)
                            val text = if (note.archived) "Not arşivden çıkarıldı" else "Not arşivlendi"
                            undoable(text) { viewModel.refile(note) }
                        },
                        onSwipeTrash = { note ->
                            viewModel.moveToTrash(setOf(note.id))
                            undoable("Not çöp kutusuna taşındı") { viewModel.refile(note) }
                        },
                        snackbarHostState = snackbar,
                    )
                } else {
                    val initial = notes.firstOrNull { it.id == id }
                        ?: pending?.takeIf { it.id == id }
                        ?: Note(id = id, createdAt = id, updatedAt = id)
                    EditorScreen(
                        initial = initial,
                        onSave = viewModel::save,
                        modifier = Modifier.sharedNoteBounds(id),
                        snackbarHostState = snackbar,
                        onDuplicate = { note ->
                            open(viewModel.duplicate(note))
                            message("Kopyası oluşturuldu")
                        },
                        onClose = { openId = null },
                        onShare = { note -> shareOrWarn(NoteRules.shareText(note)) {
                            lockViewModel.stayOpenThroughNextTrip()
                            context.shareNote(note, it)
                        } },
                        onShareMarkdown = { note -> shareOrWarn(NoteEdits.markdown(note)) {
                            lockViewModel.stayOpenThroughNextTrip()
                            context.shareNote(note, it)
                        } },
                        onCopy = { note ->
                            shareOrWarn(NoteRules.shareText(note)) { text ->
                                scope.launch {
                                    clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(note.title, text)))
                                    // Android 13 and later confirm a copy themselves.
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) message("Panoya kopyalandı")
                                }
                            }
                        },
                        onArchive = { note ->
                            viewModel.save(note)
                            viewModel.setArchived(setOf(note.id), !note.archived)
                            openId = null
                            val message = if (note.archived) "Not arşivden çıkarıldı" else "Not arşivlendi"
                            undoable(message) { viewModel.setArchived(setOf(note.id), note.archived) }
                        },
                        onTrash = { note ->
                            viewModel.save(note)
                            openId = null
                            if (!note.isBlank) {
                                viewModel.moveToTrash(setOf(note.id))
                                undoable("Not çöp kutusuna taşındı") { viewModel.restore(setOf(note.id)) }
                            }
                        },
                    )
                }
            }
        }
    }
}

private fun Context.shareNote(note: Note, text: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, note.title)
        .putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, "Notu paylaş"))
}
