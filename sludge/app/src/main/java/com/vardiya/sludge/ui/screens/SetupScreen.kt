package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.SectionTitle

const val PACKAGE_NAME = "com.vardiya.sludge"
const val LISTENER_CLASS = "com.vardiya.sludge.audio.SludgeListenerService"

@Composable
fun SetupScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StepCard(
            done = state.accessGranted,
            title = "1. Bildirim erişimi",
            body = "Çalan şarkının adını, sanatçısını ve albümünü okumak için Android bu izni ister. Bildirim metinlerini okumayız; " +
                "yalnızca medya oturumlarına bakarız. Android bu izni uygulama içinden veremez: ayarlarda \"Sludge EQ\" anahtarını sen açarsın.",
        ) {
            Button(onClick = actions::openNotificationAccess) {
                Text(if (state.accessGranted) "Ayarları aç" else "Erişimi ver")
            }
        }

        StepCard(
            done = state.accessGranted,
            title = "2. Anahtar gri ve açılmıyorsa",
            body = "Android 13 ve sonrasında APK ile kurulan uygulamalarda bu anahtar \"Kısıtlanmış ayar\" yüzünden kilitli gelir. " +
                "Uygulama bilgisi → sağ üst ⋮ menüsü → \"Kısıtlanmış ayarlara izin ver\", sonra 1. adıma dön.",
        ) {
            FilledTonalButton(onClick = actions::openAppInfo) { Text("Uygulama bilgisini aç") }
            Text("Bilgisayarın varsa aynı iş iki komutla da olur:", style = MaterialTheme.typography.bodySmall)
            SelectionContainer {
                Text(
                    "adb shell appops set $PACKAGE_NAME ACCESS_RESTRICTED_SETTINGS allow\n" +
                        "adb shell cmd notification allow_listener $PACKAGE_NAME/$LISTENER_CLASS",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        StepCard(
            done = false,
            title = "3. Arka planda açık kalsın",
            body = "Bazı telefonlar pil tasarrufu için uygulamayı kapatır; ekolayzır da susar. Pil ayarlarında Sludge EQ için " +
                "\"Kısıtlama yok\" seç.",
        ) {
            FilledTonalButton(onClick = actions::openBatterySettings) { Text("Pil ayarlarını aç") }
        }

        StepCard(
            done = state.sessions.isNotEmpty(),
            title = "4. Ses oturumu",
            body = when {
                state.sessions.isNotEmpty() -> state.sessions.joinToString("\n") { s ->
                    "• ${s.packageName.ifBlank { "bilinmeyen uygulama" }} (oturum ${s.id})" + if (s.failed) " — efekt kurulamadı" else ""
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
                Icon(
                    if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (done) "Tamam" else "Bekliyor",
                    tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(28.dp),
                )
                SectionTitle(title)
            }
            Text(body, style = MaterialTheme.typography.bodyMedium)
            actions()
        }
    }
}
