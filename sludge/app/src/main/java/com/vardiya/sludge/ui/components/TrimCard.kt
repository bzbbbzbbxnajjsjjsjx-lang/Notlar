package com.vardiya.sludge.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Trim
import com.vardiya.sludge.ui.SludgeActions
import java.util.Locale

private val TURKISH: Locale = Locale.forLanguageTag("tr-TR")

/** Two sliders for the person's own general bass and treble, for how their pair differs from the measured one. */
@Composable
fun TrimCard(settings: EqSettings, actions: SludgeActions, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Genel ince ayar", Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium)
            Text(
                "Aynı model kulaklıklar 1–2 dB farklı çıkabilir. Kendi çiftindeki farkı kulağınla burada telafi et; " +
                    "ön ayarlardan ve düzeltmeden bağımsız, her müziğe uygulanır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TrimSlider("Genel bas", settings.trimBassDb, actions::setTrimBass, settings.enabled)
            TrimSlider("Genel tiz", settings.trimTrebleDb, actions::setTrimTreble, settings.enabled)
        }
    }
}

@Composable
private fun TrimSlider(label: String, db: Float, onChange: (Float) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(String.format(TURKISH, "%+.1f dB", db), style = MaterialTheme.typography.labelLarge)
        }
        PillSlider(
            label, Trim.toSlider(db), Trim.SLIDER_MAX, { onChange(Trim.fromSlider(it)) }, enabled = enabled,
            describe = { String.format(TURKISH, "%+.1f dB", Trim.fromSlider(it)) },
        )
    }
}
