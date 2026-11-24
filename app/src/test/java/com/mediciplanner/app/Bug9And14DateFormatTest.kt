package com.mediciplanner.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCardItem
import com.mediciplanner.app.ui.screens.prescription.components.Card1BaseInfo
import com.mediciplanner.app.ui.screens.prescription.components.PrescriptionDetailBottomSheet
import com.mediciplanner.app.ui.screens.prescription.components.formatBengaliPrescriptionDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug9And14DateFormatTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun samplePrescription(id: String, date: String): PrescriptionWithMedicines {
        val prescription = PrescriptionEntity(
            id = id,
            parentId = id,
            versionNumber = 1,
            isLatest = true,
            date = date,
            doctorName = "ডা. করিম",
            createdAt = "2026-11-19T09:50:00"
        )
        val med = MedicineEntity(
            id = "med-$id",
            prescriptionId = id,
            name = "নাপা",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2026-11-19T09:50:00"
        )
        val sched = DoseScheduleEntity(
            id = "sched-$id",
            medicineId = "med-$id",
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
    fun testFullMonthNamesWithoutTruncation() {
        // ISO dates
        assertEquals("১৯ নভেম্বর, ২০২৬", formatBengaliPrescriptionDate("2026-11-19"))
        assertEquals("০৪ নভেম্বর, ২০২৬", formatBengaliPrescriptionDate("2026-11-04"))
        assertEquals("১৫ নভেম্বর, ২০২৪", formatBengaliPrescriptionDate("2024-11-15"))

        // Legacy truncated Bengali formats
        assertEquals("১৯ নভেম্বর, ২০২৬", formatBengaliPrescriptionDate("১৯ নভে., ২০২৬"))
        assertEquals("১৫ নভেম্বর, ২০২৬", formatBengaliPrescriptionDate("15 নভে, 2026"))
        assertEquals("০৫ ডিসেম্বর, ২০২৬", formatBengaliPrescriptionDate("05 ডিসে., 2026"))
        assertEquals("১০ সেপ্টেম্বর, ২০২৬", formatBengaliPrescriptionDate("১০ সেপ্টে., ২০২৬"))
        assertEquals("০১ জানুয়ারি, ২০২৬".replace("রি", "রি").let { formatBengaliDate(LocalDate.of(2026, 1, 1)) },
            formatBengaliPrescriptionDate("০১ জানু., ২০২৬"))
        assertEquals(formatBengaliDate(LocalDate.of(2026, 2, 14)), formatBengaliPrescriptionDate("14 ফেব্রু., 2026"))
        assertEquals(formatBengaliDate(LocalDate.of(2026, 4, 20)), formatBengaliPrescriptionDate("২০ এপ্রি., ২০২৬"))
        assertEquals(formatBengaliDate(LocalDate.of(2026, 8, 12)), formatBengaliPrescriptionDate("১২ আগ., ২০২৬"))
        assertEquals(formatBengaliDate(LocalDate.of(2026, 10, 8)), formatBengaliPrescriptionDate("08 অক্টো., 2026"))

        // English abbreviated month format
        assertEquals("১৯ নভেম্বর, ২০২৬", formatBengaliPrescriptionDate("19 Nov, 2026"))

        // Verify no truncated month period remains
        val formatted = formatBengaliPrescriptionDate("১৯ নভে., ২০২৬")
        assertTrue(formatted.contains("নভেম্বর"))
        assertFalse(formatted.contains("নভে."))
    }

    @Test
    fun testCard1BaseInfoDatePickerProducesFullMonthAndDisplaysFullMonth() {
        var pickedDate: String? = null
        composeTestRule.setContent {
            var dateState by remember { mutableStateOf("১৯ নভে., ২০২৬") }
            MediciTheme {
                Card1BaseInfo(
                    draftPrescription = DraftPrescription(date = dateState),
                    onDateChanged = {
                        dateState = it
                        pickedDate = it
                    },
                    onDoctorNameChanged = {},
                    onLocationChanged = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Existing legacy truncated date is rendered with full month name in Card 1
        composeTestRule.onNodeWithTag("prescription_date_input")
            .assertIsDisplayed()
            .assertTextEquals("প্রেসক্রিপশনের তারিখ *", "১৯ নভেম্বর, ২০২৬")

        // Open Material 3 DatePickerDialog and verify confirm/cancel buttons are displayed
        composeTestRule.onNodeWithTag("prescription_date_input").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prescription_date_picker_confirm")
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("prescription_date_picker_cancel")
            .assertIsDisplayed()

        // Confirm the pre-selected date (19 Nov, 2026) and verify dialog closes & full month is output
        composeTestRule.onNodeWithTag("prescription_date_picker_confirm").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prescription_date_picker_confirm")
            .assertDoesNotExist()
        assertEquals("১৯ নভেম্বর, ২০২৬", pickedDate)
        composeTestRule.onNodeWithTag("prescription_date_input")
            .assertIsDisplayed()
            .assertTextEquals("প্রেসক্রিপশনের তারিখ *", "১৯ নভেম্বর, ২০২৬")
    }

    @Test
    fun testPrescriptionListCardDisplaysFullMonthName() {
        val itemIso = samplePrescription("p1", "2026-11-19")
        val itemLegacyTruncated = samplePrescription("p2", "১৯ নভে., ২০২৬")

        composeTestRule.setContent {
            MediciTheme {
                androidx.compose.foundation.layout.Column {
                    PrescriptionCardItem(item = itemIso, onClick = {})
                    PrescriptionCardItem(item = itemLegacyTruncated, onClick = {})
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("prescription_item_date_p1", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("১৯ নভেম্বর, ২০২৬")

        composeTestRule.onNodeWithTag("prescription_item_date_p2", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("১৯ নভেম্বর, ২০২৬")
    }

    @Test
    fun testPrescriptionDetailBottomSheetDisplaysFullMonthName() {
        val item = samplePrescription("p3", "১৯ নভে., ২০২৬")

        composeTestRule.setContent {
            MediciTheme {
                PrescriptionDetailBottomSheet(
                    prescriptionWithMedicines = item,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detail_prescription_date")
            .assertExists()
            .assertTextEquals("১৯ নভেম্বর, ২০২৬")
    }
}
