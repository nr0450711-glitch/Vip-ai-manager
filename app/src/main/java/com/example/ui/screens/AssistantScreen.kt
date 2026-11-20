package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.local.JarvisSettings
import com.example.data.local.LanguageMode
import com.example.ui.AiRunState
import com.example.ui.components.ArcReactorCoreOrb
import com.example.ui.components.StatusMicroChip
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ActiveEmerald
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberGlassBorder
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.EmergencyCrimson
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.MutedSteelText
import com.example.ui.theme.SapphireCore

@Composable
fun AssistantScreen(
    messages: List<ChatMessageEntity>,
    settings: JarvisSettings,
    aiRunState: AiRunState,
    isListening: Boolean,
    isWakeModeLooping: Boolean,
    isSpeaking: Boolean,
    isEmergencyStopped: Boolean,
    partialTranscript: String,
    speechRmsLevel: Float,
    speechError: String?,
    isSpeechSupported: Boolean,
    onSendPrompt: (String) -> Unit,
    onMicClick: () -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onLanguageSelected: (LanguageMode) -> Unit,
    onStopSpeaking: () -> Unit,
    onClearChat: () -> Unit,
    onDismissSpeechError: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val quickCommands = remember {
        listOf(
            "WhatsApp संदेश भेजें" to "WhatsApp Rahul saying I will reach in 10 minutes",
            "कॉल लगाओ" to "Call 9876543210",
            "YouTube चलाएं" to "Play latest Hindi tech news on YouTube",
            "कैमरा खोलो" to "Open camera",
            "नीचे स्क्रॉल" to "Scroll down",
            "Instagram Post" to "Create an Instagram post about my fitness journey",
            "AI से पूछें" to "मुझे आज के लिए 3 प्रोडक्टिविटी टिप्स हिंदी में बताओ"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Arc Core HUD Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(
                1.dp,
                if (isEmergencyStopped) EmergencyCrimson else CyberGlassBorder
            ),
            color = CyberNavy
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = R.drawable.img_jarvis_hud_banner),
                    contentDescription = "JARVIS holographic HUD backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .alpha(0.32f)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    AbyssalVoid.copy(alpha = 0.88f),
                                    CyberNavy.copy(alpha = 0.65f)
                                )
                            )
                        )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEmergencyStopped) "SYSTEM HALTED" else "GEMINI AI CORE",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isEmergencyStopped) EmergencyCrimson else ArcCyan
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = when {
                                isEmergencyStopped -> "Emergency Stop active. Tap RESUME above."
                                isListening && isWakeModeLooping -> "Listening for \"Hey JARVIS\" / \"हे जार्विस\"..."
                                isListening -> partialTranscript.ifBlank { "सुन रहा हूँ... Speak Hindi or English" }
                                aiRunState is AiRunState.Thinking -> "Analyzing command via Gemini..."
                                aiRunState is AiRunState.Streaming -> "Streaming response..."
                                isSpeaking -> "Speaking bilingual response..."
                                else -> "Ready for Hindi + English voice & screen control"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = IceBlueText
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isWakeModeLooping,
                                onClick = { onToggleWakeWord(!isWakeModeLooping) },
                                enabled = isSpeechSupported && !isEmergencyStopped,
                                label = {
                                    Text(
                                        if (isWakeModeLooping) "Hey JARVIS: ON" else "Hey JARVIS",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Hearing,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ArcCyan.copy(alpha = 0.22f),
                                    selectedLabelColor = ArcCyan
                                ),
                                modifier = Modifier.testTag("wake_word_chip")
                            )
                            if (isSpeaking) {
                                FilterChip(
                                    selected = true,
                                    onClick = onStopSpeaking,
                                    label = { Text("Mute TTS", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.VolumeOff,
                                            contentDescription = "Stop speaking",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    ArcReactorCoreOrb(
                        isListening = isListening,
                        isThinking = aiRunState is AiRunState.Thinking || aiRunState is AiRunState.Streaming,
                        isSpeaking = isSpeaking,
                        isEmergencyStopped = isEmergencyStopped,
                        rmsLevel = speechRmsLevel,
                        onClick = onMicClick,
                        orbSize = 96.dp
                    )
                }
            }
        }

        // Language switcher + clear chat row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                LanguageMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.languageMode == mode,
                        onClick = { onLanguageSelected(mode) },
                        label = { Text(mode.code, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SapphireCore.copy(alpha = 0.28f),
                            selectedLabelColor = IceBlueText
                        )
                    )
                }
            }

            IconButton(
                onClick = onClearChat,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = "Clear conversation history",
                    tint = MutedSteelText
                )
            }
        }

        AnimatedVisibility(visible = speechError != null || aiRunState is AiRunState.Error) {
            val errText = speechError ?: (aiRunState as? AiRunState.Error)?.message.orEmpty()
            Surface(
                color = EmergencyCrimson.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmergencyCrimson.copy(alpha = 0.6f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(bottom = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = errText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = IceBlueText,
                        modifier = Modifier.weight(1f)
                    )
                    if (speechError != null) {
                        AssistChip(
                            onClick = onDismissSpeechError,
                            label = { Text("OK") }
                        )
                    }
                }
            }
        }

        if (aiRunState is AiRunState.Thinking) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ArcCyan,
                trackColor = CyberSlate
            )
        }

        // Chat Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(message = msg)
            }
        }

        // Bilingual Quick Command Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickCommands) { (label, prompt) ->
                AssistChip(
                    onClick = { onSendPrompt(prompt) },
                    label = {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = CyberSlate,
                        labelColor = ArcCyan
                    ),
                    border = BorderStroke(1.dp, CyberGlassBorder)
                )
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(top = 4.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onMicClick,
                shape = CircleShape,
                color = if (isListening) ActiveEmerald else CyberSlate,
                border = BorderStroke(1.dp, if (isListening) ActiveEmerald else ArcCyan),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("voice_mic_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isListening) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (isListening) "Stop listening" else "Speak in Hindi or English",
                        tint = if (isListening) AbyssalVoid else ArcCyan
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        "Ask JARVIS (हिन्दी / English)...",
                        color = MutedSteelText,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ArcCyan,
                    unfocusedBorderColor = CyberGlassBorder,
                    focusedContainerColor = CyberNavy,
                    unfocusedContainerColor = CyberNavy,
                    focusedTextColor = IceBlueText,
                    unfocusedTextColor = IceBlueText
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("prompt_text_field")
            )

            Surface(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onSendPrompt(inputText)
                        inputText = ""
                    }
                },
                shape = CircleShape,
                color = ArcCyan,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("send_prompt_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send command to Gemini",
                        tint = AbyssalVoid
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(message: ChatMessageEntity) {
    val isUser = message.role == "USER"
    val isSystem = message.role == "SYSTEM"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = when {
                isUser -> SapphireCore.copy(alpha = 0.25f)
                isSystem -> EmergencyCrimson.copy(alpha = 0.14f)
                else -> CyberNavy
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            border = BorderStroke(
                1.dp,
                when {
                    isUser -> SapphireCore.copy(alpha = 0.7f)
                    isSystem -> EmergencyCrimson.copy(alpha = 0.65f)
                    else -> CyberGlassBorder
                }
            ),
            modifier = Modifier.widthIn(max = 520.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isUser -> "YOU"
                            isSystem -> "SYSTEM"
                            else -> "JARVIS VIP"
                        },
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = when {
                            isUser -> IceBlueText
                            isSystem -> EmergencyCrimson
                            else -> ArcCyan
                        }
                    )
                    message.actionTag?.let { tag ->
                        StatusMicroChip(
                            label = tag,
                            color = if (isSystem) EmergencyCrimson else ArcCyan
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = IceBlueText
                )
            }
        }
    }
}
