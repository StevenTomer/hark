# Hark

Hark is an open-source Android app that reads new text messages aloud as they arrive, **only through Bluetooth audio** (earbuds, headphones, car stereo). If no Bluetooth audio device is connected, it stays silent. It never uses the phone's speaker.

[Download the latest APK](https://tomerhome.org/utils/android/#hark)

Requires Android 9 or later.

## What it reads

- Text messages from your SMS app (Google Messages, Samsung Messages, or whatever your default SMS app is).
- Google Voice texts and voicemail notifications.

Each message is read as "Message from *sender*: *text*". In group chats only the sender is named, not the other members. Very long messages are cut off at about 600 characters.

## How it works

- Hark reads message **notifications**, using Android's notification-access permission. That is how it can see Google Voice texts, and it needs no SMS permissions. Grant access from the app's Status card.
  - If you installed the APK outside the Play Store, Android may say the setting is restricted. Open **App info → ⋮ → Allow restricted settings**, then grant access again.
- Speech is first turned into an audio file by the phone's text-to-speech engine. It is then played with the Bluetooth device pinned as the output.
  - Playback stays muted until Android confirms the audio is routed to that device.
  - If the route ever moves elsewhere (for example the earbuds disconnect mid-sentence), playback is muted and stopped.
- Messages that arrive while no Bluetooth audio is connected, during a call, or during **quiet hours** (an optional daily window, such as 10 PM–7 AM), are skipped, not saved for later.
- Hark never dismisses or marks anything read: notifications stay in the shade.
- Bluetooth hands-free (SCO, call audio) is not used, so Hark won't take over the call channel.
- Music ducks briefly while a message is read. Volume follows the media volume.

## Build

Use JDK 21 and Android SDK platform 37.2. Set `ANDROID_HOME` or point `sdk.dir` in an untracked `local.properties` at your Android SDK.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The debug APK uses `org.hark.debug`. Release builds (`./gradlew assembleRelease`) are signed with the key described in `~/.android-keys/hark-release.properties` if that file exists. Keep keys outside the repository.

For a quick test on a device or emulator over adb:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell cmd notification allow_listener org.hark.debug/org.hark.service.HarkListenerService
```

See `PLAN.md` for the design and `LICENSE` for Apache-2.0 terms.
