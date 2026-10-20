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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlin.math.roundToInt
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.SweepKind
import com.vardiya.sludge.domain.processing
import com.vardiya.sludge.domain.ToneState
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
            done = state.sessions.processing().isNotEmpty(),
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
        ) {}

        StepCard(
            done = state.tone.kind != null,
            title = "3. Test tonu",
            body = "Kulaklığın alt basta zorlanıp zorlanmadığını kendi kulağınla duymak için yavaş bir ses taraması çalar. Ton, " +
                "müzikle aynı filtrelerden geçer. Sesi kısık başlat. Bir frekansta çıtırtı, vızırtı ya da sertleşme duyarsan " +
                "kulaklık orada zorlanıyor demektir; o bölgedeki yükseltmeyi azalt. Alt bas taraması en bilgilendirici olandır.",
        ) {
            ToneButtons(state.tone, actions)
        }
    }
}

@Composable
private fun ToneButtons(tone: ToneState, actions: SludgeActions) {
    val playing = tone.kind
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SweepKind.entries.forEach { kind ->
            OutlinedButton(onClick = { actions.startTone(kind) }, enabled = playing != kind, shapes = ButtonDefaults.shapes()) {
                Text(kind.label)
            }
        }
        if (playing != null) {
            Text("Çalıyor: ${tone.hz.roundToInt()} Hz", style = MaterialTheme.typography.titleMedium)
            Button(onClick = actions::stopTone, shapes = ButtonDefaults.shapes()) { Text("Durdur") }
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
