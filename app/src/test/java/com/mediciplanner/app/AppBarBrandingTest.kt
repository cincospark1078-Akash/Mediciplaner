package com.mediciplanner.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.AboutAppDialog
import com.mediciplanner.app.ui.screens.home.buildGreetingString
import com.mediciplanner.app.ui.screens.home.components.GreetingHeader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppBarBrandingTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testGreetingStringLogicWithoutName() {
        assertEquals("আসসালামু আলাইকুম", buildGreetingString("islamic", "", 10))
        assertEquals("নমস্কার", buildGreetingString("hindu", "", 10))
        assertEquals("Hello", buildGreetingString("english", "", 10))
        assertEquals("শুভ সকাল", buildGreetingString("universal", "", 10))
        assertEquals("শুভ সন্ধ্যা", buildGreetingString("universal", "", 18))
    }

    @Test
    fun testGreetingStringLogicWithName() {
        assertEquals("আসসালামু আলাইকুম, রফিক", buildGreetingString("islamic", "রফিক", 10))
        assertEquals("নমস্কার, রফিক", buildGreetingString("hindu", "রফিক", 10))
        assertEquals("Hello, Rafiq", buildGreetingString("english", "Rafiq", 10))
        assertEquals("শুভ সকাল, রফিক", buildGreetingString("universal", "রফিক", 9))
        assertEquals("শুভ সন্ধ্যা, রফিক", buildGreetingString("universal", "রফিক", 19))
    }

    @Test
    fun testAppBarTopRowAvatarReplacesGearAndBellIcons() {
        composeTestRule.setContent {
            MediciTheme {
                GreetingHeader(
                    themeMode = AppThemeMode.NEUTRAL,
                    formattedDate = "১০ নভেম্বর, ২০২৬",
                    userName = "রফিক",
                    greetingPreference = "islamic"
                )
            }
        }

        // 1. App Logo and App Name
        composeTestRule.onNodeWithTag("app_logo_icon").assertIsDisplayed()
        composeTestRule.onNodeWithTag("app_name_text").assertIsDisplayed().assertTextContains("মেডিসিপ্ল্যানার")

        // 2. Old gear & bell icons removed, replaced by User Avatar with first letter
        composeTestRule.onNodeWithTag("settings_gear_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("notifications_bell_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("user_avatar_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_avatar_initial", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("র")

        // 3. Dynamic greeting and date
        composeTestRule.onNodeWithTag("home_greeting_text").assertIsDisplayed().assertTextContains("আসসালামু আলাইকুম, রফিক")
        composeTestRule.onNodeWithTag("home_date_text").assertIsDisplayed().assertTextContains("১০ নভেম্বর, ২০২৬")
    }

    @Test
    fun testAvatarFallbackIconWhenUserNameEmpty() {
        composeTestRule.setContent {
            MediciTheme {
                GreetingHeader(
                    themeMode = AppThemeMode.NEUTRAL,
                    formattedDate = "১০ নভেম্বর, ২০২৬",
                    userName = "",
                    greetingPreference = "universal"
                )
            }
        }

        composeTestRule.onNodeWithTag("user_avatar_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_avatar_icon", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("user_avatar_initial", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun testProfileDropdownMenu4ItemsAndAboutDialog() {
        var profileClicked = 0
        var settingsClicked = 0
        var notificationsClicked = 0
        var showAbout by mutableStateOf(false)

        composeTestRule.setContent {
            MediciTheme {
                GreetingHeader(
                    themeMode = AppThemeMode.NEUTRAL,
                    formattedDate = "১০ নভেম্বর, ২০২৬",
                    userName = "Rafiq",
                    greetingPreference = "english",
                    onMyProfileClick = { profileClicked++ },
                    onSettingsClick = { settingsClicked++ },
                    onNotificationsClick = { notificationsClicked++ },
                    onAboutClick = { showAbout = true }
                )

                if (showAbout) {
                    AboutAppDialog(onDismiss = { showAbout = false })
                }
            }
        }

        composeTestRule.onNodeWithTag("user_avatar_initial", useUnmergedTree = true)
            .assertTextEquals("R")

        // Open menu & click "আমার প্রোফাইল"
        composeTestRule.onNodeWithTag("user_avatar_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("profile_dropdown_menu").assertIsDisplayed()
        composeTestRule.onNodeWithText("আমার প্রোফাইল").assertIsDisplayed()
        composeTestRule.onNodeWithText("সেটিংস").assertIsDisplayed()
        composeTestRule.onNodeWithText("নোটিফিকেশন").assertIsDisplayed()
        composeTestRule.onNodeWithText("অ্যাপ সম্পর্কে").assertIsDisplayed()

        composeTestRule.onNodeWithTag("menu_item_my_profile").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, profileClicked)

        // Open menu & click "সেটিংস"
        composeTestRule.onNodeWithTag("user_avatar_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_settings").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, settingsClicked)

        // Open menu & click "নোটিফিকেশন"
        composeTestRule.onNodeWithTag("user_avatar_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_notifications").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, notificationsClicked)

        // Open menu & click "অ্যাপ সম্পর্কে" -> Opens About Dialog
        composeTestRule.onNodeWithTag("user_avatar_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("menu_item_about").performClick()
        composeTestRule.waitForIdle()

        assertTrue(showAbout)
        composeTestRule.onNodeWithTag("about_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_app_name").assertIsDisplayed().assertTextContains("মেডিসিপ্ল্যানার")
        composeTestRule.onNodeWithTag("about_app_version").assertIsDisplayed().assertTextContains("সংস্করণ ১.০.০")
        composeTestRule.onNodeWithTag("about_privacy_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_policy_notice_box").assertIsDisplayed()
        composeTestRule.onNodeWithTag("about_terms_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("about_close_button").performClick()
        composeTestRule.waitForIdle()
        assertFalse(showAbout)
    }

    @Test
    fun testLongGreetingDisplaysWithinTwoLines() {
        val longName = "MD Arifur Rahman"
        composeTestRule.setContent {
            MediciTheme {
                GreetingHeader(
                    themeMode = AppThemeMode.NEUTRAL,
                    formattedDate = "১২ নভেম্বর, ২০২৬",
                    userName = longName,
                    greetingPreference = "islamic"
                )
            }
        }

        composeTestRule.onNodeWithTag("home_greeting_text")
            .assertIsDisplayed()
            .assertTextContains("আসসালামু আলাইকুম, $longName")
    }
}
