package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.CookieBadge
import com.vardiya.sludge.ui.components.SectionTitle

@Composable
fun SetupScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Text(
                "Sludge EQ hiçbir izin istemez: bildirim erişimi yok, internet yok. Müzik uygulaman sesini ekolayzıra " +
                    "kendisi bildirir; sen de ayarları elle yaparsın.",
                Modifier.padding(20.dp),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        StepCard(
            done = false,
            title = "1. Arka planda açık kalsın",
            body = "Bazı telefonlar pil tasarrufu için uygulamayı kapatır; ekolayzır da susar. Pil ayarlarında Sludge EQ için " +
                "\"Kısıtlama yok\" seç.",
        ) {
            OutlinedButton(onClick = actions::openBatterySettings, shapes = ButtonDefaults.shapes()) { Text("Pil ayarlarını aç") }
        }

        StepCard(
            done = state.sessions.isNotEmpty(),
            title = "2. Ses oturumu",
            body = when {
                state.sessions.isNotEmpty() -> state.sessions.joinToString("\n") { s ->
                    "• ${s.packageName.ifBlank { "bilinmeyen uygulama" }} (oturum ${s.id}): " + when {
                        s.failed -> "hiçbir efekt kurulamadı, bu çalarda ses işlenemiyor"
                        else -> "${s.path.ifBlank { "yalnızca sanallaştırıcı" }} · ${s.effects.joinToString(", ")}"
                    }
                }
                else -> "Henüz bir çalar oturumunu bildirmedi. Müzik uygulamasında bir şarkı başlat; çalar \"harici ekolayzır\" " +
                    "desteği veriyorsa (Spotify, Poweramp, VLC…) kendiliğinden görünür. Görünmezse çaların ayarlarında " +
                    "ses efektleri / ekolayzır seçeneğini ara."
            },
        ) {
            Text(
                when (state.globalMixWorks) {
                    true -> "Genel ses karışımına da bağlandı."
                    false -> "Genel ses karışımı bu telefonda kullanılamıyor (Android 10+ çoğunda böyle); çalar oturumları yeterli."
                    null -> "Genel ses karışımı denenmedi."
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun StepCard(done: Boolean, title: String, body: String, actions: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (done) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CookieBadge(
                    icon = if (done) Icons.Filled.Check else Icons.Filled.Remove,
                    size = 40.dp,
                    container = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = if (done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    contentDescription = if (done) "Tamam" else "Bekliyor",
                )
                SectionTitle(title)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}
