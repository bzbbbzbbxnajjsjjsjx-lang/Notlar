package com.vardiya.notlar

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vardiya.notlar.domain.ThemeMode
import com.vardiya.notlar.ui.LaunchAction
import com.vardiya.notlar.ui.NotesViewModel
import com.vardiya.notlar.ui.NotlarApp
import com.vardiya.notlar.ui.theme.NotlarTheme

class MainActivity : ComponentActivity() {

    private val viewModel: NotesViewModel by viewModels()

    /** A shortcut or a share from another app that the UI has not handled yet. */
    private var launchAction by mutableStateOf<LaunchAction?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // After a configuration change the intent has already been handled.
        if (savedInstanceState == null) launchAction = intent.toLaunchAction()
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // System bar icons follow the app's own light or dark choice, not only the device's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            NotlarTheme(darkTheme = dark, dynamicColor = settings.dynamicColor) {
                NotlarApp(
                    viewModel = viewModel,
                    launchAction = launchAction,
                    onLaunchActionConsume = { launchAction = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.toLaunchAction()?.let { launchAction = it }
    }

    private fun Intent.toLaunchAction(): LaunchAction? = when (action) {
        ACTION_NEW_NOTE -> LaunchAction.NewNote
        ACTION_NEW_CHECKLIST -> LaunchAction.NewChecklist
        Intent.ACTION_SEND -> getStringExtra(Intent.EXTRA_TEXT)
            ?.takeIf { it.isNotBlank() }
            ?.let { LaunchAction.Shared(it, getStringExtra(Intent.EXTRA_SUBJECT)) }
        else -> null
    }

    private companion object {
        const val ACTION_NEW_NOTE = "com.vardiya.notlar.NEW_NOTE"
        const val ACTION_NEW_CHECKLIST = "com.vardiya.notlar.NEW_CHECKLIST"
    }
}
