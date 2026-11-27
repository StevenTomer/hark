package org.hark.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioRouting
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.hark.domain.BluetoothTypes

/**
 * Plays a file through Bluetooth media audio and nowhere else (adapted from Psst's AlarmAudioPlayer).
 *
 * The player is pinned to [output] and starts muted, looping. It only becomes audible once every device
 * Android has routed it to is a Bluetooth media device, and that route has held for [WARMUP_MS] so the
 * earbuds' audio stream is awake. Then it rewinds to the start, stops looping and unmutes. If the route never
 * becomes Bluetooth within [ROUTE_TIMEOUT_MS], or leaves Bluetooth later (for example the earbuds disconnect
 * and Android falls back to the speaker), playback is muted and stopped. Success is reported only for a
 * playback that was audible on Bluetooth from start to end.
 */
class RoutedPlayer(private val context: Context) {
    private enum class State { WAITING, WARMING, PLAYING }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var routingListener: AudioRouting.OnRoutingChangedListener? = null

    fun play(uri: Uri, output: AudioDeviceInfo, onFailure: (reason: String) -> Unit, onComplete: () -> Unit) {
        stop()
        val next = MediaPlayer()
        player = next
        var lastRoute = "nothing"
        fun fail(reason: String) {
            if (player !== next) return
            stop()
            onFailure(reason)
        }
        try {
            next.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            next.setDataSource(context, uri)
            next.setVolume(0f, 0f)
            next.setOnErrorListener { _, what, extra -> fail("player error $what/$extra"); true }
            var state = State.WAITING
            next.setOnCompletionListener {
                if (player !== next) return@setOnCompletionListener
                if (state == State.PLAYING) { stop(); onComplete() }
                else fail("audio never reached Bluetooth (went to $lastRoute)")
            }
            next.setOnPreparedListener {
                if (player !== next) return@setOnPreparedListener
                if (!next.setPreferredDevice(output)) { fail("Android refused the Bluetooth output"); return@setOnPreparedListener }
                val began = SystemClock.elapsedRealtime()
                var warmStart = 0L
                fun verify() {
                    if (player !== next) return
                    val routed = if (Build.VERSION.SDK_INT >= 36) next.routedDevices else listOfNotNull(next.routedDevice)
                    if (routed.isNotEmpty()) lastRoute = routed.joinToString { BluetoothTypes.describe(it.type) }
                    val onBluetooth = routed.isNotEmpty() && routed.all { BluetoothTypes.isBluetoothMedia(it.type) }
                    val now = SystemClock.elapsedRealtime()
                    when (state) {
                        State.WAITING -> when {
                            onBluetooth -> { state = State.WARMING; warmStart = now }
                            now - began >= ROUTE_TIMEOUT_MS -> fail("audio went to $lastRoute, not Bluetooth")
                        }
                        State.WARMING -> when {
                            !onBluetooth -> state = State.WAITING
                            now - warmStart >= WARMUP_MS -> {
                                next.isLooping = false
                                next.seekTo(0)
                                next.setVolume(1f, 1f)
                                state = State.PLAYING
                            }
                        }
                        State.PLAYING -> if (!onBluetooth) {
                            next.setVolume(0f, 0f)
                            fail("audio moved to $lastRoute mid-message")
                        }
                    }
                }
                routingListener = AudioRouting.OnRoutingChangedListener { verify() }
                next.addOnRoutingChangedListener(requireNotNull(routingListener), handler)
                next.isLooping = true
                next.start()
                verify()
                handler.postDelayed(object : Runnable {
                    override fun run() {
                        if (player !== next) return
                        verify()
                        if (player === next) handler.postDelayed(this, POLL_MS)
                    }
                }, POLL_MS)
            }
            next.prepareAsync()
        } catch (e: Exception) { fail("could not play audio (${e.javaClass.simpleName})") }
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        val current = player
        player = null
        routingListener?.let { current?.removeOnRoutingChangedListener(it) }
        routingListener = null
        runCatching { current?.setVolume(0f, 0f) }
        current?.release()
    }

    private companion object {
        const val ROUTE_TIMEOUT_MS = 5_000L
        /** Time for Bluetooth earbuds to wake their audio stream before the message starts over, audibly. */
        const val WARMUP_MS = 700L
        const val POLL_MS = 50L
    }
}
