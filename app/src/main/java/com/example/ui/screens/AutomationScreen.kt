package com.example.ui.screens

import android.accessibilityservice.AccessibilityService
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.automation.PhoneActionExecutor
import com.example.automation.ScrollDirection
import com.example.ui.components.StatusMicroChip
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText

@Composable
fun AutomationScreen(
    isAccessibilityConnected: Boolean,
    activeForegroundPackage: String,
    isAutoScrolling: Boolean,
    automationStatus: String,
    onScrollDirection: (ScrollDirection) -> Unit,
    onStartAutoScroll: () -> Unit,
    onStopAutoScroll: () -> Unit,
    onGlobalNav: (Int) -> Unit,
    onOpenVoiceTyping: () -> Unit,
    onOpenSocialAssist: (String, String) -> Unit,
    onAssistLike: () -> Unit,
    onAssistComment: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var commentDraft by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Accessibility Service Status Card
        Surface(
            color = CyberNavy,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(
                1.dp,
                if (isAccessibilityConnected) ActiveEmerald.copy(alpha = 0.7f) else AmberWarning.copy(alpha = 0.7f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AccessibilityNew,
                            contentDescription = null,
                            tint = if (isAccessibilityConnected) ActiveEmerald else AmberWarning
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Screen Automation Engine",
                            style = MaterialTheme.typography.titleMedium,
                            color = IceBlueText
                        )
                    }
                    StatusMicroChip(
                        label = if (isAccessibilityConnected) "ACTIVE" else "SETUP NEEDED",
                        color = if (isAccessibilityConnected) ActiveEmerald else AmberWarning
                    )
                }

                Text(
                    text = "Status: $automationStatus\nForeground App: $activeForegroundPackage",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedSteelText
                )

                OutlinedButton(
                    onClick = { PhoneActionExecutor.openAccessibilitySettings(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_accessibility_settings_button")
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (isAccessibilityConnected) "Manage Accessibility Service"
                        else "Enable JARVIS VIP Accessibility Service"
                    )
                }
            }
        }

        // Scrolling & Navigation Deck
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
                    text = "Auto-Scroll & Screen Navigation",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArcCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onScrollDirection(ScrollDirection.UP) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scroll Up")
                    }
                    OutlinedButton(
                        onClick = { onScrollDirection(ScrollDirection.DOWN) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Scroll Down")
                    }
                }

                Button(
                    onClick = { if (isAutoScrolling) onStopAutoScroll() else onStartAutoScroll() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAutoScrolling) EmergencyCrimson else ArcCyan,
                        contentColor = if (isAutoScrolling) IceBlueText else AbyssalVoid
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("auto_scroll_toggle_button")
                ) {
                    Icon(
                        if (isAutoScrolling) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isAutoScrolling) "Stop Hands-Free Auto-Scroll" else "Start Hands-Free Auto-Scroll")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onGlobalNav(AccessibilityService.GLOBAL_ACTION_BACK) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = { onGlobalNav(AccessibilityService.GLOBAL_ACTION_HOME) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Home, contentDescription = "Home", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = { onGlobalNav(AccessibilityService.GLOBAL_ACTION_RECENTS) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Apps, contentDescription = "Recents", modifier = Modifier.size(18.dp))
                    }
                    OutlinedButton(
                        onClick = { onGlobalNav(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Notifications", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Voice Typing & Social Media Assist
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
                    text = "Voice Typing & Authorized Social Assist",
                    style = MaterialTheme.typography.titleMedium,
                    color = ArcCyan
                )
                Button(
                    onClick = onOpenVoiceTyping,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberSlate, contentColor = ArcCyan),
                    border = BorderStroke(1.dp, ArcCyan.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Filled.KeyboardVoice, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open Bilingual Voice Typing Pad")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onOpenSocialAssist("Instagram", "com.instagram.android") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Instagram")
                    }
                    OutlinedButton(
                        onClick = { onOpenSocialAssist("Facebook", "com.facebook.katana") },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Facebook")
                    }
                }

                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    label = { Text("Comment draft for active post") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAssistLike,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Assist Like")
                    }
                    OutlinedButton(
                        onClick = { onAssistComment(commentDraft) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Assist Comment")
                    }
                }
            }
        }
    }
}
