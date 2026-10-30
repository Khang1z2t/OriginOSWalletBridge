# OriginOS Wallet Bridge

No-root bridge for iQOO 15T OriginOS 7. It observes only verified Vivo Wallet Activity and optionally launches Google Wallet.

## Use

1. Install debug APK and open app.
2. Enable `OriginOS Wallet Bridge` in Accessibility Settings. App never enables it itself.
3. Test Mode logs exact Vivo Wallet matches only.
4. Bridge Mode requests Google Wallet after exact match.

Dashboard metrics persist across app restarts. `Wallet foreground confirmed` increments only after service observes Google Wallet package within six seconds of launch request.

`Foreground unconfirmed` means no Google Wallet foreground event was observed within six seconds. It is not proof that launch failed. `Launch blocked` is only counted when `startActivity()` throws. User-observed success and visible flicker need a manual test; this app does not infer either.

## OriginOS background checks

App can verify Accessibility state, battery optimization, and Battery Saver. Check these OriginOS controls manually: Autostart, Background activity, and lock app in Recents when available. App never requests a WakeLock, starts a foreground service, or tries to enable Accessibility itself.

When Accessibility is enabled but service is not bound, dashboard shows `OriginOS đã dừng dịch vụ Wallet Bridge` rather than asking for permission again. This can happen after OriginOS `single-cleaner` kills its UID from Recent Apps. No public Android API can read OriginOS Autostart, Background activity, or Recent Apps lock state; those are manual checks.

## Recents lifecycle test

1. Enable Accessibility manually and set Bridge Mode.
2. Capture baseline with `adb shell settings --user 0 get secure enabled_accessibility_services`, `adb shell dumpsys accessibility`, `adb shell pidof com.originos.walletbridge`, and `adb shell dumpsys activity services com.originos.walletbridge`.
3. Swipe app from Recents. Run same commands again, then double-press power once.
4. In app history, compare `onUnbind`, `onDestroy`, `Accessibility connected`, Vivo match, launch request, and foreground confirmation entries.

If `enabled_accessibility_services` becomes `null` or does not contain this component, OriginOS disabled permission. If it remains present but Bound services is empty, service was disconnected. If it remains bound but no Vivo match is logged, event delivery failed. If a match and launch request are logged but no confirmation arrives, it is an unconfirmed launch, not automatically a launch failure.

## Test

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -s OriginOSWalletBridge
```

Unlocked test: enable service, choose Test Mode, then double-press power. Confirm `Vivo Wallet matched`. Switch to Bridge Mode and repeat; check `Google Wallet launch requested` and `Google Wallet foreground confirmed`.

Lock-screen test: wake and unlock first. This build logs `user locked` and skips app launch until Android reports user unlocked.
