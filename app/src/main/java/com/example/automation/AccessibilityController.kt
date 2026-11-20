package com.example.automation

import android.accessibilityservice.AccessibilityService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference

enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

enum class SocialAssistType {
    LIKE_POST,
    COMMENT_FOCUS_PASTE
}

object AccessibilityController {

    private var serviceRef: WeakReference<JarvisAccessibilityService>? = null

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    private val _activePackageName = MutableStateFlow("None")
    val activePackageName: StateFlow<String> = _activePackageName.asStateFlow()

    private val _isAutoScrolling = MutableStateFlow(false)
    val isAutoScrolling: StateFlow<Boolean> = _isAutoScrolling.asStateFlow()

    private val _isEmergencyStopped = MutableStateFlow(false)
    val isEmergencyStopped: StateFlow<Boolean> = _isEmergencyStopped.asStateFlow()

    private val _lastAutomationStatus = MutableStateFlow("Ready")
    val lastAutomationStatus: StateFlow<String> = _lastAutomationStatus.asStateFlow()

    internal fun attach(service: JarvisAccessibilityService) {
        serviceRef = WeakReference(service)
        _isServiceConnected.value = true
        _lastAutomationStatus.value = "Accessibility Service Connected"
    }

    internal fun detach() {
        serviceRef = null
        _isServiceConnected.value = false
        _isAutoScrolling.value = false
        _lastAutomationStatus.value = "Accessibility Service Disconnected"
    }

    internal fun updateActivePackage(pkg: String) {
        if (pkg.isNotBlank()) {
            _activePackageName.value = pkg
        }
    }

    internal fun updateAutoScrolling(active: Boolean) {
        _isAutoScrolling.value = active
    }

    internal fun updateStatus(status: String) {
        _lastAutomationStatus.value = status
    }

    fun emergencyStop() {
        _isEmergencyStopped.value = true
        _isAutoScrolling.value = false
        serviceRef?.get()?.haltAllAutomation()
        _lastAutomationStatus.value = "EMERGENCY STOP ACTIVE — All automation halted"
    }

    fun resumeFromEmergency() {
        _isEmergencyStopped.value = false
        _lastAutomationStatus.value = "System Resumed & Ready"
    }

    fun performScroll(direction: ScrollDirection): Boolean {
        if (_isEmergencyStopped.value) {
            _lastAutomationStatus.value = "Blocked: Emergency Stop is active"
            return false
        }
        val service = serviceRef?.get() ?: run {
            _lastAutomationStatus.value = "Enable JARVIS Accessibility Service first"
            return false
        }
        return service.executeScroll(direction)
    }

    fun startAutoScroll(intervalMs: Long): Boolean {
        if (_isEmergencyStopped.value) {
            _lastAutomationStatus.value = "Blocked: Emergency Stop is active"
            return false
        }
        val service = serviceRef?.get() ?: run {
            _lastAutomationStatus.value = "Enable JARVIS Accessibility Service first"
            return false
        }
        service.startAutoScrollLoop(intervalMs)
        return true
    }

    fun stopAutoScroll() {
        _isAutoScrolling.value = false
        serviceRef?.get()?.stopAutoScrollLoop()
        _lastAutomationStatus.value = "Auto-scroll stopped"
    }

    fun performGlobalNavigation(globalAction: Int): Boolean {
        if (_isEmergencyStopped.value) {
            _lastAutomationStatus.value = "Blocked: Emergency Stop is active"
            return false
        }
        val service = serviceRef?.get() ?: run {
            _lastAutomationStatus.value = "Enable JARVIS Accessibility Service first"
            return false
        }
        val ok = service.performGlobalAction(globalAction)
        val label = when (globalAction) {
            AccessibilityService.GLOBAL_ACTION_BACK -> "Back"
            AccessibilityService.GLOBAL_ACTION_HOME -> "Home"
            AccessibilityService.GLOBAL_ACTION_RECENTS -> "Recents"
            AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS -> "Notifications"
            AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS -> "Quick Settings"
            else -> "Navigation"
        }
        _lastAutomationStatus.value = if (ok) "Executed: $label" else "Failed: $label"
        return ok
    }

    fun injectVoiceText(text: String): Boolean {
        if (_isEmergencyStopped.value) return false
        val service = serviceRef?.get() ?: return false
        return service.insertTextIntoFocusedField(text)
    }

    fun assistSocialAction(type: SocialAssistType, commentText: String? = null): Boolean {
        if (_isEmergencyStopped.value) return false
        val service = serviceRef?.get() ?: run {
            _lastAutomationStatus.value = "Enable JARVIS Accessibility Service for screen assist"
            return false
        }
        return service.performSocialAssist(type, commentText)
    }

    fun assistWhatsAppSend(): Boolean {
        if (_isEmergencyStopped.value) return false
        val service = serviceRef?.get() ?: return false
        return service.tryClickWhatsAppSend()
    }
}
