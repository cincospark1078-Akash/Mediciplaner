package com.mediciplanner.app.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.FemaleThemeTokens
import com.mediciplanner.app.core.theme.MaleThemeTokens
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.theme.MediciThemeTokens
import com.mediciplanner.app.core.theme.NeutralThemeTokens
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.ui.components.BouncingButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import androidx.compose.material3.TextButton

/**
 * ViewModel for managing ThemePicker state and persisting selection to DataStore.
 */
class ThemePickerViewModel(
    private val themePreferences: ThemePreferences,
    initialTheme: AppThemeMode = AppThemeMode.NEUTRAL
) : ViewModel() {

    private val _selectedTheme = MutableStateFlow(initialTheme)
    val selectedTheme: StateFlow<AppThemeMode> = _selectedTheme.asStateFlow()

    fun selectTheme(mode: AppThemeMode) {
        _selectedTheme.value = mode
    }

    fun saveTheme(onComplete: () -> Unit) {
        viewModelScope.launch {
            themePreferences.setThemeMode(_selectedTheme.value)
            themePreferences.setFirstLaunch(false)
            themePreferences.setOnboardingCompleted(true)
            onComplete()
        }
    }
}

@Composable
fun ThemePickerScreen(
    currentTheme: AppThemeMode,
    onThemeConfirmed: (AppThemeMode) -> Unit,
    onSaveAndContinue: (AppThemeMode) -> Unit,
    onSkip: () -> Unit = { onSaveAndContinue(currentTheme) },
    showSkipButton: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer,
            MediciTheme.colors.surface
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = MediciTheme.spacing.medium)
                .testTag("theme_picker_screen"),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
            ) {
                // Header with optional Skip Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = MediciTheme.spacing.medium, bottom = MediciTheme.spacing.small),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.theme_picker_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(MediciTheme.spacing.extraSmall))
                        Text(
                            text = stringResource(R.string.theme_picker_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MediciTheme.colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (showSkipButton) {
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.testTag("theme_picker_skip_button")
                        ) {
                            Text(
                                text = stringResource(R.string.tutorial_skip),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Neutral Theme Option (Muted Grayish Teal)
                ThemeOptionCard(
                    mode = AppThemeMode.NEUTRAL,
                    title = stringResource(R.string.theme_neutral_title),
                    description = stringResource(R.string.theme_neutral_desc),
                    tokens = NeutralThemeTokens,
                    isSelected = currentTheme == AppThemeMode.NEUTRAL,
                    onClick = { onThemeConfirmed(AppThemeMode.NEUTRAL) },
                    testTag = "theme_card_neutral"
                )

                // Male Theme Option (Soft Sky Blue)
                ThemeOptionCard(
                    mode = AppThemeMode.MALE,
                    title = stringResource(R.string.theme_male_title),
                    description = stringResource(R.string.theme_male_desc),
                    tokens = MaleThemeTokens,
                    isSelected = currentTheme == AppThemeMode.MALE,
                    onClick = { onThemeConfirmed(AppThemeMode.MALE) },
                    testTag = "theme_card_male"
                )

                // Female Theme Option (Soft Pastel Lavender)
                ThemeOptionCard(
                    mode = AppThemeMode.FEMALE,
                    title = stringResource(R.string.theme_female_title),
                    description = stringResource(R.string.theme_female_desc),
                    tokens = FemaleThemeTokens,
                    isSelected = currentTheme == AppThemeMode.FEMALE,
                    onClick = { onThemeConfirmed(AppThemeMode.FEMALE) },
                    testTag = "theme_card_female"
                )

                Spacer(modifier = Modifier.height(MediciTheme.spacing.medium))
            }

            // Bottom CTA Button: Locked 20.dp shape
            BouncingButton(
                onClick = { onSaveAndContinue(currentTheme) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MediciTheme.colors.primary,
                    contentColor = MediciTheme.colors.onPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = MediciTheme.spacing.medium)
                    .height(50.dp)
                    .testTag("theme_picker_continue_button")
            ) {
                Text(
                    text = stringResource(R.string.theme_continue_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(
    mode: AppThemeMode,
    title: String,
    description: String,
    tokens: MediciThemeTokens,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) tokens.primary else MediciTheme.colors.outlineVariant,
        label = "card_border"
    )

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = cardShape)
            .clip(cardShape)
            .background(tokens.cardBackground)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = cardShape
            )
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = tokens.primary,
                            unselectedColor = tokens.textSecondary
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(tokens.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.theme_current_tag),
                            tint = tokens.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = tokens.textSecondary,
                modifier = Modifier.padding(start = 32.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Token Swatches Preview showing soft pastel palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, top = MediciTheme.spacing.extraSmall),
                horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ColorSwatch(label = stringResource(R.string.swatch_primary_bn), color = tokens.primary)
                ColorSwatch(label = stringResource(R.string.swatch_container_bn), color = tokens.primaryContainer)
                ColorSwatch(label = stringResource(R.string.swatch_surface_bn), color = tokens.surface)
                ColorSwatch(label = stringResource(R.string.swatch_text_bn), color = tokens.textPrimary)
            }
        }
    }
}

@Composable
private fun ColorSwatch(
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MediciTheme.colors.outline.copy(alpha = 0.15f), CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MediciTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
