package com.vardiya.notlar.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.RowScope
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.vardiya.notlar.MainActivity

/** Two buttons on the home screen: a new note or a new to-do list, like the launcher shortcuts. */
class QuickNoteWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                WidgetSurface {
                    Row(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                        QuickButton(
                            "Not",
                            context,
                            checklist = false,
                            GlanceTheme.colors.primaryContainer,
                            GlanceTheme.colors.onPrimaryContainer,
                        )
                        Spacer(GlanceModifier.width(8.dp))
                        QuickButton(
                            "Liste",
                            context,
                            checklist = true,
                            GlanceTheme.colors.secondaryContainer,
                            GlanceTheme.colors.onSecondaryContainer,
                        )
                    }
                }
            }
        }
    }
}

class QuickNoteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickNoteWidget()
}

@Composable
private fun RowScope.QuickButton(label: String, context: Context, checklist: Boolean, container: ColorProvider, content: ColorProvider) {
    Box(
        GlanceModifier
            .defaultWeight()
            .fillMaxHeight()
            .background(container)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(MainActivity.newNoteIntent(context, checklist))),
        contentAlignment = Alignment.Center,
    ) {
        Text("+ $label", style = TextStyle(color = content, fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
}
