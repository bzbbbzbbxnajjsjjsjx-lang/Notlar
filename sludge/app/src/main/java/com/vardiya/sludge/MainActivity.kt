package com.vardiya.sludge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vardiya.sludge.ui.SludgeApp
import com.vardiya.sludge.ui.theme.SludgeTheme

class MainActivity : ComponentActivity() {
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
