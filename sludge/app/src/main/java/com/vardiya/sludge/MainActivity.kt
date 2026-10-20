package com.vardiya.sludge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vardiya.sludge.app.SludgeRuntime
import com.vardiya.sludge.ui.SludgeApp
import com.vardiya.sludge.ui.theme.SludgeTheme

class MainActivity : ComponentActivity() {
    override fun onStop() {
        // Settings are saved a moment after the last change; leaving the screen must not lose the latest.
        SludgeRuntime.get(this).flush()
        super.onStop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SludgeTheme {
                SludgeApp()
            }
        }
    }
}
