package com.mediciplanner.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.screens.followup.FollowupScreen
import com.mediciplanner.app.ui.screens.home.HomeScreen
import com.mediciplanner.app.ui.screens.onboarding.PersonalizationScreen
import com.mediciplanner.app.ui.screens.onboarding.SplashScreen
import com.mediciplanner.app.ui.screens.onboarding.ThemePickerScreen
import com.mediciplanner.app.ui.screens.onboarding.TutorialScreen
import com.mediciplanner.app.ui.screens.onboarding.WelcomeScreen
import com.mediciplanner.app.ui.screens.prescription.PrescriptionScreen
import com.mediciplanner.app.ui.screens.settings.NotificationSettingsScreen
import com.mediciplanner.app.ui.screens.settings.SettingsScreen
import com.mediciplanner.app.ui.screens.track.TrackScreen

object AppRoutes {
    const val SPLASH = "splash"
    const val PERSONALIZATION = "personalization"
    const val PROFILE_EDIT = "profile_edit"
    const val WELCOME = "welcome"
    const val TUTORIAL = "tutorial"
    const val THEME_PICKER = "theme_picker"
    const val HOME = "home"
    const val PRESCRIPTION = "prescription"
    const val TRACK = "track"
    const val FOLLOWUP = "followup"
    const val SETTINGS = "settings"
    const val NOTIFICATION_SETTINGS = "notification_settings"
}

@Composable
fun AppNavigation(
    currentTheme: AppThemeMode,
    onThemeSelected: (AppThemeMode) -> Unit,
    onSaveThemePermanently: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    isFirstLaunch: Boolean? = false,
    navController: NavHostController = rememberNavController(),
    startDestination: String = AppRoutes.SPLASH
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute in listOf(
        AppRoutes.HOME,
        AppRoutes.PRESCRIPTION,
        AppRoutes.TRACK,
        AppRoutes.FOLLOWUP
    )

    AppShell(
        modifier = modifier,
        currentRoute = currentRoute,
        shouldShowBottomBar = shouldShowBottomBar,
        onNavigateToDestination = { destination ->
            navController.navigate(destination.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { 60 },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        )
            },
            exitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { -60 },
                            animationSpec = androidx.compose.animation.core.tween(250)
                        )
            },
            popEnterTransition = {
                androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(300)) +
                        androidx.compose.animation.slideInHorizontally(
                            initialOffsetX = { -60 },
                            animationSpec = androidx.compose.animation.core.tween(300)
                        )
            },
            popExitTransition = {
                androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(250)) +
                        androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { 60 },
                            animationSpec = androidx.compose.animation.core.tween(250)
                        )
            }
        ) {
            composable(AppRoutes.SPLASH) {
                SplashScreen(
                    isFirstLaunch = isFirstLaunch,
                    onNavigateToPersonalization = {
                        navController.navigate(AppRoutes.PERSONALIZATION) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                        }
                    },
                    onNavigateToHome = {
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(AppRoutes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.PERSONALIZATION) {
                PersonalizationScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected,
                    onSaveAndContinue = { _, _, themeMode ->
                        onSaveThemePermanently(themeMode)
                        navController.navigate(AppRoutes.WELCOME) {
                            popUpTo(AppRoutes.PERSONALIZATION) { inclusive = true }
                        }
                    },
                    onSkip = {
                        onSaveThemePermanently(AppThemeMode.NEUTRAL)
                        navController.navigate(AppRoutes.WELCOME) {
                            popUpTo(AppRoutes.PERSONALIZATION) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.WELCOME) {
                WelcomeScreen(
                    onNavigateToTutorial = {
                        navController.navigate(AppRoutes.TUTORIAL)
                    }
                )
            }

            composable(AppRoutes.TUTORIAL) {
                TutorialScreen(
                    onFinishTutorial = {
                        onSaveThemePermanently(currentTheme)
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onSkipTutorial = {
                        onSaveThemePermanently(currentTheme)
                        navController.navigate(AppRoutes.HOME) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(AppRoutes.THEME_PICKER) {
                ThemePickerScreen(
                    currentTheme = currentTheme,
                    onThemeConfirmed = onThemeSelected,
                    onSaveAndContinue = { selectedMode ->
                        onSaveThemePermanently(selectedMode)
                        if (navController.previousBackStackEntry != null &&
                            navController.previousBackStackEntry?.destination?.route !in listOf(AppRoutes.TUTORIAL, AppRoutes.WELCOME, AppRoutes.SPLASH)
                        ) {
                            navController.navigateUp()
                        } else {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.WELCOME) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        onSaveThemePermanently(currentTheme)
                        if (navController.previousBackStackEntry != null &&
                            navController.previousBackStackEntry?.destination?.route !in listOf(AppRoutes.TUTORIAL, AppRoutes.WELCOME, AppRoutes.SPLASH)
                        ) {
                            navController.navigateUp()
                        } else {
                            navController.navigate(AppRoutes.HOME) {
                                popUpTo(AppRoutes.WELCOME) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(AppRoutes.HOME) {
                HomeScreen(
                    onNavigateToThemePicker = {
                        navController.navigate(AppRoutes.THEME_PICKER)
                    },
                    onNavigateToTrack = {
                        navController.navigate(AppRoutes.TRACK)
                    },
                    onNavigateToPrescription = {
                        navController.navigate(AppRoutes.PRESCRIPTION)
                    },
                    onNavigateToSettings = {
                        navController.navigate(AppRoutes.SETTINGS)
                    },
                    onNavigateToProfile = {
                        navController.navigate(AppRoutes.PROFILE_EDIT)
                    },
                    onNavigateToNotificationSettings = {
                        navController.navigate(AppRoutes.NOTIFICATION_SETTINGS)
                    }
                )
            }

            composable(AppRoutes.PROFILE_EDIT) {
                PersonalizationScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected,
                    onSaveAndContinue = { _, _, themeMode ->
                        onSaveThemePermanently(themeMode)
                        navController.navigateUp()
                    },
                    onSkip = {
                        navController.navigateUp()
                    },
                    isEditMode = true,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable(AppRoutes.PRESCRIPTION) {
                com.mediciplanner.app.ui.screens.prescription.PrescriptionListScreen()
            }

            composable(AppRoutes.TRACK) {
                TrackScreen()
            }

            composable(AppRoutes.FOLLOWUP) {
                FollowupScreen()
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected,
                    onSaveThemePermanently = onSaveThemePermanently,
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }

            composable(AppRoutes.NOTIFICATION_SETTINGS) {
                NotificationSettingsScreen(
                    onNavigateBack = {
                        navController.navigateUp()
                    }
                )
            }
        }
    }
}
