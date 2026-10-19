package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.CookieBadge
import com.vardiya.sludge.ui.components.CurvePreview

@Composable
fun PresetsScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 16.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(Genre.entries, key = { it.name }) { genre ->
            val selected = CurveMath.sameCurve(state.settings.bands, genre.bands) &&
                state.settings.bassBoost == genre.bass && state.settings.virtualizer == genre.virtualizer
            Card(
                onClick = { actions.applyPreset(genre) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                ),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "${genre.label}: ${genre.blurb}" + if (selected) ", seçili" else ""
                },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(genre.label, style = MaterialTheme.typography.titleMediumEmphasized)
                        Text(genre.blurb, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    CurvePreview(genre.bands, Modifier.width(88.dp).height(56.dp))
                    if (selected) {
                        CookieBadge(
                            Icons.Filled.Check,
                            size = 40.dp,
                            container = MaterialTheme.colorScheme.primary,
                            content = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}
