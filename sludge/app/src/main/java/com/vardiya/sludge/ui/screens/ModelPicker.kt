// The file is named for the picker composable; ModelListState is the state it shows.
@file:Suppress("MatchingDeclarationName")

package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.ModelCatalogProtocol
import com.vardiya.sludge.domain.ModelFilter
import com.vardiya.sludge.domain.ModelOption
import com.vardiya.sludge.domain.priceLabel

/** What the model picker shows while the list is being fetched, once it is here, or when it could not be. */
sealed interface ModelListState {
    data object Loading : ModelListState
    data class Loaded(val models: List<ModelOption>) : ModelListState
    data class Failed(val message: String) : ModelListState
}

/** The body of the model picker sheet: a search box, filter pills and the list; a tap on a model picks it. */
@Composable
fun ModelPickerContent(
    state: ModelListState,
    selectedId: String,
    onPick: (ModelOption) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(ModelFilter.ALL) }
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Model seç", style = MaterialTheme.typography.titleLargeEmphasized, modifier = Modifier.semantics { heading() })
        when (state) {
            ModelListState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
            is ModelListState.Failed -> Column(Modifier.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
                FilledTonalButton(onClick = onRetry) { Text("Tekrar dene") }
            }
            is ModelListState.Loaded -> {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Ara (ad ya da kod)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModelFilter.entries.forEach {
                        FilterChip(selected = filter == it, onClick = { filter = it }, label = { Text(it.label) }, shape = CircleShape)
                    }
                }
                val shown = ModelCatalogProtocol.filter(state.models, query, filter)
                Text(
                    "${shown.size} model",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(Modifier.heightIn(max = LIST_MAX_HEIGHT), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(shown, key = { it.id }) { model -> ModelRow(model, model.id == selectedId) { onPick(model) } }
                }
            }
        }
    }
}

private val LIST_MAX_HEIGHT = 480.dp

@Composable
private fun ModelRow(model: ModelOption, selected: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) scheme.secondaryContainer else scheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (selected) Icon(Icons.Filled.Check, contentDescription = "Seçili", Modifier.size(20.dp), tint = scheme.primary)
            Column(Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    model.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(shape = CircleShape, color = if (model.isFree) scheme.tertiary else scheme.surfaceContainerHighest) {
                Text(
                    model.priceLabel(),
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (model.isFree) scheme.onTertiary else scheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}
