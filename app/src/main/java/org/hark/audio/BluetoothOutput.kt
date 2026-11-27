package org.hark.audio

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.hark.domain.BluetoothTypes

/** Tracks the connected Bluetooth media output (adapted from Psst's AudioDeviceMonitor). */
class BluetoothOutput(context: Context) {
    private val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val _current = MutableStateFlow<AudioDeviceInfo?>(null)
    val current: StateFlow<AudioDeviceInfo?> = _current
    private val listeners = mutableListOf<(AudioDeviceInfo?) -> Unit>()

    init {
        manager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) = refresh()
            override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) = refresh()
        }, Handler(Looper.getMainLooper()))
        refresh()
    }

    /** The output to speak through right now, or null if no Bluetooth media device is connected. */
    fun find(): AudioDeviceInfo? = manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        .filter { it.isSink && BluetoothTypes.isBluetoothMedia(it.type) }
        .minByOrNull { BluetoothTypes.rank(it.type) }

    /** A call (or VoIP call) is in progress; speaking now would talk over it. */
    fun inCall(): Boolean = manager.mode != AudioManager.MODE_NORMAL

    fun addListener(listener: (AudioDeviceInfo?) -> Unit) { listeners += listener }

    private fun refresh() {
        val device = find()
        _current.value = device
        listeners.forEach { it(device) }
    }
}

val AudioDeviceInfo.displayName: String
    get() = productName?.toString()?.takeIf { it.isNotBlank() } ?: "Bluetooth audio"
