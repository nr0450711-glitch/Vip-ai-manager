package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.data.local.LanguageMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "JarvisSpeechManager"

class JarvisSpeechManager(
    private val context: Context,
    private val onFinalTranscript: (String) -> Unit,
    private val onWakePhraseDetected: (inlineCommand: String?) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isWakeModeLooping = MutableStateFlow(false)
    val isWakeModeLooping: StateFlow<Boolean> = _isWakeModeLooping.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(context)

    private var currentLanguageMode: LanguageMode = LanguageMode.AUTO
    private var listeningForWakeWord: Boolean = false
    private var isStoppedByEmergency: Boolean = false
    private var pendingCommandAfterWake: Boolean = false

    fun startSingleCommand(languageMode: LanguageMode) {
        isStoppedByEmergency = false
        listeningForWakeWord = false
        pendingCommandAfterWake = false
        _isWakeModeLooping.value = false
        currentLanguageMode = languageMode
        startRecognizerInternal(languageMode)
    }

    fun startWakeWordLoop(languageMode: LanguageMode) {
        if (!isAvailable) {
            _lastError.value = "Speech recognition service unavailable on this device."
            return
        }
        isStoppedByEmergency = false
        listeningForWakeWord = true
        pendingCommandAfterWake = false
        _isWakeModeLooping.value = true
        currentLanguageMode = languageMode
        startRecognizerInternal(languageMode)
    }

    fun stopAll() {
        isStoppedByEmergency = true
        listeningForWakeWord = false
        pendingCommandAfterWake = false
        _isWakeModeLooping.value = false
        _isListening.value = false
        _rmsLevel.value = 0f
        _partialTranscript.value = ""
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recognizer", e)
            }
        }
    }

    fun clearError() {
        _lastError.value = null
    }

    private fun startRecognizerInternal(languageMode: LanguageMode) {
        if (!isAvailable) {
            _lastError.value = "SpeechRecognizer not available on this device."
            _isListening.value = false
            return
        }
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }
                val localeTag = when (languageMode) {
                    LanguageMode.HINDI -> "hi-IN"
                    LanguageMode.ENGLISH -> "en-IN"
                    LanguageMode.AUTO -> if (Locale.getDefault().language == "hi") "hi-IN" else "en-IN"
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                    putExtra(
                        RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                        arrayListOf("hi-IN", "en-IN", "en-US")
                    )
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                _lastError.value = null
                _partialTranscript.value = ""
                _isListening.value = true
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer", e)
                _isListening.value = false
                _lastError.value = e.localizedMessage ?: "Microphone listen error"
            }
        }
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            // Normalize typical SpeechRecognizer rmsdB (-2f..10f) to 0f..1f
            _rmsLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _rmsLevel.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _rmsLevel.value = 0f
            if (isStoppedByEmergency) return

            val errorText = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client recognition reset"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "Network error for speech recognition"
                SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that. Try speaking clearly."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                else -> "Speech error ($error)"
            }

            if (listeningForWakeWord && !isStoppedByEmergency &&
                error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
            ) {
                // Quietly restart wake word loop after a short cooldown
                mainHandler.postDelayed({
                    if (listeningForWakeWord && !isStoppedByEmergency) {
                        startRecognizerInternal(currentLanguageMode)
                    }
                }, 800L)
            } else {
                _lastError.value = errorText
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val best = matches?.firstOrNull()?.trim().orEmpty()
            _partialTranscript.value = ""

            if (best.isEmpty()) {
                if (listeningForWakeWord && !isStoppedByEmergency) {
                    mainHandler.postDelayed({
                        if (listeningForWakeWord && !isStoppedByEmergency) {
                            startRecognizerInternal(currentLanguageMode)
                        }
                    }, 500L)
                }
                return
            }

            if (listeningForWakeWord && !pendingCommandAfterWake) {
                val inlineCmd = extractAfterWakePhrase(best)
                if (inlineCmd != null) {
                    if (inlineCmd.isNotBlank()) {
                        onWakePhraseDetected(inlineCmd)
                        onFinalTranscript(inlineCmd)
                        scheduleNextWakeCycle()
                    } else {
                        pendingCommandAfterWake = true
                        onWakePhraseDetected(null)
                        mainHandler.postDelayed({
                            if (!isStoppedByEmergency) {
                                startRecognizerInternal(currentLanguageMode)
                            }
                        }, 600L)
                    }
                } else {
                    scheduleNextWakeCycle()
                }
            } else if (pendingCommandAfterWake) {
                pendingCommandAfterWake = false
                onFinalTranscript(best)
                if (listeningForWakeWord && !isStoppedByEmergency) {
                    scheduleNextWakeCycle()
                }
            } else {
                onFinalTranscript(best)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()
            if (partial.isNotEmpty()) {
                _partialTranscript.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun scheduleNextWakeCycle() {
        mainHandler.postDelayed({
            if (listeningForWakeWord && !isStoppedByEmergency) {
                startRecognizerInternal(currentLanguageMode)
            }
        }, 700L)
    }

    private fun extractAfterWakePhrase(text: String): String? {
        val lower = text.lowercase(Locale.getDefault())
        val triggers = listOf(
            "hey jarvis",
            "hi jarvis",
            "ok jarvis",
            "jarvis",
            "हे जार्विस",
            "जार्विस"
        )
        for (trigger in triggers) {
            val idx = lower.indexOf(trigger)
            if (idx >= 0) {
                return text.substring(idx + trigger.length).trim(' ', ',', '.', '!', '?')
            }
        }
        return null
    }

    fun release() {
        stopAll()
    }
}
