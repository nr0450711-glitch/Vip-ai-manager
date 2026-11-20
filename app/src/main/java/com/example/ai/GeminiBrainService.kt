package com.example.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.LanguageMode
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "GeminiBrainService"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val INFERENCE_TIMEOUT_MS = 60_000L

enum class JarvisIntentType {
    CHAT_ONLY,
    WHATSAPP_MESSAGE,
    PHONE_CALL,
    YOUTUBE_SEARCH,
    WEB_SEARCH,
    OPEN_CAMERA,
    OPEN_VIDEO_CAMERA,
    TOGGLE_TORCH_ON,
    TOGGLE_TORCH_OFF,
    INSTAGRAM_ASSIST,
    FACEBOOK_ASSIST,
    SCROLL_DOWN,
    SCROLL_UP,
    AUTO_SCROLL_START,
    NAV_BACK,
    NAV_HOME,
    NAV_RECENTS,
    VOICE_TYPE,
    LIKE_ASSIST,
    COMMENT_ASSIST,
    EMERGENCY_STOP
}

data class ParsedJarvisIntent(
    val intentType: JarvisIntentType,
    val target: String,
    val messageBody: String,
    val replyText: String
)

class GeminiBrainService {

    private val intentSchema = Schema.obj(
        properties = mapOf(
            "intentType" to Schema.enumeration(
                values = JarvisIntentType.entries.map { it.name },
                description = "The classified phone command or CHAT_ONLY for general conversation."
            ),
            "target" to Schema.string(
                description = "Recipient name/phone, search query, URL, or social topic."
            ),
            "messageBody" to Schema.string(
                description = "Message body for WhatsApp, voice typing text, or social post caption/comment."
            ),
            "replyText" to Schema.string(
                description = "Short, polite bilingual or user-language confirmation/acknowledgment from JARVIS VIP."
            )
        )
    )

