package com.vardiya.sludge.ui

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vardiya.sludge.audio.SludgeListenerService
import com.vardiya.sludge.audio.SludgeRuntime
import com.vardiya.sludge.domain.AiConfig
import com.vardiya.sludge.domain.AiProfile
import com.vardiya.sludge.domain.CurveMath
import com.vardiya.sludge.domain.EqSettings
import com.vardiya.sludge.domain.Genre
import com.vardiya.sludge.domain.MAX_EFFECT
import com.vardiya.sludge.domain.MAX_LOUDNESS_MB
import com.vardiya.sludge.domain.TuneSource
import com.vardiya.sludge.domain.applying
import com.vardiya.sludge.domain.toProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class SludgeViewModel(private val app: Application) : AndroidViewModel(app), SludgeActions {
    private val runtime = SludgeRuntime.get(app)
    private val access = MutableStateFlow(isAccessGranted())

    val accessGranted: Boolean get() = access.value

    private val core = combine(runtime.settings, runtime.track, runtime.status, runtime.ai) { settings, track, status, ai ->
        UiState(settings = settings, track = track, status = status, ai = ai)
    }

    val state: StateFlow<UiState> = combine(core, runtime.engine.state, access) { ui, engine, granted ->
        ui.copy(sessions = engine.sessions, globalMixWorks = engine.globalMixWorks, accessGranted = granted)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), UiState(accessGranted = access.value))

    /** Called when the app comes back to the front: the user may just have flipped the switch in system settings. */
    fun refreshAccess() {
        access.value = isAccessGranted()
    }

    private fun isAccessGranted() = NotificationManagerCompat.getEnabledListenerPackages(app).contains(app.packageName)

    private fun manual(transform: (EqSettings) -> EqSettings) = runtime.update { transform(it).copy(source = TuneSource.MANUAL) }

    override fun setEnabled(enabled: Boolean) = runtime.update { it.copy(enabled = enabled) }

    override fun setBand(index: Int, db: Float) = manual { s ->
        s.copy(bands = s.bands.mapIndexed { i, old -> if (i == index) db else old })
    }

    override fun resetBands() = manual { it.copy(bands = EqSettings.FLAT_BANDS) }

    override fun setBass(value: Int) = manual { it.copy(bassBoost = value.coerceIn(0, MAX_EFFECT)) }

    override fun setVirtualizer(value: Int) = manual { it.copy(virtualizer = value.coerceIn(0, MAX_EFFECT)) }

    override fun setLoudness(valueMb: Int) = manual { it.copy(loudnessMb = valueMb.coerceIn(0, MAX_LOUDNESS_MB)) }

    override fun setAutoTune(enabled: Boolean) = runtime.update { it.copy(autoTune = enabled) }

    override fun applyPreset(genre: Genre) = runtime.update { it.applying(genre.toProfile(), TuneSource.PRESET) }

    override fun tuneNow() = runtime.tuneNow()

    override fun saveAi(config: AiConfig) = runtime.saveAi(config)

    override suspend fun testAi(config: AiConfig): Result<AiProfile> = runtime.testConnection(config)

    override fun openNotificationAccess() {
        val general = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                ComponentName(app, SludgeListenerService::class.java).flattenToString(),
            )
        } else {
            general
        }
        // The detail page is missing on some builds, and unreachable while the setting is restricted.
        if (!start(detail)) start(general)
    }

    override fun openAppInfo() {
        start(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", app.packageName, null)))
    }

    override fun openBatterySettings() {
        if (!start(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))) openAppInfo()
    }

    private fun start(intent: Intent): Boolean = try {
        app.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
