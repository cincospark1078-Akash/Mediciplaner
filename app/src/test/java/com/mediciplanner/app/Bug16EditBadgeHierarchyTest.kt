package com.mediciplanner.app

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextLayoutResult
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.ui.screens.home.components.MedicineCard
import com.mediciplanner.app.ui.screens.prescription.ChangeType
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug16EditBadgeHierarchyTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun extractFontSizeSp(node: SemanticsNode): Float {
        val results = mutableListOf<TextLayoutResult>()
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        action?.action?.invoke(results)
        return results.first().layoutInput.style.fontSize.value
    }

    private fun sampleMed(id: String, name: String): Pair<MedicineEntity, DoseScheduleEntity> {
        val med = MedicineEntity(
            id = id,
            prescriptionId = "pres-16",
            name = name,
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2026-11-19T09:50:00"
        )
        val sched = DoseScheduleEntity(
            id = "sched-$id",
            medicineId = id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        return med to sched
    }

    @Test
    fun testEditAndNewBadgesAreSmallerAndInlineBeforeMedicineName() {
        val (editedMed, editedSched) = sampleMed("med-edit-16", "নাপা")
        val (newMed, newSched) = sampleMed("med-new-16", "সার্জেল")

        composeTestRule.setContent {
            MediciTheme {
                androidx.compose.foundation.layout.Column {
                    MedicineCard(
                        medicine = editedMed,
                        schedule = editedSched,
                        isTaken = false,
                        isReadOnly = true,
                        changeType = ChangeType.EDITED
                    )
                    MedicineCard(
                        medicine = newMed,
                        schedule = newSched,
                        isTaken = false,
                        isReadOnly = true,
                        changeType = ChangeType.NEW
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        // 1. Verify "Edit" badge font size, text, and inline position before medicine name
        val editBadgeBox = composeTestRule.onNodeWithTag("medicine_change_badge_${editedMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val editBadgeText = composeTestRule.onNodeWithTag("medicine_change_badge_text_${editedMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("Edit")
            .fetchSemanticsNode()
        val editMedName = composeTestRule.onNodeWithTag("medicine_card_name_${editedMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()

        val editBadgeFontSp = extractFontSizeSp(editBadgeText)
        val editNameFontSp = extractFontSizeSp(editMedName)
        assertTrue("Edit badge font size should be 10-11.sp, was $editBadgeFontSp", editBadgeFontSp in 10f..11f)
        assertTrue("Medicine name font size should be 18-20.sp, was $editNameFontSp", editNameFontSp in 18f..20f)
        assertTrue("Medicine name must be larger than Edit badge", editNameFontSp > editBadgeFontSp)

        // Inline before medicine name: badge right edge <= name left edge, and vertically overlapping on the same row
        assertTrue(
            "Edit badge should be inline before medicine name: badge=${editBadgeBox.boundsInRoot}, name=${editMedName.boundsInRoot}",
            editBadgeBox.boundsInRoot.right <= editMedName.boundsInRoot.left + 1f
        )
        assertTrue(
            "Edit badge should share the same row vertically with medicine name: badge=${editBadgeBox.boundsInRoot}, name=${editMedName.boundsInRoot}",
            editBadgeBox.boundsInRoot.top < editMedName.boundsInRoot.bottom &&
                editBadgeBox.boundsInRoot.bottom > editMedName.boundsInRoot.top
        )

        // 2. Verify "New" badge font size, text, and inline position before medicine name
        val newBadgeBox = composeTestRule.onNodeWithTag("medicine_change_badge_${newMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()
        val newBadgeText = composeTestRule.onNodeWithTag("medicine_change_badge_text_${newMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("New")
            .fetchSemanticsNode()
        val newMedName = composeTestRule.onNodeWithTag("medicine_card_name_${newMed.id}", useUnmergedTree = true)
            .assertIsDisplayed()
            .fetchSemanticsNode()

        val newBadgeFontSp = extractFontSizeSp(newBadgeText)
        val newNameFontSp = extractFontSizeSp(newMedName)
        assertTrue("New badge font size should be 10-11.sp, was $newBadgeFontSp", newBadgeFontSp in 10f..11f)
        assertTrue("Medicine name font size should be 18-20.sp, was $newNameFontSp", newNameFontSp in 18f..20f)
        assertTrue("Medicine name must be larger than New badge", newNameFontSp > newBadgeFontSp)
        assertTrue(
            "New badge should be inline before medicine name: badge=${newBadgeBox.boundsInRoot}, name=${newMedName.boundsInRoot}",
            newBadgeBox.boundsInRoot.right <= newMedName.boundsInRoot.left + 1f
        )
        assertTrue(
            "New badge should share the same row vertically with medicine name: badge=${newBadgeBox.boundsInRoot}, name=${newMedName.boundsInRoot}",
            newBadgeBox.boundsInRoot.top < newMedName.boundsInRoot.bottom &&
                newBadgeBox.boundsInRoot.bottom > newMedName.boundsInRoot.top
        )
    }
}
