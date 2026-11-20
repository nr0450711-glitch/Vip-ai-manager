package com.example.ui

import android.accessibilityservice.AccessibilityService
import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiBrainService
import com.example.ai.JarvisIntentType
import com.example.automation.AccessibilityController
import com.example.automation.PhoneActionExecutor
import com.example.automation.ScrollDirection
import com.example.automation.SocialAssistType
import com.example.data.local.AuditLogEntity
import com.example.data.local.ChatMessageEntity
import com.example.data.local.DashboardToolEntity
import com.example.data.local.JarvisDatabase
import com.example.data.local.JarvisPreferences
import com.example.data.local.JarvisRepository
import com.example.data.local.JarvisSettings
import com.example.data.local.LanguageMode
import com.example.data.local.ToolActionType
import com.example.voice.JarvisSpeechManager
import com.example.voice.JarvisTtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "JarvisViewModel"

sealed interface AiRunState {
    data object Idle : AiRunState
    data object Thinking : AiRunState
    data object Streaming : AiRunState
    data class Error(val message: String) : AiRunState
}

enum class QueryPromptMode {
    YOUTUBE,
    WEB_SEARCH
}

sealed interface PendingSheetState {
    data class WhatsAppConfirm(
        val recipientOrPhone: String = "",
        val message: String = ""
    ) : PendingSheetState

    data class PhoneCallConfirm(
        val phoneNumber: String = ""
    ) : PendingSheetState

    data class SocialUploadConfirm(
        val platformLabel: String = "Instagram",
        val targetPackage: String = "com.instagram.android",
        val topic: String = "",
        val caption: String = "",
        val imageUri: Uri? = null,
        val isGeneratingCaption: Boolean = false
    ) : PendingSheetState

    data class CameraControlSheet(
        val isAnalyzing: Boolean = false,
        val lastPhotoUri: Uri? = null,
        val analysisResult: String? = null
    ) : PendingSheetState

    data class QuerySearchDialog(
        val mode: QueryPromptMode,
        val query: String = ""
    ) : PendingSheetState

    data class VoiceTypingSheet(
        val text: String = ""
    ) : PendingSheetState

    data object AddCustomToolSheet : PendingSheetState
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repository: JarvisRepository
    private val geminiBrain = GeminiBrainService()
    private val ttsManager = JarvisTtsManager(appContext)
    private lateinit var speechManager: JarvisSpeechManager

    private var activeAiJob: Job? = null

    val tools: StateFlow<List<DashboardToolEntity>>
    val messages: StateFlow<List<ChatMessageEntity>>
    val auditLogs: StateFlow<List<AuditLogEntity>>
    val settings: StateFlow<JarvisSettings>

    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val isAccessibilityConnected: StateFlow<Boolean> = AccessibilityController.isServiceConnected
    val activeForegroundPackage: StateFlow<String> = AccessibilityController.activePackageName
    val isAutoScrolling: StateFlow<Boolean> = AccessibilityController.isAutoScrolling
    val isEmergencyStopped: StateFlow<Boolean> = AccessibilityController.isEmergencyStopped
    val automationStatus: StateFlow<String> = AccessibilityController.lastAutomationStatus

    val isListening: StateFlow<Boolean>
    val isWakeModeLooping: StateFlow<Boolean>
    val partialTranscript: StateFlow<String>
    val speechRmsLevel: StateFlow<Float>
    val speechError: StateFlow<String?>

    private val _aiRunState = MutableStateFlow<AiRunState>(AiRunState.Idle)
    val aiRunState: StateFlow<AiRunState> = _aiRunState.asStateFlow()

    private val _pendingSheet = MutableStateFlow<PendingSheetState?>(null)
    val pendingSheet: StateFlow<PendingSheetState?> = _pendingSheet.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _bannerNotice = MutableStateFlow<String?>(null)
    val bannerNotice: StateFlow<String?> = _bannerNotice.asStateFlow()

    val isSpeechRecognitionSupported: Boolean
        get() = speechManager.isAvailable

    init {
        val db = JarvisDatabase.getInstance(appContext)
        repository = JarvisRepository(db.jarvisDao(), JarvisPreferences(appContext))

        tools = repository.tools.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        messages = repository.messages.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        auditLogs = repository.auditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JarvisSettings())

