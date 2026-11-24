package com.mediciplanner.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.ui.navigation.AppRoutes
import com.mediciplanner.app.ui.screens.home.HomeViewModel
import com.mediciplanner.app.ui.screens.home.components.GreetingHeader
import com.mediciplanner.app.ui.screens.home.formatPersonalizedGreeting
import com.mediciplanner.app.ui.screens.onboarding.PersonalizationScreen
import com.mediciplanner.app.ui.screens.onboarding.PersonalizationViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmartPersonalizationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        themePreferences = ThemePreferences(context)
        themePreferences.savePersonalization(
            userName = "",
            greetingPreference = "universal",
            themeGender = "neutral"
        )
    }

    @After
    fun tearDown() = runTest {
        themePreferences.savePersonalization(
            userName = "",
            greetingPreference = "universal",
            themeGender = "neutral"
        )
        Dispatchers.resetMain()
    }

    @Test
    fun testDataStoreDefaultsAndPersistence() = runTest {
        // Defaults check
        assertEquals("", themePreferences.userNameFlow.first())
        assertEquals("universal", themePreferences.greetingPreferenceFlow.first())
        assertEquals("neutral", themePreferences.themeGenderFlow.first())
        assertEquals(AppThemeMode.NEUTRAL, themePreferences.themeModeFlow.first())

        // Save personalization
        themePreferences.savePersonalization(
            userName = "রাহিম",
            greetingPreference = "salam",
            themeGender = "male"
        )

        assertEquals("রাহিম", themePreferences.userNameFlow.first())
        assertEquals("salam", themePreferences.greetingPreferenceFlow.first())
        assertEquals("male", themePreferences.themeGenderFlow.first())
        assertEquals(AppThemeMode.MALE, themePreferences.themeModeFlow.first())
    }

    @Test
    fun testPersonalizedGreetingFormatter() {
        // 1. Salam (Muslim) with and without name
        assertEquals("আসসালামু আলাইকুম, তানভীর", formatPersonalizedGreeting("salam", "তানভীর"))
        assertEquals("আসসালামু আলাইকুম", formatPersonalizedGreeting("salam", ""))

        // 2. Namaskar (Hindu) with and without name
        assertEquals("নমস্কার, পূজা", formatPersonalizedGreeting("namaskar", "পূজা"))
        assertEquals("নমস্কার", formatPersonalizedGreeting("namaskar", ""))

        // 3. Hello (English) with and without name
        assertEquals("Hello, John", formatPersonalizedGreeting("hello", "John"))
        assertEquals("Hello", formatPersonalizedGreeting("hello", ""))

        // 4. Universal (Time-based morning) with and without name
        assertEquals("শুভ সকাল, অনিক", formatPersonalizedGreeting("universal", "অনিক", hour = 8))
        assertEquals("শুভ সকাল", formatPersonalizedGreeting("universal", "", hour = 8))

        // 5. Universal (Time-based afternoon) with and without name
        assertEquals("শুভ দুপুর, অনিক", formatPersonalizedGreeting("universal", "অনিক", hour = 14))
        assertEquals("শুভ দুপুর", formatPersonalizedGreeting("universal", "", hour = 14))

        // 6. Universal (Time-based evening) with and without name
        assertEquals("শুভ সন্ধ্যা, অনিক", formatPersonalizedGreeting("universal", "অনিক", hour = 19))
        assertEquals("শুভ সন্ধ্যা", formatPersonalizedGreeting("universal", "", hour = 19))
    }

    @Test
    fun testPersonalizationScreenUiElementsAndInstantTheme() {
        val testViewModel = PersonalizationViewModel(themePreferences, AppThemeMode.NEUTRAL)
        var appliedTheme: AppThemeMode? = null
        var completedName = ""
        var completedGreeting = ""
        var completedTheme: AppThemeMode? = null

        composeTestRule.setContent {
            MediciTheme {
                PersonalizationScreen(
                    currentTheme = AppThemeMode.NEUTRAL,
                    onThemeSelected = { appliedTheme = it },
                    onSaveAndContinue = { name, greeting, mode ->
                        completedName = name
                        completedGreeting = greeting
                        completedTheme = mode
                    },
                    onSkip = {},
                    viewModel = testViewModel
                )
            }
        }

        // 1. Verify Title, Subtitle, and Note
        composeTestRule.onNodeWithTag("personalization_title")
            .assertIsDisplayed()
            .assertTextContains("আপনার অ্যাপটি সাজান")

        composeTestRule.onNodeWithTag("personalization_subtitle")
            .assertIsDisplayed()
            .assertTextContains("আপনার পছন্দমতো অভিজ্ঞতা তৈরি করুন।")

        composeTestRule.onNodeWithTag("personalization_note")
            .assertIsDisplayed()
            .assertTextContains("আপনি পরে সেটিংস থেকে এটি পরিবর্তন করতে পারবেন।")

        // 2. Section 1: Name input
        composeTestRule.onNodeWithTag("personalization_name_input")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
            .performTextReplacement("আরিফ")

        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("personalization_name_input")
            .assertTextContains("আরিফ")

        // 3. Section 2: Inclusive Greeting FilterChips
        composeTestRule.onNodeWithTag("greeting_chip_salam").assertIsDisplayed()
        composeTestRule.onNodeWithTag("greeting_chip_universal").assertIsDisplayed()
        composeTestRule.onNodeWithTag("greeting_chip_namaskar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("greeting_chip_hello").assertIsDisplayed()

        // Select Salam
        composeTestRule.onNodeWithTag("greeting_chip_salam").performClick()

        // 4. Section 3: Instant Theme Change on Card Click
        composeTestRule.onNodeWithTag("theme_card_neutral")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("সাধারণ")
        composeTestRule.onNodeWithText("নিউট্রাল").assertDoesNotExist()
        composeTestRule.onNodeWithText("শান্ত ও মনোরম মেডিকেল ব্লু কালার থিম").assertDoesNotExist()

        composeTestRule.onNodeWithTag("theme_card_male")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        assertEquals("Clicking male card should immediately apply MALE theme", AppThemeMode.MALE, appliedTheme)

        // 5. Click Save & Continue ("শুরু করুন")
        composeTestRule.onNodeWithTag("personalization_get_started_button")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()

        composeTestRule.waitForIdle()

        assertEquals("আরিফ", completedName)
        assertEquals("salam", completedGreeting)
        assertEquals(AppThemeMode.MALE, completedTheme)
    }

    @Test
    fun testNavigationSequenceIncludesPersonalizationAndExcludesThemePickerFromSequence() {
        assertEquals("splash", AppRoutes.SPLASH)
        assertEquals("personalization", AppRoutes.PERSONALIZATION)
        assertEquals("welcome", AppRoutes.WELCOME)
        assertEquals("tutorial", AppRoutes.TUTORIAL)
        assertEquals("home", AppRoutes.HOME)

        // Legacy constant retained for backward compatibility
        assertEquals("theme_picker", AppRoutes.THEME_PICKER)
    }

    @Test
    fun testGreetingHeaderDisplaysPersonalizedGreeting() {
        composeTestRule.setContent {
            MediciTheme {
                GreetingHeader(
                    themeMode = AppThemeMode.NEUTRAL,
                    formattedDate = "৯ নভেম্বর, ২০২৬",
                    userName = "করিম",
                    greetingPreference = "salam"
                )
            }
        }

        composeTestRule.onNodeWithTag("home_greeting_text")
            .assertIsDisplayed()
            .assertTextContains("আসসালামু আলাইকুম, করিম")
    }

    @Test
    fun testNameFieldLabelAlignmentAndSpacing() {
        val testViewModel = PersonalizationViewModel(themePreferences, AppThemeMode.NEUTRAL)
        composeTestRule.setContent {
            MediciTheme {
                PersonalizationScreen(
                    currentTheme = AppThemeMode.NEUTRAL,
                    onThemeSelected = {},
                    onSaveAndContinue = { _, _, _ -> },
                    onSkip = {},
                    viewModel = testViewModel
                )
            }
        }

        val labelRowBounds = composeTestRule.onNodeWithTag("personalization_name_label_row").getBoundsInRoot()
        val inputBounds = composeTestRule.onNodeWithTag("personalization_name_input").getBoundsInRoot()
        val iconBounds = composeTestRule.onNodeWithTag("personalization_name_icon").getBoundsInRoot()
        val labelTextBounds = composeTestRule.onNodeWithTag("personalization_name_label_text").getBoundsInRoot()

        // 1. Left-aligned at same X-coordinate
        assertEquals(labelRowBounds.left.value, inputBounds.left.value, 0.5f)

        // 2. Vertical spacing between label row and TextField is 8.dp
        val gapDp = inputBounds.top.value - labelRowBounds.bottom.value
        assertEquals(8f, gapDp, 0.5f)

        // 3. Icon and label are vertically centered
        val iconCenterY = (iconBounds.top.value + iconBounds.bottom.value) / 2f
        val labelCenterY = (labelTextBounds.top.value + labelTextBounds.bottom.value) / 2f
        assertEquals(iconCenterY, labelCenterY, 1.0f)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testCustomGreetingChipTextFieldDataStoreAndMaxLength() = runTest {
        val testViewModel = PersonalizationViewModel(themePreferences, AppThemeMode.NEUTRAL)

        composeTestRule.setContent {
            MediciTheme {
                PersonalizationScreen(
                    currentTheme = AppThemeMode.NEUTRAL,
                    onThemeSelected = {},
                    onSaveAndContinue = { _, _, _ -> },
                    onSkip = {},
                    viewModel = testViewModel
                )
            }
        }

        // Initially custom text field is hidden
        composeTestRule.onNodeWithTag("greeting_chip_custom").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("personalization_custom_greeting_input").assertDoesNotExist()

        // Enter user name
        composeTestRule.onNodeWithTag("personalization_name_input")
            .performScrollTo()
            .performTextReplacement("নাদিয়া")

        // Select Custom chip -> OutlinedTextField appears with expected label
        composeTestRule.onNodeWithTag("greeting_chip_custom").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("personalization_custom_greeting_input")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("আপনার পছন্দের সম্বোধন লিখুন").assertIsDisplayed()

        // Enforce max 30 chars
        val overLimit = "123456789012345678901234567890EXTRA"
        composeTestRule.onNodeWithTag("personalization_custom_greeting_input")
            .performTextReplacement(overLimit)
        composeTestRule.waitForIdle()
        assertEquals(30, testViewModel.uiState.value.customGreeting.length)
        assertEquals("123456789012345678901234567890", testViewModel.uiState.value.customGreeting)

        // Enter custom Bengali greeting and save
        composeTestRule.onNodeWithTag("personalization_custom_greeting_input")
            .performTextReplacement("সুপ্রভাত প্রিয়")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("personalization_get_started_button")
            .performClick()
        composeTestRule.waitForIdle()

        // Wait briefly for DataStore write coroutine
        val deadline = System.currentTimeMillis() + 3000L
        while (themePreferences.customGreetingFlow.first() != "সুপ্রভাত প্রিয়" && System.currentTimeMillis() < deadline) {
            delay(25)
        }

        assertEquals("custom", themePreferences.greetingPreferenceFlow.first())
        assertEquals("সুপ্রভাত প্রিয়", themePreferences.customGreetingFlow.first())
        assertEquals("নাদিয়া", themePreferences.userNameFlow.first())
    }

    @Test
    fun testHomeViewModelAppliesCustomGreetingWithAndWithoutName() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            themePreferences.savePersonalization(
                userName = "তারেক",
                greetingPreference = "custom",
                themeGender = "neutral",
                customGreeting = "সুস্থ থাকুন"
            )

            val homeVm = HomeViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                reminderRepository = ReminderRepository(db.reminderDao()),
                themePreferences = themePreferences
            )
            val collectJob = backgroundScope.launch { homeVm.uiState.collect {} }

            val deadline1 = System.currentTimeMillis() + 3000L
            while (homeVm.uiState.value.finalGreeting != "সুস্থ থাকুন, তারেক" && System.currentTimeMillis() < deadline1) {
                delay(25)
            }
            assertEquals("custom", homeVm.uiState.value.greetingPreference)
            assertEquals("সুস্থ থাকুন", homeVm.uiState.value.customGreeting)
            assertEquals("সুস্থ থাকুন, তারেক", homeVm.uiState.value.finalGreeting)

            // Without name
            themePreferences.savePersonalization(
                userName = "",
                greetingPreference = "custom",
                themeGender = "neutral",
                customGreeting = "শুভ দিন"
            )
            val deadline2 = System.currentTimeMillis() + 3000L
            while (homeVm.uiState.value.finalGreeting != "শুভ দিন" && System.currentTimeMillis() < deadline2) {
                delay(25)
            }
            assertEquals("শুভ দিন", homeVm.uiState.value.finalGreeting)

            collectJob.cancel()
        } finally {
            db.close()
        }
    }
}
