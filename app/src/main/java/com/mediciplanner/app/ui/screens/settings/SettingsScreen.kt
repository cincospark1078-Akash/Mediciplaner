package com.mediciplanner.app.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediciplanner.app.R
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.backup.BackupManager
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.BouncingOutlinedButton
import com.mediciplanner.app.ui.components.FrostedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onSaveThemePermanently: (AppThemeMode) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(ThemePreferences(LocalContext.current.applicationContext))
    )
) {
    BackHandler {
        onNavigateBack()
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val uiState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val timeFormat by settingsViewModel.timeFormat.collectAsStateWithLifecycle()
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()

    // Activity launcher for choosing backup zip document to restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(
                context,
                context.getString(R.string.settings_restore_in_progress_toast),
                Toast.LENGTH_SHORT
            ).show()

            settingsViewModel.setRestoring(true)
            scope.launch(Dispatchers.IO) {
                val result = BackupManager.restoreData(context, uri)
                withContext(Dispatchers.Main) {
                    settingsViewModel.setRestoring(false)
                    if (result.isSuccess) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.msg_settings_restore_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        BackupManager.restartApp(context)
                    } else {
                        Toast.makeText(
                            context,
                            context.getString(R.string.settings_restore_error_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer.copy(alpha = 0.45f),
            MediciTheme.colors.surface,
            MediciTheme.colors.surface
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .testTag("settings_screen"),
        containerColor = MediciTheme.colors.surface.copy(alpha = 0f),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_desc),
                            tint = MediciTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MediciTheme.colors.surface.copy(alpha = 0f)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = MediciTheme.spacing.medium)
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            // 1. THEME SELECTION OPTION
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_theme_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MediciTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .clickable { settingsViewModel.showThemeDialog(true) }
                            .testTag("theme_setting_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MediciTheme.colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_theme_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.settings_theme_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Button showing current theme (পুরুষ / নারী / নিরপেক্ষ)
                        val currentThemeLabel = when (currentTheme) {
                            AppThemeMode.MALE -> stringResource(R.string.settings_theme_male)
                            AppThemeMode.FEMALE -> stringResource(R.string.settings_theme_female)
                            AppThemeMode.NEUTRAL -> stringResource(R.string.settings_theme_neutral)
                        }

                        BouncingButton(
                            onClick = { settingsViewModel.showThemeDialog(true) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediciTheme.colors.primaryContainer,
                                contentColor = MediciTheme.colors.primary
                            ),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("theme_change_button")
                        ) {
                            Text(
                                text = currentThemeLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 1B. DARK MODE TOGGLE
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_dark_mode_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MediciTheme.spacing.medium)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("dark_mode_setting_row"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MediciTheme.colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_dark_mode_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.settings_dark_mode_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { enabled ->
                            settingsViewModel.updateDarkMode(enabled)
                            Toast.makeText(
                                context,
                                context.getString(
                                    if (enabled) R.string.msg_settings_dark_mode_on
                                    else R.string.msg_settings_dark_mode_off
                                ),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MediciTheme.colors.onPrimary,
                            checkedTrackColor = MediciTheme.colors.primary
                        ),
                        modifier = Modifier.testTag("settings_dark_mode_switch")
                    )
                }
            }

            // 2. APP LANGUAGE OPTION
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_language_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MediciTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("language_setting_row"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MediciTheme.colors.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_language_label),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(R.string.settings_language_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Language chips: Bengali active, English with "শীঘ্রই আসছে"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MediciTheme.colors.primary.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_language_bn),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MediciTheme.colors.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_language_en),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MediciTheme.colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MediciTheme.colors.warning.copy(alpha = 0.18f),
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.settings_coming_soon),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.SemiBold,
                                        color = MediciTheme.colors.warning,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. TIME FORMAT OPTION (12H / 24H)
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_time_format_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MediciTheme.spacing.medium)
                        .testTag("time_format_setting_row"),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MediciTheme.colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_time_format_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.settings_time_format_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val is24Hour = timeFormat == ThemePreferences.TIME_FORMAT_24H
                        FilterChip(
                            selected = !is24Hour,
                            onClick = {
                                if (is24Hour) {
                                    settingsViewModel.updateTimeFormat(ThemePreferences.TIME_FORMAT_12H)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.msg_settings_time_format_updated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.settings_time_format_12h),
                                    fontWeight = if (!is24Hour) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MediciTheme.colors.primary,
                                selectedLabelColor = MediciTheme.colors.onPrimary,
                                containerColor = MediciTheme.colors.surface,
                                labelColor = MediciTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("time_format_chip_12h")
                        )

                        FilterChip(
                            selected = is24Hour,
                            onClick = {
                                if (!is24Hour) {
                                    settingsViewModel.updateTimeFormat(ThemePreferences.TIME_FORMAT_24H)
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.msg_settings_time_format_updated),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.settings_time_format_24h),
                                    fontWeight = if (is24Hour) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MediciTheme.colors.primary,
                                selectedLabelColor = MediciTheme.colors.onPrimary,
                                containerColor = MediciTheme.colors.surface,
                                labelColor = MediciTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("time_format_chip_24h")
                        )
                    }
                }
            }

            // 4. DATA BACKUP & RESTORE SECTION
            FrostedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_backup_card"),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(MediciTheme.spacing.medium),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MediciTheme.colors.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.settings_backup_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(R.string.settings_backup_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MediciTheme.colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                    ) {
                        // Backup Button
                        BouncingButton(
                            onClick = {
                                if (uiState.isBackingUp) return@BouncingButton
                                settingsViewModel.setBackingUp(true)
                                scope.launch(Dispatchers.IO) {
                                    val result = BackupManager.backupData(context)
                                    withContext(Dispatchers.Main) {
                                        settingsViewModel.setBackingUp(false)
                                        if (result.isSuccess) {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_backup_success_toast),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.settings_backup_error_toast),
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MediciTheme.colors.primary,
                                contentColor = MediciTheme.colors.onPrimary
                            ),
                            enabled = !uiState.isBackingUp && !uiState.isRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("backup_data_button")
                        ) {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MediciTheme.colors.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.settings_backup_button),
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Restore Button
                        BouncingOutlinedButton(
                            onClick = {
                                settingsViewModel.showRestoreConfirmDialog(true)
                            },
                            shape = RoundedCornerShape(20.dp),
                            enabled = !uiState.isBackingUp && !uiState.isRestoring,
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .testTag("restore_data_button")
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MediciTheme.colors.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    tint = MediciTheme.colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                            }
                            Text(
                                text = stringResource(R.string.settings_restore_button),
                                fontWeight = FontWeight.Bold,
                                color = MediciTheme.colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. APP VERSION
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = MediciTheme.spacing.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_app_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.settings_app_version),
                    style = MaterialTheme.typography.bodySmall,
                    color = MediciTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("app_version_text")
                )
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // THEME CHANGE DIALOG
    if (uiState.showThemeDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.showThemeDialog(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MediciTheme.colors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.settings_theme_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_dialog"),
                    verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.small)
                ) {
                    val themes = listOf(
                        Triple(AppThemeMode.MALE, stringResource(R.string.settings_theme_male), "theme_radio_male"),
                        Triple(AppThemeMode.FEMALE, stringResource(R.string.settings_theme_female), "theme_radio_female"),
                        Triple(AppThemeMode.NEUTRAL, stringResource(R.string.settings_theme_neutral), "theme_radio_neutral")
                    )

                    val selectThemeMode: (AppThemeMode) -> Unit = { selectedMode ->
                        val changed = selectedMode != currentTheme
                        onThemeSelected(selectedMode)
                        onSaveThemePermanently(selectedMode)
                        if (changed) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.msg_settings_theme_changed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    themes.forEach { (mode, label, tag) ->
                        val isSelected = currentTheme == mode
                        val swatchColor = when (mode) {
                            AppThemeMode.MALE -> MediciTheme.colors.maleSwatch
                            AppThemeMode.FEMALE -> MediciTheme.colors.femaleSwatch
                            AppThemeMode.NEUTRAL -> MediciTheme.colors.neutralSwatch
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 48.dp)
                                .selectable(
                                    selected = isSelected,
                                    onClick = { selectThemeMode(mode) },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp)
                                .testTag(tag),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(swatchColor, CircleShape)
                                    .border(1.dp, MediciTheme.colors.outline, CircleShape)
                                    .testTag("theme_preview_circle_${mode.key}")
                            )
                            Spacer(modifier = Modifier.size(12.dp))
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectThemeMode(mode) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MediciTheme.colors.primary,
                                    unselectedColor = MediciTheme.colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected) MediciTheme.colors.primary else MediciTheme.colors.textPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            },
            confirmButton = {
                BouncingButton(
                    onClick = { settingsViewModel.showThemeDialog(false) },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.primary,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_ok_btn),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        )
    }

    // RESTORE CONFIRMATION DIALOG
    if (uiState.showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.showRestoreConfirmDialog(false) },
            shape = RoundedCornerShape(20.dp),
            containerColor = MediciTheme.colors.cardBackground,
            title = {
                Text(
                    text = stringResource(R.string.settings_restore_confirm_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MediciTheme.colors.warning,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.settings_restore_confirm_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediciTheme.colors.textPrimary,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            },
            confirmButton = {
                BouncingButton(
                    onClick = {
                        settingsViewModel.showRestoreConfirmDialog(false)
                        openDocumentLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/x-zip-compressed",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MediciTheme.colors.warning,
                        contentColor = MediciTheme.colors.onPrimary
                    ),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("restore_confirm_button")
                ) {
                    Text(
                        text = stringResource(R.string.settings_restore_confirm_btn),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            dismissButton = {
                BouncingOutlinedButton(
                    onClick = { settingsViewModel.showRestoreConfirmDialog(false) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .testTag("restore_cancel_button")
                ) {
                    Text(
                        text = stringResource(R.string.settings_cancel_btn),
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            modifier = Modifier.testTag("restore_confirm_dialog")
        )
    }
}
