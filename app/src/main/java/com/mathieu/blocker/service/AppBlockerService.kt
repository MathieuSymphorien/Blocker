package com.mathieu.blocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mathieu.blocker.data.BlockerPreferences
import com.mathieu.blocker.ui.overlay.CountdownOverlayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lifecycleOwner = ServiceLifecycleOwner()

    private var overlayView: ComposeView? = null
    private var isCountdownActive = false

    // Cache blocked apps to avoid reading DataStore on every event
    private var blockedApps: Set<String> = emptySet()
    private var timerSeconds: Int = 10

    // After the countdown finishes, don't re-block the app for a short window
    // Maps packageName -> timestamp when the cooldown expires
    private val cooldowns = mutableMapOf<String, Long>()
    private val cooldownDurationMs = 5000L // 5 seconds of grace after timer ends

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        // Observe preferences changes
        serviceScope.launch {
            BlockerPreferences.getBlockedApps(this@AppBlockerService).collect {
                blockedApps = it
            }
        }
        serviceScope.launch {
            BlockerPreferences.getTimerSeconds(this@AppBlockerService).collect {
                timerSeconds = it
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Ignore our own app and system UI
        if (packageName == this.packageName) return
        if (packageName == "com.android.systemui") return

        // If countdown is active, ignore everything
        if (isCountdownActive) return

        // Check if this app is in cooldown (just finished waiting)
        val cooldownExpiry = cooldowns[packageName]
        if (cooldownExpiry != null) {
            if (System.currentTimeMillis() < cooldownExpiry) {
                // Still in grace period, let the app through
                return
            } else {
                // Cooldown expired, remove it
                cooldowns.remove(packageName)
            }
        }

        // If this package is blocked, show overlay on top
        if (packageName in blockedApps && overlayView == null) {
            showCountdownOverlay(packageName)
        }
    }

    override fun onInterrupt() {
        dismissOverlay()
    }

    override fun onDestroy() {
        dismissOverlay()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showCountdownOverlay(packageName: String) {
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        isCountdownActive = true

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                CountdownOverlayScreen(
                    appName = appName,
                    totalSeconds = timerSeconds,
                    onFinished = {
                        // Grant a cooldown so the app isn't re-blocked immediately
                        cooldowns[packageName] = System.currentTimeMillis() + cooldownDurationMs
                        dismissOverlay()
                    },
                    onGoBack = {
                        dismissOverlay()
                        goHome()
                    }
                )
            }
        }

        wm.addView(composeView, params)
        overlayView = composeView
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
            } catch (_: Exception) {
                // View might already be removed
            }
        }
        overlayView = null
        isCountdownActive = false
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }
}
