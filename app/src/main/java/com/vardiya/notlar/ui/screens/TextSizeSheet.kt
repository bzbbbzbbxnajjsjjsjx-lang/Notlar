package com.vardiya.notlar.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DashboardCustomize
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.notlar.domain.Settings
import com.vardiya.notlar.domain.TextSize
import com.vardiya.notlar.ui.components.label
import com.vardiya.notlar.ui.components.scaled

/** "Aa" in the editor: how big the note text is, everywhere the setting applies. */
@Composable
fun TextSizeSheet(
    settings: Settings,
    onSettingsChange: (Settings) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    ModalBottomSheet(onDismissRequest = onDismissRequest, modifier = modifier, sheetState = sheetState) {
        TextSizeContent(settings, onSettingsChange)
    }
}

/** The sheet's content; separate so screenshot tests can render it without a window. */
@Composable
internal fun TextSizeContent(settings: Settings, onSettingsChange: (Settings) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Yazı boyutu",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            modifier = Modifier
                .padding(start = 8.dp, bottom = 4.dp)
                .semantics { heading() },
        )
        SettingsGroup("Önizleme", listOf { TextSizePreview(settings.textSize) })
        SettingsGroup(
            "Boyut",
            listOf(
                { TextSizeRow(settings, onSettingsChange) },
                {
                    SwitchRow(
                        Icons.Outlined.DashboardCustomize,
                        "Kartlarda da kullan",
                        "Not listesindeki kartların yazısı da büyür",
                        settings.textSizeOnCards,
                    ) { onSettingsChange(settings.copy(textSizeOnCards = it)) }
                },
            ),
        )
    }
}

/** The four sizes as a connected button group; also a row of the settings sheet. */
@Composable
internal fun TextSizeRow(settings: Settings, onSettingsChange: (Settings) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RowText(Icons.Outlined.FormatSize, "Not yazısı", subtitle = null)
        ConnectedChoice(TextSize.entries, settings.textSize, { it.label }) { onSettingsChange(settings.copy(textSize = it)) }
    }
}

@Composable
private fun TextSizePreview(size: TextSize) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Market", style = MaterialTheme.typography.headlineSmallEmphasized.scaled(size.scale))
        Text("Süt, ekmek ve domates al. Akşam annemi ara.", style = MaterialTheme.typography.bodyLarge.scaled(size.scale))
    }
}
