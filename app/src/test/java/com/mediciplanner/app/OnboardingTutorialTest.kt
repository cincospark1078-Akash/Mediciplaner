package com.mediciplanner.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.down
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.ui.components.BouncingButton
import com.mediciplanner.app.ui.components.bouncyClickable
import com.mediciplanner.app.ui.navigation.AppRoutes
import com.mediciplanner.app.ui.screens.onboarding.ThemePickerViewModel
import com.mediciplanner.app.ui.screens.onboarding.TutorialPageData
import com.mediciplanner.app.ui.screens.onboarding.TutorialScreen
import com.mediciplanner.app.ui.screens.onboarding.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class OnboardingTutorialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        themePreferences = ThemePreferences(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOnboardingRoutesSequence() {
        assertEquals("splash", AppRoutes.SPLASH)
        assertEquals("welcome", AppRoutes.WELCOME)
        assertEquals("tutorial", AppRoutes.TUTORIAL)
        assertEquals("theme_picker", AppRoutes.THEME_PICKER)
        assertEquals("home", AppRoutes.HOME)
    }

    @Test
    fun testMainViewModelFirstLaunchAndTheme() {
        val viewModel = MainViewModel(themePreferences)

        viewModel.previewTheme(AppThemeMode.MALE)
        assertEquals(AppThemeMode.MALE, viewModel.previewTheme.value)

        viewModel.saveTheme(AppThemeMode.FEMALE)
        assertEquals(AppThemeMode.FEMALE, viewModel.previewTheme.value)
    }

    @Test
    fun testThemePickerViewModelStateAndSelection() {
        val viewModel = ThemePickerViewModel(themePreferences, AppThemeMode.NEUTRAL)

        assertEquals(AppThemeMode.NEUTRAL, viewModel.selectedTheme.value)
        viewModel.selectTheme(AppThemeMode.MALE)
        assertEquals(AppThemeMode.MALE, viewModel.selectedTheme.value)
        viewModel.selectTheme(AppThemeMode.FEMALE)
        assertEquals(AppThemeMode.FEMALE, viewModel.selectedTheme.value)
    }

    @Test
    fun testTutorialScreenContentHasExactly4Pages() {
        val pages = listOf(
            TutorialPageData(
                icon = Icons.Outlined.ReceiptLong,
                titleRes = R.string.tutorial_page1_title,
                subtitleRes = R.string.tutorial_page1_subtitle
            ),
            TutorialPageData(
                icon = Icons.Outlined.NotificationsActive,
                titleRes = R.string.tutorial_page2_title,
                subtitleRes = R.string.tutorial_page2_subtitle
            ),
            TutorialPageData(
                icon = Icons.Outlined.ShowChart,
                titleRes = R.string.tutorial_page3_title,
                subtitleRes = R.string.tutorial_page3_subtitle
            ),
            TutorialPageData(
                icon = Icons.Outlined.CalendarMonth,
                titleRes = R.string.tutorial_page4_title,
                subtitleRes = R.string.tutorial_page4_subtitle
            )
        )

        // Audit check 1: exactly 4 pages
        assertEquals(4, pages.size)

        // Check titles & subtitles string resources exist
        pages.forEach { page ->
            val title = context.getString(page.titleRes)
            val subtitle = context.getString(page.subtitleRes)
            assertNotNull(title)
            assertTrue(title.isNotBlank())
            assertNotNull(subtitle)
            assertTrue(subtitle.isNotBlank())
        }

        // Verify specific Bengali copy per requirements
        assertEquals("অ্যাপ ট্যুর", context.getString(R.string.tutorial_app_tour))
        assertEquals("প্রেসক্রিপশন যোগ করুন", context.getString(R.string.tutorial_page1_title))
        assertEquals("সময়মতো ওষুধ খাওয়ার রিমাইন্ডার", context.getString(R.string.tutorial_page2_title))
        assertEquals("আজকের অগ্রগতি ট্র্যাক করুন", context.getString(R.string.tutorial_page3_title))
        assertEquals("ডাক্তারের ভিজিট ও টেস্ট", context.getString(R.string.tutorial_page4_title))
        assertEquals(
            "ডাক্তারের অ্যাপয়েন্টমেন্ট, টেস্ট এবং সব মেডিকেল রেকর্ড এক জায়গায় সংরক্ষণ করুন।",
            context.getString(R.string.tutorial_page4_subtitle)
        )

        // Verify CTA strings
        assertEquals("এড়িয়ে যান", context.getString(R.string.tutorial_skip))
        assertEquals("শুরু করুন", context.getString(R.string.tutorial_get_started))
        assertEquals("পরবর্তী", context.getString(R.string.action_next))
    }

    @Test
    fun testTutorialScreenNavigationAndPage2TitleRendered() {
        var finished = false
        var skipped = false

        composeTestRule.setContent {
            MediciTheme {
                TutorialScreen(
                    onFinishTutorial = { finished = true },
                    onSkipTutorial = { skipped = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("tutorial_app_tour_label")
            .assertIsDisplayed()
            .assertTextEquals("অ্যাপ ট্যুর")

        composeTestRule.onNodeWithTag("tutorial_page_title_1")
            .assertIsDisplayed()
            .assertTextEquals("প্রেসক্রিপশন যোগ করুন")

        composeTestRule.onNodeWithTag("tutorial_next_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tutorial_page_title_2")
            .assertIsDisplayed()
            .assertTextEquals("সময়মতো ওষুধ খাওয়ার রিমাইন্ডার")

        composeTestRule.onNodeWithTag("tutorial_next_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_next_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tutorial_page_subtitle_4")
            .assertIsDisplayed()
            .assertTextEquals("ডাক্তারের অ্যাপয়েন্টমেন্ট, টেস্ট এবং সব মেডিকেল রেকর্ড এক জায়গায় সংরক্ষণ করুন।")

        composeTestRule.onNodeWithTag("tutorial_get_started_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue("Get Started button should trigger onFinishTutorial", finished)
        assertTrue(!skipped)
    }

    @Test
    fun testBouncyClickableAndBouncingButtonTriggerClick() {
        var boxClicks = 0
        var buttonClicks = 0

        composeTestRule.setContent {
            MediciTheme {
                androidx.compose.foundation.layout.Column {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .bouncyClickable { boxClicks++ }
                            .testTag("bouncy_box")
                    ) {
                        Text("Tap")
                    }

                    BouncingButton(
                        onClick = { buttonClicks++ },
                        modifier = Modifier.testTag("bouncy_primary_btn")
                    ) {
                        Text("Save")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("bouncy_box").performClick()
        composeTestRule.onNodeWithTag("bouncy_primary_btn").performClick()
        composeTestRule.waitForIdle()

        assertEquals(1, boxClicks)
        assertEquals(1, buttonClicks)

        // Also verify touch gesture path (detectTapGestures) works cleanly
        composeTestRule.onNodeWithTag("bouncy_box").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.onNodeWithTag("bouncy_primary_btn").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()

        assertEquals(2, boxClicks)
        assertEquals(2, buttonClicks)
    }

    @Test
    fun testWelcomeScreenStaggeredFeaturesAndBouncyNextButton() {
        var navigatedToTutorial = 0

        composeTestRule.setContent {
            MediciTheme {
                WelcomeScreen(
                    onNavigateToTutorial = { navigatedToTutorial++ }
                )
            }
        }

        // Wait for staggered delays (0ms, 150ms, 300ms) + 300ms animation to complete
        composeTestRule.waitUntil(timeoutMillis = 3000L) {
            composeTestRule
                .onAllNodesWithTag("welcome_feature_row_2")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule.mainClock.advanceTimeBy(350L)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("welcome_feature_row_0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_feature_row_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("welcome_feature_row_2").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("welcome_next_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, navigatedToTutorial)

        composeTestRule.onNodeWithTag("welcome_next_button").performTouchInput {
            down(center)
            up()
        }
        composeTestRule.waitForIdle()
        assertEquals(2, navigatedToTutorial)
    }
}
