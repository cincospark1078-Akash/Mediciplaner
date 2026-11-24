package com.mediciplanner.app.core.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * MediciTheme accessor object to access design tokens and spacing cleanly in Composables.
 * Usage: MediciTheme.tokens.primary, MediciTheme.spacing.medium
 */
object MediciTheme {
    val tokens: MediciThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalMediciThemeTokens.current

    val colors: MediciThemeTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalMediciThemeTokens.current

    val spacing: MediciSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalMediciSpacing.current
}

/**
 * Main application theme wrapper for MediciPlanner.
 * Dynamically updates Material 3 ColorScheme and MediciThemeTokens based on the user's selected AppThemeMode.
 */
@Composable
fun MediciTheme(
    themeMode: AppThemeMode = AppThemeMode.NEUTRAL,
    isDarkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val tokens = if (isDarkTheme) {
        when (themeMode) {
            AppThemeMode.MALE -> MaleDarkThemeTokens
            AppThemeMode.FEMALE -> FemaleDarkThemeTokens
            AppThemeMode.NEUTRAL -> NeutralDarkThemeTokens
        }
    } else {
        when (themeMode) {
            AppThemeMode.MALE -> MaleThemeTokens
            AppThemeMode.FEMALE -> FemaleThemeTokens
            AppThemeMode.NEUTRAL -> NeutralThemeTokens
        }
    }

    val spacing = MediciSpacing()

    val colorScheme = if (isDarkTheme) {
        darkColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.onPrimaryContainer,
            secondary = tokens.primary,
            onSecondary = tokens.onPrimary,
            secondaryContainer = tokens.primaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.success,
            onTertiary = tokens.onPrimary,
            background = tokens.surface,
            onBackground = tokens.textPrimary,
            surface = tokens.cardBackground,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.primaryContainer,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.outline,
            outlineVariant = tokens.outlineVariant
        )
    } else {
        lightColorScheme(
            primary = tokens.primary,
            onPrimary = tokens.onPrimary,
            primaryContainer = tokens.primaryContainer,
            onPrimaryContainer = tokens.onPrimaryContainer,
            secondary = tokens.primary,
            onSecondary = tokens.onPrimary,
            secondaryContainer = tokens.primaryContainer,
            onSecondaryContainer = tokens.textPrimary,
            tertiary = tokens.success,
            onTertiary = tokens.onPrimary,
            background = tokens.surface,
            onBackground = tokens.textPrimary,
            surface = tokens.cardBackground,
            onSurface = tokens.textPrimary,
            surfaceVariant = tokens.primaryContainer,
            onSurfaceVariant = tokens.textSecondary,
            outline = tokens.outline,
            outlineVariant = tokens.outlineVariant
        )
    }

    CompositionLocalProvider(
        LocalMediciThemeTokens provides tokens,
        LocalMediciSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MediciTypography,
            shapes = MediciShapes,
            content = content
        )
    }
}
