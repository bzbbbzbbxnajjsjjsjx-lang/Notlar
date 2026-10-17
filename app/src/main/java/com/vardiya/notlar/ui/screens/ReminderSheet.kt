package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlarmOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.ReminderRepeat
import com.vardiya.notlar.domain.Reminders
import com.vardiya.notlar.ui.components.formatReminder
import com.vardiya.notlar.ui.components.label
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

private val DefaultTime: LocalTime = LocalTime.of(9, 0)

/** What the reminder sheet changes: a time with its repeat, or no reminder at all. */
class ReminderActions(
    val onSet: (atMillis: Long, repeat: ReminderRepeat) -> Unit,
    val onRemove: () -> Unit,
)

/**
 * Sets the reminder of a note: a ready-made time, or a date and time of one's own, and how it
 * repeats. Choosing a time closes the sheet; changing the repeat of a set reminder keeps it open.
 */
@Composable
fun ReminderSheet(
    reminderAt: Long?,
    repeat: ReminderRepeat,
    actions: ReminderActions,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    val zone = ZoneId.systemDefault()
    var chosenRepeat by rememberSaveable { mutableStateOf(repeat) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    fun set(at: LocalDateTime) {
        actions.onSet(at.atZone(zone).toInstant().toEpochMilli(), chosenRepeat)
        onDismissRequest()
    }
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        ReminderContent(
            reminderAt = reminderAt,
            repeat = chosenRepeat,
            now = LocalDateTime.now(zone),
            onQuick = ::set,
            onRepeatChange = { changed ->
                chosenRepeat = changed
                if (reminderAt != null) actions.onSet(reminderAt, changed)
            },
            onPickDate = { pickingDate = true },
            onRemove = {
                actions.onRemove()
                onDismissRequest()
            },
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
    if (pickingDate) {
        DateDialog(
            initial = reminderAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() } ?: LocalDate.now(zone),
            onPick = { day ->
                pickingDate = false
                pickedDay = day.toEpochDay()
            },
            onDismiss = { pickingDate = false },
        )
    }
    pickedDay?.let { epochDay ->
        val day = LocalDate.ofEpochDay(epochDay)
        TimeDialog(
            initial = reminderAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalTime() } ?: DefaultTime,
            day = day,
            onPick = { time ->
                pickedDay = null
                set(day.atTime(time))
            },
            onDismiss = { pickedDay = null },
        )
    }
}

/** The sheet's content; separate so screenshot tests can render it without a window. */
@Composable
internal fun ReminderContent(
    reminderAt: Long?,
    repeat: ReminderRepeat,
    now: LocalDateTime,
    onQuick: (LocalDateTime) -> Unit,
    onRepeatChange: (ReminderRepeat) -> Unit,
    onPickDate: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Hatırlatıcı",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        if (reminderAt != null) {
            Text(
                formatReminder(reminderAt, zone, now.toLocalDate()) +
                    if (repeat != ReminderRepeat.NONE) " · ${repeat.label}" else "",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
        }
        val today = now.toLocalDate()
        val rows = Reminders.quickOptions(now).map { option ->
            // "Bugün 18:00" says it all; "Gelecek hafta" gets its day and time underneath.
            val time = formatReminder(option.millis(zone), zone, today).takeIf { it != option.label }
            tile(option.icon, option.label, time) { onQuick(option.at) }
        } + tile(Icons.Outlined.CalendarMonth, "Tarih ve saat seç…", null, onPickDate)
        SettingsGroup("Ne zaman?", rows)
        SettingsGroup(
            "Tekrar",
            listOf {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Repeat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Tekrarla", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
                    }
                    ConnectedChoice(ReminderRepeat.entries, repeat, { it.shortLabel }, onRepeatChange)
                }
            },
        )
        if (reminderAt != null) {
            TextButton(onClick = onRemove, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.AlarmOff, contentDescription = null)
                Text("Hatırlatıcıyı kaldır", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

private val Reminders.Option.icon: ImageVector
    get() = when {
        label.startsWith("Bugün") -> Icons.Outlined.NightsStay
        label.startsWith("Yarın") -> Icons.Outlined.LightMode
        else -> Icons.Outlined.DateRange
    }

private fun Reminders.Option.millis(zone: ZoneId): Long = at.atZone(zone).toInstant().toEpochMilli()

/** Fits four buttons on one line. */
private val ReminderRepeat.shortLabel: String
    get() = when (this) {
        ReminderRepeat.NONE -> "Yok"
        ReminderRepeat.DAILY -> "Günlük"
        ReminderRepeat.WEEKLY -> "Haftalık"
        ReminderRepeat.MONTHLY -> "Aylık"
    }

/** A row of the "Ne zaman?" group, as the group takes it. */
private fun tile(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit): @Composable () -> Unit =
    { ChoiceTile(icon, title, subtitle, onClick) }

@Composable
private fun ChoiceTile(icon: ImageVector, title: String, subtitle: String?, onClick: () -> Unit) {
    RowText(
        icon,
        title,
        subtitle,
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

/** Days before today cannot be picked. */
private class FromToday(private val today: LocalDate) : SelectableDates {
    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
        Instant.ofEpochMilli(utcTimeMillis).atOffset(ZoneOffset.UTC).toLocalDate() >= today

    override fun isSelectableYear(year: Int): Boolean = year >= today.year
}

@Composable
private fun DateDialog(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    // The date picker counts days in UTC.
    val state = rememberDatePickerState(
        initialSelectedDateMillis = maxOf(initial, today).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        selectableDates = remember(today) { FromToday(today) },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = {
                    state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atOffset(ZoneOffset.UTC).toLocalDate()) }
                },
            ) { Text("İleri") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    ) {
        DatePicker(state = state, title = { Text("Tarih seç", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

@Composable
private fun TimeDialog(initial: LocalTime, day: LocalDate, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    val picked = LocalTime.of(state.hour, state.minute)
    val inPast = day.atTime(picked) <= LocalDateTime.now()
    TimePickerDialog(
        title = { Text("Saat seç", style = MaterialTheme.typography.labelLarge) },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = !inPast, onClick = { onPick(picked) }) { Text("Kur") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } },
    ) {
        TimePicker(state = state)
        if (inPast) {
            Text(
                "Bu saat geçti; ileri bir saat seç.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
