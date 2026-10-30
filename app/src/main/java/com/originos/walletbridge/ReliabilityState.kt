package com.originos.walletbridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings

data class ReliabilityState(
    val accessibility: String,
    val battery: String,
    val batterySaverOn: Boolean,
    val autostart: String,
    val backgroundActivity: String,
    val recentAppsLock: String,
    val needsGuidance: Boolean,
)

fun readReliabilityState(context: Context, accessibilityEnabled: Boolean, serviceConnected: Boolean): ReliabilityState {
    val powerManager = context.getSystemService(PowerManager::class.java)
    val unrestricted = powerManager.isIgnoringBatteryOptimizations(context.packageName)
    val accessibility = when {
        !accessibilityEnabled -> "Disabled"
        serviceConnected -> "Connected"
        else -> "Enabled / disconnected"
    }
    return ReliabilityState(
        accessibility = accessibility,
        battery = if (unrestricted) "Unrestricted" else "Optimized",
        batterySaverOn = powerManager.isPowerSaveMode,
        autostart = "Manual verification required",
        backgroundActivity = "Manual verification required",
        recentAppsLock = "Manual verification required",
        needsGuidance = !accessibilityEnabled || !unrestricted,
    )
}

fun appBatterySettingsIntent(context: Context): Intent {
    val intent = Intent("android.settings.APP_BATTERY_SETTINGS")
        .setData(Uri.parse("package:${context.packageName}"))
    return if (intent.resolveActivity(context.packageManager) != null) intent
    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.parse("package:${context.packageName}"))
}
