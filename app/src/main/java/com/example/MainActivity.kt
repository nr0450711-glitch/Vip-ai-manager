package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.security.configureAppCheck
import com.example.ui.AiRunState
import com.example.ui.JarvisViewModel
import com.example.ui.QueryPromptMode
import com.example.ui.components.JarvisTopHudBar
import com.example.ui.components.PendingActionHost
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PrivacyScreen
import com.example.ui.theme.AbyssalVoid
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.CyberNavy
import com.example.ui.theme.CyberSlate
import com.example.ui.theme.IceBlueText
import com.example.ui.theme.JarvisVipTheme
import com.example.ui.theme.MutedSteelText

enum class JarvisTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ASSISTANT("JARVIS", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome),
    TOOLS("Tools", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    AUTOMATE("Automate", Icons.Filled.Memory, Icons.Outlined.Memory),
    PRIVACY("Privacy", Icons.Filled.Security, Icons.Outlined.Security)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory: Configure Firebase App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            JarvisVipTheme {
                JarvisVipRootApp()
            }
        }
    }
}

@Composable
fun JarvisVipRootApp(
    viewModel: JarvisViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentTab by rememberSaveable { mutableStateOf(JarvisTab.ASSISTANT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val tools by viewModel.tools.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val aiRunState by viewModel.aiRunState.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val isAccessibilityConnected by viewModel.isAccessibilityConnected.collectAsStateWithLifecycle()
    val activePackage by viewModel.activeForegroundPackage.collectAsStateWithLifecycle()
    val isAutoScrolling by viewModel.isAutoScrolling.collectAsStateWithLifecycle()
    val isEmergencyStopped by viewModel.isEmergencyStopped.collectAsStateWithLifecycle()
    val automationStatus by viewModel.automationStatus.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isWakeModeLooping by viewModel.isWakeModeLooping.collectAsStateWithLifecycle()
    val partialTranscript by viewModel.partialTranscript.collectAsStateWithLifecycle()
    val speechRmsLevel by viewModel.speechRmsLevel.collectAsStateWithLifecycle()
    val speechError by viewModel.speechError.collectAsStateWithLifecycle()
    val pendingSheet by viewModel.pendingSheet.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val bannerNotice by viewModel.bannerNotice.collectAsStateWithLifecycle()

    var pendingVoiceAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingVoiceAction?.invoke()
        }
        pendingVoiceAction = null
    }

    fun runWithMicPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            action()
        } else {
            pendingVoiceAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(bannerNotice) {
        val msg = bannerNotice
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissBannerNotice()
        }
    }

    BackHandler(enabled = currentTab != JarvisTab.ASSISTANT) {
        currentTab = JarvisTab.ASSISTANT
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(AbyssalVoid, CyberNavy, AbyssalVoid)
                )
            )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    JarvisTopHudBar(
                        isEmergencyStopped = isEmergencyStopped,
                        isAccessibilityConnected = isAccessibilityConnected,
                        isWakeModeActive = isWakeModeLooping,
                        isThinking = aiRunState is AiRunState.Thinking || aiRunState is AiRunState.Streaming,
                        onEmergencyStop = { viewModel.triggerEmergencyStop() },
                        onResumeSystem = { viewModel.resumeSystem() }
                    )
                },
                bottomBar = {
                    if (!useNavRail) {
                        NavigationBar(
                            containerColor = CyberNavy.copy(alpha = 0.96f),
                            tonalElevation = 8.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            JarvisTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = ArcCyan,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = CyberSlate,
                                        unselectedIconColor = MutedSteelText,
                                        unselectedTextColor = MutedSteelText
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NavigationRail(
                            containerColor = CyberNavy.copy(alpha = 0.92f),
                            modifier = Modifier
                                .fillMaxHeight()
                                .windowInsetsPadding(WindowInsets.navigationBars)
                        ) {
                            JarvisTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = ArcCyan,
                                        selectedTextColor = ArcCyan,
                                        indicatorColor = CyberSlate,
                                        unselectedIconColor = MutedSteelText,
                                        unselectedTextColor = MutedSteelText
                                    )
                                )
                            }
                        }
                    }

                    Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                        when (currentTab) {
                            JarvisTab.ASSISTANT -> AssistantScreen(
                                messages = messages,
                                settings = settings,
                                aiRunState = aiRunState,
                                isListening = isListening,
                                isWakeModeLooping = isWakeModeLooping,
                                isSpeaking = isSpeaking,
                                isEmergencyStopped = isEmergencyStopped,
                                partialTranscript = partialTranscript,
                                speechRmsLevel = speechRmsLevel,
                                speechError = speechError,
                                isSpeechSupported = viewModel.isSpeechRecognitionSupported,
                                onSendPrompt = { viewModel.handleUserPrompt(it) },
                                onMicClick = { runWithMicPermission { viewModel.toggleSingleVoiceListen() } },
                                onToggleWakeWord = { enable ->
                                    if (enable) {
                                        runWithMicPermission { viewModel.toggleWakeWordMode(true) }
                                    } else {
                                        viewModel.toggleWakeWordMode(false)
                                    }
                                },
                                onLanguageSelected = { viewModel.setLanguageMode(it) },
                                onStopSpeaking = { viewModel.stopSpeaking() },
                                onClearChat = { viewModel.clearChatHistory() },
                                onDismissSpeechError = { viewModel.dismissSpeechError() }
                            )

                            JarvisTab.TOOLS -> DashboardScreen(
                                tools = tools,
                                languageMode = settings.languageMode,
                                isTorchOn = isTorchOn,
                                isAutoScrolling = isAutoScrolling,
                                onLaunchTool = { viewModel.launchTool(it) },
                                onAddToolClick = { viewModel.openAddToolSheet() },
                                onRemoveTool = { viewModel.removeTool(it) },
                                onRestoreDefaults = { viewModel.restoreDefaultTools() }
                            )

                            JarvisTab.AUTOMATE -> AutomationScreen(
                                isAccessibilityConnected = isAccessibilityConnected,
                                activeForegroundPackage = activePackage,
                                isAutoScrolling = isAutoScrolling,
                                automationStatus = automationStatus,
                                onScrollDirection = { viewModel.performScroll(it) },
                                onStartAutoScroll = { viewModel.startAutoScroll() },
                                onStopAutoScroll = { viewModel.stopAutoScroll() },
                                onGlobalNav = { viewModel.performGlobalNav(it) },
                                onOpenVoiceTyping = { viewModel.openVoiceTypingSheet() },
                                onOpenSocialAssist = { label, pkg -> viewModel.openSocialSheet(label, pkg) },
                                onAssistLike = { viewModel.executeSocialLikeAssist() },
                                onAssistComment = { viewModel.executeSocialCommentAssist(it) }
                            )

                            JarvisTab.PRIVACY -> PrivacyScreen(
                                settings = settings,
                                isAccessibilityConnected = isAccessibilityConnected,
                                auditLogs = auditLogs,
                                onToggleAutoSpeak = { viewModel.setAutoSpeak(it) },
                                onToggleWakeWord = { enable ->
                                    if (enable) {
                                        runWithMicPermission { viewModel.toggleWakeWordMode(true) }
                                    } else {
                                        viewModel.toggleWakeWordMode(false)
                                    }
                                },
                                onToggleConfirmationGuard = { viewModel.setConfirmationGuard(it) },
                                onClearAuditLogs = { viewModel.clearAuditLogs() }
                            )
                        }
                    }
                }
            }
        }
    }

    PendingActionHost(
        sheet = pendingSheet,
        isAccessibilityConnected = isAccessibilityConnected,
        isTorchOn = isTorchOn,
        onDismiss = { viewModel.dismissPendingSheet() },
        onConfirmWhatsApp = { recipient, message, assist ->
            viewModel.confirmWhatsAppSend(recipient, message, assist)
        },
        onConfirmCall = { phone -> viewModel.confirmPhoneCall(phone) },
        onUpdateSocial = { transform -> viewModel.updateSocialSheet(transform) },
        onGenerateSocialCaption = { viewModel.generateAiCaptionForSocialSheet() },
        onConfirmSocialUpload = { viewModel.confirmSocialUpload() },
        onCreateCameraUri = { viewModel.createCameraCaptureUri() },
        onAnalyzeCameraPhoto = { uri, prompt -> viewModel.analyzeCapturedCameraPhoto(uri, prompt) },
        onLaunchSystemCamera = { videoMode -> viewModel.launchCameraExternal(videoMode) },
        onToggleTorch = { enable -> viewModel.setTorch(enable) },
        onSubmitQuery = { mode, query ->
            when (mode) {
                QueryPromptMode.YOUTUBE -> viewModel.executeYouTubeSearch(query)
                QueryPromptMode.WEB_SEARCH -> viewModel.executeWebSearch(query)
            }
        },
        onExecuteVoiceType = { text -> viewModel.executeVoiceType(text) },
        onStartVoiceDictation = {
            viewModel.dismissPendingSheet()
            runWithMicPermission { viewModel.toggleSingleVoiceListen() }
        },
        onAddCustomTool = { title, titleHi, sub, type, target, confirm ->
            viewModel.addCustomTool(title, titleHi, sub, type, target, confirm)
        }
    )
}
