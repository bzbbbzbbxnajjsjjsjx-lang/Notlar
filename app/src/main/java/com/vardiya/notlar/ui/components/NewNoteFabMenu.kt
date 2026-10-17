package com.vardiya.notlar.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.dp

private const val ICON_SWAP_PROGRESS = 0.5f
private val MENU_PADDING = 16.dp

private class MenuEntry(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * The Material 3 Expressive FAB menu: the "+" button morphs into a close button and the choices
 * fan out above it, one after another.
 */
@Composable
fun NewNoteFabMenu(
    visible: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onNewNote: () -> Unit,
    onNewChecklist: () -> Unit,
    onPaste: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val currentOnExpandedChange by rememberUpdatedState(onExpandedChange)
    BackHandler(expanded) { onExpandedChange(false) }
    // Selection mode or another section hides the button; it should not come back open.
    LaunchedEffect(visible) { if (!visible) currentOnExpandedChange(false) }

    val entries = listOf(
        MenuEntry(Icons.AutoMirrored.Outlined.Notes, "Not", onNewNote),
        MenuEntry(Icons.Outlined.Checklist, "Yapılacak listesi", onNewChecklist),
        MenuEntry(Icons.Outlined.ContentPaste, "Panodan not", onPaste),
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        // The menu pads itself by 16dp on top of the Scaffold's FAB margin; keep the button where the FAB was.
        modifier = modifier.offset(x = MENU_PADDING, y = MENU_PADDING),
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = {
                    haptic.performHapticFeedback(if (expanded) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
                    onExpandedChange(!expanded)
                },
                modifier = Modifier
                    .semantics {
                        traversalIndex = -1f
                        contentDescription = "Yeni not menüsü"
                        stateDescription = if (expanded) "Açık" else "Kapalı"
                    }
                    .animateFloatingActionButton(visible = visible || expanded, alignment = Alignment.BottomEnd),
            ) {
                val icon by remember { derivedStateOf { if (checkedProgress > ICON_SWAP_PROGRESS) Icons.Filled.Close else Icons.Filled.Add } }
                Icon(
                    painter = rememberVectorPainter(icon),
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress }),
                )
            }
        },
    ) {
        entries.forEachIndexed { index, entry ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onExpandedChange(false)
                    entry.onClick()
                },
                text = { Text(entry.label) },
                icon = { Icon(entry.icon, contentDescription = null) },
                modifier = Modifier.semantics {
                    isTraversalGroup = true
                    // The close button comes before the items, so the last one offers closing too.
                    if (index == entries.lastIndex) {
                        customActions = listOf(
                            CustomAccessibilityAction("Menüyü kapat") {
                                onExpandedChange(false)
                                true
                            },
                        )
                    }
                },
            )
        }
    }
}
