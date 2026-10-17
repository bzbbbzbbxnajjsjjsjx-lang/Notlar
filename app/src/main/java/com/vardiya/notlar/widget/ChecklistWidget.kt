package com.vardiya.notlar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.vardiya.notlar.MainActivity
import com.vardiya.notlar.R
import com.vardiya.notlar.data.NoteRepository
import com.vardiya.notlar.domain.ChecklistItem
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.NoteEdits

/** The note this widget shows; unset until a list is chosen in the widget itself. */
private val NoteIdKey = longPreferencesKey("noteId")
private val NoteParam = ActionParameters.Key<Long>("noteId")
private val ItemParam = ActionParameters.Key<Long>("itemId")
private const val CHOOSER_LIMIT = 8

/**
 * A to-do list on the home screen. A new widget first offers the lists to choose from; the chosen
 * list's items can be checked right there. While the app lock is on, nothing of the notes shows.
 */
class ChecklistWidget : GlanceAppWidget() {

    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repository = NoteRepository.get(context)
        provideContent {
            val notes by repository.notes.collectAsState(initial = null)
            val lock by repository.lock.collectAsState(initial = null)
            val chosenId = currentState<Preferences>()[NoteIdKey]
            GlanceTheme(colors = WidgetColors) {
                WidgetSurface {
                    val loadedNotes = notes
                    val loadedLock = lock
                    when {
                        loadedNotes == null || loadedLock == null -> Unit
                        loadedLock.hasPin || loadedLock.unreadable -> Locked(context)
                        else -> {
                            val note = loadedNotes.firstOrNull { it.id == chosenId && it.isChecklist && !it.isTrashed }
                            if (note == null) {
                                Chooser(loadedNotes.filter { it.isChecklist && !it.isTrashed })
                            } else {
                                ListContent(context, note)
                            }
                        }
                    }
                }
            }
        }
    }
}

class ChecklistWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ChecklistWidget()
}

/** Toggles one item of the widget's list. */
class ToggleItemAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[NoteParam] ?: return
        val itemId = parameters[ItemParam] ?: return
        val now = System.currentTimeMillis()
        NoteRepository.get(context).edit { notes ->
            notes.map { if (it.id == noteId) NoteEdits.toggleItem(it, itemId).copy(updatedAt = now) else it }
        }
        ChecklistWidget().update(context, glanceId)
    }
}

/** Shows the chosen list in this widget, or, without a note, the chooser again. */
class ChooseListAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        updateAppWidgetState(context, glanceId) { prefs ->
            val noteId = parameters[NoteParam]
            if (noteId == null) prefs.remove(NoteIdKey) else prefs[NoteIdKey] = noteId
        }
        ChecklistWidget().update(context, glanceId)
    }
}

@Composable
private fun ListContent(context: Context, note: Note) {
    val open = note.items.filter { !it.checked && it.text.isNotBlank() }
    val done = note.items.filter { it.checked && it.text.isNotBlank() }
    Column(GlanceModifier.fillMaxSize()) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                note.title.trim().ifEmpty { "Yapılacaklar" },
                style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(actionStartActivity(MainActivity.openNoteIntent(context, note.id))),
            )
            Text(
                "${done.size}/${open.size + done.size}",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                modifier = GlanceModifier.padding(horizontal = 8.dp),
            )
            IconAction(R.drawable.ic_widget_swap, "Başka liste seç", actionRunCallback<ChooseListAction>())
            IconAction(
                R.drawable.ic_widget_add,
                "Öğe eklemek için notu aç",
                actionStartActivity(MainActivity.openNoteIntent(context, note.id)),
            )
        }
        Spacer(GlanceModifier.height(4.dp))
        if (open.isEmpty() && done.isEmpty()) {
            Text("Liste boş", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp))
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(open + done, itemId = { it.id }) { item -> ItemRow(note.id, item) }
            }
        }
    }
}

@Composable
private fun ItemRow(noteId: Long, item: ChecklistItem) {
    CheckBox(
        checked = item.checked,
        onCheckedChange = actionRunCallback<ToggleItemAction>(actionParametersOf(NoteParam to noteId, ItemParam to item.id)),
        text = item.text,
        style = TextStyle(
            color = if (item.checked) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
            fontSize = 15.sp,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
        ),
        maxLines = 2,
        modifier = GlanceModifier.fillMaxWidth(),
    )
}

/** A new widget: pick which list it shows. */
@Composable
private fun Chooser(lists: List<Note>) {
    Column(GlanceModifier.fillMaxSize()) {
        Text(
            "Hangi liste görünsün?",
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(GlanceModifier.height(8.dp))
        if (lists.isEmpty()) {
            Text(
                "Henüz yapılacak listesi yok. Notlar'da + ile bir liste oluştur.",
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
            )
        } else {
            LazyColumn(GlanceModifier.fillMaxSize()) {
                items(lists.sortedByDescending { it.updatedAt }.take(CHOOSER_LIMIT), itemId = { it.id }) { note ->
                    Box(GlanceModifier.padding(vertical = 2.dp)) {
                        Text(
                            note.title.trim().ifEmpty { "Adsız liste" },
                            style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 15.sp),
                            maxLines = 1,
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(GlanceTheme.colors.secondaryContainer)
                                .cornerRadius(16.dp)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .clickable(actionRunCallback<ChooseListAction>(actionParametersOf(NoteParam to note.id))),
                        )
                    }
                }
            }
        }
    }
}

/** With the app lock on, the widget only offers to open the app. */
@Composable
private fun Locked(context: Context) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(MainActivity.launchIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            ImageProvider(R.drawable.ic_widget_lock),
            contentDescription = null,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(32.dp),
        )
        Spacer(GlanceModifier.height(8.dp))
        Text("Notlar kilitli", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold))
        Text("Açmak için dokun", style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
    }
}

@Composable
internal fun WidgetSurface(content: @Composable () -> Unit) {
    Box(
        GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(12.dp),
    ) { content() }
}

@Composable
private fun IconAction(icon: Int, description: String, onClick: Action) {
    Box(
        GlanceModifier
            .size(48.dp)
            .cornerRadius(24.dp)
            .clickable(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            ImageProvider(icon),
            contentDescription = description,
            colorFilter = ColorFilter.tint(GlanceTheme.colors.primary),
            modifier = GlanceModifier.size(24.dp),
        )
    }
}
