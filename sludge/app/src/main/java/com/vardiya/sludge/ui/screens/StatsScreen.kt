package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.DayCount
import com.vardiya.sludge.domain.GenreShare
import com.vardiya.sludge.domain.ListenStats
import com.vardiya.sludge.domain.chartLabel
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.SectionTitle
import java.time.format.TextStyle
import java.util.Locale

private val TR = Locale.forLanguageTag("tr-TR")
private val BAR_HEIGHT = 32.dp
private val DAY_CHART_HEIGHT = 120.dp

@Composable
fun StatsScreen(state: UiState, actions: SludgeActions, padding: PaddingValues, modifier: Modifier = Modifier) {
    val summary = state.listens
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (summary.total == 0) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Henüz kayıt yok")
                    Text(
                        "Müzik dinledikçe hangi türleri çaldığın burada çıkar. Şarkıyı görmek için Kurulum sekmesinden " +
                            "bildirim erişimini aç.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            return@Column
        }

        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Son ${ListenStats.WINDOW_DAYS} günde",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "${summary.total} şarkı",
                    style = MaterialTheme.typography.displaySmallEmphasized,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                summary.genres.firstOrNull()?.let {
                    Text(
                        "En çok ${it.genre.chartLabel} dinledin (%${percent(it)})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Türler")
                summary.genres.forEachIndexed { rank, share -> GenreBar(share, highlighted = rank == 0) }
            }
        }

        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle("Son 7 gün")
                DayChart(summary.days)
            }
        }

        if (summary.topArtists.isNotEmpty()) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("En çok dinlenen sanatçılar")
                    summary.topArtists.forEach { (artist, plays) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(artist, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            Text("$plays", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        TextButton(onClick = { confirmClear = true }, modifier = Modifier.align(Alignment.End)) { Text("Geçmişi temizle") }
        Text(
            "Kayıt yalnızca bu telefonda durur; hiçbir yere gönderilmez.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Geçmiş silinsin mi?") },
            text = { Text("Dinleme kayıtların ve grafikler sıfırlanır.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    actions.clearHistory()
                }) { Text("Sil") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Vazgeç") } },
        )
    }
}

private fun percent(share: GenreShare) = (share.fraction * PERCENT).toInt()

private const val PERCENT = 100

/** One genre as a flat horizontal bar, longest first; the width is the share of plays. */
@Composable
private fun GenreBar(share: GenreShare, highlighted: Boolean) {
    val color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Column(
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = "${share.genre.chartLabel}: ${share.count} şarkı, yüzde ${percent(share)}"
        },
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(share.genre.chartLabel, style = MaterialTheme.typography.titleSmall)
            Text("${share.count} · %${percent(share)}", style = MaterialTheme.typography.labelLarge)
        }
        Box(Modifier.fillMaxWidth().height(BAR_HEIGHT).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
            Box(Modifier.fillMaxWidth(share.fraction.coerceIn(MIN_BAR, 1f)).fillMaxHeight().background(color))
        }
    }
}

/** Seven rectangular columns, one per day, today on the right. */
@Composable
private fun DayChart(days: List<DayCount>) {
    val peak = (days.maxOfOrNull { it.count } ?: 0).coerceAtLeast(1)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        days.forEachIndexed { index, day ->
            val label = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, TR)
            Column(
                Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = "$label: ${day.count} şarkı" },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${day.count}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Box(
                    Modifier.fillMaxWidth().height(DAY_CHART_HEIGHT).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    if (day.count > 0) {
                        Box(
                            Modifier.fillMaxWidth().fillMaxHeight(day.count.toFloat() / peak).background(
                                if (index == days.lastIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    }
                }
                Text(
                    label,
                    Modifier.clearAndSetSemantics {},
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val MIN_BAR = 0.03f
