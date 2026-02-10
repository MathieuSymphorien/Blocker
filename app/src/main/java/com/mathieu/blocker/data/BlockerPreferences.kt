package com.mathieu.blocker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "blocker_settings")

object BlockerPreferences {

    private val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")
    private val TIMER_SECONDS = intPreferencesKey("timer_seconds")

    fun getBlockedApps(context: Context): Flow<Set<String>> {
        return context.dataStore.data.map { prefs ->
            prefs[BLOCKED_APPS] ?: emptySet()
        }
    }

    fun getTimerSeconds(context: Context): Flow<Int> {
        return context.dataStore.data.map { prefs ->
            prefs[TIMER_SECONDS] ?: 10
        }
    }

    suspend fun setBlockedApps(context: Context, apps: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[BLOCKED_APPS] = apps
        }
    }

    suspend fun toggleBlockedApp(context: Context, packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[BLOCKED_APPS] ?: emptySet()
            prefs[BLOCKED_APPS] = if (packageName in current) {
                current - packageName
            } else {
                current + packageName
            }
        }
    }

    suspend fun setTimerSeconds(context: Context, seconds: Int) {
        context.dataStore.edit { prefs ->
            prefs[TIMER_SECONDS] = seconds
        }
    }
}
