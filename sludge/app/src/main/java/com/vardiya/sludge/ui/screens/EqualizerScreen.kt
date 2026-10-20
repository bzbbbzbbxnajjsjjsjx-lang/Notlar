package com.vardiya.sludge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ToggleButton
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import com.vardiya.sludge.ui.components.ParametricEditor
import com.vardiya.sludge.ui.components.ResponseChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Headphones
import com.vardiya.sludge.domain.Health
import com.vardiya.sludge.domain.MAX_EFFECT
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.Status
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.OutputRoute
import com.vardiya.sludge.domain.correctionActive
import com.vardiya.sludge.domain.label
import com.vardiya.sludge.domain.statusOf
import com.vardiya.sludge.ui.SludgeActions
import com.vardiya.sludge.ui.UiState
import com.vardiya.sludge.ui.components.EffectCard
import com.vardiya.sludge.ui.components.PillSlider
import com.vardiya.sludge.ui.components.RotaryKnob
import com.vardiya.sludge.ui.components.SectionTitle
import com.vardiya.sludge.ui.components.SwitchRow
import com.vardiya.sludge.ui.components.WavyLevel

private const val DEFAULT_EFFECT = 500
private const val DEFAULT_LOUDNESS = 400
private const val PERCENT_FULL = 100f
private const val PERCENT_MAX = 100

@Composable
fun EqualizerScreen(
    state: UiState,
    actions: SludgeActions,
    padding: PaddingValues,
    onOpenSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = state.settings
    val status = statusOf(s.enabled, state.sessions.size)
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatusHeader(status, onOpenSetup)

        StatCard(
            icon = Icons.Filled.Tune,
            label = "Ayar (${s.source.label})",
            value = if (s.genre.isBlank()) "Henüz seçilmedi" else s.genre,
            pill = if (s.genre.isBlank()) null else "${s.bassBoost / PERCENT_STEP}% bas",
            pillContainer = MaterialTheme.colorScheme.tertiary,
            pillContent = MaterialTheme.colorScheme.onTertiary,
        )
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            SwitchRow(s.enabled, actions::setEnabled, Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Icon(Icons.Filled.Equalizer, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    Text("Ekolayzır", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (s.enabled) "Açık" else "Kapalı", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        HeadphoneCard(s, state.route, actions)

        PresetRow(state, actions)

        ElevatedCard {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("Parametrik ekolayzır")
                ParametricEditor(s.filters, s.enabled, actions, Modifier.padding(top = 12.dp))
            }
        }

        SectionTitle("Efektler", Modifier.padding(start = 8.dp, top = 8.dp))
        EffectCard(
            title = "Bas güçlendirici",
            icon = Icons.Filled.Speaker,
            polygon = MaterialShapes.Cookie9Sided,
            value = s.bassBoost,
            max = MAX_EFFECT,
            enabled = s.enabled,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onValue = actions::setBass,
            defaultOn = DEFAULT_EFFECT,
        ) { RotaryKnob("Bas güçlendirici", s.bassBoost, MAX_EFFECT, actions::setBass, enabled = s.enabled) }
        EffectCard(
            title = "Sanallaştırıcı (geniş sahne)",
            icon = Icons.Filled.Headphones,
            polygon = MaterialShapes.Flower,
            value = s.virtualizer,
            max = MAX_EFFECT,
            enabled = s.enabled,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onValue = actions::setVirtualizer,
            defaultOn = DEFAULT_EFFECT,
        ) { PillSlider("Sanallaştırıcı", s.virtualizer, MAX_EFFECT, actions::setVirtualizer, enabled = s.enabled) }
        EffectCard(
            title = "Ses yükseltici",
            icon = Icons.AutoMirrored.Filled.VolumeUp,
            polygon = MaterialShapes.Clover4Leaf,
            value = s.loudnessMb,
            max = MAX_LOUDNESS_MB,
            enabled = s.enabled,
            container = MaterialTheme.colorScheme.surfaceContainerHigh,
            onValue = actions::setLoudness,
            defaultOn = DEFAULT_LOUDNESS,
        ) { WavyLevel("Ses yükseltici", s.loudnessMb, MAX_LOUDNESS_MB, actions::setLoudness, enabled = s.enabled) }
    }
}

private const val PERCENT_STEP = 10

/** The line the page leads with: what is happening, in words and in the colour of the strip under it. */
@Composable
private fun StatusHeader(status: Status, onOpenSetup: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val strip = when (status.health) {
        Health.ACTIVE -> scheme.tertiary
        Health.WAITING -> scheme.secondary
        Health.OFF -> scheme.outline
    }
    Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest)) {
        Column(
            Modifier.fillMaxWidth().semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(status.headline, style = MaterialTheme.typography.headlineMediumEmphasized, textAlign = TextAlign.Center)
                Text(
                    status.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (status.health == Health.WAITING) {
                    FilledTonalButton(onClick = onOpenSetup, shapes = ButtonDefaults.shapes(), modifier = Modifier.padding(top = 12.dp)) {
                        Text("Kuruluma git")
                    }
                }
            }
            Box(Modifier.fillMaxWidth().height(STRIP_HEIGHT).background(strip))
        }
    }
}

