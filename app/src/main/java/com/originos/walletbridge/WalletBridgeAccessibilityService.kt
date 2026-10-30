package com.originos.walletbridge

import android.accessibilityservice.AccessibilityService
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.os.SystemClock
import android.os.UserManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class WalletBridgeAccessibilityService : AccessibilityService() {
    private var lastMatchedAt = 0L
    private var lastLaunchRequestedAt = 0L
    private var awaitingWalletForegroundUntil = 0L
    private var lastLaunchStartedAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        BridgeStore.setServiceConnected(this, true)
        BridgeStore.expirePendingLaunch(this)
        BridgeStore.diagnostic(this, "SVC_CONNECTED", "bridge=${BridgeStore.snapshot(this).bridgeMode}")
        debugLog("onServiceConnected; bridge=${BridgeStore.snapshot(this).bridgeMode}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName ?: return
        val now = SystemClock.elapsedRealtime()

        BridgeStore.expirePendingLaunch(this)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            packageName.contentEquals(GOOGLE_WALLET_PACKAGE)) {
            val latency = BridgeStore.recordConfirmedForeground(this, System.currentTimeMillis()) ?: return
            awaitingWalletForegroundUntil = 0L
            BridgeStore.append(this, "Google Wallet foreground confirmed in ${latency}ms")
            BridgeStore.diagnostic(this, "WALLET_FOREGROUND", "latency=${latency}ms")
            debugLog("Google Wallet foreground confirmed in ${latency}ms")
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            !packageName.contentEquals(VIVO_PACKAGE)) return
        val className = event.className ?: return
        if (!className.contentEquals(VIVO_ACTIVITY)) return

        if (now - lastMatchedAt < DUPLICATE_WINDOW_MS) {
            BridgeStore.diagnostic(this, "SKIP_DUPLICATE")
            return
        }
        lastMatchedAt = now
        BridgeStore.recordMatchedEvent(this)
        BridgeStore.diagnostic(this, "VIVO_MATCH", "activity=$VIVO_ACTIVITY")

        if (!BridgeStore.snapshot(this).bridgeMode) {
            BridgeStore.append(this, "Test Mode: Vivo Wallet matched")
            BridgeStore.diagnostic(this, "SKIP_TEST_MODE")
            return
        }
        if (!getSystemService(UserManager::class.java).isUserUnlocked) {
            BridgeStore.append(this, "Launch skipped: device locked")
            BridgeStore.diagnostic(this, "SKIP_USER_LOCKED")
            return
        }
        if (now - lastLaunchRequestedAt < COOLDOWN_MS) {
            BridgeStore.diagnostic(this, "SKIP_COOLDOWN")
            return
        }

        lastLaunchRequestedAt = now
        launchGoogleWallet()
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        BridgeStore.setServiceConnected(this, false)
        BridgeStore.append(this, "Accessibility onUnbind")
        BridgeStore.diagnostic(this, "SVC_UNBOUND")
        debugLog("onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        BridgeStore.setServiceConnected(this, false)
        BridgeStore.append(this, "Accessibility onDestroy")
        BridgeStore.diagnostic(this, "SVC_DESTROYED")
        debugLog("onDestroy")
        super.onDestroy()
    }

    private fun launchGoogleWallet() {
        val requestedAt = System.currentTimeMillis()
        try {
            BridgeStore.recordLaunchAttempt(this, requestedAt)
            BridgeStore.diagnostic(this, "LAUNCH_REQUESTED")
            // Direct component avoids package-manager resolution after Vivo Wallet is foregrounded.
            startActivity(Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(ComponentName(GOOGLE_WALLET_PACKAGE, GOOGLE_WALLET_ACTIVITY))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            lastLaunchStartedAt = SystemClock.elapsedRealtime()
            awaitingWalletForegroundUntil = lastLaunchStartedAt + FOREGROUND_CONFIRMATION_MS
            BridgeStore.append(this, "Google Wallet launch requested")
            BridgeStore.diagnostic(this, "LAUNCH_API_OK")
            debugLog("Google Wallet launch requested")
        } catch (error: ActivityNotFoundException) {
            BridgeStore.recordLaunchBlocked(this, "Google Wallet missing")
            BridgeStore.diagnostic(this, "LAUNCH_MISSING")
        } catch (error: SecurityException) {
            BridgeStore.recordLaunchBlocked(this, "SecurityException")
            BridgeStore.diagnostic(this, "LAUNCH_SECURITY")
        } catch (error: RuntimeException) {
            BridgeStore.recordLaunchBlocked(this, error.javaClass.simpleName)
            BridgeStore.diagnostic(this, "LAUNCH_RUNTIME", error.javaClass.simpleName)
        }
    }

    private fun debugLog(message: String) {
        if (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) Log.d(TAG, message)
    }

    companion object {
        private const val TAG = "OriginOSWalletBridge"
        private const val VIVO_PACKAGE = "com.vivo.nfcbaseapp"
        private const val VIVO_ACTIVITY = "com.vivo.pay.swipe.activity.NewSwipeActivity"
        private const val GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"
        private const val GOOGLE_WALLET_ACTIVITY = "com.google.commerce.tapandpay.android.wallet.WalletActivity"
        private const val DUPLICATE_WINDOW_MS = 180L
        private const val COOLDOWN_MS = 900L
        private const val FOREGROUND_CONFIRMATION_MS = 6_000L
    }
}
