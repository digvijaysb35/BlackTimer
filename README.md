# Black Timer — Android source project

A minimal, true-black countdown timer for Android (Kotlin, XML/programmatic views).

## Included
- Countdown entry in hours, minutes and seconds.
- Start and stop controls; quick 1/5/10/15-minute presets.
- Save, load and delete custom named presets.
- Select a system alarm ringtone or import an audio file through Android's document picker.
- App-level ring-volume slider.
- Foreground countdown service, ongoing notification, partial wake lock and exact-alarm scheduling fallback.
- Looping alarm playback, test-ring button and volume-key handling through an optional AccessibilityService.
- Pure black UI suitable for AMOLED screens.

## Build with GitHub Actions
1. Open the repository's **Actions** tab.
2. Select **Build Black Timer APK**.
3. Push to main or choose **Run workflow**.
4. When the workflow completes, open the run and download the `BlackTimer-debug-apk` artifact.
5. Extract the artifact ZIP and install `app-debug.apk` on your Android phone.

The workflow builds a debug APK for testing; it does not publish to Google Play.

## Build locally
Open this folder in Android Studio, install Android SDK 35, sync Gradle and choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## First-run notes
- Grant notification permission on Android 13+.
- On Android 12+, allow **Alarms & reminders** if exact timing is needed.
- To use volume keys to stop the countdown or silence the alarm, enable **Settings > Accessibility > Black Timer**. This is optional and Android/device restrictions may affect key capture.
- The in-app ring-volume slider controls app playback gain; Android system volume and audio policy can still limit loudness.
