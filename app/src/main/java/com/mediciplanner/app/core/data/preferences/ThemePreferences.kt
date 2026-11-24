package com.mediciplanner.app.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mediciplanner.app.core.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.mediciDataStore: DataStore<Preferences> by preferencesDataStore(name = "medici_planner_prefs")

class ThemePreferences(private val context: Context) {

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("selected_theme_mode")
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_USER_NAME = stringPreferencesKey("userName")
        val KEY_USER_MOBILE = stringPreferencesKey("userMobile")
        val KEY_USER_EMAIL = stringPreferencesKey("userEmail")
        val KEY_USER_ADDRESS = stringPreferencesKey("userAddress")
        val KEY_USER_PROFILE_PIC_PATH = stringPreferencesKey("userProfilePicPath")
        val KEY_GREETING_PREFERENCE = stringPreferencesKey("greetingPreference")
        val KEY_CUSTOM_GREETING = stringPreferencesKey("customGreeting")
        val KEY_THEME_GENDER = stringPreferencesKey("themeGender")
        val KEY_TIME_FORMAT = stringPreferencesKey("timeFormat")
        val KEY_MEDICINE_REMINDERS_ENABLED = booleanPreferencesKey("medicineRemindersEnabled")
        val KEY_FOLLOWUP_REMINDERS_ENABLED = booleanPreferencesKey("followupRemindersEnabled")
        val KEY_NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notificationSoundEnabled")
        val KEY_DARK_MODE = booleanPreferencesKey("darkModeEnabled")
        val KEY_UI_THEME_MODE = stringPreferencesKey("themeMode")
        const val TIME_FORMAT_12H = "12H"
        const val TIME_FORMAT_24H = "24H"
        const val UI_THEME_LIGHT = "light"
        const val UI_THEME_DARK = "dark"
    }

    val userNameFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_NAME] ?: ""
        }

    val userMobileFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_MOBILE] ?: ""
        }

    val userEmailFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_EMAIL] ?: ""
        }

    val userAddressFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_ADDRESS] ?: ""
        }

    val userProfilePicPathFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_USER_PROFILE_PIC_PATH] ?: ""
        }

    val greetingPreferenceFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_GREETING_PREFERENCE] ?: "universal"
        }

    val customGreetingFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_CUSTOM_GREETING] ?: ""
        }

    val themeGenderFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_THEME_GENDER] ?: "neutral"
        }

    val timeFormatFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_TIME_FORMAT] ?: TIME_FORMAT_12H
        }

    val medicineRemindersEnabledFlow: Flow<Boolean> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_MEDICINE_REMINDERS_ENABLED] ?: true
        }

    val followupRemindersEnabledFlow: Flow<Boolean> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_FOLLOWUP_REMINDERS_ENABLED] ?: true
        }

    val notificationSoundEnabledFlow: Flow<Boolean> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_NOTIFICATION_SOUND_ENABLED] ?: true
        }

    val uiThemeModeFlow: Flow<String> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val mode = preferences[KEY_UI_THEME_MODE]
            when {
                mode.equals(UI_THEME_DARK, ignoreCase = true) -> UI_THEME_DARK
                mode.equals(UI_THEME_LIGHT, ignoreCase = true) -> UI_THEME_LIGHT
                preferences[KEY_DARK_MODE] == true -> UI_THEME_DARK
                else -> UI_THEME_LIGHT
            }
        }

    val isDarkModeFlow: Flow<Boolean> = uiThemeModeFlow.map { mode ->
        mode == UI_THEME_DARK
    }

    val themeModeFlow: Flow<AppThemeMode> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeKey = preferences[KEY_THEME_MODE] ?: preferences[KEY_THEME_GENDER]
            AppThemeMode.fromKey(modeKey)
        }

    val isFirstLaunchFlow: Flow<Boolean> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] ?: !(preferences[KEY_ONBOARDING_COMPLETED] ?: false)
        }

    val isOnboardingCompletedFlow: Flow<Boolean> = context.mediciDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: !(preferences[KEY_IS_FIRST_LAUNCH] ?: true)
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = mode.key
            preferences[KEY_THEME_GENDER] = mode.key
        }
    }

    suspend fun savePersonalization(
        userName: String,
        greetingPreference: String,
        themeGender: String,
        customGreeting: String = "",
        userMobile: String = "",
        userEmail: String = "",
        userAddress: String = "",
        userProfilePicPath: String? = null
    ) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = userName
            preferences[KEY_USER_MOBILE] = userMobile
            preferences[KEY_USER_EMAIL] = userEmail
            preferences[KEY_USER_ADDRESS] = userAddress
            if (userProfilePicPath != null) {
                preferences[KEY_USER_PROFILE_PIC_PATH] = userProfilePicPath
            }
            preferences[KEY_GREETING_PREFERENCE] = greetingPreference
            preferences[KEY_CUSTOM_GREETING] = customGreeting.trim().take(30)
            preferences[KEY_THEME_GENDER] = themeGender
            val mode = when (themeGender.lowercase()) {
                "male" -> AppThemeMode.MALE
                "female" -> AppThemeMode.FEMALE
                else -> AppThemeMode.NEUTRAL
            }
            preferences[KEY_THEME_MODE] = mode.key
        }
    }

    suspend fun setUserName(name: String) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
        }
    }

    suspend fun setUserProfilePicPath(path: String) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_USER_PROFILE_PIC_PATH] = path
        }
    }

    suspend fun setGreetingPreference(preference: String) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_GREETING_PREFERENCE] = preference
        }
    }

    suspend fun setCustomGreeting(greeting: String) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_CUSTOM_GREETING] = greeting.trim().take(30)
        }
    }

    suspend fun setThemeGender(themeGender: String) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_THEME_GENDER] = themeGender
            val mode = when (themeGender.lowercase()) {
                "male" -> AppThemeMode.MALE
                "female" -> AppThemeMode.FEMALE
                else -> AppThemeMode.NEUTRAL
            }
            preferences[KEY_THEME_MODE] = mode.key
        }
    }

    suspend fun setFirstLaunch(isFirstLaunch: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_IS_FIRST_LAUNCH] = isFirstLaunch
            preferences[KEY_ONBOARDING_COMPLETED] = !isFirstLaunch
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
            preferences[KEY_IS_FIRST_LAUNCH] = !completed
        }
    }

    suspend fun setTimeFormat(format: String) {
        val normalized = if (format == TIME_FORMAT_24H) TIME_FORMAT_24H else TIME_FORMAT_12H
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_TIME_FORMAT] = normalized
        }
    }

    suspend fun setMedicineRemindersEnabled(enabled: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_MEDICINE_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setFollowupRemindersEnabled(enabled: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_FOLLOWUP_REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_NOTIFICATION_SOUND_ENABLED] = enabled
        }
    }

    suspend fun setUiThemeMode(mode: String) {
        val normalized = if (mode.equals(UI_THEME_DARK, ignoreCase = true)) {
            UI_THEME_DARK
        } else {
            UI_THEME_LIGHT
        }
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_UI_THEME_MODE] = normalized
            preferences[KEY_DARK_MODE] = (normalized == UI_THEME_DARK)
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.mediciDataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = enabled
            preferences[KEY_UI_THEME_MODE] = if (enabled) UI_THEME_DARK else UI_THEME_LIGHT
        }
    }
}
