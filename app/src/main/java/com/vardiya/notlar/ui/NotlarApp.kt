package com.vardiya.notlar.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteRules
import com.vardiya.notlar.domain.Section
import com.vardiya.notlar.ui.screens.EditorScreen
import com.vardiya.notlar.ui.screens.MainScreen
import kotlinx.coroutines.launch

private const val EDITOR_START_SCALE = 0.92f

@Composable
fun NotlarApp(viewModel: NotesViewModel = viewModel()) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val loaded by viewModel.loaded.collectAsStateWithLifecycle()
    var section by rememberSaveable { mutableStateOf(Section.NOTES) }
    // Id of the note open in the editor; a new note gets its id before it is first saved.
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val motion = MaterialTheme.motionScheme

    fun undoable(message: String, undo: () -> Unit) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(message, actionLabel = "Geri al", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) undo()
        }
    }

    AnimatedContent(
        targetState = openId,
        transitionSpec = {
            (fadeIn(motion.defaultEffectsSpec()) + scaleIn(motion.defaultSpatialSpec(), EDITOR_START_SCALE)) togetherWith
                fadeOut(motion.fastEffectsSpec())
        },
        label = "editor",
    ) { id ->
        if (id == null) {
            MainScreen(
                notes = notes,
                loaded = loaded,
                section = section,
                onSectionChange = { section = it },
                onOpen = { openId = it.id },
                onNewNote = { openId = viewModel.newNote().id },
                onRestore = { note ->
                    viewModel.restore(note.id)
                    undoable("Not geri yüklendi") { viewModel.moveToTrash(note.id) }
                },
                onDeleteForever = { viewModel.deleteForever(it.id) },
                onEmptyTrash = viewModel::emptyTrash,
                snackbarHostState = snackbar,
            )
        } else {
            val initial = notes.firstOrNull { it.id == id } ?: Note(id = id, createdAt = id, updatedAt = id)
            EditorScreen(
                initial = initial,
                onSave = viewModel::save,
                onClose = { openId = null },
                onShare = { context.shareNote(it) },
                onArchive = { note ->
                    viewModel.save(note)
                    viewModel.setArchived(note.id, !note.archived)
                    openId = null
                    val message = if (note.archived) "Not arşivden çıkarıldı" else "Not arşivlendi"
                    undoable(message) { viewModel.setArchived(note.id, note.archived) }
                },
                onTrash = { note ->
                    viewModel.save(note)
                    openId = null
                    if (!note.isBlank) {
                        viewModel.moveToTrash(note.id)
                        undoable("Not çöp kutusuna taşındı") { viewModel.restore(note.id) }
                    }
                },
            )
        }
    }
}

private fun Context.shareNote(note: Note) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_SUBJECT, note.title)
        .putExtra(Intent.EXTRA_TEXT, NoteRules.shareText(note))
    startActivity(Intent.createChooser(send, "Notu paylaş"))
}
