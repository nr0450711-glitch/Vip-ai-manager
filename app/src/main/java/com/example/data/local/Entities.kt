package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ToolCategory {
    COMMUNICATION,
    SOCIAL,
    MEDIA,
    AUTOMATION,
    CUSTOM
}

enum class ToolActionType {
    WHATSAPP_COMPOSE,
    PHONE_CALL,
    CAMERA_CONTROL,
    FLASHLIGHT_TOGGLE,
    YOUTUBE_SEARCH,
    WEB_SEARCH,
    INSTAGRAM_UPLOAD,
    FACEBOOK_UPLOAD,
    SOCIAL_POST_AI,
    VOICE_TYPING,
    AUTO_SCROLL,
    CUSTOM_URL
}

@Entity(tableName = "dashboard_tools")
data class DashboardToolEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val titleHi: String,
    val subtitle: String,
    val subtitleHi: String,
    val category: String,
    val actionType: String,
    val targetValue: String = "",
    val iconKey: String = "bolt",
    val requiresConfirmation: Boolean = true,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String, // "USER", "JARVIS", "SYSTEM"
    val content: String,
    val actionTag: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val detail: String,
    val status: String, // "EXECUTED", "CONFIRMED", "CANCELLED", "EMERGENCY_STOP"
    val timestamp: Long = System.currentTimeMillis()
)
