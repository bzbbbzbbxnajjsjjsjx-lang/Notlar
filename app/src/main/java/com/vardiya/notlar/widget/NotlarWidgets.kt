package com.vardiya.notlar.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.glance.material3.ColorProviders
import com.vardiya.notlar.ui.theme.DarkColors
import com.vardiya.notlar.ui.theme.LightColors

/** The widgets wear the app's amber scheme, light or dark with the device. */
internal val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

object NotlarWidgets {
    /** Redraws the list widgets after the notes or the lock changed outside them. */
    suspend fun refresh(context: Context) {
        ChecklistWidget().updateAll(context)
    }
}
