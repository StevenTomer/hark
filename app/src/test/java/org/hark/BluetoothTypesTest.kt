package org.hark

import android.media.AudioDeviceInfo
import org.hark.domain.BluetoothTypes
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BluetoothTypesTest {
    @Test fun acceptsBluetoothMediaOnly() {
        assertTrue(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
        assertTrue(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_BLE_HEADSET))
        assertFalse(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        assertFalse(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE))
        assertFalse(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_BLUETOOTH_SCO))
        assertFalse(BluetoothTypes.isBluetoothMedia(AudioDeviceInfo.TYPE_WIRED_HEADPHONES))
    }
}

class BluetoothTypesDescribeTest {
    @Test fun describesSpeakerAndBluetooth() {
        org.junit.Assert.assertEquals("phone speaker", BluetoothTypes.describe(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER))
        org.junit.Assert.assertEquals("Bluetooth A2DP", BluetoothTypes.describe(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP))
    }
}
