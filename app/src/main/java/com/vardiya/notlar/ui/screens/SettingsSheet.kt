package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.LockTimeout
import com.vardiya.notlar.domain.NoteSort
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.domain.ThemeMode
import com.vardiya.notlar.ui.theme.DynamicColorAvailable

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "Sistem"
        ThemeMode.LIGHT -> "Açık"
        ThemeMode.DARK -> "Koyu"
    }

private val LockTimeout.label: String
    get() = when (this) {
        LockTimeout.IMMEDIATE -> "Hemen"
        LockTimeout.ONE_MINUTE -> "1 dk"
        LockTimeout.FIVE_MINUTES -> "5 dk"
    }

private val NoteSort.label: String
    get() = when (this) {
        NoteSort.EDITED -> "Düzenlenme"
        NoteSort.CREATED -> "Oluşturulma"
        NoteSort.TITLE -> "Başlık"
    }

/** What the privacy rows do beyond changing a setting: the lock needs a PIN first. */
class PrivacyActions(
    val onLockChange: (enabled: Boolean) -> Unit,
    val onChangePin: () -> Unit,
    val biometricAvailable: Boolean,
)

/** Appearance, privacy, order and backup, in a bottom sheet over the lists. */
@Composable
fun SettingsSheet(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    privacy: PrivacyActions,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        SettingsContent(
            settings,
            onSettingsChange,
            privacy,
            onExport,
            onImport,
            Modifier.verticalScroll(rememberScrollState()),
        )
    }
}

/** The sheet's content; separate so screenshot tests can render it without a window. */
@Composable
internal fun SettingsContent(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    privacy: PrivacyActions,
    onExport: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    dynamicColorAvailable: Boolean = DynamicColorAvailable,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Heading("Görünüm")
        Label("Tema")
        ConnectedChoice(
            options = ThemeMode.entries,
            selected = settings.themeMode,
            label = { it.label },
            onSelect = { onSettingsChange(settings.copy(themeMode = it)) },
        )
        // Wallpaper colors exist from Android 12; older devices do not see the option at all.
        if (dynamicColorAvailable) {
            SwitchRow(
                title = "Dinamik renkler",
                subtitle = "Duvar kağıdından renk al",
                checked = settings.dynamicColor,
                onCheckedChange = { onSettingsChange(settings.copy(dynamicColor = it)) },
            )
        }
        Label("Sıralama")
        ConnectedChoice(
            options = NoteSort.entries,
            selected = settings.sort,
            label = { it.label },
            onSelect = { onSettingsChange(settings.copy(sort = it)) },
        )

        Heading("Gizlilik", Modifier.padding(top = 12.dp))
        SwitchRow(
            title = "Uygulama kilidi",
            subtitle = "Açılışta PIN sor",
            checked = settings.lockEnabled,
            onCheckedChange = privacy.onLockChange,
        )
        if (settings.lockEnabled) {
            TextButton(onClick = privacy.onChangePin, shapes = ButtonDefaults.shapes()) { Text("PIN'i değiştir") }
            if (privacy.biometricAvailable) {
                SwitchRow(
                    title = "Biyometrik ile aç",
                    subtitle = "Parmak izi veya yüz",
                    checked = settings.biometricUnlock,
                    onCheckedChange = { onSettingsChange(settings.copy(biometricUnlock = it)) },
                )
            }
            Label("Kilitle")
            ConnectedChoice(
                options = LockTimeout.entries,
                selected = settings.lockTimeout,
                label = { it.label },
                onSelect = { onSettingsChange(settings.copy(lockTimeout = it)) },
            )
        }
        SwitchRow(
            title = "Ekranı gizle",
            subtitle = "Son uygulamalarda içerik görünmez, ekran görüntüsü alınamaz",
            checked = settings.hideContent,
            onCheckedChange = { onSettingsChange(settings.copy(hideContent = it)) },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Shield, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Notlar cihazda şifreli saklanır (AES-256, anahtar Android Keystore'da).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Heading("Yedek", Modifier.padding(top = 12.dp))
        Text(
            "Tüm notlar tek bir JSON dosyasına kaydedilir. İçe aktarırken aynı not varsa en son düzenlenen kalır.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onExport, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.FileUpload, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Text("Dışa aktar", Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
            FilledTonalButton(onClick = onImport, shapes = ButtonDefaults.shapes(), modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.FileDownload, contentDescription = null, Modifier.size(ButtonDefaults.IconSize))
                Text("İçe aktar", Modifier.padding(start = ButtonDefaults.IconSpacing))
            }
        }
    }
}

/** A single-choice connected button group that fills the width. */
@Composable
private fun <T> ConnectedChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            ToggleButton(
                checked = option == selected,
                onCheckedChange = { onSelect(option) },
                shapes = when (index) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                // The default unchecked container is surfaceContainer, almost the sheet's own color.
                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(label(option), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Heading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
private fun Label(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
