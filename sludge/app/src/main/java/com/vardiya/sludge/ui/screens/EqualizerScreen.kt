package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.audio.TuneStatus
import com.vardiya.sludge.domain.BAND_COUNT
import com.vardiya.sludge.domain.MAX_EFFECT
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.BandColumn
import com.vardiya.sludge.ui.components.BandScale
import com.vardiya.sludge.ui.components.CurvePreview
import com.vardiya.sludge.ui.components.SectionTitle
import com.vardiya.sludge.ui.components.SwitchRow

private val BAND_LABELS = listOf("60 Hz", "230 Hz", "910 Hz", "3,6 kHz", "14 kHz")

@Composable
fun EqualizerScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    val s = state.settings
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            SwitchRow(s.enabled, actions::setEnabled, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Ekolayzır", style = MaterialTheme.typography.titleLargeEmphasized, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        when {
                            !s.enabled -> "Kapalı: ses olduğu gibi çalıyor"
                            state.sessions.isEmpty() -> "Açık: çalar bağlanınca ses işlenir"
                            else -> "Açık: ses işleniyor"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        NowPlayingCard(state)

        ElevatedCard {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("Bantlar")
                CurvePreview(s.bands, Modifier.fillMaxWidth().height(64.dp).padding(vertical = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
                    // The scale sits beside the columns; the empty text above keeps it level with the bars.
                    Column {
                        Text("", style = MaterialTheme.typography.labelLarge)
                        BandScale()
                    }
                    for (i in 0 until BAND_COUNT) {
                        BandColumn(BAND_LABELS[i], s.bands[i], { actions.setBand(i, it) }, Modifier.weight(1f), enabled = s.enabled)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = actions::resetBands, enabled = s.enabled) { Text("Sıfırla") }
                }
            }
        }

        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Efektler")
                EffectRow("Bas güçlendirici", s.bassBoost, MAX_EFFECT, 500, s.enabled, actions::setBass)
                EffectRow("Sanallaştırıcı (geniş sahne)", s.virtualizer, MAX_EFFECT, 500, s.enabled, actions::setVirtualizer)
                EffectRow("Ses yükseltici", s.loudnessMb, MAX_LOUDNESS_MB, 400, s.enabled, actions::setLoudness)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwitchRow(s.autoTune, actions::setAutoTune) {
                    Column(Modifier.weight(1f)) {
                        Text("Şarkı değişince otomatik ayarla", style = MaterialTheme.typography.titleMedium)
                        Text("Yapay zekâ ya da yerel tür tanıma ayarı seçer", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick = actions::tuneNow,
                    enabled = state.track != null && state.status != TuneStatus.Working,
                    modifier = Modifier.fillMaxWidth().height(64.dp),
                ) {
                    if (state.status == TuneStatus.Working) {
                        LoadingIndicator(Modifier.size(32.dp))
                        Text("Ayarlanıyor…", Modifier.padding(start = 12.dp))
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Text("Yapay zekâyla ayarla", Modifier.padding(start = 12.dp), style = MaterialTheme.typography.titleMedium)
                    }
                }
                (state.status as? TuneStatus.Failed)?.let {
                    Text("Yapay zekâ yanıt vermedi: ${it.message}. Yerel ayar kullanıldı.", color = MaterialTheme.colorScheme.error)
                }
                if (state.track == null) {
                    Text("Çalan bir şarkı görünmüyor; Kurulum sekmesinden bildirim erişimini aç.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        if (s.explanation.isNotBlank() && s.source != TuneSource.MANUAL) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SectionTitle("Neden bu ayar? (${s.source.label})")
                    Text(s.explanation, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
        }
    }
}

@Composable
private fun NowPlayingCard(state: UiState) {
    val track = state.track
    ElevatedCard {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(
                Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    track?.title?.ifBlank { "Adsız parça" } ?: "Çalan parça yok",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track?.artist?.ifBlank { "Bilinmeyen sanatçı" } ?: "Bir şarkı başlat",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.settings.genre.isNotBlank()) AssistChip(onClick = {}, label = { Text(state.settings.genre) })
                    if (!track?.source.isNullOrBlank()) AssistChip(onClick = {}, label = { Text(track?.source.orEmpty()) })
                }
            }
        }
    }
}

@Composable
private fun EffectRow(title: String, value: Int, max: Int, onValue: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Column {
        SwitchRow(value > 0, { on -> onChange(if (on) onValue else 0) }, enabled = enabled) {
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text("${value * 100 / max}%", style = MaterialTheme.typography.labelLarge)
        }
        val slider = rememberSliderState(value = value.toFloat(), trackRange = 0f..max.toFloat())
        // Follow the stored value when it changes from outside (a preset, the AI, the switch above).
        LaunchedEffect(value) { if (slider.value.toInt() != value) slider.value = value.toFloat() }
        Slider(
            state = slider,
            onValueChange = {
                slider.value = it
                onChange(it.toInt())
            },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = title },
        )
    }
}
