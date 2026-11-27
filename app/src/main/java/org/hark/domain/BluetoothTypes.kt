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

    /** Short name of an output type, for the activity list. */
    @Suppress("InlinedApi")
    fun describe(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "phone speaker"
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "phone earpiece"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth call audio (SCO)"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "Bluetooth LE speaker"
        AudioDeviceInfo.TYPE_HEARING_AID -> "hearing aid"
        AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "wired headphones"
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> "USB audio"
        else -> "output type $type"
    }
}
