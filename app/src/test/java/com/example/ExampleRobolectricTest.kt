package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.automation.AccessibilityController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches JARVIS VIP`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JARVIS VIP", appName)
    }

    @Test
    fun `emergency stop halts and resume restores automation controller`() {
        AccessibilityController.emergencyStop()
        assertTrue(AccessibilityController.isEmergencyStopped.value)
        assertFalse(AccessibilityController.isAutoScrolling.value)

        AccessibilityController.resumeFromEmergency()
        assertFalse(AccessibilityController.isEmergencyStopped.value)
    }
}
