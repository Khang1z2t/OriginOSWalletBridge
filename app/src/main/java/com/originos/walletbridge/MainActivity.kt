package com.originos.walletbridge

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private lateinit var accessibilityManager: AccessibilityManager
    private var refreshToken by mutableIntStateOf(0)
    private val accessibilityListener = AccessibilityManager.AccessibilityStateChangeListener {
        refreshToken++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        accessibilityManager = getSystemService(AccessibilityManager::class.java)
        setContent {
            WalletBridgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WalletBridgeApp(
                        serviceEnabled = { isAccessibilityServiceEnabled() },
                        openAccessibilitySettings = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                        openBatterySettings = { startActivity(appBatterySettingsIntent(this)) },
                        openAppInfo = { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) },
                        refreshToken = refreshToken,
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accessibilityManager.addAccessibilityStateChangeListener(accessibilityListener)
        refreshToken++
    }

    override fun onPause() {
        accessibilityManager.removeAccessibilityStateChangeListener(accessibilityListener)
        super.onPause()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getSystemService(AccessibilityManager::class.java)
        val component = ComponentName(this, WalletBridgeAccessibilityService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any { info ->
            info.resolveInfo.serviceInfo.run { packageName == component.packageName && name == component.className }
        }
    }
}

@Composable
private fun WalletBridgeApp(
    serviceEnabled: () -> Boolean,
    openAccessibilitySettings: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppInfo: () -> Unit,
    refreshToken: Int,
) {
    val context = LocalContext.current
    val enabled = serviceEnabled()
    var snapshot by remember(refreshToken) { mutableStateOf(BridgeStore.snapshot(context)) }
    val reliability = remember(refreshToken, snapshot, enabled) {
        readReliabilityState(context, enabled, snapshot.serviceConnected)
    }

    when {
        !enabled -> OnboardingScreen(openAccessibilitySettings)
        else -> Dashboard(
            snapshot = snapshot,
            reliability = reliability,
            openAccessibilitySettings = openAccessibilitySettings,
            openBatterySettings = openBatterySettings,
            openAppInfo = openAppInfo,
            onModeChanged = {
                BridgeStore.setBridgeMode(context, it)
                snapshot = BridgeStore.snapshot(context)
            },
            onDiagnosticsChanged = {
                BridgeStore.setDiagnosticsEnabled(context, it)
                snapshot = BridgeStore.snapshot(context)
            },
        )
    }
}

@Composable
private fun LoadingScreen() = Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    CircularProgressIndicator()
    Spacer(Modifier.height(14.dp))
    Text("Checking Accessibility Service")
}