        speechManager = JarvisSpeechManager(
            context = appContext,
            onFinalTranscript = { transcript ->
                if (!AccessibilityController.isEmergencyStopped.value) {
                    handleUserPrompt(transcript, fromVoice = true)
                }
            },
            onWakePhraseDetected = { inlineCmd ->
                viewModelScope.launch {
                    repository.logAudit(
                        title = "Wake Word Triggered",
                        detail = if (inlineCmd.isNullOrBlank()) "Hey JARVIS detected" else "Inline command: $inlineCmd",
                        status = "EXECUTED"
                    )
                }
                if (inlineCmd.isNullOrBlank() && settings.value.autoSpeakEnabled) {
                    val ack = if (settings.value.languageMode == LanguageMode.HINDI) {
                        "जी सर, सुन रहा हूँ।"
                    } else {
                        "Yes sir, listening."
                    }
                    ttsManager.speak(ack, settings.value.languageMode)
                }
            }
        )

        isListening = speechManager.isListening
        isWakeModeLooping = speechManager.isWakeModeLooping
        partialTranscript = speechManager.partialTranscript
        speechRmsLevel = speechManager.rmsLevel
        speechError = speechManager.lastError

        viewModelScope.launch {
            repository.seedDefaultToolsIfEmpty()
            seedWelcomeMessageIfEmpty()
        }
    }

    private suspend fun seedWelcomeMessageIfEmpty() {
        // Wait briefly for initial DB emission if needed, or insert welcome if empty
        if (messages.value.isEmpty()) {
            repository.addMessage(
                role = "JARVIS",
                content = "JARVIS VIP online. Namaste! मैं आपकी हिंदी और English दोनों में मदद कर सकता हूँ। Ask me anything, or tap Mic / a VIP Tool below.",
                actionTag = "CORE_ONLINE"
            )
        }
    }

    fun dismissBannerNotice() {
        _bannerNotice.value = null
    }

    fun dismissSpeechError() {
        speechManager.clearError()
    }

    fun dismissPendingSheet() {
        _pendingSheet.value = null
    }

    fun toggleSingleVoiceListen() {
        if (isEmergencyStopped.value) {
            _bannerNotice.value = "Emergency Stop is active. Tap RESUME first."
            return
        }
        if (speechManager.isListening.value && !speechManager.isWakeModeLooping.value) {
            speechManager.stopAll()
        } else {
            ttsManager.stop()
            speechManager.startSingleCommand(settings.value.languageMode)
        }
    }

    fun toggleWakeWordMode(enable: Boolean) {
        viewModelScope.launch {
            if (enable && isEmergencyStopped.value) {
                _bannerNotice.value = "Resume system before enabling Hey JARVIS wake mode."
                return@launch
            }
            repository.setWakeWordEnabled(enable)
            if (enable) {
                speechManager.startWakeWordLoop(settings.value.languageMode)
                repository.logAudit("Wake Mode Enabled", "Listening for 'Hey JARVIS' / 'हे जार्विस'", "EXECUTED")
            } else {
                speechManager.stopAll()
                repository.logAudit("Wake Mode Disabled", "Continuous wake listening paused", "EXECUTED")
            }
        }
    }

    fun setLanguageMode(mode: LanguageMode) {
        viewModelScope.launch {
            repository.setLanguageMode(mode)
            if (settings.value.wakeWordEnabled && !isEmergencyStopped.value) {
                speechManager.startWakeWordLoop(mode)
            }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoSpeakEnabled(enabled)
            if (!enabled) ttsManager.stop()
        }
    }

    fun setConfirmationGuard(enabled: Boolean) {
        viewModelScope.launch {
            repository.setConfirmationGuard(enabled)
        }
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun triggerEmergencyStop() {
        activeAiJob?.cancel()
        activeAiJob = null
        _aiRunState.value = AiRunState.Idle
        _pendingSheet.value = null
        ttsManager.stop()
        speechManager.stopAll()
        AccessibilityController.emergencyStop()
        if (_isTorchOn.value) {
            PhoneActionExecutor.setFlashlight(appContext, false)
            _isTorchOn.value = false
        }
        viewModelScope.launch {
            repository.setWakeWordEnabled(false)
            repository.logAudit(
                title = "EMERGENCY STOP",
                detail = "All AI jobs, voice recognition, TTS, torch, and screen automation halted immediately.",
                status = "EMERGENCY_STOP"
            )
            repository.addMessage(
                role = "SYSTEM",
                content = "EMERGENCY STOP ACTIVATED / इमरजेंसी स्टॉप सक्रिय: All voice, AI, and screen automation tasks halted.",
                actionTag = "EMERGENCY_STOP"
            )
        }
    }

    fun resumeSystem() {
        AccessibilityController.resumeFromEmergency()
        _bannerNotice.value = "JARVIS VIP systems resumed."
        viewModelScope.launch {
            repository.logAudit("System Resumed", "Cleared Emergency Stop lock", "EXECUTED")
        }
    }

    fun handleUserPrompt(rawText: String, fromVoice: Boolean = false) {
        val text = rawText.trim()
        if (text.isBlank()) return
        if (isEmergencyStopped.value) {
            _bannerNotice.value = "System halted by Emergency Stop. Tap RESUME to continue."
            return
        }

        activeAiJob?.cancel()
        activeAiJob = viewModelScope.launch {
            _aiRunState.value = AiRunState.Thinking
            repository.addMessage(
                role = "USER",
                content = text,
                actionTag = if (fromVoice) "VOICE" else null
            )

            try {
                val intent = geminiBrain.classifyIntent(
                    userMessage = text,
                    languageMode = settings.value.languageMode,
                    accessibilityConnected = isAccessibilityConnected.value
                )

                if (intent.intentType == JarvisIntentType.EMERGENCY_STOP) {
                    triggerEmergencyStop()
                    return@launch
                }

                if (intent.intentType == JarvisIntentType.CHAT_ONLY) {
                    val historyPairs = messages.value.takeLast(8).map { it.role to it.content }
                    val msgId = repository.addMessage(role = "JARVIS", content = "…")
                    _aiRunState.value = AiRunState.Streaming
                    var finalReply = ""
                    geminiBrain.streamConversation(
                        userMessage = text,
                        recentHistory = historyPairs,
                        languageMode = settings.value.languageMode
                    ).collect { partial ->
                        finalReply = partial
                        repository.updateMessage(msgId, partial, "GEMINI_AI")
                    }
                    _aiRunState.value = AiRunState.Idle
                    if (settings.value.autoSpeakEnabled && finalReply.isNotBlank()) {
                        ttsManager.speak(finalReply, settings.value.languageMode)
                    }
                } else {
                    val reply = intent.replyText.ifBlank { "Executing ${intent.intentType.name} command." }
                    repository.addMessage(
                        role = "JARVIS",
                        content = reply,
                        actionTag = intent.intentType.name
                    )
                    _aiRunState.value = AiRunState.Idle
                    if (settings.value.autoSpeakEnabled) {
                        ttsManager.speak(reply, settings.value.languageMode)
                    }
                    dispatchStructuredIntent(intent.intentType, intent.target, intent.messageBody)
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Gemini timeout", e)
                val msg = "Gemini AI request timed out after 60s. Please check internet and try again."
                _aiRunState.value = AiRunState.Error(msg)
                repository.addMessage("SYSTEM", msg, "TIMEOUT")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini request failed", e)
                val errMsg = e.localizedMessage ?: "Firebase AI Logic request failed."
                _aiRunState.value = AiRunState.Error(errMsg)
                repository.addMessage("SYSTEM", "AI Error: $errMsg", "ERROR")
            }
        }
    }

    private fun dispatchStructuredIntent(
        type: JarvisIntentType,
        target: String,
        messageBody: String
    ) {
        when (type) {
            JarvisIntentType.WHATSAPP_MESSAGE -> {
                _pendingSheet.value = PendingSheetState.WhatsAppConfirm(
                    recipientOrPhone = target,
                    message = messageBody
                )
            }
            JarvisIntentType.PHONE_CALL -> {
                _pendingSheet.value = PendingSheetState.PhoneCallConfirm(phoneNumber = target)
            }
            JarvisIntentType.YOUTUBE_SEARCH -> {
                if (target.isNotBlank()) {
                    executeYouTubeSearch(target)
                } else {
                    _pendingSheet.value = PendingSheetState.QuerySearchDialog(QueryPromptMode.YOUTUBE)
                }
            }
            JarvisIntentType.WEB_SEARCH -> {
                if (target.isNotBlank()) {
                    executeWebSearch(target)
                } else {
                    _pendingSheet.value = PendingSheetState.QuerySearchDialog(QueryPromptMode.WEB_SEARCH)
                }
            }
            JarvisIntentType.OPEN_CAMERA -> {
                _pendingSheet.value = PendingSheetState.CameraControlSheet()
            }
            JarvisIntentType.OPEN_VIDEO_CAMERA -> {
                launchCameraExternal(videoMode = true)
            }
            JarvisIntentType.TOGGLE_TORCH_ON -> setTorch(true)
            JarvisIntentType.TOGGLE_TORCH_OFF -> setTorch(false)
            JarvisIntentType.INSTAGRAM_ASSIST -> {
                _pendingSheet.value = PendingSheetState.SocialUploadConfirm(
                    platformLabel = "Instagram",
                    targetPackage = "com.instagram.android",
                    topic = target,
                    caption = messageBody
                )
            }
            JarvisIntentType.FACEBOOK_ASSIST -> {
                _pendingSheet.value = PendingSheetState.SocialUploadConfirm(
                    platformLabel = "Facebook",
                    targetPackage = "com.facebook.katana",
                    topic = target,
                    caption = messageBody
                )
            }
            JarvisIntentType.SCROLL_DOWN -> performScroll(ScrollDirection.DOWN)
            JarvisIntentType.SCROLL_UP -> performScroll(ScrollDirection.UP)
            JarvisIntentType.AUTO_SCROLL_START -> startAutoScroll()
            JarvisIntentType.NAV_BACK -> performGlobalNav(AccessibilityService.GLOBAL_ACTION_BACK)
            JarvisIntentType.NAV_HOME -> performGlobalNav(AccessibilityService.GLOBAL_ACTION_HOME)
            JarvisIntentType.NAV_RECENTS -> performGlobalNav(AccessibilityService.GLOBAL_ACTION_RECENTS)
            JarvisIntentType.VOICE_TYPE -> {
                if (messageBody.isNotBlank()) {
                    executeVoiceType(messageBody)
                } else {
                    _pendingSheet.value = PendingSheetState.VoiceTypingSheet()
                }
            }
            JarvisIntentType.LIKE_ASSIST -> executeSocialLikeAssist()
            JarvisIntentType.COMMENT_ASSIST -> executeSocialCommentAssist(messageBody)
            JarvisIntentType.EMERGENCY_STOP -> triggerEmergencyStop()
            JarvisIntentType.CHAT_ONLY -> Unit
        }
    }

    fun launchTool(tool: DashboardToolEntity) {
        if (isEmergencyStopped.value) {
            _bannerNotice.value = "Emergency Stop is active. Tap RESUME first."
            return
        }
        val actionType = ToolActionType.entries.find { it.name == tool.actionType } ?: ToolActionType.CUSTOM_URL
        when (actionType) {
            ToolActionType.WHATSAPP_COMPOSE -> {
                _pendingSheet.value = PendingSheetState.WhatsAppConfirm(
                    recipientOrPhone = tool.targetValue
                )
            }
            ToolActionType.PHONE_CALL -> {
                _pendingSheet.value = PendingSheetState.PhoneCallConfirm(
                    phoneNumber = tool.targetValue
                )
            }
            ToolActionType.CAMERA_CONTROL -> {
                _pendingSheet.value = PendingSheetState.CameraControlSheet()
            }
            ToolActionType.FLASHLIGHT_TOGGLE -> {
                setTorch(!_isTorchOn.value)
            }
            ToolActionType.YOUTUBE_SEARCH -> {
                if (tool.targetValue.isNotBlank()) {
                    executeYouTubeSearch(tool.targetValue)
                } else {
                    _pendingSheet.value = PendingSheetState.QuerySearchDialog(QueryPromptMode.YOUTUBE)
                }
            }
            ToolActionType.WEB_SEARCH -> {
                if (tool.targetValue.isNotBlank()) {
                    executeWebSearch(tool.targetValue)
                } else {
                    _pendingSheet.value = PendingSheetState.QuerySearchDialog(QueryPromptMode.WEB_SEARCH)
                }
            }
            ToolActionType.INSTAGRAM_UPLOAD -> {
                _pendingSheet.value = PendingSheetState.SocialUploadConfirm(
                    platformLabel = "Instagram",
                    targetPackage = "com.instagram.android"
                )
            }
            ToolActionType.FACEBOOK_UPLOAD, ToolActionType.SOCIAL_POST_AI -> {
                _pendingSheet.value = PendingSheetState.SocialUploadConfirm(
                    platformLabel = "Facebook",
                    targetPackage = "com.facebook.katana"
                )
            }
            ToolActionType.VOICE_TYPING -> {
                _pendingSheet.value = PendingSheetState.VoiceTypingSheet()
            }
            ToolActionType.AUTO_SCROLL -> {
                if (isAutoScrolling.value) stopAutoScroll() else startAutoScroll()
            }
            ToolActionType.CUSTOM_URL -> {
                val res = PhoneActionExecutor.openUrl(appContext, tool.targetValue)
                _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "Unable to open URL" }
                viewModelScope.launch {
                    repository.logAudit("Custom Tool: ${tool.title}", tool.targetValue, "EXECUTED")
                }
            }
        }
    }

    fun confirmWhatsAppSend(recipientOrPhone: String, message: String, assistSendTap: Boolean) {
        if (message.isBlank()) {
            _bannerNotice.value = "Please enter a WhatsApp message."
            return
        }
        val res = PhoneActionExecutor.openWhatsAppMessage(appContext, recipientOrPhone, message)
        _pendingSheet.value = null
        val msg = res.getOrElse { it.localizedMessage ?: "Failed to open WhatsApp" }
        _bannerNotice.value = msg
        viewModelScope.launch {
            repository.logAudit(
                title = "WhatsApp Confirmed",
                detail = "To: ${recipientOrPhone.ifBlank { "Chooser" }} | Msg: ${message.take(40)}",
                status = "CONFIRMED"
            )
            if (assistSendTap && isAccessibilityConnected.value) {
                kotlinx.coroutines.delay(1400L)
                AccessibilityController.assistWhatsAppSend()
            }
        }
    }

    fun confirmPhoneCall(phoneNumber: String) {
        val res = PhoneActionExecutor.makePhoneCall(appContext, phoneNumber)
        _pendingSheet.value = null
        _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "Call failed" }
        viewModelScope.launch {
            repository.logAudit("Phone Call Confirmed", "Target: $phoneNumber", "CONFIRMED")
        }
    }

    fun updateSocialSheet(transform: (PendingSheetState.SocialUploadConfirm) -> PendingSheetState.SocialUploadConfirm) {
        val current = _pendingSheet.value as? PendingSheetState.SocialUploadConfirm ?: return
        _pendingSheet.value = transform(current)
    }

    fun generateAiCaptionForSocialSheet() {
        val current = _pendingSheet.value as? PendingSheetState.SocialUploadConfirm ?: return
        val topic = current.topic.ifBlank { "My new update" }
        _pendingSheet.value = current.copy(isGeneratingCaption = true)
        viewModelScope.launch {
            try {
                val caption = geminiBrain.generateSocialCaption(
                    topic = topic,
                    platform = current.platformLabel,
                    languageMode = settings.value.languageMode
                )
                val latest = _pendingSheet.value as? PendingSheetState.SocialUploadConfirm
                if (latest != null) {
                    _pendingSheet.value = latest.copy(
                        caption = caption,
                        isGeneratingCaption = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val latest = _pendingSheet.value as? PendingSheetState.SocialUploadConfirm
                if (latest != null) {
                    _pendingSheet.value = latest.copy(isGeneratingCaption = false)
                }
                _bannerNotice.value = "Caption error: ${e.localizedMessage}"
            }
        }
    }

    fun confirmSocialUpload() {
        val current = _pendingSheet.value as? PendingSheetState.SocialUploadConfirm ?: return
        val res = PhoneActionExecutor.sharePhotoToSocialApp(
            context = appContext,
            targetPackage = current.targetPackage,
            appLabel = current.platformLabel,
            imageUri = current.imageUri,
            caption = current.caption
        )
        _pendingSheet.value = null
        _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "Share failed" }
        viewModelScope.launch {
            repository.logAudit(
                title = "${current.platformLabel} Upload Assisted",
                detail = "Photo attached: ${current.imageUri != null}",
                status = "CONFIRMED"
            )
        }
    }

    fun launchCameraExternal(videoMode: Boolean) {
        _pendingSheet.value = null
        val res = PhoneActionExecutor.launchSystemCamera(appContext, videoMode)
        _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "Camera unavailable" }
        viewModelScope.launch {
            repository.logAudit("Camera Launched", if (videoMode) "Video mode" else "Photo mode", "EXECUTED")
        }
    }

    fun createCameraCaptureUri(): Uri = PhoneActionExecutor.createCameraCaptureUri(appContext)

    fun analyzeCapturedCameraPhoto(photoUri: Uri, prompt: String) {
        val sheet = (_pendingSheet.value as? PendingSheetState.CameraControlSheet)
            ?: PendingSheetState.CameraControlSheet()
        _pendingSheet.value = sheet.copy(isAnalyzing = true, lastPhotoUri = photoUri, analysisResult = null)
        viewModelScope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    loadScaledBitmap(photoUri)
                }
                val analysis = geminiBrain.analyzeCameraImage(
                    bitmap = bitmap,
                    userPrompt = prompt.ifBlank { "Describe what you see and highlight key details." },
                    languageMode = settings.value.languageMode
                )
                val current = _pendingSheet.value as? PendingSheetState.CameraControlSheet
                if (current != null) {
                    _pendingSheet.value = current.copy(isAnalyzing = false, analysisResult = analysis)
                }
                repository.logAudit("Gemini Vision Analysis", "Analyzed captured camera image", "EXECUTED")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val current = _pendingSheet.value as? PendingSheetState.CameraControlSheet
                if (current != null) {
                    _pendingSheet.value = current.copy(
                        isAnalyzing = false,
                        analysisResult = "Vision error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun loadScaledBitmap(uri: Uri): Bitmap {
        val raw = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(appContext.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(appContext.contentResolver, uri)
        }
        val maxDim = 1024
        val ratio = minOf(maxDim.toFloat() / raw.width, maxDim.toFloat() / raw.height, 1f)
        return if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                raw,
                (raw.width * ratio).toInt().coerceAtLeast(1),
                (raw.height * ratio).toInt().coerceAtLeast(1),
                true
            )
        } else {
            raw
        }
    }

    fun setTorch(enable: Boolean) {
        val res = PhoneActionExecutor.setFlashlight(appContext, enable)
        res.onSuccess {
            _isTorchOn.value = it
            _bannerNotice.value = if (it) "Flashlight ON" else "Flashlight OFF"
            viewModelScope.launch {
                repository.logAudit("Flashlight", if (it) "Turned ON" else "Turned OFF", "EXECUTED")
            }
        }.onFailure {
            _bannerNotice.value = it.localizedMessage ?: "Torch unavailable"
        }
    }

    fun executeYouTubeSearch(query: String) {
        _pendingSheet.value = null
        val res = PhoneActionExecutor.searchYouTube(appContext, query)
        _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "YouTube launch failed" }
        viewModelScope.launch {
            repository.logAudit("YouTube Search", query, "EXECUTED")
        }
    }

    fun executeWebSearch(query: String) {
        _pendingSheet.value = null
        val res = PhoneActionExecutor.searchWeb(appContext, query)
        _bannerNotice.value = res.getOrElse { it.localizedMessage ?: "Web search failed" }
        viewModelScope.launch {
            repository.logAudit("Web Search", query, "EXECUTED")
        }
    }

    fun executeVoiceType(text: String) {
        if (text.isBlank()) return
        val injected = AccessibilityController.injectVoiceText(text)
        PhoneActionExecutor.copyToClipboard(appContext, "JARVIS Voice Text", text)
        _bannerNotice.value = if (injected) {
            "Typed into active screen field (also copied)"
        } else {
            "Copied to clipboard — tap Paste in target app (enable Accessibility for direct typing)"
        }
        _pendingSheet.value = null
        viewModelScope.launch {
            repository.logAudit("Voice Typing", "Text length: ${text.length} | Direct inject: $injected", "EXECUTED")
        }
    }

    fun performScroll(direction: ScrollDirection) {
        val ok = AccessibilityController.performScroll(direction)
        if (!ok) _bannerNotice.value = AccessibilityController.lastAutomationStatus.value
    }

    fun startAutoScroll() {
        val ok = AccessibilityController.startAutoScroll(settings.value.autoScrollIntervalMs)
        if (!ok) {
            _bannerNotice.value = AccessibilityController.lastAutomationStatus.value
        } else {
            viewModelScope.launch {
                repository.logAudit("Auto-Scroll Started", "Interval: ${settings.value.autoScrollIntervalMs}ms", "EXECUTED")
            }
        }
    }

    fun stopAutoScroll() {
        AccessibilityController.stopAutoScroll()
    }

    fun performGlobalNav(action: Int) {
        val ok = AccessibilityController.performGlobalNavigation(action)
        if (!ok) _bannerNotice.value = AccessibilityController.lastAutomationStatus.value
    }

    fun executeSocialLikeAssist() {
        val ok = AccessibilityController.assistSocialAction(SocialAssistType.LIKE_POST)
        _bannerNotice.value = AccessibilityController.lastAutomationStatus.value
        viewModelScope.launch {
            repository.logAudit("Social Like Assist", "Executed: $ok", "EXECUTED")
        }
    }

    fun executeSocialCommentAssist(commentText: String) {
        if (commentText.isNotBlank()) {
            PhoneActionExecutor.copyToClipboard(appContext, "JARVIS Comment", commentText)
        }
        val ok = AccessibilityController.assistSocialAction(SocialAssistType.COMMENT_FOCUS_PASTE, commentText)
        _bannerNotice.value = AccessibilityController.lastAutomationStatus.value
        viewModelScope.launch {
            repository.logAudit("Social Comment Assist", "Assisted: $ok", "EXECUTED")
        }
    }

    fun openAddToolSheet() {
        _pendingSheet.value = PendingSheetState.AddCustomToolSheet
    }

    fun openVoiceTypingSheet() {
        _pendingSheet.value = PendingSheetState.VoiceTypingSheet()
    }

    fun openCameraSheet() {
        _pendingSheet.value = PendingSheetState.CameraControlSheet()
    }

    fun openSocialSheet(platformLabel: String, pkg: String) {
        _pendingSheet.value = PendingSheetState.SocialUploadConfirm(
            platformLabel = platformLabel,
            targetPackage = pkg
        )
    }

    fun addCustomTool(
        title: String,
        titleHi: String,
        subtitle: String,
        actionType: ToolActionType,
        targetValue: String,
        requiresConfirmation: Boolean
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTool(
                DashboardToolEntity(
                    title = title.trim(),
                    titleHi = titleHi.ifBlank { title.trim() },
                    subtitle = subtitle.ifBlank { "Custom VIP shortcut" },
                    subtitleHi = subtitle.ifBlank { "कस्टम VIP शॉर्टकट" },
                    category = "CUSTOM",
                    actionType = actionType.name,
                    targetValue = targetValue.trim(),
                    iconKey = actionType.name.lowercase(),
                    requiresConfirmation = requiresConfirmation,
                    isCustom = true
                )
            )
            repository.logAudit("Tool Added", title.trim(), "EXECUTED")
            _pendingSheet.value = null
            _bannerNotice.value = "Added \"$title\" to Dashboard"
        }
    }

    fun removeTool(tool: DashboardToolEntity) {
        viewModelScope.launch {
            repository.removeTool(tool.id)
            repository.logAudit("Tool Removed", tool.title, "EXECUTED")
        }
    }

    fun restoreDefaultTools() {
        viewModelScope.launch {
            repository.restoreDefaultTools()
            _bannerNotice.value = "Restored default VIP tools"
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            repository.clearChat()
            seedWelcomeMessageIfEmpty()
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch {
            repository.clearAuditLogs()
        }
    }

    override fun onCleared() {
        speechManager.release()
        ttsManager.release()
        super.onCleared()
    }
}
