package com.example.data.local

import kotlinx.coroutines.flow.Flow

class JarvisRepository(
    private val dao: JarvisDao,
    private val preferences: JarvisPreferences
) {
    val tools: Flow<List<DashboardToolEntity>> = dao.observeTools()
    val messages: Flow<List<ChatMessageEntity>> = dao.observeMessages()
    val auditLogs: Flow<List<AuditLogEntity>> = dao.observeAuditLogs()
    val settings: Flow<JarvisSettings> = preferences.settingsFlow

    suspend fun seedDefaultToolsIfEmpty() {
        if (dao.getToolCount() == 0) {
            dao.insertTools(defaultTools())
        }
    }

    suspend fun restoreDefaultTools() {
        dao.insertTools(defaultTools())
    }

    suspend fun addTool(tool: DashboardToolEntity) {
        dao.insertTool(tool)
    }

    suspend fun removeTool(id: Int) {
        dao.deleteToolById(id)
    }

    suspend fun addMessage(role: String, content: String, actionTag: String? = null): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                role = role,
                content = content,
                actionTag = actionTag
            )
        )
    }

    suspend fun updateMessage(id: Long, content: String, actionTag: String? = null) {
        dao.updateMessage(id, content, actionTag)
    }

    suspend fun clearChat() {
        dao.clearMessages()
    }

    suspend fun logAudit(title: String, detail: String, status: String) {
        dao.insertAuditLog(
            AuditLogEntity(
                title = title,
                detail = detail,
                status = status
            )
        )
    }

    suspend fun clearAuditLogs() {
        dao.clearAuditLogs()
    }

    suspend fun setLanguageMode(mode: LanguageMode) = preferences.setLanguageMode(mode)
    suspend fun setWakeWordEnabled(enabled: Boolean) = preferences.setWakeWordEnabled(enabled)
    suspend fun setAutoSpeakEnabled(enabled: Boolean) = preferences.setAutoSpeakEnabled(enabled)
    suspend fun setConfirmationGuard(enabled: Boolean) = preferences.setConfirmationGuard(enabled)
    suspend fun setAutoScrollInterval(ms: Long) = preferences.setAutoScrollInterval(ms)

    companion object {
        fun defaultTools(): List<DashboardToolEntity> = listOf(
            DashboardToolEntity(
                title = "WhatsApp",
                titleHi = "व्हाट्सएप संदेश",
                subtitle = "Compose & send with confirmation",
                subtitleHi = "पुष्टि के साथ संदेश भेजें",
                category = ToolCategory.COMMUNICATION.name,
                actionType = ToolActionType.WHATSAPP_COMPOSE.name,
                iconKey = "whatsapp",
                requiresConfirmation = true
            ),
            DashboardToolEntity(
                title = "Phone Call",
                titleHi = "फ़ोन कॉल",
                subtitle = "Smart dialer with safety check",
                subtitleHi = "सुरक्षा पुष्टि के साथ कॉल",
                category = ToolCategory.COMMUNICATION.name,
                actionType = ToolActionType.PHONE_CALL.name,
                iconKey = "call",
                requiresConfirmation = true
            ),
            DashboardToolEntity(
                title = "Camera & AI Vision",
                titleHi = "कैमरा और AI विज़न",
                subtitle = "Capture & analyze with Gemini",
                subtitleHi = "फ़ोटो लें और AI से समझें",
                category = ToolCategory.MEDIA.name,
                actionType = ToolActionType.CAMERA_CONTROL.name,
                iconKey = "camera",
                requiresConfirmation = false
            ),
            DashboardToolEntity(
                title = "Torch Control",
                titleHi = "फ्लैशलाइट",
                subtitle = "Instant flashlight toggle",
                subtitleHi = "तुरंत टॉर्च चालू/बंद करें",
                category = ToolCategory.MEDIA.name,
                actionType = ToolActionType.FLASHLIGHT_TOGGLE.name,
                iconKey = "flashlight",
                requiresConfirmation = false
            ),
            DashboardToolEntity(
                title = "Instagram Photo",
                titleHi = "इंस्टाग्राम अपलोड",
                subtitle = "Pick photo + Gemini caption",
                subtitleHi = "फ़ोटो चुनें व AI कैप्शन पाएं",
                category = ToolCategory.SOCIAL.name,
                actionType = ToolActionType.INSTAGRAM_UPLOAD.name,
                iconKey = "instagram",
                requiresConfirmation = true
            ),
            DashboardToolEntity(
                title = "Facebook Post",
                titleHi = "फेसबुक पोस्ट",
                subtitle = "Photo upload & post composer",
                subtitleHi = "फ़ोटो अपलोड और पोस्ट सहायक",
                category = ToolCategory.SOCIAL.name,
                actionType = ToolActionType.FACEBOOK_UPLOAD.name,
                iconKey = "facebook",
                requiresConfirmation = true
            ),
            DashboardToolEntity(
                title = "YouTube Control",
                titleHi = "यूट्यूब सर्च",
                subtitle = "Search & play videos fast",
                subtitleHi = "वीडियो खोजें और चलाएं",
                category = ToolCategory.MEDIA.name,
                actionType = ToolActionType.YOUTUBE_SEARCH.name,
                iconKey = "youtube",
                requiresConfirmation = false
            ),
            DashboardToolEntity(
                title = "Web Search",
                titleHi = "इंटरनेट सर्च",
                subtitle = "Google search & AI answers",
                subtitleHi = "वेब और AI जानकारी खोजें",
                category = ToolCategory.MEDIA.name,
                actionType = ToolActionType.WEB_SEARCH.name,
                iconKey = "search",
                requiresConfirmation = false
            ),
            DashboardToolEntity(
                title = "Voice Typing",
                titleHi = "वॉइस टाइपिंग",
                subtitle = "Dictate Hindi/English to apps",
                subtitleHi = "बोलकर किसी भी ऐप में लिखें",
                category = ToolCategory.AUTOMATION.name,
                actionType = ToolActionType.VOICE_TYPING.name,
                iconKey = "mic",
                requiresConfirmation = false
            ),
            DashboardToolEntity(
                title = "Auto Scroll",
                titleHi = "ऑटो स्क्रॉल",
                subtitle = "Hands-free screen scrolling",
                subtitleHi = "बिना हाथ लगाए स्क्रीन स्क्रॉल",
                category = ToolCategory.AUTOMATION.name,
                actionType = ToolActionType.AUTO_SCROLL.name,
                iconKey = "scroll",
                requiresConfirmation = false
            )
        )
    }
}
