package com.mediciplanner.app.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mediciplanner.app.R
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.screens.home.buildGreetingString
import java.io.File

/**
 * App Bar Branding & Dynamic Greeting Header for Home Screen:
 * - Top Row (App Name & User Avatar):
 *   - Left: Small App Logo icon (20.dp) and App Name (14.sp, Medium, textSecondary)
 *   - Right: Circular User Avatar (48.dp) with Profile Dropdown Menu
 * - 16.dp Spacing between Top Bar and Greeting
 * - Greeting text: 24.sp, Bold, textPrimary (Main Focus)
 * - 4.dp Spacing between Greeting and Date
 * - Date text: 12.sp, Normal, textSecondary (Subtle)
 */
@Composable
fun GreetingHeader(
    themeMode: AppThemeMode,
    formattedDate: String,
    greetingText: String? = null,
    userName: String = "",
    userProfilePicPath: String = "",
    greetingPreference: String = "universal",
    customGreeting: String = "",
    onMyProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val finalGreeting = greetingText ?: buildGreetingString(greetingPreference, userName, customGreeting = customGreeting)
    var showProfileMenu by remember { mutableStateOf(false) }

    val trimmedName = userName.trim()
    val avatarInitial = trimmedName.firstOrNull()?.let { ch ->
        if (ch.isLowerCase()) ch.uppercaseChar() else ch
    }?.toString()
    val avatarDesc = stringResource(R.string.profile_avatar_desc)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
            .testTag("greeting_header")
    ) {
        // 1. Top Row: App Logo + Name (Left) & User Avatar + Dropdown Menu (Right)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: App Logo + App Name (vertically centered)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = stringResource(R.string.app_name_bengali),
                    tint = MediciTheme.colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .testTag("app_logo_icon")
                )
                Text(
                    text = stringResource(R.string.app_name_bengali),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MediciTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("app_name_text")
                )
            }

            // Right: Circular User Avatar (48.dp) anchored with Profile Dropdown Menu
            Box {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MediciTheme.colors.primary, CircleShape)
                        .clickable { showProfileMenu = true }
                        .semantics {
                            role = Role.Button
                            contentDescription = avatarDesc
                        }
                        .testTag("user_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val profileFile = if (userProfilePicPath.isNotBlank()) File(userProfilePicPath) else null
                    if (profileFile != null && profileFile.exists()) {
                        AsyncImage(
                            model = profileFile,
                            contentDescription = avatarDesc,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(CircleShape)
                                .testTag("user_avatar_image")
                        )
                    } else if (!avatarInitial.isNullOrEmpty()) {
                        Text(
                            text = avatarInitial,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MediciTheme.colors.onPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.testTag("user_avatar_initial")
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = avatarDesc,
                            tint = MediciTheme.colors.onPrimary,
                            modifier = Modifier
                                .size(24.dp)
                                .testTag("user_avatar_icon")
                        )
                    }
                }

                DropdownMenu(
                    expanded = showProfileMenu,
                    onDismissRequest = { showProfileMenu = false },
                    containerColor = MediciTheme.colors.card,
                    shape = RoundedCornerShape(20.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp))
                        .background(MediciTheme.colors.card, RoundedCornerShape(20.dp))
                        .padding(8.dp)
                        .testTag("profile_dropdown_menu")
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.menu_my_profile),
                                fontSize = 14.sp,
                                color = MediciTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = {
                            showProfileMenu = false
                            onMyProfileClick()
                        },
                        modifier = Modifier.testTag("menu_item_my_profile")
                    )

                    HorizontalDivider(
                        color = MediciTheme.colors.outlineVariant.copy(alpha = 0.5f)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.menu_settings),
                                fontSize = 14.sp,
                                color = MediciTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = {
                            showProfileMenu = false
                            onSettingsClick()
                        },
                        modifier = Modifier.testTag("menu_item_settings")
                    )

                    HorizontalDivider(
                        color = MediciTheme.colors.outlineVariant.copy(alpha = 0.5f)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.menu_notifications),
                                fontSize = 14.sp,
                                color = MediciTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = {
                            showProfileMenu = false
                            onNotificationsClick()
                        },
                        modifier = Modifier.testTag("menu_item_notifications")
                    )

                    HorizontalDivider(
                        color = MediciTheme.colors.outlineVariant.copy(alpha = 0.5f)
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.menu_about_app),
                                fontSize = 14.sp,
                                color = MediciTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MediciTheme.colors.primary
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                        onClick = {
                            showProfileMenu = false
                            onAboutClick()
                        },
                        modifier = Modifier.testTag("menu_item_about")
                    )
                }
            }
        }

        // Spacing: 16.dp between Top Bar (App Name) and Greeting
        Spacer(modifier = Modifier.height(16.dp))

        // 2. Greeting (Main Focus)
        Text(
            text = finalGreeting,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MediciTheme.colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_greeting_text")
        )

        // Spacing: 4.dp between Greeting and Date
        Spacer(modifier = Modifier.height(4.dp))

        // 3. Date (Subtle)
        Text(
            text = formattedDate,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = MediciTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("home_date_text")
        )
    }
}
