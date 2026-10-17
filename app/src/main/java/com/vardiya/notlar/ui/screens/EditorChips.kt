package com.vardiya.notlar.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vardiya.notlar.domain.Note
import com.vardiya.notlar.domain.ReminderRepeat
import com.vardiya.notlar.ui.components.formatReminder
import com.vardiya.notlar.ui.components.label
import java.time.LocalDate

private const val CHIP_BORDER_ALPHA = 0.4f

/** Asks for the notification permission (Android 13+) the first time a reminder is set. */
@Composable
internal fun rememberNotificationPermission(): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

/** The reminder and labels under the title; tapping one opens its sheet. */
@Composable
internal fun EditorChips(note: Note, today: LocalDate, onReminder: () -> Unit, onLabels: () -> Unit) {
    val at = note.reminderAt
    if (at == null && note.labels.isEmpty()) return
    val content = LocalContentColor.current
    val colors = AssistChipDefaults.assistChipColors(labelColor = content, leadingIconContentColor = content)
    val border = AssistChipDefaults.assistChipBorder(enabled = true, borderColor = content.copy(alpha = CHIP_BORDER_ALPHA))
    Row(
        Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (at != null) {
            val repeat = if (note.reminderRepeat != ReminderRepeat.NONE) " · ${note.reminderRepeat.label}" else ""
            AssistChip(
                onClick = onReminder,
                label = { Text(formatReminder(at, today = today) + repeat) },
                leadingIcon = { Icon(Icons.Outlined.Alarm, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize)) },
                colors = colors,
                border = border,
            )
        }
        note.labels.forEach { name ->
            AssistChip(
                onClick = onLabels,
                label = { Text(name, maxLines = 1) },
                leadingIcon = {
                    Icon(Icons.AutoMirrored.Outlined.Label, contentDescription = null, Modifier.size(AssistChipDefaults.IconSize))
                },
                colors = colors,
                border = border,
            )
        }
    }
}
