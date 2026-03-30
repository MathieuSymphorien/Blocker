package com.mathieu.blocker.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.annotation.RequiresApi
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.mathieu.blocker.data.BlockerPreferences
import com.mathieu.blocker.data.Profile
import com.mathieu.blocker.data.db.BlockerDatabase
import com.mathieu.blocker.data.db.UsageStatsEntity
import com.mathieu.blocker.ui.overlay.CountdownOverlayScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AppBlockerService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val lifecycleOwner = ServiceLifecycleOwner()

    private var overlayView: ComposeView? = null
    private var overlayParams: WindowManager.LayoutParams? = null
    private var isCountdownActive = false

    private var effectiveProfile: Profile = Profile(name = "Maison")


    // Nombre d'events WINDOW_STATE_CHANGED à ignorer après "non je reviens" (rebond de fermeture d'app)
    // On skip exactement 1 event par package, pas plus — évite le bypass de longue durée
    private val goBackSkipCount = mutableMapOf<String, Int>()

    // Foreground time tracking (primary, works on all apps)
    private var trackedApp: String? = null
    private var trackedAppStart: Long = 0L

    // Scroll counting — TYPE_WINDOW_CONTENT_CHANGED uniquement, debounce 3s (uniforme sur tous apps)
    private var pendingScrollCount: Int = 0
    private var lastScrollEventMs: Long = 0L
    private val SCROLL_DEBOUNCE_MS = 3000L

    // Daily scroll limit
    private var dailyScrollLimit: Int = 0  // 0 = disabled
    private var dailyScrollCount: Int = 0  // in-memory counter, synced from DB on startup
    private var dailyLimitReached: Boolean = false
    private var lastCheckedDate: String = ""

    // System packages to never track or block (regardless of blocked list)
    private val systemPackages = setOf(
        "android",
        "com.android.systemui"
    )

    // Launchers to never block
    private val launcherPackages = mutableSetOf<String>()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private val dao by lazy {
        BlockerDatabase.getInstance(this).usageStatsDao()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        lifecycleOwner.performRestore(null)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

        detectLaunchers()

        // Observe daily scroll limit
        serviceScope.launch {
            BlockerPreferences.getDailyScrollLimit(this@AppBlockerService).collect { limit ->
                dailyScrollLimit = limit
            }
        }

        // Load today's scroll count from DB on startup (restores state after service restart)
        serviceScope.launch(Dispatchers.IO) {
            val today = dateFormat.format(Date())
            val totals = dao.getDailyTotals(today).first()
            val limit = BlockerPreferences.getDailyScrollLimit(this@AppBlockerService).first()
            serviceScope.launch(Dispatchers.Main) {
                dailyScrollCount = totals.scrollCount
                if (limit > 0 && dailyScrollCount >= limit) dailyLimitReached = true
            }
        }

        // Observe effective profile from DataStore changes
        serviceScope.launch {
            BlockerPreferences.getEffectiveProfile(this@AppBlockerService).collect { profile ->
                val changed = profile.id != effectiveProfile.id
                effectiveProfile = profile
                // If the tracked app is no longer in the new profile's blocked list, flush and stop
                if (changed) {
                    trackedApp?.let { pkg ->
                        if (pkg !in profile.blockedApps) flushTrackedApp()
                    }
                }
            }
        }

        // Periodic planner check every 60s
        // DataStore flow only emits on data changes, not on time changes.
        // We manually re-evaluate the planner every minute so profile switches happen on time.
        serviceScope.launch {
            while (true) {
                delay(60_000L)
                try {
                    val newProfile = BlockerPreferences.getEffectiveProfile(
                        this@AppBlockerService
                    ).first()
                    if (newProfile.id != effectiveProfile.id) {
                        trackedApp?.let { pkg ->
                            if (pkg !in newProfile.blockedApps) flushTrackedApp()
                        }
                        effectiveProfile = newProfile
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun detectLaunchers() {
        try {
            val homeIntent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) }
            val resolveInfoList = packageManager.queryIntentActivities(homeIntent, 0)
            resolveInfoList.forEach { launcherPackages.add(it.activityInfo.packageName) }
        } catch (_: Exception) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        // Si l'utilisateur navigue vers n'importe quel autre package (système, launcher, Blocker lui-même)
        // et qu'on trackait une app, on flush la session — sinon trackedApp reste set et bypass le blocage
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            trackedApp != null && trackedApp != packageName) {
            flushTrackedApp()
        }

        if (packageName in systemPackages) return

        if (packageName == this.packageName) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChanged(packageName)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> handleScrollEvent(packageName)
        }
    }

    private fun handleWindowChanged(packageName: String) {
        if (isCountdownActive) return

        // Reset daily limit flag at midnight
        val today = dateFormat.format(Date())
        if (today != lastCheckedDate) {
            lastCheckedDate = today
            dailyScrollCount = 0
            dailyLimitReached = false
        }

        // Never block or track launchers
        if (packageName in launcherPackages) {
            trackedApp = null
            return
        }

        // Skip exactement 1 event de rebond après "non je reviens" (l'app émet un event quand elle se ferme)
        val skipCount = goBackSkipCount[packageName] ?: 0
        if (skipCount > 0) {
            goBackSkipCount[packageName] = skipCount - 1
            if (goBackSkipCount[packageName] == 0) goBackSkipCount.remove(packageName)
            return
        }

        if (packageName !in effectiveProfile.blockedApps) return

        // Vérification limite journalière : compteur mémoire + scrolls en cours
        val effectiveScrolls = dailyScrollCount + pendingScrollCount
        if (!dailyLimitReached && dailyScrollLimit > 0 && effectiveScrolls >= dailyScrollLimit) {
            dailyLimitReached = true
        }
        if (dailyLimitReached && overlayView == null) {
            showCountdownOverlay(packageName, limitReached = true)
            return
        }

        // Sync DB en arrière-plan pour corriger toute désynchronisation (redémarrage service, etc.)
        // Si la DB révèle que la limite est atteinte, on expulse l'utilisateur
        if (dailyScrollLimit > 0 && !dailyLimitReached) {
            serviceScope.launch(Dispatchers.IO) {
                val dbCount = dao.getDailyTotals(today).first().scrollCount
                serviceScope.launch(Dispatchers.Main) {
                    if (dbCount > dailyScrollCount) dailyScrollCount = dbCount
                    if (!dailyLimitReached && dailyScrollCount + pendingScrollCount >= dailyScrollLimit) {
                        dailyLimitReached = true
                        goHome()
                    }
                }
            }
        }

        // User is already actively inside this tracked app (e.g. Instagram internal navigation)
        // → do NOT re-block mid-session, and do NOT refresh the cooldown
        if (trackedApp == packageName) return


        if (overlayView == null) {
            recordOpenAttempt(packageName)
            showCountdownOverlay(packageName)
        }
    }

    /** Start foreground time tracking for this package (idempotent). */
    private fun startTracking(packageName: String) {
        if (trackedApp == packageName) return
        trackedApp = packageName
        trackedAppStart = System.currentTimeMillis()
        pendingScrollCount = 0
        lastScrollEventMs = 0L
    }

    /** TYPE_WINDOW_CONTENT_CHANGED — debounce 3s, uniforme pour tous les apps. */
    private fun handleScrollEvent(packageName: String) {
        if (packageName != trackedApp) return
        val now = System.currentTimeMillis()
        if (now - lastScrollEventMs > SCROLL_DEBOUNCE_MS) {
            pendingScrollCount++
            lastScrollEventMs = now
        }
    }

    /** Save foreground time + scroll count to DB, then reset state. */
    private fun flushTrackedApp() {
        val pkg = trackedApp ?: return
        val start = trackedAppStart
        trackedApp = null
        trackedAppStart = 0L

        val duration = if (start > 0L) System.currentTimeMillis() - start else 0L
        val count = pendingScrollCount
        pendingScrollCount = 0

        if (duration < 1000L) return // ignore sessions shorter than 1 second

        // Update in-memory counter synchronously so handleWindowChanged sees it immediately
        dailyScrollCount += count
        if (dailyScrollLimit > 0 && dailyScrollCount >= dailyScrollLimit && !dailyLimitReached) {
            dailyLimitReached = true
            goHome()
        }

        val today = dateFormat.format(Date())
        serviceScope.launch(Dispatchers.IO) {
            ensureEntry(today, pkg)
            dao.incrementScroll(today, pkg, count, duration)
        }
    }

    private fun recordReturn(packageName: String) {
        val today = dateFormat.format(Date())
        serviceScope.launch(Dispatchers.IO) {
            ensureEntry(today, packageName)
            dao.incrementReturnCount(today, packageName)
        }
    }

    private fun recordOpenAttempt(packageName: String) {
        val today = dateFormat.format(Date())
        serviceScope.launch(Dispatchers.IO) {
            ensureEntry(today, packageName)
            dao.incrementOpenCount(today, packageName)
        }
    }

    private suspend fun ensureEntry(date: String, packageName: String) {
        val existing = dao.getForAppAndDate(date, packageName)
        if (existing == null) {
            dao.upsert(UsageStatsEntity(date = date, packageName = packageName))
        }
    }

    override fun onInterrupt() {
        flushTrackedApp()
        dismissOverlay()
    }

    override fun onDestroy() {
        flushTrackedApp()
        dismissOverlay()
        lifecycleOwner.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showCountdownOverlay(packageName: String, limitReached: Boolean = false) {
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        isCountdownActive = true
        requestAudioFocus()

        val profile = effectiveProfile
        val challengeString = if (profile.challengeEnabled) {
            BlockerPreferences.generateChallengeString(profile.challengeLength)
        } else ""

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        overlayParams = params

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)

            setContent {
                CountdownOverlayScreen(
                    appName = appName,
                    totalSeconds = profile.timerSeconds,
                    challengeEnabled = if (limitReached) false else profile.challengeEnabled,
                    challengeString = if (limitReached) "" else challengeString,
                    limitReached = limitReached,
                    onFinished = {
                        dismissOverlay()
                        startTracking(packageName)
                    },
                    onGoBack = {
                        if (!limitReached) recordReturn(packageName)
                        goBackSkipCount[packageName] = 1
                        dismissOverlay()
                        goHome()
                    },
                    onNeedKeyboard = { makeFocusable() }
                )
            }
        }

        wm.addView(composeView, params)
        overlayView = composeView
    }

    private fun makeFocusable() {
        val view = overlayView ?: return
        val params = overlayParams ?: return
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            params.flags = params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
            wm.updateViewLayout(view, params)
        } catch (_: Exception) {
        }
    }

    private fun dismissOverlay() {
        overlayView?.let { view ->
            try {
                val wm = getSystemService(WINDOW_SERVICE) as WindowManager
                wm.removeView(view)
            } catch (_: Exception) {
            }
        }
        overlayView = null
        overlayParams = null
        isCountdownActive = false
        abandonAudioFocus()
    }

    private val audioFocusListener = AudioManager.OnAudioFocusChangeListener { }
    private var audioFocusReq: Any? = null

    private fun requestAudioFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requestAudioFocusApi26(am)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun requestAudioFocusApi26(am: AudioManager) {
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
        audioFocusReq = req
        am.requestAudioFocus(req)
    }

    private fun abandonAudioFocus() {
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            abandonAudioFocusApi26(am)
        } else {
            @Suppress("DEPRECATION")
            am.abandonAudioFocus(audioFocusListener)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun abandonAudioFocusApi26(am: AudioManager) {
        audioFocusReq = null
        @Suppress("DEPRECATION")
        am.abandonAudioFocus(audioFocusListener)
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
    }
}
