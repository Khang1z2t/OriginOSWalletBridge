package com.originos.walletbridge

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log

class IntentTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.action !in VIVO_SWIPE_ACTIONS) {
            finish()
            return
        }
        if (!recordLaunch()) {
            Log.w(TAG, "Wallet launch rate limited")
            finish()
            return
        }

        val walletIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(GOOGLE_WALLET_PACKAGE, GOOGLE_WALLET_ACTIVITY))
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )

        try {
            if (walletIntent.resolveActivity(packageManager) != null) {
                startActivity(walletIntent)
            } else {
                Log.w(TAG, "Google Wallet activity unavailable")
            }
        } catch (error: ActivityNotFoundException) {
            Log.w(TAG, "Google Wallet activity missing", error)
        } catch (error: SecurityException) {
            Log.w(TAG, "Google Wallet launch blocked", error)
        } finally {
            finish()
        }
    }

    private companion object {
        const val LAUNCH_STATE = "launch_state"
        const val LAST_LAUNCH_ELAPSED = "last_launch_elapsed"
        const val MIN_LAUNCH_INTERVAL_MS = 750L
        val VIVO_SWIPE_ACTIONS = setOf(
            "vivo.intent.action.NFC_BASEAPP_SWIPE_ACTIVITY",
            "vivo.intent.action.NFC_BUS_SWINGSPLASH",
        )
        const val GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"
        const val GOOGLE_WALLET_ACTIVITY = "com.google.commerce.tapandpay.android.wallet.WalletActivity"
        const val TAG = "OriginOSWalletBridge"
    }

    private fun recordLaunch(): Boolean {
        val now = SystemClock.elapsedRealtime()
        val preferences = getSharedPreferences(LAUNCH_STATE, MODE_PRIVATE)
        val last = preferences.getLong(LAST_LAUNCH_ELAPSED, 0)
        if (now >= last && now - last < MIN_LAUNCH_INTERVAL_MS) return false
        preferences.edit().putLong(LAST_LAUNCH_ELAPSED, now).apply()
        return true
    }
}
