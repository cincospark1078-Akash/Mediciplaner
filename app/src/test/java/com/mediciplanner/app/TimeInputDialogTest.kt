package com.mediciplanner.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.components.AccessibleTimeInputDialog
import com.mediciplanner.app.ui.screens.followup.FollowUpAddEditSheet
import com.mediciplanner.app.ui.screens.followup.FollowUpViewModel
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.components.Card3ScheduleDose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
class TimeInputDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: MediciDatabase

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testAccessibleTimeInputDialogConfirmAndDismiss() {
        var confirmedHour = -1
        var confirmedMinute = -1
        var dismissed = false

        composeTestRule.setContent {
            MediciTheme {
                AccessibleTimeInputDialog(
                    initialHour = 14,
                    initialMinute = 35,
                    is24Hour = true,
                    onConfirm = { h, m ->
                        confirmedHour = h
                        confirmedMinute = m
                    },
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_confirm_button").assertIsDisplayed().performClick()

        assertEquals(14, confirmedHour)
        assertEquals(35, confirmedMinute)

        composeTestRule.onNodeWithTag("time_input_cancel_button").assertIsDisplayed().performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testCard3ScheduleDoseOpensTimeInputDialog() {
        var updatedSlot: DoseSlot? = null
        var updatedTime: String? = null
        val draftMedicine = DraftMedicine(
            name = "Napa",
            type = MedicineType.TABLET,
            schedules = listOf(
                DraftSchedule(
                    slot = DoseSlot.MORNING,
                    time = "08:30",
                    doseAmount = "1",
                    doseUnit = "টি",
                    isExpanded = true
                )
            )
        )

        composeTestRule.setContent {
            MediciTheme {
                Card3ScheduleDose(
                    draftMedicine = draftMedicine,
                    onToggleSlot = {},
                    onToggleExpanded = {},
                    onTimeChanged = { slot, time ->
                        updatedSlot = slot
                        updatedTime = time
                    },
                    onAmountChanged = { _, _ -> },
                    onUnitChanged = { _, _ -> },
                    onSlotFoodRuleChanged = { _, _ -> }
                )
            }
        }

        composeTestRule.onNodeWithTag("slot_time_row_morning").performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_confirm_button").performClick()

        assertEquals(DoseSlot.MORNING, updatedSlot)
        assertEquals("08:30", updatedTime)
        composeTestRule.onNodeWithTag("time_input_dialog").assertDoesNotExist()
    }

    @Test
    fun testFollowUpAddEditSheetOpensTimeInputDialog() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val followUpViewModel = FollowUpViewModel(
            FollowUpRepository(database.followUpDao()),
            ReminderScheduler(context)
        )

        composeTestRule.setContent {
            MediciTheme {
                FollowUpAddEditSheet(
                    viewModel = followUpViewModel,
                    onDismiss = {},
                    onDelete = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        composeTestRule.onNodeWithTag("doctor_visit_time_clickable")
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_field").assertIsDisplayed()
        composeTestRule.onNodeWithTag("time_input_cancel_button").performClick()
        composeTestRule.onNodeWithTag("time_input_dialog").assertDoesNotExist()
    }
}
