package com.example.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "JarvisAccessibility"

class JarvisAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var autoScrollJob: Job? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityController.attach(this)
        Log.d(TAG, "JarvisAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString().orEmpty()
        if (pkg.isNotEmpty() && pkg != packageName) {
            AccessibilityController.updateActivePackage(pkg)
        }
    }

    override fun onInterrupt() {
        haltAllAutomation()
    }

    override fun onDestroy() {
        haltAllAutomation()
        serviceScope.cancel()
        AccessibilityController.detach()
        super.onDestroy()
    }

    fun haltAllAutomation() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        AccessibilityController.updateAutoScrolling(false)
    }

    fun startAutoScrollLoop(intervalMs: Long) {
        autoScrollJob?.cancel()
        AccessibilityController.updateAutoScrolling(true)
        AccessibilityController.updateStatus("Auto-scrolling active (${intervalMs / 1000.0}s interval)")
        autoScrollJob = serviceScope.launch {
            while (isActive && !AccessibilityController.isEmergencyStopped.value) {
                executeScroll(ScrollDirection.DOWN)
                delay(intervalMs.coerceIn(1200L, 10_000L))
            }
            AccessibilityController.updateAutoScrolling(false)
        }
    }

    fun stopAutoScrollLoop() {
        autoScrollJob?.cancel()
        autoScrollJob = null
        AccessibilityController.updateAutoScrolling(false)
    }

    fun executeScroll(direction: ScrollDirection): Boolean {
        if (AccessibilityController.isEmergencyStopped.value) return false

        val metrics = resources.displayMetrics
        val w = metrics.widthPixels.toFloat()
        val h = metrics.heightPixels.toFloat()
        val path = Path()

        when (direction) {
            ScrollDirection.DOWN -> {
                path.moveTo(w * 0.5f, h * 0.72f)
                path.lineTo(w * 0.5f, h * 0.28f)
            }
            ScrollDirection.UP -> {
                path.moveTo(w * 0.5f, h * 0.28f)
                path.lineTo(w * 0.5f, h * 0.72f)
            }
            ScrollDirection.LEFT -> {
                path.moveTo(w * 0.8f, h * 0.5f)
                path.lineTo(w * 0.2f, h * 0.5f)
            }
            ScrollDirection.RIGHT -> {
                path.moveTo(w * 0.2f, h * 0.5f)
                path.lineTo(w * 0.8f, h * 0.5f)
            }
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 320L))
            .build()

        val dispatched = dispatchGesture(gesture, null, null)
        AccessibilityController.updateStatus(
            if (dispatched) "Scrolled ${direction.name.lowercase()}" else "Gesture dispatch unavailable"
        )
        return dispatched
    }

    fun insertTextIntoFocusedField(text: String): Boolean {
        if (AccessibilityController.isEmergencyStopped.value || text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            ?: findFirstEditableNode(root)
            ?: return false

        focused.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val existing = focused.text?.toString().orEmpty()
        val combined = if (existing.isBlank() || existing == focused.hintText?.toString()) {
            text
        } else {
            "$existing $text"
        }
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, combined)
        }
        val ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        AccessibilityController.updateStatus(
            if (ok) "Voice text injected into active app" else "Target field rejected direct injection"
        )
        return ok
    }

    fun performSocialAssist(type: SocialAssistType, commentText: String?): Boolean {
        if (AccessibilityController.isEmergencyStopped.value) return false
        val root = rootInActiveWindow ?: run {
            AccessibilityController.updateStatus("No active external app window detected")
            return false
        }
        return when (type) {
            SocialAssistType.LIKE_POST -> {
                val target = findNodeByContentOrText(
                    root,
                    listOf("like", "heart", "पसंद")
                )
                val clicked = target?.let { clickClickableAncestor(it) } ?: false
                AccessibilityController.updateStatus(
                    if (clicked) "Assisted Like action executed" else "No Like button found on current screen"
                )
                clicked
            }
            SocialAssistType.COMMENT_FOCUS_PASTE -> {
                val commentBtn = findNodeByContentOrText(
                    root,
                    listOf("comment", "add a comment", "write a comment", "टिप्पणी")
                )
                commentBtn?.let { clickClickableAncestor(it) }
                if (!commentText.isNullOrBlank()) {
                    val editable = findFirstEditableNode(root)
                    if (editable != null) {
                        val args = Bundle().apply {
                            putCharSequence(
                                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                                commentText
                            )
                        }
                        val setOk = editable.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                        AccessibilityController.updateStatus(
                            if (setOk) "Comment text inserted — review and tap Post"
                            else "Comment field focused — paste from clipboard"
                        )
                        return setOk
                    }
                }
                AccessibilityController.updateStatus("Comment action assisted")
                commentBtn != null
            }
        }
    }

    fun tryClickWhatsAppSend(): Boolean {
        if (AccessibilityController.isEmergencyStopped.value) return false
        val root = rootInActiveWindow ?: return false
        val pkg = root.packageName?.toString().orEmpty()
        if (!pkg.contains("whatsapp")) return false
        val sendNode = findNodeByContentOrText(root, listOf("send", "भेजें"))
        val ok = sendNode?.let { clickClickableAncestor(it) } ?: false
        AccessibilityController.updateStatus(
            if (ok) "WhatsApp Send tapped after user confirmation" else "WhatsApp opened — tap Send manually"
        )
        return ok
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findFirstEditableNode(child)
            if (match != null) return match
        }
        return null
    }

    private fun findNodeByContentOrText(
        node: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase().orEmpty()
        val txt = node.text?.toString()?.lowercase().orEmpty()
        if (keywords.any { kw -> desc.contains(kw) || txt.contains(kw) }) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val match = findNodeByContentOrText(child, keywords)
            if (match != null) return match
        }
        return null
    }

    private fun clickClickableAncestor(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }
}
