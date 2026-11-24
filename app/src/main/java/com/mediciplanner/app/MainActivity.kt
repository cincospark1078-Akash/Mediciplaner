package com.mediciplanner.app

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.notification.ReminderConstants
import com.mediciplanner.app.ui.navigation.AppNavigation
import com.mediciplanner.app.ui.navigation.AppRoutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MainViewModel(
    private val themePreferences: ThemePreferences
) : ViewModel() {

    val persistedTheme: StateFlow<AppThemeMode> = themePreferences.themeModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.NEUTRAL
        )

    val isFirstLaunch: StateFlow<Boolean?> = themePreferences.isFirstLaunchFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val isOnboardingCompleted: StateFlow<Boolean> = themePreferences.isOnboardingCompletedFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val isDarkTheme: StateFlow<Boolean> = themePreferences.isDarkModeFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _previewTheme = MutableStateFlow<AppThemeMode?>(null)
    val previewTheme: StateFlow<AppThemeMode?> = _previewTheme.asStateFlow()

    fun previewTheme(mode: AppThemeMode) {
        _previewTheme.value = mode
    }

    fun saveTheme(mode: AppThemeMode) {
        _previewTheme.value = mode
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
            themePreferences.setFirstLaunch(false)
            themePreferences.setOnboardingCompleted(true)
        }
    }
}

typealias ThemeViewModel = MainViewModel

class MainViewModelFactory(
    private val themePreferences: ThemePreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainViewModel(themePreferences) as T
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val app = application as MediciApplication
        MainViewModelFactory(app.themePreferences)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val locale = java.util.Locale("bn")
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS permission on Android 13+
        requestNotificationPermissionIfNeeded()

        // Handle notification tap action (marks reminder as Taken in Room DB)
        val isFromNotification = handleNotificationTap(intent)

        setContent {
            val persistedMode by viewModel.persistedTheme.collectAsStateWithLifecycle()
            val previewMode by viewModel.previewTheme.collectAsStateWithLifecycle()
            val isFirstLaunch by viewModel.isFirstLaunch.collectAsStateWithLifecycle()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            val activeTheme = previewMode ?: persistedMode

            MediciTheme(themeMode = activeTheme, isDarkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MediciTheme.tokens.surface
                ) {
                    AppNavigation(
                        currentTheme = activeTheme,
                        isFirstLaunch = isFirstLaunch,
                        onThemeSelected = { newMode ->
                            viewModel.previewTheme(newMode)
                        },
                        onSaveThemePermanently = { confirmedMode ->
                            viewModel.saveTheme(confirmedMode)
                        },
                        startDestination = if (isFromNotification) AppRoutes.HOME else AppRoutes.SPLASH
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationTap(intent)
    }

    /**
     * CRITICAL REQUIREMENT:
     * When user taps the main body of the notification, the app opens directly to Home Screen
     * and immediately marks that specific reminder as "Taken" in the Room Database.
     */
    private fun handleNotificationTap(intent: Intent?): Boolean {
        if (intent == null) return false

        val reminderId = intent.getStringExtra(ReminderConstants.EXTRA_REMINDER_ID)
        val type = intent.getStringExtra("notification_type") ?: "medicine"
        val isTapAction = intent.action == ReminderConstants.ACTION_NOTIFICATION_TAP || !reminderId.isNullOrBlank()

        if (isTapAction && !reminderId.isNullOrBlank()) {
            val notificationId = intent.getIntExtra(ReminderConstants.EXTRA_NOTIFICATION_ID, reminderId.hashCode())

            // 1. Dismiss notification
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notificationId)

            // 2. If medicine reminder, immediately mark as TAKEN in Room Database and cancel alarm
            if (type == "medicine") {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val app = application as MediciApplication
                        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        app.reminderRepository.markAsTaken(reminderId, nowIso)
                        app.reminderScheduler.cancelReminder(reminderId)
                        Log.d("MainActivity", "Notification body tap: Marked $reminderId as TAKEN in database")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error marking reminder as taken on notification tap", e)
                    }
                }
            }
            return true
        }
        return false
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permission), 1001)
            }
        }
    }
}
