package org.hark.domain

import android.media.AudioDeviceInfo

/**
 * Bluetooth outputs Hark may speak through. SCO is left out on purpose: it is the call/headset-profile
 * link, and using it would hijack calls. The phone's own speaker and earpiece are never in this set.
 */
object BluetoothTypes {
    // Values are inlined constants, so this compiles on every API level; newer types simply never appear on older phones.
    @Suppress("InlinedApi")
    val media: Set<Int> = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLE_HEADSET,
        AudioDeviceInfo.TYPE_BLE_SPEAKER,
        AudioDeviceInfo.TYPE_HEARING_AID,
    )

    fun isBluetoothMedia(type: Int) = type in media

    /** Prefer classic A2DP, then LE Audio, then hearing aids. */
    fun rank(type: Int): Int = when (type) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> 0
        @Suppress("InlinedApi") AudioDeviceInfo.TYPE_BLE_HEADSET -> 1
        @Suppress("InlinedApi") AudioDeviceInfo.TYPE_BLE_SPEAKER -> 2
        else -> 3
    }
}
