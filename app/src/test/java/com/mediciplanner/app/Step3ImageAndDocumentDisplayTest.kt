package com.mediciplanner.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.ui.components.FullScreenImageDialog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Step3ImageAndDocumentDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testPrescriptionEntityImagePathsSupport() {
        val images = listOf("prescription_1.jpg", "prescription_2.jpg")
        val entity = PrescriptionEntity(
            date = "2026-11-09",
            doctorName = "ডা. আহমেদ",
            createdAt = "2026-11-09T10:00:00",
            prescriptionImagePaths = images
        )

        assertEquals(2, entity.prescriptionImagePaths.size)
        assertEquals("prescription_1.jpg", entity.prescriptionImagePaths[0])
        assertEquals("prescription_2.jpg", entity.prescriptionImagePaths[1])
    }

    @Test
    fun testFollowUpEntityDocumentPathsSupport() {
        val documents = listOf("blood_report.jpg", "xray.png")
        val entity = FollowUpEntity(
            title = "রক্ত পরীক্ষা",
            type = FollowUpType.TEST,
            date = "2026-11-15",
            documentPaths = documents
        )

        assertEquals(2, entity.documentPaths.size)
        assertEquals("blood_report.jpg", entity.documentPaths[0])
        assertEquals("xray.png", entity.documentPaths[1])
    }

    @Test
    fun testFullScreenImageDialogRendersAndDismisses() {
        var dismissed = false

        composeTestRule.setContent {
            MediciTheme {
                FullScreenImageDialog(
                    imageModels = listOf("file:///android_asset/mock_image.jpg"),
                    onDismiss = { dismissed = true },
                    title = "টেস্ট প্রেসক্রিপশন"
                )
            }
        }

        // Verify close button is present
        composeTestRule.onNodeWithTag("close_fullscreen_image_button")
            .assertIsDisplayed()
            .performClick()

        assertTrue("onDismiss should be called when close button is clicked", dismissed)
    }

    @Test
    fun testFullScreenImageDialogNavigationWithMultipleImages() {
        val models = listOf("img_1.jpg", "img_2.jpg", "img_3.jpg")

        composeTestRule.setContent {
            MediciTheme {
                FullScreenImageDialog(
                    imageModels = models,
                    initialIndex = 0,
                    onDismiss = {}
                )
            }
        }

        // Verify counter indicates first image
        composeTestRule.onNodeWithTag("image_counter_text").assertIsDisplayed()

        // Verify next button is displayed and clickable
        composeTestRule.onNodeWithTag("next_image_button")
            .assertIsDisplayed()
            .performClick()

        // After click, prev button should be displayed
        composeTestRule.onNodeWithTag("prev_image_button")
            .assertIsDisplayed()
    }
}
