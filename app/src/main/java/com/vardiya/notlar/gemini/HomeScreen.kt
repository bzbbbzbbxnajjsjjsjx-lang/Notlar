package com.vardiya.notlar.gemini

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

private val MinTouch = 48.dp

@Composable
fun HomeScreen(state: ParserUiState, link: String, actions: HomeActions, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Gemini Sohbet Aktarıcı", fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        )
        if (state is ParserUiState.Success) {
            ResultList(state, actions, contentPadding)
        } else {
            LinkForm(state, link, actions, contentPadding)
        }
    }
}

@Composable
private fun LinkForm(state: ParserUiState, link: String, actions: HomeActions, padding: PaddingValues) {
    val loading = state is ParserUiState.Loading
    val invalid = (state as? ParserUiState.Error)?.reason == ParseFailure.InvalidLink
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = padding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            OutlinedTextField(
                value = link,
                onValueChange = actions.onLinkChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Gemini paylaşım linki") },
                leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                supportingText = {
                    Text(
                        if (invalid) "Geçersiz link. https://gemini.google.com/share/... biçiminde olmalı."
                        else "https://gemini.google.com/share/...",
                    )
                },
                isError = invalid,
                enabled = !loading,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { actions.onFetch() }),
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text
                                ?.let { actions.onLinkChange(it.toString().trim()) }
                        }
                    },
                    enabled = !loading,
                    modifier = Modifier.weight(1f).heightIn(min = MinTouch),
                ) { Text("Yapıştır") }
                OutlinedButton(
                    onClick = actions.onReset,
                    enabled = !loading && link.isNotEmpty(),
                    modifier = Modifier.weight(1f).heightIn(min = MinTouch),
                ) { Text("Temizle") }
            }
        }
        item {
            Button(
                onClick = actions.onFetch,
                enabled = !loading && link.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) { Text("İçeriği Çek") }
        }
        when (state) {
            is ParserUiState.Loading -> item { LoadingBlock(state, actions.onCancel) }
            is ParserUiState.Error -> item { ErrorBlock(state.reason, actions.onFetch) }
            else -> item { HintBlock() }
        }
    }
}

@Composable
private fun HintBlock() {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "Gemini'de bir sohbeti \"Bağlantıyı paylaş\" ile herkese açın, linki buraya yapıştırın ya da paylaşım menüsünden bu uygulamayı seçin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LoadingBlock(state: ParserUiState.Loading, onCancel: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        LinearWavyProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text("Sayfa taranıyor...", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, enabled = false, label = { Text("Deneme ${state.attempt}/${state.maxAttempts}") })
            OutlinedButton(onClick = onCancel, modifier = Modifier.heightIn(min = MinTouch)) { Text("İptal") }
        }
    }
}

@Composable
private fun ErrorBlock(reason: ParseFailure, onRetry: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                Text(reason.title(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            Text(reason.message(), style = MaterialTheme.typography.bodyMedium)
            if (reason != ParseFailure.InvalidLink) {
                Button(onClick = onRetry, modifier = Modifier.heightIn(min = MinTouch)) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text("Yeniden Dene", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

private fun ParseFailure.title() = when (this) {
    ParseFailure.InvalidLink -> "Bu bir Gemini paylaşım linki değil"
    ParseFailure.NotFound -> "Sohbet bulunamadı"
    else -> "İçerik alınamadı"
}

private fun ParseFailure.message() = when (this) {
    ParseFailure.InvalidLink -> "Yalnızca https://gemini.google.com/share/... biçimindeki linkler açılır."
    ParseFailure.Network -> "Sayfa yüklenemedi. Bağlantınızı ve linki kontrol edip yeniden deneyin."
    ParseFailure.NotFound -> "Link silinmiş ya da paylaşım kapatılmış olabilir."
    ParseFailure.Blocked -> "Sayfa güvenli olmayan bir adrese yönlendirdi, bu yüzden açılmadı."
    ParseFailure.Empty -> "Sayfada soru ya da cevap bulunamadı. Sohbet herkese açık değilse ya da sayfa geç yükleniyorsa böyle olur."
    ParseFailure.Crashed -> "Sayfa çöktü. Yeniden deneyin."
    ParseFailure.Timeout -> "Sayfa zamanında yüklenmedi. Bağlantınızı kontrol edip yeniden deneyin."
}

@Composable
private fun ResultList(state: ParserUiState.Success, actions: HomeActions, padding: PaddingValues) {
    val questions = GeminiChat.questionCount(state.turns)
    LazyColumn(contentPadding = padding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${state.turns.size} tur · $questions soru, ${state.turns.size - questions} yanıt",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                TextButton(onClick = actions.onReset, modifier = Modifier.heightIn(min = MinTouch)) { Text("Yeni Link") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = actions.onCopyMarkdown, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                    Text("Markdown Olarak Kopyala", modifier = Modifier.padding(start = 8.dp))
                }
                FilledTonalButton(onClick = actions.onShareMarkdown, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null)
                    Text("Markdown (.md) Kaydet/Paylaş", modifier = Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = actions.onSaveJson, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Text("JSON (.json) İndir", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        items(state.turns) { turn -> TurnCard(turn) }
    }
}

private const val COLLAPSED_LINES = 3

@Composable
private fun TurnCard(turn: ChatTurn) {
    val user = turn.role == ChatRole.User
    var expanded by rememberSaveable(turn) { mutableStateOf(false) }
    val long = turn.text.length > LONG_TEXT || turn.text.count { it == '\n' } >= COLLAPSED_LINES
    Column(
        horizontalAlignment = if (user) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        AssistChip(
            onClick = {},
            label = { Text(if (user) "Sen" else "Gemini") },
            leadingIcon = { Icon(if (user) Icons.Outlined.Person else Icons.Outlined.AutoAwesome, contentDescription = null) },
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (user) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(if (user) 0.9f else 1f),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    turn.text,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = if (expanded) Int.MAX_VALUE else COLLAPSED_LINES + 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (long) {
                    TextButton(onClick = { expanded = !expanded }, modifier = Modifier.heightIn(min = MinTouch)) {
                        Text(if (expanded) "Daralt" else "Devamı")
                    }
                }
            }
        }
    }
}

private const val LONG_TEXT = 200
