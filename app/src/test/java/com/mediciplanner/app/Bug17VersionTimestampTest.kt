package com.mediciplanner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.relation.DoseScheduleWithReminders
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.ui.screens.prescription.components.PrescriptionDetailBottomSheet
import com.mediciplanner.app.ui.screens.prescription.components.formatVersionTimestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug17VersionTimestampTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun samplePrescription(createdAt: String): PrescriptionWithMedicines {
        val prescription = PrescriptionEntity(
            id = "pres-17",
            parentId = "pres-17",
            versionNumber = 1,
            isLatest = true,
            date = "2026-11-19",
            doctorName = "ডা. করিম",
            createdAt = createdAt
        )
        val med = MedicineEntity(
            id = "med-17",
            prescriptionId = "pres-17",
            name = "নাপা",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = createdAt
        )
        val sched = DoseScheduleEntity(
            id = "sched-17",
            medicineId = "med-17",
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        return PrescriptionWithMedicines(
            prescription = prescription,
            medicines = listOf(MedicineWithSchedules(med, listOf(DoseScheduleWithReminders(sched))))
        )
    }

    @Test
    fun testFormatVersionTimestampFullMonthAnd12H24H() {
        val morning12H = formatVersionTimestamp("2026-11-19T09:50:00", is24Hour = false)
        assertEquals("১৯ নভেম্বর, ২০২৬ - 09:50 AM", morning12H)
        assertTrue(morning12H.contains("নভেম্বর"))
        assertTrue(morning12H.contains("AM"))

        val evening12H = formatVersionTimestamp("2026-11-19T21:50:00", is24Hour = false)
        assertEquals("১৯ নভেম্বর, ২০২৬ - 09:50 PM", evening12H)
        assertTrue(evening12H.contains("নভেম্বর"))
        assertTrue(evening12H.contains("PM"))

        val morning24H = formatVersionTimestamp("2026-11-19T09:50:00", is24Hour = true)
        assertEquals("১৯ নভেম্বর, ২০২৬ - 09:50", morning24H)
        assertTrue(morning24H.contains("নভেম্বর"))
        assertFalse(morning24H.contains("AM"))

        val evening24H = formatVersionTimestamp("2026-11-19T21:50:00", is24Hour = true)
        assertEquals("১৯ নভেম্বর, ২০২৬ - 21:50", evening24H)
        assertTrue(evening24H.contains("নভেম্বর"))
        assertFalse(evening24H.contains("PM"))
    }

    @Test
    fun testVersionHistoryDialogDisplaysTimestampFor12HAnd24H() {
        val app = androidx.test.core.app.ApplicationProvider.getApplicationContext<MediciApplication>()
        val item = samplePrescription("2026-11-19T21:50:00")
        kotlinx.coroutines.runBlocking {
            app.prescriptionRepository.saveFullPrescription(
                prescription = item.prescription,
                medicines = item.medicines.map { it.medicine },
                schedules = item.medicines.flatMap { m -> m.doseSchedules.map { it.doseSchedule } }
            )
        }

        try {
            val is24HourState = androidx.compose.runtime.mutableStateOf(false)

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionDetailBottomSheet(
                        prescriptionWithMedicines = item,
                        onDismiss = {},
                        onEditClick = {},
                        onDeleteClick = {},
                        is24HourOverride = is24HourState.value
                    )
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // Open version history dialog
            composeTestRule.onNodeWithTag("version_history_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.mainClock.advanceTimeBy(500)
            composeTestRule.waitForIdle()

            // 12H shows full month + AM/PM
            composeTestRule.onNodeWithTag("version_item_timestamp_1", useUnmergedTree = true)
                .assertExists()
                .assertTextEquals("১৯ নভেম্বর, ২০২৬ - 09:50 PM")

            // Switch to 24H -> shows full month + 24H time
            is24HourState.value = true
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("version_item_timestamp_1", useUnmergedTree = true)
                .assertExists()
                .assertTextEquals("১৯ নভেম্বর, ২০২৬ - 21:50")
        } finally {
            kotlinx.coroutines.runBlocking {
                app.prescriptionRepository.deletePrescriptionById(item.prescription.id)
            }
        }
    }
}
