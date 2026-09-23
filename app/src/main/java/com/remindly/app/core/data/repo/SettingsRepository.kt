package com.remindly.app.core.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "remindly_settings")

/**
 * App settings + onboarding/auth flags (PRD §12 guest mode, §16 profile,
 * §59 timezone mode, §68 guest architecture).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
        val SIGNED_IN = booleanPreferencesKey("signed_in")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val AUTH_PROVIDER = stringPreferencesKey("auth_provider") // guest|google|email
        val PERMISSIONS_DONE = booleanPreferencesKey("permissions_done")
        val DEFAULT_REMINDER_MINUTES = intPreferencesKey("default_reminder_minutes")
        val THEME_MODE = stringPreferencesKey("theme_mode") // system|light|dark
        val FOLLOW_DEVICE_TZ = booleanPreferencesKey("follow_device_tz")
        val SAVED_ZONE = stringPreferencesKey("saved_zone")
        val LOCAL_USER_ID = stringPreferencesKey("local_user_id")
        val LAUNCH_COUNT = intPreferencesKey("launch_count")
        val LAST_SYNC = longPreferencesKey("last_sync")
    }

    data class Settings(
        val onboardingDone: Boolean,
        val guestMode: Boolean,
        val signedIn: Boolean,
        val userName: String,
        val permissionsDone: Boolean,
        val defaultReminderMinutes: Int,
        val themeMode: String,
        val followDeviceTz: Boolean,
        val authProvider: String,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            onboardingDone = p[Keys.ONBOARDING_DONE] ?: false,
            guestMode = p[Keys.GUEST_MODE] ?: false,
            signedIn = p[Keys.SIGNED_IN] ?: false,
            userName = p[Keys.USER_NAME] ?: "",
            permissionsDone = p[Keys.PERMISSIONS_DONE] ?: false,
            defaultReminderMinutes = p[Keys.DEFAULT_REMINDER_MINUTES] ?: 21 * 60,
            themeMode = p[Keys.THEME_MODE] ?: "system",
            followDeviceTz = p[Keys.FOLLOW_DEVICE_TZ] ?: true,
            authProvider = p[Keys.AUTH_PROVIDER] ?: "guest",
        )
    }

    suspend fun current(): Settings = settings.first()

    suspend fun markOnboardingDone() = context.dataStore.edit {
        it[Keys.ONBOARDING_DONE] = true
    }

    suspend fun continueAsGuest(name: String = "there") = context.dataStore.edit {
        it[Keys.GUEST_MODE] = true
        it[Keys.SIGNED_IN] = false
        it[Keys.AUTH_PROVIDER] = "guest"
        it[Keys.USER_NAME] = name
        if (it[Keys.LOCAL_USER_ID] == null) {
            it[Keys.LOCAL_USER_ID] = java.util.UUID.randomUUID().toString()
        }
    }

    /** PRD §67 — linking keeps local data; we only flip identity flags. */
    suspend fun signIn(provider: String, name: String, email: String? = null) = context.dataStore.edit {
        it[Keys.GUEST_MODE] = false
        it[Keys.SIGNED_IN] = true
        it[Keys.AUTH_PROVIDER] = provider
        it[Keys.USER_NAME] = name
        email?.let { e -> it[Keys.USER_EMAIL] = e }
    }

    suspend fun completeProfile(name: String, defaultMinutes: Int) = context.dataStore.edit {
        it[Keys.USER_NAME] = name
        it[Keys.DEFAULT_REMINDER_MINUTES] = defaultMinutes
    }

    suspend fun markPermissionsDone() = context.dataStore.edit {
        it[Keys.PERMISSIONS_DONE] = true
    }

    suspend fun setThemeMode(mode: String) = context.dataStore.edit {
        it[Keys.THEME_MODE] = mode
    }

    suspend fun setFollowDeviceTz(follow: Boolean) = context.dataStore.edit {
        it[Keys.FOLLOW_DEVICE_TZ] = follow
        it[Keys.SAVED_ZONE] = java.util.TimeZone.getDefault().id
    }

    suspend fun bumpLaunchCount(): Int {
        var count = 0
        context.dataStore.edit {
            count = (it[Keys.LAUNCH_COUNT] ?: 0) + 1
            it[Keys.LAUNCH_COUNT] = count
        }
        return count
    }

    suspend fun logout() = context.dataStore.edit {
        it[Keys.SIGNED_IN] = false
        it[Keys.GUEST_MODE] = true
        it[Keys.AUTH_PROVIDER] = "guest"
    }
}
