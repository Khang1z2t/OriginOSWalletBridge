package com.originos.walletbridge

import android.content.Context
import android.app.KeyguardManager
import android.os.PowerManager
import android.os.Process
import org.json.JSONArray
import org.json.JSONException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BridgeSnapshot(
    val bridgeMode: Boolean,
    val serviceConnected: Boolean,
    val matchedEvents: Int,
    val launchAttempts: Int,
    val confirmedWalletForegrounds: Int,
    val unconfirmedLaunches: Int,
    val blockedLaunches: Int,
    val averageLaunchLatencyMs: Long?,
    val reliabilityReminderDismissed: Boolean,
    val diagnosticsEnabled: Boolean,
    val diagnostics: List<String>,
    val history: List<String>,
)

object BridgeStore {
    private const val PREFS = "bridge_settings"
    private const val MODE = "bridge_mode"
    private const val SERVICE_CONNECTED = "service_connected"
    private const val SERVICE_PROCESS_ID = "service_process_id"
    private const val MATCHED_EVENTS = "matched_events"
    private const val LAUNCH_ATTEMPTS = "launch_attempts"
    private const val CONFIRMED_FOREGROUNDS = "confirmed_foregrounds"
    private const val UNCONFIRMED_LAUNCHES = "unconfirmed_launches"
    private const val BLOCKED_LAUNCHES = "blocked_launches"
    private const val TOTAL_CONFIRMED_LATENCY = "total_confirmed_latency"
    private const val PENDING_LAUNCH_AT = "pending_launch_at"
    private const val HISTORY = "history"
    private const val RELIABILITY_REMINDER_DISMISSED = "reliability_reminder_dismissed"
    private const val DIAGNOSTICS_ENABLED = "diagnostics_enabled"
    private const val DIAGNOSTICS = "diagnostics"
    private const val MAX_HISTORY = 24
    private const val MAX_DIAGNOSTICS = 80

    fun snapshot(context: Context): BridgeSnapshot {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        expirePendingLaunch(context, prefs)
        return BridgeSnapshot(
            bridgeMode = prefs.getBoolean(MODE, false),
            // AccessibilityService and UI share this process. A different PID after OEM cleanup
            // means saved "connected" state is stale, not evidence that service remains bound.
            serviceConnected = prefs.getBoolean(SERVICE_CONNECTED, false) &&
                prefs.getInt(SERVICE_PROCESS_ID, -1) == Process.myPid(),
            matchedEvents = prefs.getInt(MATCHED_EVENTS, 0),
            launchAttempts = prefs.getInt(LAUNCH_ATTEMPTS, 0),
            confirmedWalletForegrounds = prefs.getInt(CONFIRMED_FOREGROUNDS, 0),
            unconfirmedLaunches = prefs.getInt(UNCONFIRMED_LAUNCHES, 0),
            blockedLaunches = prefs.getInt(BLOCKED_LAUNCHES, 0),
            averageLaunchLatencyMs = averageLatency(prefs),
            reliabilityReminderDismissed = prefs.getBoolean(RELIABILITY_REMINDER_DISMISSED, false),
            diagnosticsEnabled = prefs.getBoolean(DIAGNOSTICS_ENABLED, false),
            diagnostics = readHistory(prefs.getString(DIAGNOSTICS, "[]").orEmpty()),
            history = readHistory(prefs.getString(HISTORY, "[]").orEmpty()),
        )
    }

