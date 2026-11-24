package com.mediciplanner.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.followup.FollowUpDraftField
import com.mediciplanner.app.ui.screens.followup.FollowUpViewModel
import com.mediciplanner.app.ui.screens.followup.FollowupScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class Bug25FollowUpDraftTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: MediciDatabase
    private lateinit var followUpRepository: FollowUpRepository
    private lateinit var reminderScheduler: ReminderScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        followUpRepository = FollowUpRepository(database.followUpDao())
        reminderScheduler = ReminderScheduler(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testHasDraftReflectsSavedFieldsAndClearsOnSaveOrCancel() = runTest {
        val handle = SavedStateHandle()
        val vm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)

        assertFalse(vm.uiState.value.hasDraft)

        // Updating any draft field sets hasDraft = true and writes to SavedStateHandle
        vm.updateDraft(FollowUpDraftField.TITLE, "ডা. তানভীর")
        val stateWithTitle = vm.uiState.first { it.hasDraft }
        assertTrue(stateWithTitle.hasDraft)
        assertEquals("ডা. তানভীর", handle.get<String>(FollowUpViewModel.KEY_DRAFT_TITLE))

        // Cancel / clearDraft resets hasDraft = false and clears SavedStateHandle
        vm.clearDraft()
        val clearedState = vm.uiState.first { !it.hasDraft }
        assertFalse(clearedState.hasDraft)
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))

        // Updating another field (e.g., note + type + date + time + documentPaths)
        vm.updateDraft(FollowUpDraftField.TYPE, FollowUpType.TEST)
        vm.updateDraft(FollowUpDraftField.TITLE, "Thyroid Profile")
        vm.updateDraft(FollowUpDraftField.DATE, "2026-12-22")
        vm.updateDraft(FollowUpDraftField.TIME, "10:15")
        vm.updateDraft(FollowUpDraftField.NOTE, "খালি পেটে")
        vm.updateDraft(FollowUpDraftField.DOCUMENT_PATHS, listOf("report.jpg"))

        val recreatedVm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)
        assertTrue(recreatedVm.uiState.value.hasDraft)
        assertEquals("Thyroid Profile", recreatedVm.uiState.value.draftFollowUp.title)
        assertEquals("10:15", recreatedVm.uiState.value.draftFollowUp.time)

        // Saving clears draft and sets hasDraft = false
        var saved = false
        recreatedVm.saveFollowUp(onSaved = { saved = true })
        val afterSave = recreatedVm.uiState.first { !it.hasDraft && it.followUps.isNotEmpty() }
        testScheduler.advanceUntilIdle()
        assertTrue(saved)
        assertFalse(afterSave.hasDraft)
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))
    }

    @Test
    fun testMidwayCloseShowsDraftCardAndClickRestoresAndCancelClears() {
        val handle = SavedStateHandle()
        val vm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)

        composeTestRule.setContent {
            MediciTheme {
                FollowupScreen(followUpViewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Initially no draft card
        composeTestRule.onNodeWithTag("followup_draft_card").assertDoesNotExist()

        // Open sheet via FAB and type doctor name & note
        composeTestRule.onNodeWithTag("add_followup_fab").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("doctor_name_input").performTextInput("ডা. মাহবুব")
        composeTestRule.onNodeWithTag("doctor_visit_note_input").performTextInput("রিপোর্ট দেখাতে হবে")
        composeTestRule.waitForIdle()

        // Close sheet midway by swiping down (NOT clicking Cancel)
        composeTestRule.onNodeWithTag("followup_add_edit_sheet").performTouchInput {
            swipeDown()
        }
        composeTestRule.waitForIdle()

        // Draft section must appear at the top with exact Bengali copy
        composeTestRule.onNodeWithTag("followup_draft_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("আপনার একটি অসম্পূর্ণ ড্রাফট আছে").assertIsDisplayed()
        composeTestRule.onNodeWithText("এটি সম্পূর্ণ করতে এখানে ক্লিক করুন").assertIsDisplayed()
        assertTrue(vm.uiState.value.hasDraft)

        // Clicking Draft Card opens sheet with draft data loaded
        composeTestRule.onNodeWithTag("followup_draft_card").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("followup_add_edit_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithTag("doctor_name_input").assertTextContains("ডা. মাহবুব")
        composeTestRule.onNodeWithTag("doctor_visit_note_input").assertTextContains("রিপোর্ট দেখাতে হবে")

        // Clicking Cancel scrolls to button, clicks it, closes sheet, clears draft, and hides Draft Card
        composeTestRule.onNodeWithTag("followup_cancel_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("followup_add_edit_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("followup_draft_card").assertDoesNotExist()
        assertFalse(vm.uiState.value.hasDraft)
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))
    }

    @Test
    fun testDraftCardResumesAndSaveClearsDraft() {
        val handle = SavedStateHandle()
        val vm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)

        // Pre-populate draft (simulating midway exit before returning to screen)
        vm.updateDraft(FollowUpDraftField.TITLE, "ডা. শামীম")
        vm.updateDraft(FollowUpDraftField.DATE, "2026-12-25")

        composeTestRule.setContent {
            MediciTheme {
                FollowupScreen(followUpViewModel = vm)
            }
        }
        composeTestRule.waitForIdle()

        // Draft card visible; click to resume
        composeTestRule.onNodeWithTag("followup_draft_card").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("doctor_name_input").assertTextContains("ডা. শামীম")

        // Click Save button -> scrolls to button, saves, closes sheet, and clears draft
        composeTestRule.onNodeWithTag("followup_save_button")
            .performScrollTo()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            !vm.uiState.value.hasDraft && vm.uiState.value.followUps.any { it.title == "ডা. শামীম" }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("followup_add_edit_sheet").assertDoesNotExist()
        composeTestRule.onNodeWithTag("followup_draft_card").assertDoesNotExist()
        assertFalse(vm.uiState.value.hasDraft)
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))
        assertTrue(vm.uiState.value.followUps.any { it.title == "ডা. শামীম" })
    }
}
