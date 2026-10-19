package com.vardiya.sludge.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vardiya.sludge.ui.screens.AiScreen
import com.vardiya.sludge.ui.screens.EqualizerScreen
import com.vardiya.sludge.ui.screens.PresetsScreen
import com.vardiya.sludge.ui.screens.SetupScreen
import com.vardiya.sludge.ui.screens.StatsScreen

// The one stateful entry point: it hands the view model on as the actions interface and nothing else.
@Suppress("ViewModelForwarding")
@Composable
fun SludgeApp(viewModel: SludgeViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // First launch lands on the setup page; once notification access is on, the equalizer comes first.
    var tab by rememberSaveable { mutableStateOf(if (viewModel.accessGranted) Tab.EQUALIZER else Tab.SETUP) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    SludgeScreen(state = state, tab = tab, onTabChange = { tab = it }, actions = viewModel)
}

/** The whole app for one [UiState]; stateless, so screenshot tests can draw it. */
@Composable
fun SludgeScreen(state: UiState, tab: Tab, onTabChange: (Tab) -> Unit, actions: SludgeActions, modifier: Modifier = Modifier) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(if (tab == Tab.EQUALIZER) "Sludge EQ" else tab.title, fontWeight = FontWeight.ExtraBold) },
                scrollBehavior = scrollBehavior,
            )
        },
        bottomBar = { SludgeNavigationBar(tab, onTabChange) },
    ) { padding ->
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "tab",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                Tab.EQUALIZER -> EqualizerScreen(state, actions, padding)
                Tab.STATS -> StatsScreen(state, actions, padding)
                Tab.AI -> AiScreen(state, actions, padding)
                Tab.PRESETS -> PresetsScreen(state, actions, padding)
                Tab.SETUP -> SetupScreen(state, actions, padding)
            }
        }
    }
}

/** The tall four-item bar: labels always visible, 80dp high. */
@Composable
private fun SludgeNavigationBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val haptic = LocalHapticFeedback.current
    NavigationBar {
        Tab.entries.forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) haptic.performHapticFeedback(HapticFeedbackType.VirtualKey)
                    onSelect(tab)
                },
                icon = { Icon(if (isSelected) tab.selectedIcon else tab.icon, contentDescription = null) },
                label = { Text(tab.title) },
                alwaysShowLabel = true,
            )
        }
    }
}

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.EQUALIZER -> Icons.Outlined.Equalizer
        Tab.STATS -> Icons.Outlined.BarChart
        Tab.AI -> Icons.Outlined.AutoAwesome
        Tab.PRESETS -> Icons.Outlined.LibraryMusic
        Tab.SETUP -> Icons.Outlined.Settings
    }

private val Tab.selectedIcon: ImageVector
    get() = when (this) {
        Tab.EQUALIZER -> Icons.Filled.Equalizer
        Tab.STATS -> Icons.Filled.BarChart
        Tab.AI -> Icons.Filled.AutoAwesome
        Tab.PRESETS -> Icons.Filled.LibraryMusic
        Tab.SETUP -> Icons.Filled.Settings
    }
