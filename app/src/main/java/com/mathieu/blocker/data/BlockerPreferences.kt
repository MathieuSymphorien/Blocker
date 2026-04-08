package com.mathieu.blocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blocker_settings")

object BlockerPreferences {

    // Legacy
    private val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")

    // Profiles
    private val PROFILES_JSON = stringPreferencesKey("profiles_json")
    private val ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")

    // Planner
    private val PLANNER_JSON = stringPreferencesKey("planner_json")

    // Daily limits (global)
    private val DAILY_SCROLL_LIMIT = intPreferencesKey("daily_scroll_limit")
    private val DAILY_TIME_LIMIT_MS = longPreferencesKey("daily_time_limit_ms")

    // First launch
    private val HAS_LAUNCHED = booleanPreferencesKey("has_launched")

    private val gson = Gson()
    private const val DEFAULT_PROFILE_ID = "default_home"

    private fun defaultProfile(): Profile = Profile(
        id = DEFAULT_PROFILE_ID,
        name = "Maison",
        blockedApps = emptySet(),
        timerSeconds = 10,
        challengeEnabled = false,
        challengeLength = 12
    )

    // ---- Profiles ----

    fun getProfiles(context: Context): Flow<List<Profile>> {
        return context.dataStore.data.map { prefs -> parseProfiles(prefs) }
    }

    fun getActiveProfileId(context: Context): Flow<String> {
        return context.dataStore.data.map { prefs -> prefs[ACTIVE_PROFILE_ID] ?: DEFAULT_PROFILE_ID }
    }

    fun getActiveProfile(context: Context): Flow<Profile> {
        return context.dataStore.data.map { prefs -> resolveActiveProfile(prefs) }
    }

    /**
     * Returns the effective profile: if a planner entry is active now, use that profile.
     * Otherwise use the manually selected active profile.
     */
    fun getEffectiveProfile(context: Context): Flow<Profile> {
        return context.dataStore.data.map { prefs ->
            val profiles = parseProfiles(prefs)
            val plannerEntries = parsePlanner(prefs)

            // Check if a planner entry is active
            val activeEntry = plannerEntries.firstOrNull { it.isActiveNow() }
            if (activeEntry != null) {
                profiles.find { it.id == activeEntry.profileId } ?: resolveActiveProfile(prefs)
            } else {
                resolveActiveProfile(prefs)
            }
        }
    }

    private fun resolveActiveProfile(prefs: Preferences): Profile {
        val activeId = prefs[ACTIVE_PROFILE_ID] ?: DEFAULT_PROFILE_ID
        val profiles = parseProfiles(prefs)
        return profiles.find { it.id == activeId } ?: profiles.firstOrNull() ?: defaultProfile()
    }

    suspend fun setActiveProfile(context: Context, profileId: String) {
        context.dataStore.edit { prefs ->
            prefs[ACTIVE_PROFILE_ID] = profileId
            val profiles = parseProfiles(prefs)
            val profile = profiles.find { it.id == profileId }
            if (profile != null) {
                prefs[BLOCKED_APPS] = profile.blockedApps
            }
        }
    }

