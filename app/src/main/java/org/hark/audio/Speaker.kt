package org.hark.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.Locale

/**
 * Reads text aloud, one item at a time, through the connected Bluetooth output only.
 *
 * Speech is synthesized to a WAV file first (TextToSpeech can't be pinned to a device) and then played by
 * [RoutedPlayer], which refuses to be audible anywhere but the Bluetooth device. With no Bluetooth output,
 * or during a call, items are dropped. All methods must be called on the main thread.
 */
class Speaker(
    private val context: Context,
    private val bluetooth: BluetoothOutput,
    private val speechRate: () -> Float,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val player = RoutedPlayer(context)
    private val queue = ArrayDeque<String>()
    private var engine: TextToSpeech? = null
    private var engineReady = false
    private var busy = false
    private var currentFile: File? = null
    private var utteranceCounter = 0
    private var focusRequest: AudioFocusRequest? = null

    private val _lastEvent = MutableStateFlow<String?>(null)
    /** A short human-readable note of what happened to the most recent message, shown in the app. */
    val lastEvent: StateFlow<String?> = _lastEvent

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

    init {
        bluetooth.addListener { device -> if (device == null) clear() }
    }

    /** Returns false (and drops [text]) when there is no Bluetooth output to speak through. */
    fun speak(text: String, label: String = "Message"): Boolean {
        if (bluetooth.find() == null) {
            note("$label not read: no Bluetooth audio connected")
            return false
        }
        if (bluetooth.inCall()) {
            note("$label not read: a call is in progress")
            return false
        }
        queue.addLast(text)
        ensureEngine()
        pump()
        return true
    }

    fun clear() {
        queue.clear()
        player.stop()
        currentFile?.delete()
        currentFile = null
        if (busy) note("Stopped: Bluetooth audio disconnected")
        busy = false
        abandonFocus()
    }

    private fun note(text: String) { _lastEvent.value = text }

    private fun ensureEngine() {
        if (engine != null) return
        engine = TextToSpeech(context) { status ->
            handler.post {
                val tts = engine ?: return@post
                if (status != TextToSpeech.SUCCESS) {
                    note("Text-to-speech is unavailable on this phone")
                    tts.shutdown(); engine = null; queue.clear()
                    return@post
                }
                tts.language = Locale.getDefault()
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onDone(utteranceId: String?) { handler.post { synthesized(utteranceId) } }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { handler.post { failed(utteranceId, "speech synthesis failed") } }
                    override fun onError(utteranceId: String?, errorCode: Int) { handler.post { failed(utteranceId, "speech synthesis failed") } }
                })
                engineReady = true
                pump()
            }
        }
    }

    private fun pump() {
        if (busy || !engineReady) return
        val tts = engine ?: return
        val text = queue.removeFirstOrNull() ?: run { abandonFocus(); return }
        if (bluetooth.find() == null || bluetooth.inCall()) { clear(); return }
        val file = runCatching { File.createTempFile("hark-", ".wav", context.cacheDir) }.getOrNull() ?: return
        busy = true
        currentFile = file
        val id = "hark-${++utteranceCounter}"
        tts.setSpeechRate(speechRate())
        if (tts.synthesizeToFile(text, null, file, id) != TextToSpeech.SUCCESS) failed(id, "speech synthesis failed")
    }

    private fun isCurrent(utteranceId: String?) = busy && utteranceId == "hark-$utteranceCounter"

    private fun synthesized(utteranceId: String?) {
        if (!isCurrent(utteranceId)) return
        val file = currentFile ?: return
        val output = bluetooth.find()
        if (output == null || bluetooth.inCall()) { clear(); return }
        requestFocus()
        player.play(Uri.fromFile(file), output,
            onFailure = { finished("Not read: audio would not route to ${output.displayName}") },
            onComplete = { finished("Read aloud on ${output.displayName}") })
    }

    private fun failed(utteranceId: String?, reason: String) {
        if (isCurrent(utteranceId)) finished("Not read: $reason")
    }

    private fun finished(event: String) {
        note(event)
        currentFile?.delete()
        currentFile = null
        busy = false
        // A short pause between messages.
        handler.postDelayed({ pump() }, 400)
    }

    private fun requestFocus() {
        if (focusRequest != null) return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { }
            .build()
        audioManager.requestAudioFocus(request)
        focusRequest = request
    }

    private fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}
