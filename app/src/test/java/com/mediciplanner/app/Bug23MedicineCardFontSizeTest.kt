package com.mediciplanner.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.sp
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.theme.NeutralTextSecondary
import com.mediciplanner.app.core.theme.WarningAmber
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.relation.DoseScheduleWithReminders
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.ui.screens.home.components.MedicineCard
import com.mediciplanner.app.ui.screens.prescription.components.PrescriptionDetailBottomSheet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug23MedicineCardFontSizeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun extractFontSizeSp(node: SemanticsNode): Float {
        val results = mutableListOf<TextLayoutResult>()
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        action?.action?.invoke(results)
        return results.first().layoutInput.style.fontSize.value
    }

    private fun sampleMedicine(id: String = "med-23"): Pair<MedicineEntity, DoseScheduleEntity> {
        val medicine = MedicineEntity(
            id = id,
            prescriptionId = "pres-23",
            name = "নাপা এক্সটেন্ড ট্যাবলেট খুব লম্বা নাম",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            note = "জ্বর থাকলে ভরা পেটে খাবেন",
            createdAt = "2026-11-19T09:50:00"
        )
        val schedule = DoseScheduleEntity(
            id = "sched-23",
            medicineId = id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি",
            foodRule = FoodRule.AFTER
        )
        return medicine to schedule
    }

    @Test
    fun testMedicineCardFontSizesAndLayoutIntact() {
        val (medicine, schedule) = sampleMedicine()

        composeTestRule.setContent {
            MediciTheme {
                MedicineCard(
                    medicine = medicine,
                    schedule = schedule,
                    isTaken = false,
                    slotName = "MORNING",
                    isSlotActive = true
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val nameNode = composeTestRule.onNodeWithTag("medicine_card_name_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val strengthNode = composeTestRule.onNodeWithTag("medicine_card_strength_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val typeNode = composeTestRule.onNodeWithTag("medicine_card_type_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val doseChipNode = composeTestRule.onNodeWithTag("medicine_card_dose_chip_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val foodChipNode = composeTestRule.onNodeWithTag("medicine_card_food_chip_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val noteNode = composeTestRule.onNodeWithTag("medicine_card_note_${medicine.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()

        val nameFontSize = extractFontSizeSp(nameNode)
        val strengthFontSize = extractFontSizeSp(strengthNode)
        val typeFontSize = extractFontSizeSp(typeNode)
        val doseChipFontSize = extractFontSizeSp(doseChipNode)
        val foodChipFontSize = extractFontSizeSp(foodChipNode)
        val noteFontSize = extractFontSizeSp(noteNode)

        assertTrue("Medicine name font size should be 18-20.sp, was $nameFontSize", nameFontSize in 18f..20f)
        assertEquals(14f, strengthFontSize, 0.01f)
        assertEquals(14f, typeFontSize, 0.01f)
        assertTrue("Dose chip font size should be 13-14.sp, was $doseChipFontSize", doseChipFontSize in 13f..14f)
        assertTrue("Food chip font size should be 13-14.sp, was $foodChipFontSize", foodChipFontSize in 13f..14f)
        assertEquals(13f, noteFontSize, 0.01f)

        // Verify card bounds remain within container without horizontal overflow
        val cardBounds = composeTestRule.onNodeWithTag("medicine_card_${medicine.id}").fetchSemanticsNode().boundsInRoot
        assertTrue(strengthNode.boundsInRoot.right <= cardBounds.right)
        assertTrue(foodChipNode.boundsInRoot.right <= cardBounds.right)
        assertTrue(noteNode.boundsInRoot.right <= cardBounds.right)
    }

    @Test
    fun testPrescriptionDetailBottomSheetUsesUpdatedMedicineCardFontSizes() {
        val (medicine, schedule) = sampleMedicine("med-23-detail")
        val item = PrescriptionWithMedicines(
            prescription = PrescriptionEntity(
                id = "pres-23-detail",
                parentId = "pres-23-detail",
                versionNumber = 1,
                isLatest = true,
                date = "2026-11-19",
                doctorName = "ডা. করিম",
                createdAt = "2026-11-19T09:50:00"
            ),
            medicines = listOf(MedicineWithSchedules(medicine, listOf(DoseScheduleWithReminders(schedule))))
        )

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
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        val nameNode = composeTestRule.onNodeWithTag("medicine_card_name_${medicine.id}", useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()
        val typeNode = composeTestRule.onNodeWithTag("medicine_card_type_${medicine.id}", useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()
        val strengthNode = composeTestRule.onNodeWithTag("medicine_card_strength_${medicine.id}", useUnmergedTree = true)
            .assertExists()
            .fetchSemanticsNode()

        assertEquals(18f, extractFontSizeSp(nameNode), 0.01f)
        assertEquals(14f, extractFontSizeSp(typeNode), 0.01f)
        assertEquals(14f, extractFontSizeSp(strengthNode), 0.01f)
    }

    @Test
    fun testMedicineCardStockVisibilityAndLowStockAmberColor() {
        val (baseMed, schedule) = sampleMedicine("med-stock-zero")
        val medNormalStock = baseMed.copy(id = "med-stock-normal", stockCount = 10, initialStock = 10)
        val medLowStock = baseMed.copy(id = "med-stock-low", stockCount = 5, initialStock = 10)

        composeTestRule.setContent {
            MediciTheme {
                androidx.compose.foundation.layout.Column {
                    MedicineCard(
                        medicine = baseMed.copy(stockCount = 0),
                        schedule = schedule,
                        isTaken = false,
                        slotName = "MORNING"
                    )
                    MedicineCard(
                        medicine = medNormalStock,
                        schedule = schedule,
                        isTaken = false,
                        slotName = "MORNING"
                    )
                    MedicineCard(
                        medicine = medLowStock,
                        schedule = schedule,
                        isTaken = false,
                        slotName = "MORNING"
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // stockCount == 0 -> hidden
        composeTestRule.onNodeWithTag("medicine_card_stock_med-stock-zero", useUnmergedTree = true)
            .assertDoesNotExist()

        // stockCount == 10 -> displayed in 12.sp with textSecondary
        val normalNode = composeTestRule.onNodeWithTag("medicine_card_stock_med-stock-normal", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("স্টক: ১০ পিস")
            .fetchSemanticsNode()
        val normalResults = mutableListOf<TextLayoutResult>()
        normalNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(normalResults)
        assertEquals(12f, normalResults.first().layoutInput.style.fontSize.value, 0.01f)
        assertEquals(NeutralTextSecondary, normalResults.first().layoutInput.style.color)

        // stockCount <= 5 -> displayed in WarningAmber
        val lowNode = composeTestRule.onNodeWithTag("medicine_card_stock_med-stock-low", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("স্টক: ৫ পিস")
            .fetchSemanticsNode()
        val lowResults = mutableListOf<TextLayoutResult>()
        lowNode.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(lowResults)
        assertEquals(12f, lowResults.first().layoutInput.style.fontSize.value, 0.01f)
        assertEquals(WarningAmber, lowResults.first().layoutInput.style.color)
    }
}