    suspend fun addProfile(context: Context, profile: Profile) {
        context.dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs).toMutableList()
            profiles.add(profile)
            prefs[PROFILES_JSON] = gson.toJson(profiles)
        }
    }

    suspend fun deleteProfile(context: Context, profileId: String) {
        if (profileId == DEFAULT_PROFILE_ID) return
        context.dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs).toMutableList()
            profiles.removeAll { it.id == profileId }
            prefs[PROFILES_JSON] = gson.toJson(profiles)
            // Also remove planner entries for this profile
            val planner = parsePlanner(prefs).toMutableList()
            planner.removeAll { it.profileId == profileId }
            prefs[PLANNER_JSON] = gson.toJson(planner)
            if (prefs[ACTIVE_PROFILE_ID] == profileId) {
                prefs[ACTIVE_PROFILE_ID] = DEFAULT_PROFILE_ID
            }
        }
    }

    suspend fun updateProfile(context: Context, profile: Profile) {
        context.dataStore.edit { prefs ->
            val profiles = parseProfiles(prefs).toMutableList()
            val index = profiles.indexOfFirst { it.id == profile.id }
            if (index >= 0) {
                profiles[index] = profile
                prefs[PROFILES_JSON] = gson.toJson(profiles)
                // Update legacy key if this is the active profile
                if (prefs[ACTIVE_PROFILE_ID] == profile.id || (prefs[ACTIVE_PROFILE_ID] == null && profile.id == DEFAULT_PROFILE_ID)) {
                    prefs[BLOCKED_APPS] = profile.blockedApps
                }
            }
        }
    }

    // ---- Blocked Apps (from effective profile) ----

    fun getBlockedApps(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            val profiles = parseProfiles(prefs)
            val plannerEntries = parsePlanner(prefs)
            val activeEntry = plannerEntries.firstOrNull { it.isActiveNow() }
            val profile = if (activeEntry != null) {
                profiles.find { it.id == activeEntry.profileId } ?: resolveActiveProfile(prefs)
            } else {
                resolveActiveProfile(prefs)
            }
            profile.blockedApps
        }
    }

    suspend fun toggleBlockedApp(context: Context, packageName: String) {
        context.dataStore.edit { prefs ->
            val activeId = prefs[ACTIVE_PROFILE_ID] ?: DEFAULT_PROFILE_ID
            val profiles = parseProfiles(prefs).toMutableList()
            val index = profiles.indexOfFirst { it.id == activeId }
            if (index >= 0) {
                val current = profiles[index].blockedApps
                val updated = if (packageName in current) current - packageName else current + packageName
                profiles[index] = profiles[index].copy(blockedApps = updated)
                prefs[PROFILES_JSON] = gson.toJson(profiles)
                prefs[BLOCKED_APPS] = updated
            } else {
                // No profile found, create default with this app
                val oldApps = prefs[BLOCKED_APPS] ?: emptySet()
                val updated = if (packageName in oldApps) oldApps - packageName else oldApps + packageName
                val newProfiles = listOf(defaultProfile().copy(blockedApps = updated))
                prefs[PROFILES_JSON] = gson.toJson(newProfiles)
                prefs[BLOCKED_APPS] = updated
            }
        }
    }

    // ---- Planner ----

    fun getPlanner(context: Context): Flow<List<PlannerEntry>> {
        return context.dataStore.data.map { prefs -> parsePlanner(prefs) }
    }

    suspend fun addPlannerEntry(context: Context, entry: PlannerEntry) {
        context.dataStore.edit { prefs ->
            val planner = parsePlanner(prefs).toMutableList()
            planner.add(entry)
            prefs[PLANNER_JSON] = gson.toJson(planner)
        }
    }

    suspend fun updatePlannerEntry(context: Context, entry: PlannerEntry) {
        context.dataStore.edit { prefs ->
            val planner = parsePlanner(prefs).toMutableList()
            val index = planner.indexOfFirst { it.id == entry.id }
            if (index >= 0) {
                planner[index] = entry
                prefs[PLANNER_JSON] = gson.toJson(planner)
            }
        }
    }

    suspend fun removePlannerEntry(context: Context, entryId: String) {
        context.dataStore.edit { prefs ->
            val planner = parsePlanner(prefs).toMutableList()
            planner.removeAll { it.id == entryId }
            prefs[PLANNER_JSON] = gson.toJson(planner)
        }
    }

    // ---- Daily Limit (global) ----

    fun getDailyScrollLimit(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs -> prefs[DAILY_SCROLL_LIMIT] ?: 0 }
    }

    suspend fun setDailyScrollLimit(context: Context, limit: Int) {
        context.dataStore.edit { prefs -> prefs[DAILY_SCROLL_LIMIT] = limit }
    }

    fun getDailyTimeLimit(context: Context): Flow<Long> {
        return context.dataStore.data.map { prefs -> prefs[DAILY_TIME_LIMIT_MS] ?: 0L }
    }

    suspend fun setDailyTimeLimit(context: Context, limitMs: Long) {
        context.dataStore.edit { prefs -> prefs[DAILY_TIME_LIMIT_MS] = limitMs }
    }

    // ---- First Launch ----

    fun isFirstLaunch(context: Context): Flow<Boolean> {
        return context.dataStore.data.map { prefs -> prefs[HAS_LAUNCHED] != true }
    }

    suspend fun markLaunched(context: Context) {
        context.dataStore.edit { prefs -> prefs[HAS_LAUNCHED] = true }
    }

    // ---- Utilities ----

    fun generateChallengeString(length: Int = 12): String {
        val chars = "aAbBcCdDeEfFgGhHiIjJkKlLmMnNoOpPqQrRsStTuUvVwWxXyYzZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }

    private fun parseProfiles(prefs: Preferences): List<Profile> {
        val json = prefs[PROFILES_JSON]
        return if (json != null) {
            try {
                val profiles: List<Profile> = gson.fromJson(json, object : TypeToken<List<Profile>>() {}.type)
                if (profiles.isEmpty()) listOf(defaultProfile()) else profiles
            } catch (_: Exception) {
                listOf(defaultProfile().copy(blockedApps = prefs[BLOCKED_APPS] ?: emptySet()))
            }
        } else {
            listOf(defaultProfile().copy(blockedApps = prefs[BLOCKED_APPS] ?: emptySet()))
        }
    }

    private fun parsePlanner(prefs: Preferences): List<PlannerEntry> {
        val json = prefs[PLANNER_JSON] ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<PlannerEntry>>() {}.type)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
