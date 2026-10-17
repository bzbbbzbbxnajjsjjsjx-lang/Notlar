package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

private val OUTER_CORNER = 24.dp
private val INNER_CORNER = 4.dp
private val GROUP_GAP = 2.dp

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
    onManageLabels: () -> Unit = {},
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
            onManageLabels = onManageLabels,
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
    onManageLabels: () -> Unit = {},
    dynamicColorAvailable: Boolean = DynamicColorAvailable,
) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SettingsGroup(
            "Görünüm",
            buildList {
                add {
                    ChoiceRow(Icons.Outlined.Palette, "Tema", ThemeMode.entries, settings.themeMode, { it.label }) {
                        onSettingsChange(settings.copy(themeMode = it))
                    }
                }
                // Wallpaper colors exist from Android 12; older devices do not see the option at all.
                if (dynamicColorAvailable) {
                    add {
                        SwitchRow(Icons.Outlined.AutoAwesome, "Dinamik renkler", "Duvar kağıdından renk al", settings.dynamicColor) {
                            onSettingsChange(settings.copy(dynamicColor = it))
                        }
                    }
                }
                add { TextSizeRow(settings, onSettingsChange) }
                add {
                    SwitchRow(
                        Icons.Outlined.DashboardCustomize,
                        "Kartlarda da kullan",
                        "Not listesindeki kartların yazısı da büyür",
                        settings.textSizeOnCards,
                    ) { onSettingsChange(settings.copy(textSizeOnCards = it)) }
                }
                add {
                    ChoiceRow(Icons.AutoMirrored.Outlined.Sort, "Sıralama", NoteSort.entries, settings.sort, { it.label }) {
                        onSettingsChange(settings.copy(sort = it))
                    }
                }
            },
        )
        SettingsGroup(
            "Düzen",
            listOf { ActionRow(Icons.AutoMirrored.Outlined.Label, "Etiketler", "Ekle, yeniden adlandır, sil", onManageLabels) },
        )
        SettingsGroup(
            "Gizlilik",
            buildList {
                add { SwitchRow(Icons.Outlined.Lock, "Uygulama kilidi", "Açılışta PIN sor", settings.lockEnabled, privacy.onLockChange) }
                if (settings.lockEnabled) {
                    add { ActionRow(Icons.Outlined.Password, "PIN'i değiştir", "Yeni bir PIN belirle", privacy.onChangePin) }
                    if (privacy.biometricAvailable) {
                        add {
                            SwitchRow(Icons.Outlined.Fingerprint, "Biyometrik ile aç", "Parmak izi veya yüz", settings.biometricUnlock) {
                                onSettingsChange(settings.copy(biometricUnlock = it))
                            }
                        }
                    }
                    add {
                        ChoiceRow(Icons.Outlined.Timer, "Kilitle", LockTimeout.entries, settings.lockTimeout, { it.label }) {
                            onSettingsChange(settings.copy(lockTimeout = it))
                        }
                    }
                }
                add {
                    SwitchRow(
                        Icons.Outlined.VisibilityOff,
                        "Ekranı gizle",
                        "Son uygulamalarda içerik görünmez, ekran görüntüsü alınamaz",
                        settings.hideContent,
                    ) { onSettingsChange(settings.copy(hideContent = it)) }
                }
                add { InfoRow(Icons.Outlined.Shield, "Notlar cihazda şifreli saklanır (AES-256, anahtar Android Keystore'da).") }
            },
        )
        SettingsGroup(
            "Yedek",
            listOf(
                { ActionRow(Icons.Outlined.FileUpload, "Dışa aktar", "Tüm notları bir JSON dosyasına kaydet, istersen parolayla", onExport) },
                { ActionRow(Icons.Outlined.FileDownload, "İçe aktar", "Aynı not varsa en son düzenlenen kalır", onImport) },
            ),
        )
    }
}

/**
 * A titled group of rows, Android-settings style: the rows sit 2dp apart as one block whose outer
 * corners are round and whose inner corners are tight.
 */
@Composable
internal fun SettingsGroup(title: String, rows: List<@Composable () -> Unit>) {
    Column(verticalArrangement = Arrangement.spacedBy(GROUP_GAP)) {
        Heading(title, Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp))
        rows.forEachIndexed { index, row ->
            val top = if (index == 0) OUTER_CORNER else INNER_CORNER
            val bottom = if (index == rows.lastIndex) OUTER_CORNER else INNER_CORNER
            Surface(
                shape = RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth(),
            ) { row() }
        }
    }
}

/** The leading icon and the two lines every row shares. */
@Composable
internal fun RowText(icon: ImageVector, title: String, subtitle: String?, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(
            Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    RowText(
        icon,
        title,
        subtitle,
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun InfoRow(icon: ImageVector, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun <T> ChoiceRow(
    icon: ImageVector,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowText(icon, title, subtitle = null)
        ConnectedChoice(options, selected, label, onSelect)
    }
}

/** A single-choice connected button group that fills the width. */
@Composable
internal fun <T> ConnectedChoice(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
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
                // Unchecked buttons take the sheet's color so they stand out on the row's tile.
                colors = ToggleButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.RadioButton },
            ) {
                Text(label(option), maxLines = 1, textAlign = TextAlign.Center)
            }
        }
    }
}

/** A row that flips a setting; the switch shows a check while it is on. */
@Composable
internal fun SwitchRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowText(icon, title, subtitle, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            thumbContent = if (checked) {
                { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(SwitchDefaults.IconSize)) }
            } else {
                null
            },
            modifier = Modifier.padding(start = 16.dp),
        )
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
