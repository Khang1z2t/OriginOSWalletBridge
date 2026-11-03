# OriginOS Wallet Bridge

[Tiếng Việt](README.vi.md)

OriginOS Wallet Bridge is a small, independent Android app for compatible OriginOS devices. It lets Android route Vivo's double-press Power shortcut to Google Wallet through either verified implicit action:

- `vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY` from Vivo NFC Base App (`com.vivo.nfcbaseapp`)
- `vivo.intent.action.NFC_BUS_SWINGSPLASH` from Vivo Wallet (`com.vivo.wallet`)

## How It Works

1. OriginOS sends its double-press Power implicit intent.
2. Android routes it to the default handler chosen in Resolver.
3. OriginOS Wallet Bridge starts Google Wallet in its own task and finishes.

The app does not modify NFC payment routing, Wallet Role, Vivo packages, or system settings.

## Features

- No AccessibilityService, root, Shizuku, background service, polling, or WakeLock.
- Keeps Google Wallet in its own Recents task.
- Uses one lightweight Android framework Activity.
- Does not require USB Debugging or Wireless Debugging after installation.
- Adaptive launcher icon: red shortcut ring and floating dot on a soft light background.

## V1.1

- Handles both verified OriginOS shortcut actions: NFC Base App and Vivo Wallet.
- Keeps the same application ID and resolver Activity, preserving existing defaults for the original action.
- A newly detected shortcut action needs its own one-time **Always** choice in Android Resolver.

## Compatibility

Verified device: iQOO 15T running OriginOS 7.

Other OriginOS devices are unverified. The device must dispatch either supported implicit action and show Android Resolver for compatible handlers.

## Install And Setup

1. Install a signed release APK, or a debug APK for testing.
2. Double-press Power.
3. In Android Resolver, choose **OriginOS Wallet Bridge** then **Always**.
4. If Vivo Wallet/NFC Base App changes which shortcut action it sends, repeat step 3 for that action.
5. Double-press Power again to open Google Wallet.

The application ID and resolver Activity component are intentionally stable. Updating the APK with the same signing key preserves the selected default handler.

## Use Google Wallet

Set up cards and NFC payment in Google Wallet normally. This app only launches Google Wallet; it does not configure or alter NFC payment defaults.

## Rollback To Vivo Handler

Open Android App Info for OriginOS Wallet Bridge and clear defaults, then double-press Power and choose **NFC Base App** or **Vivo Wallet** in Resolver. Uninstalling OriginOS Wallet Bridge also removes its handler and returns each action to its Vivo handler when it is the remaining handler.

## Lock Screen Limitation

This app does not bypass Keyguard or request lock-screen privileges. Whether Google Wallet is visible before unlock is controlled by OriginOS and Google Wallet. Test lock-screen behavior on your device.

## Build From Source

```bash
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

The release build enables R8 and resource shrinking. Without signing environment variables it produces an unsigned APK. To create an installable signed release, set `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`.

## Privacy

The app requests no runtime permissions. It does not read screen content, NFC payment data, cards, accounts, or banking-app data. It stores only a monotonic launch timestamp to rate-limit shortcut abuse. Any app can send a supported action, but the app ignores incoming extras and starts only the fixed Google Wallet Activity.

## Disclaimer

This is an independent project. It is not affiliated with, endorsed by, or supported by Vivo, iQOO, Google, or Google Wallet. Use at your own risk and test before relying on it.

## Author And License

Author: Đinh Quốc Bảo Khang.

Licensed under the [MIT License](LICENSE).
