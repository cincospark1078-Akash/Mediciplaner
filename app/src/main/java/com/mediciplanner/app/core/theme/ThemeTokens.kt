package com.mediciplanner.app.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Supported gender/preference-based visual theme modes for MediciPlanner.
 */
enum class AppThemeMode(val key: String) {
    MALE("male"),
    FEMALE("female"),
    NEUTRAL("neutral");

    companion object {
        fun fromKey(key: String?): AppThemeMode {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: NEUTRAL
        }
    }
}

/**
 * Immutable theme token bundle providing consistent design tokens across the application.
 */
@Immutable
data class MediciThemeTokens(
    val primary: Color,
    val primaryContainer: Color,
    val surface: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color = SuccessGreen,
    val warning: Color = WarningAmber,
    val cardBackground: Color = CardWhite,
    val onPrimary: Color = Color.White,
    val onPrimaryContainer: Color = textPrimary,
    val outline: Color = textSecondary.copy(alpha = 0.24f),
    val outlineVariant: Color = textSecondary.copy(alpha = 0.12f),
    val maleSwatch: Color = MalePreviewSwatch,
    val femaleSwatch: Color = FemalePreviewSwatch,
    val neutralSwatch: Color = NeutralPreviewSwatch,
    val isDark: Boolean = false
) {
    val card: Color
        get() = cardBackground
}


val MaleThemeTokens = MediciThemeTokens(
    primary = MalePrimary,
    primaryContainer = MalePrimaryContainer,
    surface = MaleSurface,
    textPrimary = MaleTextPrimary,
    textSecondary = MaleTextSecondary
)

val FemaleThemeTokens = MediciThemeTokens(
    primary = FemalePrimary,
    primaryContainer = FemalePrimaryContainer,
    surface = FemaleSurface,
    textPrimary = FemaleTextPrimary,
    textSecondary = FemaleTextSecondary
)

val NeutralThemeTokens = MediciThemeTokens(
    primary = NeutralPrimary,
    primaryContainer = NeutralPrimaryContainer,
    surface = NeutralSurface,
    textPrimary = NeutralTextPrimary,
    textSecondary = NeutralTextSecondary
)

val MaleDarkThemeTokens = MediciThemeTokens(
    primary = MalePrimary,
    primaryContainer = MaleDarkPrimaryContainer,
    surface = DarkSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    cardBackground = DarkCardBackground,
    isDark = true
)

val FemaleDarkThemeTokens = MediciThemeTokens(
    primary = FemalePrimary,
    primaryContainer = FemaleDarkPrimaryContainer,
    surface = DarkSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    cardBackground = DarkCardBackground,
    isDark = true
)

val NeutralDarkThemeTokens = MediciThemeTokens(
    primary = NeutralPrimary,
    primaryContainer = NeutralDarkPrimaryContainer,
    surface = DarkSurface,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    cardBackground = DarkCardBackground,
    isDark = true
)

/**
 * CompositionLocal provider for MediciThemeTokens.
 */
val LocalMediciThemeTokens = staticCompositionLocalOf { NeutralThemeTokens }

typealias MediciTokens = MediciThemeTokens
val LocalMediciTokens = LocalMediciThemeTokens
