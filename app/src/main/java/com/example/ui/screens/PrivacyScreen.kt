package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.automation.PhoneActionExecutor
import com.example.data.local.AuditLogEntity
import com.example.data.local.JarvisSettings
import com.example.security.AppCheckStatus
import com.example.ui.components.StatusMicroChip
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PrivacyScreen(
    settings: JarvisSettings,
    isAccessibilityConnected: Boolean,
    auditLogs: List<AuditLogEntity>,
    onToggleAutoSpeak: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleConfirmationGuard: (Boolean) -> Unit,
    onClearAuditLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var permissionRefreshTrigger by remember { mutableIntStateOf(0) }

    val singlePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        permissionRefreshTrigger++
    }

    val micGranted = remember(permissionRefreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val callGranted = remember(permissionRefreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
    }
    val cameraGranted = remember(permissionRefreshTrigger) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    val notifGranted = remember(permissionRefreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Security & App Check Banner
        Surface(
            color = CyberNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, ArcCyan.copy(alpha = 0.65f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = ArcCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Zero-Key Gemini Security", style = MaterialTheme.typography.titleMedium, color = IceBlueText)
                    }
                    StatusMicroChip(
                        label = if (AppCheckStatus.isConfigured) "APP CHECK ON" else "PENDING",
                        color = ActiveEmerald
                    )
                }
                Text(
                    text = "Provider: Firebase AI Logic (${AppCheckStatus.modeLabel}). No API keys are embedded in the APK. Photos use the zero-permission Android Photo Picker.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSteelText
                )
            }
        }

        // Permissions Matrix
        Surface(
            color = CyberNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyberGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Permissions Dashboard",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArcCyan
                )
                PermissionStatusRow(
                    title = "Microphone (Hindi + English Voice)",
                    granted = micGranted,
                    actionLabel = if (micGranted) "Granted" else "Grant Mic",
                    onAction = {
                        if (micGranted) PhoneActionExecutor.openAppSettings(context)
                        else singlePermLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
                PermissionStatusRow(
                    title = "Camera & Torch Control",
                    granted = cameraGranted,
                    actionLabel = if (cameraGranted) "Granted" else "Grant Camera",
                    onAction = {
                        if (cameraGranted) PhoneActionExecutor.openAppSettings(context)
                        else singlePermLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
                PermissionStatusRow(
                    title = "Direct Calling (Optional — Dialer works without)",
                    granted = callGranted,
                    actionLabel = if (callGranted) "Granted" else "Grant Call",
                    onAction = {
                        if (callGranted) PhoneActionExecutor.openAppSettings(context)
                        else singlePermLauncher.launch(Manifest.permission.CALL_PHONE)
                    }
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionStatusRow(
                        title = "Notifications",
                        granted = notifGranted,
                        actionLabel = if (notifGranted) "Granted" else "Grant",
                        onAction = {
                            if (notifGranted) PhoneActionExecutor.openAppSettings(context)
                            else singlePermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
                PermissionStatusRow(
                    title = "Accessibility Screen Service",
                    granted = isAccessibilityConnected,
                    actionLabel = if (isAccessibilityConnected) "Active" else "Open Settings",
                    onAction = { PhoneActionExecutor.openAccessibilitySettings(context) }
                )
            }
        }

        // Privacy & Voice Guardrails
        Surface(
            color = CyberNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyberGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Privacy & Safety Controls",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArcCyan
                )
                PrivacySwitchRow(
                    title = "Require Confirmation (Calls & WhatsApp)",
                    checked = settings.confirmationGuardEnabled,
                    onCheckedChange = onToggleConfirmationGuard
                )
                PrivacySwitchRow(
                    title = "Auto-Speak Gemini Responses (TTS)",
                    checked = settings.autoSpeakEnabled,
                    onCheckedChange = onToggleAutoSpeak
                )
                PrivacySwitchRow(
                    title = "\"Hey JARVIS\" Active Wake Listener",
                    checked = settings.wakeWordEnabled,
                    onCheckedChange = onToggleWakeWord
                )
            }
        }

        // Audit Log
        Surface(
            color = CyberNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyberGlassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Security, contentDescription = null, tint = ArcCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Action Audit Log", style = MaterialTheme.typography.titleMedium, color = IceBlueText)
                    }
                    TextButton(onClick = onClearAuditLogs) {
                        Text("Clear")
                    }
                }
                if (auditLogs.isEmpty()) {
                    Text(
                        text = "No actions logged yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedSteelText
                    )
                } else {
                    val formatter = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
                    auditLogs.take(15).forEach { log ->
                        Surface(
                            color = CyberSlate,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.title,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (log.status == "EMERGENCY_STOP") EmergencyCrimson else IceBlueText
                                    )
                                    Text(
                                        text = formatter.format(Date(log.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MutedSteelText
                                    )
                                }
                                Text(
                                    text = log.detail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedSteelText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (granted) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                contentDescription = null,
                tint = if (granted) ActiveEmerald else AmberWarning
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = IceBlueText
            )
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onAction,
            modifier = Modifier.height(44.dp)
        ) {
            Text(actionLabel, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("switch_$title"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = IceBlueText,
            modifier = Modifier.weight(1f)
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
