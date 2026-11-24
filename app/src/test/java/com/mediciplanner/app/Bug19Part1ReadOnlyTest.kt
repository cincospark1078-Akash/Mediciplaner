package com.mediciplanner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bug19Part1ReadOnlyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createTestPrescriptionWithMedicines(isLatest: Boolean, versionNumber: Int): PrescriptionWithMedicines {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            parentId = "parent-123",
            versionNumber = versionNumber,
            isLatest = isLatest,
            date = "2024-11-20",
            doctorName = "ডা. আহমেদ",
            createdAt = "2024-11-20T10:00:00"
        )
        val med = MedicineEntity(
            id = "med-test-1",
            prescriptionId = presId,
            name = "প্যারাসিটামল",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-20T10:00:00"
        )
        val sched = DoseScheduleEntity(
            id = "sched-test-1",
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val medWithSched = MedicineWithSchedules(
            medicine = med,
            doseSchedules = listOf(DoseScheduleWithReminders(doseSchedule = sched))
        )
        return PrescriptionWithMedicines(
            prescription = prescription,
            medicines = listOf(medWithSched)
        )
    }

    @Test
    fun testLatestVersionShowsEditDeleteAndAddMedicineCta() {
        val latestPrescription = createTestPrescriptionWithMedicines(isLatest = true, versionNumber = 2)

        composeTestRule.setContent {
            MediciTheme {
                PrescriptionDetailBottomSheet(
                    prescriptionWithMedicines = latestPrescription,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        // 1. Dot-Menu (More Options) on top right is displayed
        composeTestRule.onNodeWithTag("prescription_more_options_button").assertIsDisplayed()

        // 2. Add New Medicine Bottom CTA exists
        composeTestRule.onNodeWithTag("add_medicine_bottom_button").assertExists()

        // 3. Warning banner does NOT exist for latest version
        composeTestRule.onNodeWithTag("prescription_readonly_warning_banner").assertDoesNotExist()

        // 4. Medicine card More button exists
        composeTestRule.onNodeWithTag("medicine_more_button_med-test-1").assertExists()
    }

    @Test
    fun testOldVersionHidesDotMenuAndAddMedicineCtaAndShowsWarningBanner() {
        val oldPrescription = createTestPrescriptionWithMedicines(isLatest = false, versionNumber = 1)

        composeTestRule.setContent {
            MediciTheme {
                PrescriptionDetailBottomSheet(
                    prescriptionWithMedicines = oldPrescription,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        // 1. Dot-Menu on top right does NOT exist (hidden)
        composeTestRule.onNodeWithTag("prescription_more_options_button").assertDoesNotExist()

        // 2. Add New Medicine Bottom CTA does NOT exist (hidden)
        composeTestRule.onNodeWithTag("add_medicine_bottom_button").assertDoesNotExist()

        // 3. Warning banner appears and is displayed
        composeTestRule.onNodeWithTag("prescription_readonly_warning_banner").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prescription_readonly_warning_text").assertIsDisplayed()

        // 4. Medicine card More button does NOT exist (hidden for old versions)
        composeTestRule.onNodeWithTag("medicine_more_button_med-test-1").assertDoesNotExist()
    }
}
