package com.vardiya.sludge.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.vardiya.sludge.domain.OutputKind
import com.vardiya.sludge.domain.OutputRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Watches which output devices are connected, so a headphone correction is used only on that headphone. */
class OutputMonitor(context: Context) {
    private val audio = context.applicationContext.getSystemService(AudioManager::class.java)
    private val _route = MutableStateFlow(current())
    val route: StateFlow<OutputRoute> = _route.asStateFlow()

    init {
        audio?.registerAudioDeviceCallback(
            object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refresh()

                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refresh()
            },
            Handler(Looper.getMainLooper()),
        )
    }

    private fun refresh() {
        _route.value = current()
    }

    private fun current(): OutputRoute {
        val outputs = audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        val bluetooth = outputs.filter { it.type in BLUETOOTH_TYPES || isBle(it.type) }
        return when {
            bluetooth.isNotEmpty() -> OutputRoute(OutputKind.BLUETOOTH, bluetooth.map { it.productName?.toString().orEmpty() })
            outputs.any { it.type in WIRED_TYPES } -> OutputRoute(OutputKind.WIRED)
            outputs.isNotEmpty() -> OutputRoute(OutputKind.SPEAKER)
            else -> OutputRoute.UNKNOWN
        }
    }

    private fun isBle(type: Int) = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET

    private companion object {
        val BLUETOOTH_TYPES = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLUETOOTH_SCO)
        val WIRED_TYPES = setOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET)
    }
}
