# Privacy Island

Privacy Island is a small Android prototype for an iPhone Dynamic Island-style notification pill.

The important privacy choice: this app does **not** request Internet permission. Notifications are processed locally on the phone.

## Features

- Floating island overlay near the selfie camera area.
- Notification listener with private preview mode.
- Sensitive app filtering for banking, UPI, and OTP-heavy apps.
- Battery saver default: the overlay appears for fresh events, then the service stops.
- Optional always-visible idle pill for the punch-hole look.
- Quick buttons for notification access and overlay permission.
- No analytics, no account, no network access.

## Build

Open this folder in Android Studio, let Gradle sync, then run the `app` module on your iQOO phone.

Required permissions on the phone:

- Notification Access
- Display over other apps
- Notifications permission on Android 13+

## Build APK Without Android Studio

You can build the APK on GitHub for free:

1. Create a new GitHub repository.
2. Upload this project folder's contents to the repository root.
3. Open the repository's **Actions** tab.
4. Run **Build APK**.
5. Download the `PrivacyIsland-debug-apk` artifact.

The APK inside the artifact is `app-debug.apk`.

## Notes

Some iQOO/Funtouch OS versions may stop background services aggressively. If the island disappears, allow background activity and disable battery optimization for Privacy Island.

For best battery life, keep "Always visible island" off. Android recommends foreground services only for work that is noticeable to the user; keeping the idle pill always visible is intentionally optional.
