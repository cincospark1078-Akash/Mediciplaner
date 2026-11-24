package com.mediciplanner.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showThemeDialog: Boolean = false,
    val showRestoreConfirmDialog: Boolean = false,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false
)

class SettingsViewModel(
    private val themePreferences: ThemePreferences? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _timeFormat = MutableStateFlow(ThemePreferences.TIME_FORMAT_12H)
    val timeFormat: StateFlow<String> = _timeFormat.asStateFlow()

    private val _uiThemeMode = MutableStateFlow(ThemePreferences.UI_THEME_LIGHT)
    val uiThemeMode: StateFlow<String> = _uiThemeMode.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    init {
        if (themePreferences != null) {
            viewModelScope.launch {
                themePreferences.timeFormatFlow.collect { format ->
                    _timeFormat.value = format
                }
            }
            viewModelScope.launch {
                themePreferences.uiThemeModeFlow.collect { mode ->
                    _uiThemeMode.value = mode
                    _isDarkMode.value = (mode == ThemePreferences.UI_THEME_DARK)
                }
            }
        }
    }

    fun updateTimeFormat(format: String) {
        val normalized = if (format == ThemePreferences.TIME_FORMAT_24H) {
            ThemePreferences.TIME_FORMAT_24H
        } else {
            ThemePreferences.TIME_FORMAT_12H
        }
        _timeFormat.value = normalized
        if (themePreferences != null) {
            viewModelScope.launch {
                themePreferences.setTimeFormat(normalized)
            }
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        val targetMode = if (enabled) {
            ThemePreferences.UI_THEME_DARK
        } else {
            ThemePreferences.UI_THEME_LIGHT
        }
        updateUiThemeMode(targetMode)
    }

    fun updateUiThemeMode(mode: String) {
        val normalized = if (mode.equals(ThemePreferences.UI_THEME_DARK, ignoreCase = true)) {
            ThemePreferences.UI_THEME_DARK
        } else {
            ThemePreferences.UI_THEME_LIGHT
        }
        _uiThemeMode.value = normalized
        _isDarkMode.value = (normalized == ThemePreferences.UI_THEME_DARK)
        if (themePreferences != null) {
            viewModelScope.launch {
                themePreferences.setUiThemeMode(normalized)
            }
        }
    }

    fun showThemeDialog(show: Boolean) {
        _uiState.update { it.copy(showThemeDialog = show) }
    }

    fun showRestoreConfirmDialog(show: Boolean) {
        _uiState.update { it.copy(showRestoreConfirmDialog = show) }
    }

    fun setBackingUp(isBackingUp: Boolean) {
        _uiState.update { it.copy(isBackingUp = isBackingUp) }
    }

    fun setRestoring(isRestoring: Boolean) {
        _uiState.update { it.copy(isRestoring = isRestoring) }
    }
}

class SettingsViewModelFactory(
    private val themePreferences: ThemePreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(themePreferences) as T
    }
}