private val STRIP_HEIGHT = 6.dp

/** An icon, a small label, the value and a coloured pill on the right, like the stat rows of a connection screen. */
@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    pill: String?,
    pillContainer: Color,
    pillContent: Color,
    detail: String? = null,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                }
            }
            if (pill != null) {
                Surface(shape = CircleShape, color = pillContainer, contentColor = pillContent) {
                    Text(
                        pill,
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** Picks the headphone whose measured response is corrected toward the Harman target, or none. */
@Composable
private fun HeadphoneCard(settings: EqSettings, route: OutputRoute, actions: SludgeActions) {
    val scheme = MaterialTheme.colorScheme
    val selectedId = settings.headphoneId
    val profile = Headphones.byId(selectedId)
    Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainer)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Icon(Icons.Filled.Headphones, contentDescription = null, tint = scheme.onSurfaceVariant)
                Column(Modifier.weight(1f)) {
                    Text("Kulaklık düzeltmesi", style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant)
                    Text(profile?.let { "${it.name} · Harman hedefi" } ?: "Kapalı", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = profile == null, onClick = { actions.setHeadphone(null) }, label = { Text("Yok") }, shape = CircleShape)
                Headphones.all.forEach { headphone ->
                    FilterChip(
                        selected = headphone.id == selectedId,
                        onClick = { actions.setHeadphone(headphone.id) },
                        label = { Text(headphone.name) },
                        shape = CircleShape,
                    )
                }
            }
            if (profile != null) {
                val active = settings.correctionActive(route)
                Text(
                    if (active) {
                        "Düzeltme şu an açık (çıkış: ${route.label()})."
                    } else {
                        "Düzeltme beklemede: çıkış ${route.label()}; ${profile.name} bağlanınca kendiliğinden açılır."
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (active) scheme.tertiary else scheme.onSurfaceVariant,
                )
                SwitchRow(settings.headphoneAlways, actions::setHeadphoneAlways) {
                    Text("Her çıkışta uygula", Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                }
                val strength = settings.headphoneStrength / PERCENT_FULL
                val preamp = remember(profile, strength) { profile.preampDb(strength) }
                Text(
                    "Düzeltme gücü: %${settings.headphoneStrength} · ses yaklaşık " +
                        "${String.format(java.util.Locale.forLanguageTag("tr-TR"), "%.1f", -preamp)} dB kısılır",
                    style = MaterialTheme.typography.titleSmall,
                )
                PillSlider("Düzeltme gücü", settings.headphoneStrength, PERCENT_MAX, actions::setHeadphoneStrength)
                val correction = remember(profile, strength) { profile.correctionCurve(strength) }
                ResponseChart(emptyList(), Modifier.fillMaxWidth().height(96.dp), curve = correction)
                Text(
                    "Çizgi, düzeltmenin hangi frekansı ne kadar değiştirdiğini gösterir (kısılma payı dahil). " +
                        "Sesin bu kulaklığın ölçülmüş yanıtına göre Harman hedefine yaklaştırılır: 3,5 kHz civarındaki sivrilik " +
                        "alınır, 10 kHz üstü ve alt bas yükseltilir. Çok alçak bas (35 Hz altı) bu kulaklıkta çalmadığı için " +
                        "artışlar ${profile.maxBoostDb.toInt()} dB ile sınırlıdır; böylece ses gereğinden fazla kısılmaz. " +
                        "Kaynak: ${profile.source}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** The genre presets as one scrolling row of pills; the one that matches the current sound is selected. */
@Composable
private fun PresetRow(state: UiState, actions: SludgeActions) {
    val s = state.settings
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Ön ayar", Modifier.padding(start = 8.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Genre.entries.forEach { genre ->
                val selected = s.filters == genre.filters && s.bassBoost == genre.bass && s.virtualizer == genre.virtualizer
                FilterChip(
                    selected = selected,
                    onClick = { actions.applyPreset(genre) },
                    label = { Text(genre.label) },
                    leadingIcon = if (selected) {
                        { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                    shape = CircleShape,
                )
            }
        }
        Genre.entries.firstOrNull { it.filters == s.filters && it.bass == s.bassBoost && it.virtualizer == s.virtualizer }?.let {
            Text(
                it.blurb,
                Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}