    fun setBridgeMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(MODE, enabled).apply()
        append(context, "Mode: ${if (enabled) "Bridge" else "Test"}")
    }

    fun setServiceConnected(context: Context, connected: Boolean) {
        val edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(SERVICE_CONNECTED, connected)
        if (connected) edit.putInt(SERVICE_PROCESS_ID, Process.myPid())
        else edit.remove(SERVICE_PROCESS_ID)
        edit.apply()
        append(context, if (connected) "Accessibility connected" else "Accessibility disconnected")
    }

    fun dismissReliabilityReminder(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(RELIABILITY_REMINDER_DISMISSED, true)
            .apply()
    }

    fun setDiagnosticsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(DIAGNOSTICS_ENABLED, enabled)
            .apply()
        if (enabled) diagnostic(context, "SESSION_STARTED")
        else append(context, "Diagnostic session stopped")
    }

    fun diagnostic(context: Context, code: String, detail: String = "") {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(DIAGNOSTICS_ENABLED, false)) return
        val powerManager = context.getSystemService(PowerManager::class.java)
        val keyguardManager = context.getSystemService(KeyguardManager::class.java)
        val state = "screen=${if (powerManager.isInteractive) "ON" else "OFF"}, keyguard=${if (keyguardManager.isKeyguardLocked) "LOCKED" else "UNLOCKED"}"
        val events = readHistory(prefs.getString(DIAGNOSTICS, "[]").orEmpty()).toMutableList()
        events.add(0, "${timestamp()} | $code | $state${if (detail.isBlank()) "" else " | $detail"}")
        if (events.size > MAX_DIAGNOSTICS) events.subList(MAX_DIAGNOSTICS, events.size).clear()
        prefs.edit().putString(DIAGNOSTICS, JSONArray(events).toString()).apply()
    }

    fun recordMatchedEvent(context: Context) = increment(context, MATCHED_EVENTS)

    fun recordLaunchAttempt(context: Context, requestedAt: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(LAUNCH_ATTEMPTS, prefs.getInt(LAUNCH_ATTEMPTS, 0) + 1)
            .putLong(PENDING_LAUNCH_AT, requestedAt)
            .apply()
    }

    fun recordConfirmedForeground(context: Context, foregroundAt: Long): Long? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val requestedAt = prefs.getLong(PENDING_LAUNCH_AT, 0)
        if (requestedAt == 0L || foregroundAt - requestedAt !in 0..FOREGROUND_CONFIRMATION_MS) return null
        val latencyMs = foregroundAt - requestedAt
        prefs.edit()
            .putInt(CONFIRMED_FOREGROUNDS, prefs.getInt(CONFIRMED_FOREGROUNDS, 0) + 1)
            .putLong(TOTAL_CONFIRMED_LATENCY, prefs.getLong(TOTAL_CONFIRMED_LATENCY, 0) + latencyMs)
            .remove(PENDING_LAUNCH_AT)
            .apply()
        return latencyMs
    }

    fun recordLaunchBlocked(context: Context, reason: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(BLOCKED_LAUNCHES, prefs.getInt(BLOCKED_LAUNCHES, 0) + 1)
            .remove(PENDING_LAUNCH_AT)
            .apply()
        append(context, "Launch blocked: $reason")
    }

    fun expirePendingLaunch(context: Context) {
        expirePendingLaunch(context, context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))
    }

    fun append(context: Context, message: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val history = readHistory(prefs.getString(HISTORY, "[]").orEmpty()).toMutableList()
        history.add(0, "${timestamp()} | $message")
        if (history.size > MAX_HISTORY) history.subList(MAX_HISTORY, history.size).clear()
        prefs.edit().putString(HISTORY, JSONArray(history).toString()).apply()
    }

    private fun increment(context: Context, counter: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putInt(counter, prefs.getInt(counter, 0) + 1).apply()
    }

    private fun expirePendingLaunch(context: Context, prefs: android.content.SharedPreferences) {
        val requestedAt = prefs.getLong(PENDING_LAUNCH_AT, 0)
        if (requestedAt == 0L || System.currentTimeMillis() - requestedAt <= FOREGROUND_CONFIRMATION_MS) return
        prefs.edit()
            .putInt(UNCONFIRMED_LAUNCHES, prefs.getInt(UNCONFIRMED_LAUNCHES, 0) + 1)
            .remove(PENDING_LAUNCH_AT)
            .apply()
        append(context, "Wallet foreground unconfirmed after 6s")
        diagnostic(context, "WALLET_FOREGROUND_TIMEOUT")
    }

    private fun averageLatency(prefs: android.content.SharedPreferences): Long? {
        val confirmed = prefs.getInt(CONFIRMED_FOREGROUNDS, 0)
        return if (confirmed == 0) null else prefs.getLong(TOTAL_CONFIRMED_LATENCY, 0) / confirmed
    }

    private fun readHistory(raw: String): List<String> = try {
        val array = JSONArray(raw)
        List(array.length()) { array.getString(it) }
    } catch (_: JSONException) {
        emptyList()
    }

    private fun timestamp(): String = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

    private const val FOREGROUND_CONFIRMATION_MS = 6_000L
}
