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

/**
 * Plays a file through one specific output device and nowhere else (adapted from Psst's AlarmAudioPlayer).
 *
 * The player starts muted. It becomes audible only while every device Android has routed it to is the
 * requested one. If the route never matches within [ROUTE_TIMEOUT_MS], or moves away later (for example the
 * earbuds disconnect mid-sentence and Android falls back to the speaker), playback is muted and stopped.
 */
class RoutedPlayer(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var routingListener: AudioRouting.OnRoutingChangedListener? = null

    fun play(uri: Uri, output: AudioDeviceInfo, onFailure: () -> Unit, onComplete: () -> Unit) {
        stop()
        val next = MediaPlayer()
        player = next
        fun fail() {
            if (player !== next) return
            stop()
            onFailure()
        }
        try {
            next.setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            next.setDataSource(context, uri)
            next.setVolume(0f, 0f)
            next.setOnErrorListener { _, _, _ -> fail(); true }
            next.setOnCompletionListener { if (player === next) { stop(); onComplete() } }
            next.setOnPreparedListener {
                if (player !== next) return@setOnPreparedListener
                if (!next.setPreferredDevice(output)) { fail(); return@setOnPreparedListener }
                val began = SystemClock.elapsedRealtime()
                var audible = false
                fun verify() {
                    if (player !== next) return
                    val routed = if (Build.VERSION.SDK_INT >= 36) next.routedDevices else listOfNotNull(next.routedDevice)
                    val matches = routed.isNotEmpty() && routed.all { it.id == output.id }
                    val volume = if (matches) 1f else 0f
                    next.setVolume(volume, volume)
                    if (matches) audible = true
                    else if (audible || SystemClock.elapsedRealtime() - began >= ROUTE_TIMEOUT_MS) fail()
                }
                routingListener = AudioRouting.OnRoutingChangedListener { verify() }
                next.addOnRoutingChangedListener(requireNotNull(routingListener), handler)
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
        } catch (_: Exception) { fail() }
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
        const val POLL_MS = 100L
    }
}
