package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.local.ToolActionType
import com.example.ui.PendingSheetState
import com.example.ui.QueryPromptMode
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText

@Composable
fun PendingActionHost(
    sheet: PendingSheetState?,
    isAccessibilityConnected: Boolean,
    isTorchOn: Boolean,
    onDismiss: () -> Unit,
    onConfirmWhatsApp: (recipient: String, message: String, assistSend: Boolean) -> Unit,
    onConfirmCall: (phoneNumber: String) -> Unit,
    onUpdateSocial: ((PendingSheetState.SocialUploadConfirm) -> PendingSheetState.SocialUploadConfirm) -> Unit,
    onGenerateSocialCaption: () -> Unit,
    onConfirmSocialUpload: () -> Unit,
    onCreateCameraUri: () -> Uri,
    onAnalyzeCameraPhoto: (Uri, String) -> Unit,
    onLaunchSystemCamera: (Boolean) -> Unit,
    onToggleTorch: (Boolean) -> Unit,
    onSubmitQuery: (QueryPromptMode, String) -> Unit,
    onExecuteVoiceType: (String) -> Unit,
    onStartVoiceDictation: () -> Unit,
    onAddCustomTool: (String, String, String, ToolActionType, String, Boolean) -> Unit
) {
    when (sheet) {
        null -> Unit
        is PendingSheetState.WhatsAppConfirm -> {
            var recipient by remember(sheet) { mutableStateOf(sheet.recipientOrPhone) }
            var message by remember(sheet) { mutableStateOf(sheet.message) }
            var assistSend by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text("WhatsApp Confirmation", style = MaterialTheme.typography.titleLarge, color = ArcCyan)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Review & confirm before JARVIS opens WhatsApp (पुष्टि करें):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSteelText
                        )
                        OutlinedTextField(
                            value = recipient,
                            onValueChange = { recipient = it },
                            label = { Text("Phone (+91...) or Contact") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = message,
                            onValueChange = { message = it },
                            label = { Text("Message (Hindi / English)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (isAccessibilityConnected) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = assistSend, onCheckedChange = { assistSend = it })
                                Text(
                                    "Assist tapping Send via Accessibility",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IceBlueText
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onConfirmWhatsApp(recipient, message, assistSend) },
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveEmerald, contentColor = AbyssalVoid),
                        modifier = Modifier.testTag("confirm_whatsapp_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm & Open")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is PendingSheetState.PhoneCallConfirm -> {
            var phone by remember(sheet) { mutableStateOf(sheet.phoneNumber) }
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text("Confirm Phone Call", style = MaterialTheme.typography.titleLarge, color = ArcCyan)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Verify the phone number before dialing (कॉल पुष्टि):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSteelText
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Phone Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { onConfirmCall(phone) },
                        colors = ButtonDefaults.buttonColors(containerColor = ActiveEmerald, contentColor = AbyssalVoid),
                        modifier = Modifier.testTag("confirm_call_button")
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Call Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is PendingSheetState.SocialUploadConfirm -> {
            val photoPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                if (uri != null) {
                    onUpdateSocial { it.copy(imageUri = uri) }
                }
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text(
                        "${sheet.platformLabel} Photo & Post Assist",
                        style = MaterialTheme.typography.titleLarge,
                        color = ArcCyan
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = sheet.platformLabel == "Instagram",
                                onClick = {
                                    onUpdateSocial {
                                        it.copy(
                                            platformLabel = "Instagram",
                                            targetPackage = "com.instagram.android"
                                        )
                                    }
                                },
                                label = { Text("Instagram") }
                            )
                            FilterChip(
                                selected = sheet.platformLabel == "Facebook",
                                onClick = {
                                    onUpdateSocial {
                                        it.copy(
                                            platformLabel = "Facebook",
                                            targetPackage = "com.facebook.katana"
                                        )
                                    }
                                },
                                label = { Text("Facebook") }
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (sheet.imageUri == null) "Pick Photo (Zero-Permission)" else "Change Selected Photo")
                        }

                        sheet.imageUri?.let { uri ->
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected social upload photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }

                        OutlinedTextField(
                            value = sheet.topic,
                            onValueChange = { newTopic -> onUpdateSocial { it.copy(topic = newTopic) } },
                            label = { Text("Topic for Gemini AI Caption") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = onGenerateSocialCaption,
                            enabled = !sheet.isGeneratingCaption,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (sheet.isGeneratingCaption) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("Generate AI Caption (HI/EN)")
                        }

                        OutlinedTextField(
                            value = sheet.caption,
                            onValueChange = { newCap -> onUpdateSocial { it.copy(caption = newCap) } },
                            label = { Text("Post Caption") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmSocialUpload,
                        colors = ButtonDefaults.buttonColors(containerColor = ArcCyan, contentColor = AbyssalVoid)
                    ) {
                        Text("Open ${sheet.platformLabel}")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is PendingSheetState.CameraControlSheet -> {
            var pendingUri by remember { mutableStateOf<Uri?>(null) }
            var visionPrompt by remember { mutableStateOf("Identify objects and summarize in Hindi & English.") }

            val takePictureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success ->
                val uri = pendingUri
                if (success && uri != null) {
                    onAnalyzeCameraPhoto(uri, visionPrompt)
                }
            }

            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text("Camera & AI Vision", style = MaterialTheme.typography.titleLarge, color = ArcCyan)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = visionPrompt,
                            onValueChange = { visionPrompt = it },
                            label = { Text("Ask Gemini Vision") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                val uri = onCreateCameraUri()
                                pendingUri = uri
                                takePictureLauncher.launch(uri)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Capture & Analyze Photo")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onLaunchSystemCamera(false) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Camera")
                            }
                            OutlinedButton(
                                onClick = { onLaunchSystemCamera(true) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Video")
                            }
                        }
                        OutlinedButton(
                            onClick = { onToggleTorch(!isTorchOn) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                if (isTorchOn) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(if (isTorchOn) "Turn Torch OFF" else "Turn Torch ON")
                        }
                        if (sheet.isAnalyzing) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Gemini Vision analyzing...", color = ArcCyan)
                            }
                        }
                        sheet.analysisResult?.let { result ->
                            Surface(
                                color = CyberSlate,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, CyberGlassBorder)
                            ) {
                                Text(
                                    text = result,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = IceBlueText,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = onDismiss) { Text("Close") }
                }
            )
        }

        is PendingSheetState.QuerySearchDialog -> {
            var query by remember(sheet) { mutableStateOf(sheet.query) }
            val isYouTube = sheet.mode == QueryPromptMode.YOUTUBE
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text(
                        if (isYouTube) "YouTube Search & Play" else "Internet Web Search",
                        style = MaterialTheme.typography.titleLarge,
                        color = ArcCyan
                    )
                },
                text = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(if (isYouTube) "Video or song name" else "Search query") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = { onSubmitQuery(sheet.mode, query) }) {
                        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Search")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is PendingSheetState.VoiceTypingSheet -> {
            var typedText by remember(sheet) { mutableStateOf(sheet.text) }
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text("Voice Typing Assistant", style = MaterialTheme.typography.titleLarge, color = ArcCyan)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Injects text into the active app field via Accessibility Service (and copies to clipboard).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedSteelText
                        )
                        OutlinedTextField(
                            value = typedText,
                            onValueChange = { typedText = it },
                            label = { Text("Text to type (Hindi / English)") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedButton(
                            onClick = onStartVoiceDictation,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Mic, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Speak to Dictate")
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onExecuteVoiceType(typedText) }) {
                        Text("Inject / Copy Text")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }

        is PendingSheetState.AddCustomToolSheet -> {
            var title by remember { mutableStateOf("") }
            var titleHi by remember { mutableStateOf("") }
            var subtitle by remember { mutableStateOf("") }
            var target by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf(ToolActionType.CUSTOM_URL) }
            var requireConfirm by remember { mutableStateOf(true) }

            val types = listOf(
                ToolActionType.CUSTOM_URL to "Website / URL",
                ToolActionType.WHATSAPP_COMPOSE to "WhatsApp",
                ToolActionType.PHONE_CALL to "Quick Call",
                ToolActionType.YOUTUBE_SEARCH to "YouTube",
                ToolActionType.WEB_SEARCH to "Web Search"
            )

            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = CyberNavy,
                title = {
                    Text("Add VIP Dashboard Tool", style = MaterialTheme.typography.titleLarge, color = ArcCyan)
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Tool Name (English)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("new_tool_title_input")
                        )
                        OutlinedTextField(
                            value = titleHi,
                            onValueChange = { titleHi = it },
                            label = { Text("Hindi Label (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            label = { Text("Short Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Action Type", style = MaterialTheme.typography.labelMedium, color = ArcCyan)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            types.forEach { (type, label) ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = { Text(label) }
                                )
                            }
                        }
                        OutlinedTextField(
                            value = target,
                            onValueChange = { target = it },
                            label = {
                                Text(
                                    when (selectedType) {
                                        ToolActionType.PHONE_CALL, ToolActionType.WHATSAPP_COMPOSE -> "Phone Number (optional)"
                                        ToolActionType.CUSTOM_URL -> "URL (e.g. news.google.com)"
                                        else -> "Default Query (optional)"
                                    }
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Require Confirmation", color = IceBlueText)
                            Switch(checked = requireConfirm, onCheckedChange = { requireConfirm = it })
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onAddCustomTool(title, titleHi, subtitle, selectedType, target, requireConfirm)
                        },
                        enabled = title.isNotBlank(),
                        modifier = Modifier.testTag("save_custom_tool_button")
                    ) {
                        Text("Add Tool")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                }
            )
        }
    }
}