@Composable
private fun OnboardingScreen(openSettings: () -> Unit) = Column(
    modifier = Modifier.fillMaxSize().padding(28.dp),
    verticalArrangement = Arrangement.Center,
) {
    Text("OriginOS\nWallet Bridge", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(20.dp))
    Text("Accessibility lets this app detect one Vivo Wallet window and optionally open Google Wallet. It does not read screen content or press buttons.")
    Spacer(Modifier.height(24.dp))
    OutlinedButton(onClick = openSettings, modifier = Modifier.fillMaxWidth()) { Text("Open Accessibility Settings") }
    Spacer(Modifier.height(12.dp))
    Text("Enable OriginOS Wallet Bridge, then return here.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun ErrorScreen(message: String, openSettings: () -> Unit) = Column(
    modifier = Modifier.fillMaxSize().padding(28.dp),
    verticalArrangement = Arrangement.Center,
) {
    Text("State unavailable", style = MaterialTheme.typography.headlineSmall)
    Spacer(Modifier.height(8.dp))
    Text(message)
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = openSettings) { Text("Open Accessibility Settings") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dashboard(
    snapshot: BridgeSnapshot,
    reliability: ReliabilityState,
    openAccessibilitySettings: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppInfo: () -> Unit,
    onModeChanged: (Boolean) -> Unit,
    onDiagnosticsChanged: (Boolean) -> Unit,
) = Column(Modifier.fillMaxSize()) {
    val context = LocalContext.current
    TopAppBar(
        title = { Text("Wallet Bridge", fontWeight = FontWeight.Bold) },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { StatusCard(snapshot.bridgeMode, reliability.accessibility) }
        if (!snapshot.serviceConnected) {
            item { ServiceStoppedWarning() }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !snapshot.bridgeMode, onClick = { onModeChanged(false) }, label = { Text("Test Mode") })
                FilterChip(selected = snapshot.bridgeMode, onClick = { onModeChanged(true) }, label = { Text("Bridge Mode") })
            }
        }
        item {
            FilterChip(
                selected = snapshot.diagnosticsEnabled,
                onClick = { onDiagnosticsChanged(!snapshot.diagnosticsEnabled) },
                label = { Text(if (snapshot.diagnosticsEnabled) "Diagnostic: ON" else "Diagnostic: OFF") },
            )
        }
        item { MetricsCard(snapshot) }
        item {
            ReliabilityCard(
                state = reliability,
                reminderDismissed = snapshot.reliabilityReminderDismissed,
                openAccessibilitySettings = openAccessibilitySettings,
                openBatterySettings = openBatterySettings,
                openAppInfo = openAppInfo,
                onDismissReminder = {
                    BridgeStore.dismissReliabilityReminder(context)
                },
            )
        }
        item { Text("Activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
        if (snapshot.history.isEmpty()) item { EmptyHistory() }
        // History messages may repeat; LazyColumn keys must stay unique.
        else items(snapshot.history) { HistoryRow(it) }
        if (snapshot.diagnosticsEnabled) {
            item { Text("Diagnostic events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold) }
            if (snapshot.diagnostics.isEmpty()) item { EmptyDiagnostics() }
            else items(snapshot.diagnostics) { HistoryRow(it) }
        }
    }
}

@Composable
private fun StatusCard(bridgeMode: Boolean, accessibility: String) = Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    modifier = Modifier.fillMaxWidth(),
) {
    Column(Modifier.padding(18.dp)) {
        AssistChip(onClick = {}, label = { Text("Accessibility: $accessibility") })
        Spacer(Modifier.height(8.dp))
        Text(if (bridgeMode) "Google Wallet bridge armed" else "Event logging only", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(if (bridgeMode) "Exact Vivo Wallet matches request Google Wallet." else "Switch to Bridge Mode after confirming logs.")
    }
}

@Composable
private fun ServiceStoppedWarning() = Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    modifier = Modifier.fillMaxWidth(),
) {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("OriginOS đã dừng dịch vụ Wallet Bridge", fontWeight = FontWeight.Bold)
        Text("Accessibility vẫn được cấp quyền nhưng service chưa được bind. Mở Trợ năng, tắt/bật lại service nếu OriginOS không tự kết nối lại.")
    }
}

@Composable
private fun MetricsCard(snapshot: BridgeSnapshot) = Card(modifier = Modifier.fillMaxWidth()) {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Metric("Vivo events detected", snapshot.matchedEvents.toString())
        HorizontalDivider()
        Metric("Google Wallet launch requests", snapshot.launchAttempts.toString())
        HorizontalDivider()
        Metric("Wallet foreground confirmed", snapshot.confirmedWalletForegrounds.toString())
        HorizontalDivider()
        Metric("Foreground unconfirmed", snapshot.unconfirmedLaunches.toString())
        HorizontalDivider()
        Metric("Launch blocked", snapshot.blockedLaunches.toString())
        snapshot.averageLaunchLatencyMs?.let {
            HorizontalDivider()
            Metric("Average launch latency", "$it ms")
        }
        if (snapshot.launchAttempts > 0) {
            HorizontalDivider()
            Metric("Foreground confirmation rate", "${snapshot.confirmedWalletForegrounds * 100 / snapshot.launchAttempts}%")
        }
    }
}

@Composable
private fun ReliabilityCard(
    state: ReliabilityState,
    reminderDismissed: Boolean,
    openAccessibilitySettings: () -> Unit,
    openBatterySettings: () -> Unit,
    openAppInfo: () -> Unit,
    onDismissReminder: () -> Unit,
) = Card(modifier = Modifier.fillMaxWidth()) {
    var dismissed by remember(reminderDismissed) { mutableStateOf(reminderDismissed) }
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Background Reliability", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Metric("Accessibility", state.accessibility)
        Metric("Battery optimization", state.battery)
        Metric("Autostart", state.autostart)
        Metric("Background activity", state.backgroundActivity)
        Metric("Lock in Recent Apps", state.recentAppsLock)
        if (state.batterySaverOn) Text("Battery Saver đang bật; OriginOS có thể trì hoãn launch.", style = MaterialTheme.typography.bodySmall)
        if (state.needsGuidance && !dismissed) {
            Text("OriginOS Background Protection: kiểm tra thủ công Autostart, Background activity, Battery Settings và Lock app trong Recent Apps. Android không có API công khai để xác minh các mục OEM này.", style = MaterialTheme.typography.bodySmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = openAccessibilitySettings) { Text("Trợ năng") }
            OutlinedButton(onClick = openBatterySettings) { Text("Pin") }
            OutlinedButton(onClick = openAppInfo) { Text("App info") }
            if (state.needsGuidance && !dismissed) {
                OutlinedButton(onClick = {
                    dismissed = true
                    onDismissReminder()
                }) { Text("Đã hiểu") }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, style = MaterialTheme.typography.bodyMedium)
    Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyHistory() = Card(modifier = Modifier.fillMaxWidth()) {
    Text("No matching Vivo Wallet event yet.", modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun EmptyDiagnostics() = Card(modifier = Modifier.fillMaxWidth()) {
    Text("No diagnostic event yet.", modifier = Modifier.padding(18.dp), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun HistoryRow(entry: String) = Card(modifier = Modifier.fillMaxWidth()) {
    Text(entry, modifier = Modifier.padding(14.dp), maxLines = 3, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
}
