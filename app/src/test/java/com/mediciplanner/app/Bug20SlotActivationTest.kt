package com.mediciplanner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.SlotActivationHelper
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.ui.screens.home.components.MedicineCard
import com.mediciplanner.app.ui.screens.home.components.MedicineDetailBottomSheet
import com.mediciplanner.app.ui.screens.home.components.NextReminderCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug20SlotActivationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleMedicine(id: String = "med-20"): MedicineEntity = MedicineEntity(
        id = id,
        prescriptionId = "pres-20",
        name = "নাপা",
        type = MedicineType.TABLET,
        strength = 500.0,
        strengthUnit = "mg",
        durationDays = 7,
        foodRule = FoodRule.AFTER,
        createdAt = "2026-11-18T08:00:00"
    )

    private fun sampleSchedule(time: String = "20:00"): DoseScheduleEntity = DoseScheduleEntity(
        id = "sched-20",
        medicineId = "med-20",
        slot = DoseSlot.NIGHT,
        time = time,
        doseAmount = 1.0,
        doseUnit = "টি"
    )

    @Test
    fun testIsSlotActiveCalculationBoundary() {
        val date = LocalDate.of(2026, 11, 18)

        // 31 minutes before 08:00 -> 07:29 -> inactive
        assertFalse(
            SlotActivationHelper.isSlotActive(
                scheduledTime = "08:00",
                currentTime = LocalDateTime.of(date, LocalTime.of(7, 29))
            )
        )

        // Exactly 30 minutes before 08:00 -> 07:30 -> active
        assertTrue(
            SlotActivationHelper.isSlotActive(
                scheduledTime = "08:00",
                currentTime = LocalDateTime.of(date, LocalTime.of(7, 30))
            )
        )

        // 15 minutes before 08:00 -> 07:45 -> active
        assertTrue(
            SlotActivationHelper.isSlotActive(
                scheduledTime = "08:00",
                currentTime = LocalDateTime.of(date, LocalTime.of(7, 45))
            )
        )

        // After scheduled time -> 09:10 -> active
        assertTrue(
            SlotActivationHelper.isSlotActive(
                scheduledTime = "08:00",
                currentTime = LocalDateTime.of(date, LocalTime.of(9, 10))
            )
        )

        // Midnight wraparound: 00:15 scheduled, current 00:05 -> active
        assertTrue(
            SlotActivationHelper.isSlotActive(
                scheduledTime = "00:15",
                currentTime = LocalDateTime.of(date, LocalTime.of(0, 5))
            )
        )
    }

    @Test
    fun testMedicineCardDisabledBeforeWindowAndShowsSamayHoyni() {
        val med = sampleMedicine()
        val sched = sampleSchedule("20:00")
        var clickedCount = 0

        composeTestRule.setContent {
            MediciTheme {
                MedicineCard(
                    medicine = med,
                    schedule = sched,
                    isTaken = false,
                    isSlotActive = false,
                    onMarkAsTaken = { clickedCount++ }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_card_take_button_med-20")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, clickedCount)
        composeTestRule.onNodeWithText("সময় হয়নি").assertIsDisplayed()
    }

    @Test
    fun testMedicineCardEnabledWithinWindowAndHidesSamayHoyni() {
        val med = sampleMedicine()
        val sched = sampleSchedule("20:00")
        var clickedCount = 0

        composeTestRule.setContent {
            MediciTheme {
                MedicineCard(
                    medicine = med,
                    schedule = sched,
                    isTaken = false,
                    isSlotActive = true,
                    onMarkAsTaken = { clickedCount++ }
                )
            }
        }
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_card_take_button_med-20")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, clickedCount)
        composeTestRule.onNodeWithText("সময় হয়নি").assertDoesNotExist()
    }

    @Test
    fun testMedicineDetailBottomSheetDisabledBeforeWindowAndShowsSamayHoyni() {
        val med = sampleMedicine()
        val sched = sampleSchedule("20:00")
        var clickedCount = 0

        composeTestRule.setContent {
            MediciTheme {
                MedicineDetailBottomSheet(
                    medicine = med,
                    schedules = listOf(sched),
                    isTaken = false,
                    isSlotActive = false,
                    onTakeNowClick = { clickedCount++ },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("take_now_bottom_sheet_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
            .performClick()

        assertEquals(0, clickedCount)
        composeTestRule.onNodeWithText("সময় হয়নি").assertIsDisplayed()
    }

    @Test
    fun testMedicineDetailBottomSheetEnabledWithinWindow() {
        val med = sampleMedicine()
        val sched = sampleSchedule("20:00")
        var clickedCount = 0

        composeTestRule.setContent {
            MediciTheme {
                MedicineDetailBottomSheet(
                    medicine = med,
                    schedules = listOf(sched),
                    isTaken = false,
                    isSlotActive = true,
                    onTakeNowClick = { clickedCount++ },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("take_now_bottom_sheet_button")
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()

        assertEquals(1, clickedCount)
        composeTestRule.onNodeWithText("সময় হয়নি").assertDoesNotExist()
    }

    @Test
    fun testNextReminderCardDisabledBeforeWindowAndEnabledWithinWindow() {
        val med = sampleMedicine()
        val sched = sampleSchedule("20:00")
        val reminder = ReminderEntity(
            id = "rem-20",
            doseScheduleId = sched.id,
            scheduledAt = "2026-11-18T20:00:00",
            status = ReminderStatus.PENDING
        )
        val activeState = androidx.compose.runtime.mutableStateOf(false)

        composeTestRule.setContent {
            MediciTheme {
                NextReminderCard(
                    reminder = reminder,
                    medicine = med,
                    schedule = sched,
                    onMarkAsTaken = {},
                    isSlotActive = activeState.value
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mark_as_taken_button")
            .assertIsDisplayed()
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("সময় হয়নি").assertIsDisplayed()

        activeState.value = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("mark_as_taken_button")
            .assertIsDisplayed()
            .assertIsEnabled()
        composeTestRule.onNodeWithText("সময় হয়নি").assertDoesNotExist()
    }
}
