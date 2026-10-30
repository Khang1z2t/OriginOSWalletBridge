# OriginOS Wallet Bridge V2

V2 receives the OriginOS implicit action `vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY`, starts the verified Google Wallet activity, then finishes. It has no AccessibilityService, NFC routing, Wallet Role changes, background service, Shizuku, polling, or WakeLock.

`applicationId` and handler component remain `com.originos.walletbridge/.IntentTestActivity` so an already saved Android default handler remains valid across an in-place update.

## Test

Install only after approval. Test unlocked, lock screen, USB/Wireless Debugging off, Recent Apps removal, and reboot separately. Do not expect a resolver at lock screen. Google Wallet may still reject a foreground launch before unlock; V2 does not bypass Keyguard.

```powershell
adb shell cmd package resolve-activity --brief -a vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY -c android.intent.category.DEFAULT
```

Rollback: clear Wallet Bridge defaults in Android App Info, choose Vivo in Resolver, or uninstall V2. The original Accessibility V1 is preserved by Git tag `v0.1` and branch `main`.

## Test

```powershell
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb logcat -s OriginOSWalletBridge
```

Unlocked test: enable service, choose Test Mode, then double-press power. Confirm `Vivo Wallet matched`. Switch to Bridge Mode and repeat; check `Google Wallet launch requested` and `Google Wallet foreground confirmed`.

Lock-screen test: wake and unlock first. This build logs `user locked` and skips app launch until Android reports user unlocked.
