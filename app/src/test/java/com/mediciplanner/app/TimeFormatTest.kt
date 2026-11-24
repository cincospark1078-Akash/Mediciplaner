package com.mediciplanner.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.TimeFormatHelper
import com.mediciplanner.app.core.util.formatTime
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.notification.NotificationHelper
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.followup.FollowUpAddEditSheet
import com.mediciplanner.app.ui.screens.followup.FollowUpDetailsBottomSheet
import com.mediciplanner.app.ui.screens.followup.FollowUpDraftField
import com.mediciplanner.app.ui.screens.followup.FollowUpViewModel
import com.mediciplanner.app.ui.screens.home.HomeViewModel
import com.mediciplanner.app.ui.screens.home.components.MedicineDetailBottomSheet
import com.mediciplanner.app.ui.screens.home.components.NextReminderCard
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.components.Card3ScheduleDose
import com.mediciplanner.app.ui.screens.track.TrackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class TimeFormatTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: MediciDatabase
    private lateinit var themePreferences: ThemePreferences

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        themePreferences = ThemePreferences(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testFormatTimeHelper12And24HourEdgeCases() {
        // 12H formatting
        assertEquals("07:00 PM", TimeFormatHelper.formatTime("19:00", is24Hour = false))
        assertEquals("08:30 AM", formatTime("08:30", is24Hour = false))
        assertEquals("12:15 AM", formatTime("00:15", is24Hour = false))
        assertEquals("12:00 PM", formatTime("12:00", is24Hour = false))
        assertEquals("07:00 PM", formatTime("2026-11-12T19:00:00", is24Hour = false))

        // 24H formatting
        assertEquals("19:00", TimeFormatHelper.formatTime("19:00", is24Hour = true))
        assertEquals("08:30", formatTime("08:30", is24Hour = true))
        assertEquals("00:15", formatTime("00:15", is24Hour = true))
        assertEquals("12:00", formatTime("12:00", is24Hour = true))
        assertEquals("19:00", formatTime("07:00 PM", is24Hour = true))
        assertEquals("19:45", formatTime("2026-11-12T19:45:00", is24Hour = true))

        // Blank handling
        assertEquals("", formatTime("", is24Hour = false))
    }

    @Test
    fun testViewModelsExposeTimeFormatFromDataStore() = runTest {
        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_24H)

        val prescriptionRepo = PrescriptionRepository(database.prescriptionDao())
        val reminderRepo = ReminderRepository(database.reminderDao())
        val followUpRepo = FollowUpRepository(database.followUpDao())

        val homeVm = HomeViewModel(prescriptionRepo, reminderRepo, themePreferences)
        val trackVm = TrackViewModel(prescriptionRepo, reminderRepo, null, themePreferences)
        val followUpVm = FollowUpViewModel(followUpRepo, null, themePreferences = themePreferences)
        val carouselVm = PrescriptionCarouselViewModel(prescriptionRepo, reminderRepo, null, themePreferences)

        val jobs = listOf(
            backgroundScope.launch { homeVm.uiState.collect {} },
            backgroundScope.launch { trackVm.uiState.collect {} },
            backgroundScope.launch { followUpVm.uiState.collect {} },
            backgroundScope.launch { carouselVm.timeFormat.collect {} }
        )

        val deadline = System.currentTimeMillis() + 3000L
        while ((!homeVm.uiState.value.is24Hour ||
                    !trackVm.uiState.value.is24Hour ||
                    !followUpVm.uiState.value.is24Hour ||
                    carouselVm.timeFormat.value != ThemePreferences.TIME_FORMAT_24H) &&
            System.currentTimeMillis() < deadline
        ) {
            kotlinx.coroutines.delay(25)
        }

        assertTrue(homeVm.uiState.value.is24Hour)
        assertTrue(trackVm.uiState.value.is24Hour)
        assertTrue(followUpVm.uiState.value.is24Hour)
        assertEquals(ThemePreferences.TIME_FORMAT_24H, carouselVm.timeFormat.value)

        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_12H)
        val deadline2 = System.currentTimeMillis() + 3000L
        while ((homeVm.uiState.value.is24Hour ||
                    trackVm.uiState.value.is24Hour ||
                    followUpVm.uiState.value.is24Hour ||
                    carouselVm.timeFormat.value != ThemePreferences.TIME_FORMAT_12H) &&
            System.currentTimeMillis() < deadline2
        ) {
            kotlinx.coroutines.delay(25)
        }

        assertFalse(homeVm.uiState.value.is24Hour)
        assertFalse(trackVm.uiState.value.is24Hour)
        assertFalse(followUpVm.uiState.value.is24Hour)
        assertEquals(ThemePreferences.TIME_FORMAT_12H, carouselVm.timeFormat.value)

        jobs.forEach { it.cancel() }
    }

    @Test
    fun testNotificationHelperFormatsScheduledTimeAccordingToPreference() = runTest {
        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_24H)
        assertTrue(NotificationHelper.is24HourPreferred(context))

        val (_, body24Medicine) = NotificationHelper.buildReminderNotificationText(
            context = context,
            type = "medicine",
            itemName = "Napa",
            scheduledAt = "2026-11-12T19:00:00",
            is24Hour = true
        )
        assertTrue("Expected 19:00 in body, got: $body24Medicine", body24Medicine.contains("19:00"))

        val (_, body12FollowUp) = NotificationHelper.buildReminderNotificationText(
            context = context,
            type = "followup",
            itemName = "Dr. Rahman",
            scheduledAt = "2026-11-12T19:00:00",
            is24Hour = false
        )
        assertTrue("Expected 07:00 PM in body, got: $body12FollowUp", body12FollowUp.contains("07:00 PM"))

        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_12H)
        assertFalse(NotificationHelper.is24HourPreferred(context))
        assertEquals(ThemePreferences.TIME_FORMAT_12H, themePreferences.timeFormatFlow.first())
    }

    @Test
    fun testNextReminderCardApplies12HAnd24H() {
        val med = MedicineEntity(
            id = "m1",
            prescriptionId = "p1",
            name = "Napa Extra",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            foodRule = FoodRule.AFTER,
            durationDays = 7,
            createdAt = "2026-11-12T08:00:00"
        )
        val sched = DoseScheduleEntity(
            id = "s1",
            medicineId = "m1",
            slot = DoseSlot.EVENING,
            time = "19:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val rem = ReminderEntity(
            id = "r1",
            doseScheduleId = "s1",
            scheduledAt = "2026-11-12T19:00:00",
            status = ReminderStatus.PENDING
        )

        val use24HourState = androidx.compose.runtime.mutableStateOf(false)
        composeTestRule.setContent {
            MediciTheme {
                NextReminderCard(
                    reminder = rem,
                    medicine = med,
                    schedule = sched,
                    onMarkAsTaken = {},
                    is24Hour = use24HourState.value
                )
            }
        }
        composeTestRule.onNodeWithText("সন্ধ্যা ০৭:০০ PM").assertIsDisplayed()

        use24HourState.value = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("সন্ধ্যা ১৯:০০").assertIsDisplayed()
    }

    @Test
    fun testCard3ScheduleDoseAndTimePickerUsePreference() {
        val draftMed = DraftMedicine(
            name = "Seclo",
            type = MedicineType.CAPSULE,
            schedules = listOf(
                DraftSchedule(
                    slot = DoseSlot.EVENING,
                    time = "19:00",
                    doseAmount = "1",
                    doseUnit = "টি",
                    isExpanded = true
                )
            )
        )

        composeTestRule.setContent {
            MediciTheme {
                Card3ScheduleDose(
                    draftMedicine = draftMed,
                    onToggleSlot = {},
                    onToggleExpanded = {},
                    onTimeChanged = { _, _ -> },
                    onAmountChanged = { _, _ -> },
                    onUnitChanged = { _, _ -> },
                    onSlotFoodRuleChanged = { _, _ -> },
                    is24Hour = false
                )
            }
        }

        // 12H formatted time shown in header & row
        composeTestRule.onNodeWithText("(07:00 PM)").assertIsDisplayed()
        composeTestRule.onNodeWithText("07:00 PM").assertIsDisplayed()

        // Open time picker in 12H mode -> AM/PM selector should be shown
        composeTestRule.onNodeWithTag("slot_time_row_evening").performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithText("PM").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun testCard3TimePickerIn24HourModeHidesAmPmSelector() {
        val draftMed = DraftMedicine(
            name = "Seclo",
            type = MedicineType.CAPSULE,
            schedules = listOf(
                DraftSchedule(
                    slot = DoseSlot.EVENING,
                    time = "19:00",
                    doseAmount = "1",
                    doseUnit = "টি",
                    isExpanded = true
                )
            )
        )

        composeTestRule.setContent {
            MediciTheme {
                Card3ScheduleDose(
                    draftMedicine = draftMed,
                    onToggleSlot = {},
                    onToggleExpanded = {},
                    onTimeChanged = { _, _ -> },
                    onAmountChanged = { _, _ -> },
                    onUnitChanged = { _, _ -> },
                    onSlotFoodRuleChanged = { _, _ -> },
                    is24Hour = true
                )
            }
        }

        composeTestRule.onNodeWithText("(19:00)").assertIsDisplayed()
        composeTestRule.onNodeWithText("19:00").assertIsDisplayed()

        composeTestRule.onNodeWithTag("slot_time_row_evening").performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        assertTrue(
            composeTestRule.onAllNodesWithText("PM").fetchSemanticsNodes().isEmpty()
        )
    }

    @Test
    fun testFollowUpDetailsAndSheetUseTimeFormatPreference() = runTest {
        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_24H)

        val followUpVm = FollowUpViewModel(
            FollowUpRepository(database.followUpDao()),
            ReminderScheduler(context),
            themePreferences = themePreferences
        )
        followUpVm.updateDraft(FollowUpDraftField.TIME, "19:30")

        val job = backgroundScope.launch { followUpVm.uiState.collect {} }
        val deadline = System.currentTimeMillis() + 3000L
        while (!followUpVm.uiState.value.is24Hour && System.currentTimeMillis() < deadline) {
            kotlinx.coroutines.delay(25)
        }
        assertTrue(followUpVm.uiState.value.is24Hour)

        composeTestRule.setContent {
            MediciTheme {
                FollowUpAddEditSheet(
                    viewModel = followUpVm,
                    onDismiss = {},
                    onDelete = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        composeTestRule.onNodeWithTag("doctor_visit_time_clickable").performScrollTo()
        composeTestRule.onNodeWithText("19:30").assertIsDisplayed()

        composeTestRule.onNodeWithTag("doctor_visit_time_clickable").performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        assertTrue(composeTestRule.onAllNodesWithText("PM").fetchSemanticsNodes().isEmpty())

        job.cancel()
        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_12H)
    }

    @Test
    fun testFollowUpDetailsBottomSheetDisplaysFormattedTime() {
        val followUp = FollowUpEntity(
            id = "fu1",
            type = FollowUpType.DOCTOR_VISIT,
            title = "Dr. Karim",
            location = "Square Hospital",
            date = "2026-12-15",
            reminderTime = "19:45",
            reminderEnabled = true
        )

        composeTestRule.setContent {
            MediciTheme {
                FollowUpDetailsBottomSheet(
                    followUp = followUp,
                    is24Hour = false,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteConfirm = {}
                )
            }
        }
        composeTestRule.onNodeWithText("07:45 PM").assertIsDisplayed()
    }
}
