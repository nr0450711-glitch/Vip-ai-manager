package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.jarvisDataStore by preferencesDataStore(name = "jarvis_vip_prefs")

enum class LanguageMode(val code: String, val label: String, val labelHi: String) {
    AUTO("AUTO", "Auto (HI + EN)", "ऑटो (हिन्दी + EN)"),
    HINDI("HI", "Hindi (हिन्दी)", "हिन्दी"),
    ENGLISH("EN", "English", "अंग्रेज़ी")
}

data class JarvisSettings(
    val languageMode: LanguageMode = LanguageMode.AUTO,
    val wakeWordEnabled: Boolean = false,
    val autoSpeakEnabled: Boolean = true,
    val confirmationGuardEnabled: Boolean = true,
    val autoScrollIntervalMs: Long = 2800L
)

class JarvisPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE_MODE = stringPreferencesKey("language_mode")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val AUTO_SPEAK_ENABLED = booleanPreferencesKey("auto_speak_enabled")
        val CONFIRMATION_GUARD = booleanPreferencesKey("confirmation_guard")
        val AUTO_SCROLL_INTERVAL = longPreferencesKey("auto_scroll_interval")
    }

    val settingsFlow: Flow<JarvisSettings> = context.jarvisDataStore.data.map { prefs ->
        val modeCode = prefs[Keys.LANGUAGE_MODE] ?: LanguageMode.AUTO.code
        val mode = LanguageMode.entries.find { it.code == modeCode } ?: LanguageMode.AUTO
        JarvisSettings(
            languageMode = mode,
            wakeWordEnabled = prefs[Keys.WAKE_WORD_ENABLED] ?: false,
            autoSpeakEnabled = prefs[Keys.AUTO_SPEAK_ENABLED] ?: true,
            confirmationGuardEnabled = prefs[Keys.CONFIRMATION_GUARD] ?: true,
            autoScrollIntervalMs = prefs[Keys.AUTO_SCROLL_INTERVAL] ?: 2800L
        )
    }

    suspend fun setLanguageMode(mode: LanguageMode) {
        context.jarvisDataStore.edit { it[Keys.LANGUAGE_MODE] = mode.code }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    }

    suspend fun setAutoSpeakEnabled(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.AUTO_SPEAK_ENABLED] = enabled }
    }

    suspend fun setConfirmationGuard(enabled: Boolean) {
        context.jarvisDataStore.edit { it[Keys.CONFIRMATION_GUARD] = enabled }
    }

    suspend fun setAutoScrollInterval(intervalMs: Long) {
        context.jarvisDataStore.edit { it[Keys.AUTO_SCROLL_INTERVAL] = intervalMs }
    }
}