    private val intentModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.2f
                responseMimeType = "application/json"
                responseSchema = intentSchema
            },
            systemInstruction = content {
                text(
                    """
                    You are JARVIS VIP, a futuristic bilingual (Hindi + English) Android AI phone manager.
                    Classify the user's request into one of the exact intentType enums:
                    - WHATSAPP_MESSAGE: sending/composing a WhatsApp message (extract recipient/number in target, message in messageBody).
                    - PHONE_CALL: calling or dialing someone (extract name/number in target).
                    - YOUTUBE_SEARCH: searching or playing something on YouTube (extract query in target).
                    - WEB_SEARCH: searching Google/web (extract query in target).
                    - OPEN_CAMERA / OPEN_VIDEO_CAMERA: opening photo or video camera.
                    - TOGGLE_TORCH_ON / TOGGLE_TORCH_OFF: turning flashlight/torch on or off.
                    - INSTAGRAM_ASSIST / FACEBOOK_ASSIST: uploading a photo or creating a social post.
                    - SCROLL_DOWN / SCROLL_UP / AUTO_SCROLL_START: scrolling the screen or starting auto-scroll.
                    - NAV_BACK / NAV_HOME / NAV_RECENTS: system navigation commands.
                    - VOICE_TYPE: typing/dictating text into a focused app field (put dictated text in messageBody).
                    - LIKE_ASSIST / COMMENT_ASSIST: liking or commenting on a social post (put comment in messageBody).
                    - EMERGENCY_STOP: stopping/cancelling all automation, speech, or scrolling.
                    - CHAT_ONLY: all general questions, coding, knowledge, Hindi/English translation, or conversation.
                    Keep replyText concise and in the same language (Hindi or English) the user used.
                    """.trimIndent()
                )
            }
        )
    }

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.7f
            },
            systemInstruction = content {
                text(
                    """
                    You are JARVIS VIP, an intelligent, crisp, respectful AI Phone Manager built for Android.
                    You speak fluent English and Hindi (Devanagari). Match the user's language naturally:
                    - If the user writes or speaks in Hindi or Hinglish, reply clearly in natural Hindi (or requested language).
                    - If the user uses English, reply concisely in crisp, futuristic HUD-style English.
                    Never claim to have executed restricted actions without user confirmation. Keep responses practical and well-formatted.
                    """.trimIndent()
                )
            }
        )
    }

    suspend fun classifyIntent(
        userMessage: String,
        languageMode: LanguageMode,
        accessibilityConnected: Boolean
    ): ParsedJarvisIntent {
        return try {
            val prompt = buildString {
                append("Language preference: ${languageMode.label}. ")
                append("Accessibility connected: $accessibilityConnected. ")
                append("User input: $userMessage")
            }
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                intentModel.generateContent(prompt)
            }
            val jsonStr = response.text ?: throw IllegalStateException("Empty intent response.")
            val json = JSONObject(jsonStr)
            val typeStr = json.optString("intentType", JarvisIntentType.CHAT_ONLY.name)
            val parsedType = JarvisIntentType.entries.find { it.name == typeStr }
                ?: JarvisIntentType.CHAT_ONLY
            ParsedJarvisIntent(
                intentType = parsedType,
                target = json.optString("target", ""),
                messageBody = json.optString("messageBody", ""),
                replyText = json.optString("replyText", "")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Intent classification timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Intent classification error", e)
            throw e
        }
    }

    fun streamConversation(
        userMessage: String,
        recentHistory: List<Pair<String, String>>,
        languageMode: LanguageMode
    ): Flow<String> = flow {
        val historyContent: List<Content> = recentHistory
            .takeLast(8)
            .map { (role, text) ->
                val mappedRole = if (role == "USER") "user" else "model"
                content(mappedRole) { text(text) }
            }
        val languageDirective = when (languageMode) {
            LanguageMode.HINDI -> "Respond in natural Hindi (Devanagari script)."
            LanguageMode.ENGLISH -> "Respond in crisp English."
            LanguageMode.AUTO -> "Match the user's language (Hindi or English)."
        }

        val chat = chatModel.startChat(history = historyContent)
        val accumulated = StringBuilder()
        try {
            withTimeout(INFERENCE_TIMEOUT_MS) {
                chat.sendMessageStream("$languageDirective\n\n$userMessage").collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Chat streaming timed out", e)
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Chat streaming failed", e)
            throw e
        }
    }

    suspend fun analyzeCameraImage(
        bitmap: Bitmap,
        userPrompt: String,
        languageMode: LanguageMode
    ): String {
        val langNote = when (languageMode) {
            LanguageMode.HINDI -> "Reply in Hindi (Devanagari)."
            LanguageMode.ENGLISH -> "Reply in concise English."
            LanguageMode.AUTO -> "Reply in clear English with a 1-line Hindi summary."
        }
        return try {
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                chatModel.generateContent(
                    content {
                        image(bitmap)
                        text("$langNote\n$userPrompt")
                    }
                )
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini Vision returned an empty analysis.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Image analysis failed", e)
            throw e
        }
    }

    suspend fun generateSocialCaption(
        topic: String,
        platform: String,
        languageMode: LanguageMode
    ): String {
        val langNote = when (languageMode) {
            LanguageMode.HINDI -> "Write the caption in engaging Hindi with relevant hashtags."
            LanguageMode.ENGLISH -> "Write the caption in engaging English with relevant hashtags."
            LanguageMode.AUTO -> "Write a stylish bilingual Hinglish/English + Hindi caption with 5 hashtags."
        }
        return try {
            val response = withTimeout(INFERENCE_TIMEOUT_MS) {
                chatModel.generateContent(
                    "Create a short, high-engagement $platform post caption about: \"$topic\". $langNote Output only the ready-to-post caption text."
                )
            }
            response.text?.trim()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("Gemini returned an empty caption.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Social caption generation failed", e)
            throw e
        }
    }
}
