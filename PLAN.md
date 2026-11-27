# Hark design

## Context
An open-source Android app that reads incoming text messages (SMS and Google Voice) aloud as they arrive. It must speak only through a connected Bluetooth device and never through the phone speaker. Decisions: name **Hark** (`org.hark`), Apache-2.0, sources = default SMS app + Google Messages + Google Voice (texts and voicemail transcripts), and speech of the form "Message from <sender>: <text>".

Psst (github.com/StevenTomer/psst) already solves the hard part: routing speech to one specific output device and checking the route. Hark copies that code and adapts it.

## Approach

### How messages are captured
- `HarkListenerService : NotificationListenerService`. This is the only way to see Google Voice texts, and it needs no SMS permissions. The user grants it under "Notification access".
- Allowed packages: `Telephony.Sms.getDefaultSmsPackage()`, `com.google.android.apps.messaging`, `com.google.android.apps.googlevoice`, `com.samsung.android.messaging`. The user can turn each source on or off.
- Ignore group summaries, ongoing notifications, and outgoing or self messages (a MessagingStyle message whose person is the user).
- Pull the sender and text from `NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification` and use the newest message(s). If the notification has no MessagingStyle, use `EXTRA_TITLE` / `EXTRA_TEXT` (Voice voicemail notifications work this way).
- Dedupe: messaging apps repost the same notification when it is read or updated. Keep the last spoken (key, message timestamp, text hash) for each key and speak only messages that are new.
- Put the pure logic in `domain/MessageExtractor.kt` and `domain/Deduper.kt` so it can be unit-tested.

### Bluetooth-only output (the core guarantee)
- `audio/BluetoothOutput.kt`, adapted from `psst/app/src/main/java/org/psst/audio/AudioDeviceMonitor.kt`. It picks a Bluetooth media output: `TYPE_BLUETOOTH_A2DP`, `TYPE_BLE_HEADSET`, `TYPE_BLE_SPEAKER`, or `TYPE_HEARING_AID`. SCO is excluded because it is call audio.
- No Bluetooth output means nothing is spoken, and the message is dropped rather than queued.
- If a phone call is in progress (`AudioManager.mode != MODE_NORMAL`), skip the message.
- `audio/RoutedPlayer.kt` is copied from `psst/.../audio/AlarmAudioPlayer.kt` (the `output != null` branch only). It uses `MediaPlayer` with `USAGE_MEDIA` and `setPreferredDevice(bt)`, and the volume stays 0 until every routed device has the target id.
  - If the route doesn't match within 5 s, it fails silently.
  - If the route moves off Bluetooth during playback, it mutes and stops right away.
  - So even when Android reroutes, the speaker hears nothing.
- `audio/Speaker.kt`, adapted from `psst/.../audio/SpokenAlarmPlayer.kt`. It keeps one warm `TextToSpeech` engine, uses `synthesizeToFile` to make a cache WAV, and plays that WAV through RoutedPlayer.
  - It holds transient audio focus with ducking, so music dips while a message is read.
  - Messages are read one at a time from a FIFO queue. The queue is cleared when Bluetooth disconnects (an `AudioDeviceCallback`).
- Long messages are capped at about 600 characters, with "message truncated" spoken at the end.

### App UI (Compose, one screen, DataStore settings)
- Master on/off switch.
- Status card:
  - whether notification access is granted, with a button to `ACTION_NOTIFICATION_LISTENER_SETTINGS`;
  - a hint about Android's "Allow restricted settings" for sideloaded apps;
  - the current Bluetooth output name (live).
- Source toggles: SMS app and Google Voice.
- Speech rate slider.
- "Speak a test message" button. It goes through the same Bluetooth-only path and shows "No Bluetooth audio connected" when there is none.

### Project scaffold (copy Psst's form)
- Copy from psst: `gradle/` wrapper, `gradlew*`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, and `gradle/libs.versions.toml`. Drop Room, KSP and media3 and keep DataStore.
- Toolchain: compileSdk 37.2, targetSdk 37, and **minSdk 28**, because route verification needs API 28 (Psst refuses private playback below P).
- Debug suffix `.debug`. Release signing from `~/.android-keys/hark-release.{jks,properties}` (not committed).
- Manifest: the listener service with `BIND_NOTIFICATION_LISTENER_SERVICE`, `<queries>` for `TTS_SERVICE`, and `MainActivity`. No runtime permissions are needed.
- Files: `.github/workflows/build.yml` (psst's, with the artifact renamed), `LICENSE` (Apache-2.0), `README.md`, and `PLAN.md`.
- Layout: `org.hark.{HarkApplication, domain, audio, data, service, ui}`.

### Unit tests
- MessageExtractor: MessagingStyle, fallback to title/text, self-message skipped, group summary skipped.
- Deduper: a repost is not respoken; a new message in the same thread is spoken.
- Speech text formatting and truncation.
- Bluetooth device-type classification.

## Verification
1. `./gradlew testDebugUnitTest lintDebug assembleDebug` passes.
2. If a phone is on adb:
   - install, then grant access with `adb shell cmd notification allow_listener org.hark.debug/org.hark.service.HarkListenerService`;
   - with Bluetooth earbuds connected, the test button speaks in the earbuds;
   - with Bluetooth off, the test button is silent and the app shows the message.
3. Manual checks on a real phone:
   - an SMS and a Google Voice text sent from another phone are spoken over Bluetooth with Bluetooth on, and nothing is spoken with Bluetooth off;
   - reading the thread doesn't make it speak again;
   - disconnecting the earbuds mid-message stops it with no speaker leak.

