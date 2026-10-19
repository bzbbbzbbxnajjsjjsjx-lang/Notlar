package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProvider
import com.vardiya.sludge.domain.UrlRules
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.SectionTitle
import com.vardiya.sludge.ui.components.SwitchRow
import kotlinx.coroutines.launch

@Composable
fun AiScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    // The draft lives in plain remembered state on purpose: rememberSaveable would copy the key into the saved-state bundle.
    var draft by remember(state.ai) { mutableStateOf(state.ai) }
    var showKey by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val urlOk = UrlRules.isAcceptable(draft.baseUrl)

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle("Sağlayıcı")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                    AiProvider.entries.forEachIndexed { index, provider ->
                        ToggleButton(
                            checked = draft.provider == provider,
                            onCheckedChange = {
                                draft = draft.copy(provider = provider, model = provider.defaultModel, baseUrl = provider.defaultBaseUrl)
                            },
                            shapes = if (index == 0) {
                                ButtonGroupDefaults.connectedLeadingButtonShapes()
                            } else {
                                ButtonGroupDefaults.connectedTrailingButtonShapes()
                            },
                            modifier = Modifier.weight(1f),
                        ) { Text(provider.label) }
                    }
                }
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { draft = draft.copy(apiKey = it.trim()) },
                    label = { Text("API anahtarı") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                if (showKey) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (showKey) "Anahtarı gizle" else "Anahtarı göster",
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.model,
                    onValueChange = { draft = draft.copy(model = it) },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.baseUrl,
                    onValueChange = { draft = draft.copy(baseUrl = it.trim()) },
                    label = { Text("Adres") },
                    supportingText = { Text(if (urlOk) "Anahtar yalnızca bu adrese gider" else "https:// ile başlamalı (yerel ağ hariç)") },
                    isError = !urlOk,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { actions.saveAi(draft) },
                        enabled = draft != state.ai && urlOk,
                        modifier = Modifier.weight(1f),
                    ) { Text("Kaydet") }
                    FilledTonalButton(
                        onClick = {
                            testing = true
                            testResult = ""
                            scope.launch {
                                testResult = actions.testAi(draft).fold(
                                    onSuccess = { "Çalıştı: ${it.genre.ifBlank { "tür yok" }} → ${it.bands.joinToString(", ")} dB" },
                                    onFailure = { "Olmadı: ${it.message.orEmpty()}" },
                                )
                                testing = false
                            }
                        },
                        enabled = draft.isUsable && !testing,
                        modifier = Modifier.weight(1f),
                    ) { Text(if (testing) "Deneniyor…" else "Bağlantıyı dene") }
                }
                if (testResult.isNotEmpty()) Text(testResult, style = MaterialTheme.typography.bodyMedium)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SwitchRow(state.settings.autoTune, actions::setAutoTune) {
                    Column(Modifier.weight(1f)) {
                        Text("Şarkı değişince otomatik ayarla", style = MaterialTheme.typography.titleMedium)
                        Text("Anahtar yoksa ya da ağ yoksa yerel tür tanıma devreye girer", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Gizlilik")
                Text(
                    "Sağlayıcıya yalnızca şarkı adı, sanatçı, albüm, tür etiketi ve çalar uygulamanın adı gider. " +
                        "Bildirim içerikleri okunmaz. Anahtar bu cihazda Android Keystore ile şifrelenir; yedeğe girmez. " +
                        "Aynı şarkı için sonuç saklanır, tekrar istek atılmaz.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
