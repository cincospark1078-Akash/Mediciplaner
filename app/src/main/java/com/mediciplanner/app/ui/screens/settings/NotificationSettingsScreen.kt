package com.mediciplanner.app.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediciplanner.app.MediciApplication
import com.mediciplanner.app.R
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.BouncingOutlinedButton
import com.mediciplanner.app.ui.components.FrostedCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onNavigateBack() }

    val context = LocalContext.current
    val themePreferences = (context.applicationContext as? MediciApplication)?.themePreferences
        ?: ThemePreferences(context)
    val scope = rememberCoroutineScope()

    val medicineEnabled by themePreferences.medicineRemindersEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val followupEnabled by themePreferences.followupRemindersEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)
    val soundEnabled by themePreferences.notificationSoundEnabledFlow
        .collectAsStateWithLifecycle(initialValue = true)

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MediciTheme.colors.primaryContainer.copy(alpha = 0.5f),
            MediciTheme.colors.surface
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundGradient)
            .statusBarsPadding()
            .testTag("notification_settings_screen"),
        containerColor = MediciTheme.colors.surface.copy(alpha = 0f),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.notification_settings_title),
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
                        modifier = Modifier.testTag("notification_settings_back_button")
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(MediciTheme.spacing.medium)
        ) {
            NotificationToggleCard(
                icon = Icons.Default.Medication,
                title = stringResource(R.string.notification_setting_medicine_title),
                subtitle = stringResource(R.string.notification_setting_medicine_desc),
                checked = medicineEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { themePreferences.setMedicineRemindersEnabled(enabled) }
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_settings_notification_toggled),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                testTag = "notification_medicine_toggle"
            )

            NotificationToggleCard(
                icon = Icons.Default.CalendarToday,
                title = stringResource(R.string.notification_setting_followup_title),
                subtitle = stringResource(R.string.notification_setting_followup_desc),
                checked = followupEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { themePreferences.setFollowupRemindersEnabled(enabled) }
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_settings_notification_toggled),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                testTag = "notification_followup_toggle"
            )

            NotificationToggleCard(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(R.string.notification_setting_sound_title),
                subtitle = stringResource(R.string.notification_setting_sound_desc),
                checked = soundEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { themePreferences.setNotificationSoundEnabled(enabled) }
                    Toast.makeText(
                        context,
                        context.getString(R.string.msg_settings_notification_toggled),
                        Toast.LENGTH_SHORT
                    ).show()
                },
                testTag = "notification_sound_toggle"
            )

            BouncingOutlinedButton(
                onClick = {
                    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                    } else {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        try {
                            context.startActivity(fallback)
                        } catch (_: Exception) {
                        }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MediciTheme.colors.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("open_system_notification_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.notification_system_settings_btn),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NotificationToggleCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    FrostedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MediciTheme.spacing.medium),
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = MediciTheme.colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MediciTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MediciTheme.colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MediciTheme.colors.onPrimary,
                    checkedTrackColor = MediciTheme.colors.primary
                ),
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
