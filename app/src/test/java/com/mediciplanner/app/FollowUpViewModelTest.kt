package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.dao.FollowUpDao
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.followup.FollowUpViewModel
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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FollowUpViewModelTest {

    private lateinit var database: MediciDatabase
    private lateinit var followUpDao: FollowUpDao
    private lateinit var followUpRepository: FollowUpRepository
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var viewModel: FollowUpViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        followUpDao = database.followUpDao()
        followUpRepository = FollowUpRepository(followUpDao)
        reminderScheduler = ReminderScheduler(context)
        viewModel = FollowUpViewModel(followUpRepository, reminderScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testAddFollowUpAndQueryState() = runTest {
        val targetDate = LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModel.addFollowUp(
            title = "ডা. জামান আহমেদ ভিজিট",
            type = FollowUpType.DOCTOR_VISIT,
            date = targetDate,
            note = "ইসিজি ও প্রেসক্রিপশন রিভিউ",
            reminderEnabled = true
        )

        val state = viewModel.uiState.first { it.followUps.isNotEmpty() }
        assertEquals(1, state.followUps.size)
        val item = state.followUps.first()
        assertEquals("ডা. জামান আহমেদ ভিজিট", item.title)
        assertEquals(FollowUpType.DOCTOR_VISIT, item.type)
        assertEquals(targetDate, item.date)
        assertTrue(item.reminderEnabled)
    }

    @Test
    fun testUpdateAndDeleteFollowUp() = runTest {
        val targetDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)

        viewModel.addFollowUp(
            title = "সিবিসি ব্লাড টেস্ট",
            type = FollowUpType.TEST,
            date = targetDate,
            note = "সকালে খালি পেটে",
            reminderEnabled = true
        )

        val initialState = viewModel.uiState.first { it.followUps.isNotEmpty() }
        val created = initialState.followUps.first()

        // Test update
        val updated = created.copy(
            title = "লিপিদ প্রোফাইল টেস্ট",
            reminderEnabled = false
        )
        viewModel.updateFollowUp(updated)

        val updatedState = viewModel.uiState.first { it.followUps.firstOrNull()?.title == "লিপিদ প্রোফাইল টেস্ট" }
        assertEquals("লিপিদ প্রোফাইল টেস্ট", updatedState.followUps.first().title)
        assertFalse(updatedState.followUps.first().reminderEnabled)

        // Test delete
        viewModel.deleteFollowUp(updated)
        val finalState = viewModel.uiState.first { it.followUps.isEmpty() }
        assertTrue(finalState.followUps.isEmpty())
    }

    @Test
    fun testSeparateDraftStatesAndTypeSwitchingClearsOtherForm() = runTest {
        // Initially DOCTOR_VISIT
        assertEquals(FollowUpType.DOCTOR_VISIT, viewModel.selectedType.value)

        // 1. Fill Doctor Visit draft
        viewModel.updateDoctorName("ডা. কবির হোসেন")
        viewModel.updateHospitalName("স্কয়ার হাসপাতাল")
        viewModel.updateDoctorVisitDate("2026-11-20")
        viewModel.updateDoctorVisitNote("প্রেসক্রিপশন রিভিউ")
        viewModel.updateDoctorVisitReminder(true)

        val docDraft = viewModel.draftDoctorVisit.value
        assertEquals("ডা. কবির হোসেন", docDraft.doctorName)
        assertEquals("স্কয়ার হাসপাতাল", docDraft.hospitalName)
        assertEquals("2026-11-20", docDraft.date)

        // 2. Switch type to TEST
        viewModel.onTypeChanged(FollowUpType.TEST)
        assertEquals(FollowUpType.TEST, viewModel.selectedType.value)

        // Check 2 audit: switching type MUST clear the other form's data so there is no data overlap!
        assertEquals("", viewModel.draftDoctorVisit.value.doctorName)
        assertEquals("", viewModel.draftDoctorVisit.value.hospitalName)

        // 3. Fill Test draft
        viewModel.updateTestName("Thyroid Profile (TSH, FT4)")
        viewModel.updateTestCenter("ইবনে সিনা ডায়াগনস্টিক")
        viewModel.updateTestDate("2026-11-25")
        viewModel.updateTestNote("সকালে খালি পেটে স্যাম্পল দেওয়া")
        viewModel.updateTestReminder(true)

        val testDraft = viewModel.draftTest.value
        assertEquals("Thyroid Profile (TSH, FT4)", testDraft.testName)
        assertEquals("ইবনে সিনা ডায়াগনস্টিক", testDraft.testCenter)
        assertEquals("2026-11-25", testDraft.date)

        // 4. Switch type back to DOCTOR_VISIT
        viewModel.onTypeChanged(FollowUpType.DOCTOR_VISIT)
        assertEquals(FollowUpType.DOCTOR_VISIT, viewModel.selectedType.value)

        // Check that Test draft is cleared
        assertEquals("", viewModel.draftTest.value.testName)
        assertEquals("", viewModel.draftTest.value.testCenter)
    }

    @Test
    fun testSaveBothDoctorVisitAndTestSmartDrafts() = runTest {
        // 1. Save Doctor Visit
        viewModel.onTypeChanged(FollowUpType.DOCTOR_VISIT)
        viewModel.updateDoctorName("ডা. ফারহানা হক")
        viewModel.updateHospitalName("ইউনাইটেড হাসপাতাল")
        viewModel.updateDoctorVisitDate("2026-11-18")
        viewModel.updateDoctorVisitNote("ডায়াবেটিস ফলো-আপ")
        viewModel.updateDoctorVisitReminder(true)

        var doctorSaved = false
        viewModel.saveCurrentDraft(onSaved = { doctorSaved = true })

        val docState = viewModel.uiState.first { it.followUps.any { f -> f.title == "ডা. ফারহানা হক" } }
        val docItem = docState.followUps.first { it.title == "ডা. ফারহানা হক" }
        assertEquals(FollowUpType.DOCTOR_VISIT, docItem.type)
        assertEquals("ইউনাইটেড হাসপাতাল", docItem.location)
        assertEquals("2026-11-18", docItem.date)
        assertTrue(docItem.reminderEnabled)

        // 2. Save Test
        viewModel.onTypeChanged(FollowUpType.TEST)
        viewModel.updateTestName("HbA1c Blood Test")
        viewModel.updateTestCenter("ল্যাবএইড ডায়াগনস্টিক")
        viewModel.updateTestDate("2026-11-17")
        viewModel.updateTestNote("খাবার পূর্বে টেস্ট")
        viewModel.updateTestReminder(true)

        var testSaved = false
        viewModel.saveCurrentDraft(onSaved = { testSaved = true })

        val bothState = viewModel.uiState.first { it.followUps.size >= 2 }
        assertEquals(2, bothState.followUps.size)

        val testItem = bothState.followUps.first { it.title == "HbA1c Blood Test" }
        assertEquals(FollowUpType.TEST, testItem.type)
        assertEquals("ল্যাবএইড ডায়াগনস্টিক", testItem.location)
        assertEquals("2026-11-17", testItem.date)
        assertTrue(testItem.reminderEnabled)
    }

    @Test
    fun testLoadFollowUpForEditPopulatesCorrespondingDraftAndClearsOther() = runTest {
        val doctorEntity = FollowUpEntity(
            title = "ডা. সুমাইয়া আক্তার",
            type = FollowUpType.DOCTOR_VISIT,
            date = "2026-11-30",
            note = "নিয়মিত চেকআপ",
            reminderEnabled = true,
            location = "ডেল্টা মেডিকেল"
        )
        viewModel.loadFollowUpForEdit(doctorEntity)

        assertEquals(FollowUpType.DOCTOR_VISIT, viewModel.selectedType.value)
        assertEquals("ডা. সুমাইয়া আক্তার", viewModel.draftDoctorVisit.value.doctorName)
        assertEquals("ডেল্টা মেডিকেল", viewModel.draftDoctorVisit.value.hospitalName)
        assertEquals("", viewModel.draftTest.value.testName)

        val testEntity = FollowUpEntity(
            title = "Chest X-Ray P/A View",
            type = FollowUpType.TEST,
            date = "2026-12-05",
            note = "পুরনো রিপোর্ট সাথে নেওয়া",
            reminderEnabled = false,
            location = "পপুলার ডায়াগনস্টিক"
        )
        viewModel.loadFollowUpForEdit(testEntity)

        assertEquals(FollowUpType.TEST, viewModel.selectedType.value)
        assertEquals("Chest X-Ray P/A View", viewModel.draftTest.value.testName)
        assertEquals("পপুলার ডায়াগনস্টিক", viewModel.draftTest.value.testCenter)
        assertFalse(viewModel.draftTest.value.reminderEnabled)
        assertEquals("", viewModel.draftDoctorVisit.value.doctorName)
    }

    @Test
    fun testCustomReminderTimeInSmartDraftAndEntity() = runTest {
        // Test custom reminder time for Doctor Visit
        viewModel.onTypeChanged(FollowUpType.DOCTOR_VISIT)
        viewModel.updateDoctorName("ডা. সাজিদ রহমান")
        viewModel.updateDoctorVisitDate("2026-11-22")
        viewModel.updateDoctorVisitTime("10:30")
        assertEquals("10:30", viewModel.draftDoctorVisit.value.reminderTime)

        var saved = false
        viewModel.saveCurrentDraft(onSaved = { saved = true })

        val state = viewModel.uiState.first { it.followUps.any { f -> f.title == "ডা. সাজিদ রহমান" } }
        val item = state.followUps.first { it.title == "ডা. সাজিদ রহমান" }
        assertEquals("10:30", item.reminderTime)
        assertFalse(item.isCompleted)

        // Test custom reminder time for Test
        viewModel.onTypeChanged(FollowUpType.TEST)
        viewModel.updateTestName("Ultra-sound Whole Abdomen")
        viewModel.updateTestDate("2026-11-24")
        viewModel.updateTestTime("16:45")
        assertEquals("16:45", viewModel.draftTest.value.reminderTime)

        viewModel.saveCurrentDraft()
        val state2 = viewModel.uiState.first { it.followUps.any { f -> f.title == "Ultra-sound Whole Abdomen" } }
        val testItem = state2.followUps.first { it.title == "Ultra-sound Whole Abdomen" }
        assertEquals("16:45", testItem.reminderTime)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testUpcomingAndHistoryTabsAndMarkAsCompleted() = runTest {
        viewModel.addFollowUp(
            title = "রুটিন ব্লাড সুগার টেস্ট",
            type = FollowUpType.TEST,
            date = "2026-11-19",
            reminderTime = "08:00"
        )
        testScheduler.advanceUntilIdle()

        // Item should be in upcoming list and NOT in history list
        val state1 = viewModel.uiState.first { it.upcomingFollowUps.isNotEmpty() }
        assertEquals(1, state1.upcomingFollowUps.size)
        assertTrue(state1.historyFollowUps.isEmpty())

        val upcomingItem = state1.upcomingFollowUps.first()
        assertEquals("রুটিন ব্লাড সুগার টেস্ট", upcomingItem.title)
        assertFalse(upcomingItem.isCompleted)

        // Mark as completed
        viewModel.markAsCompleted(upcomingItem)
        testScheduler.advanceUntilIdle()

        // Item should move to history list and disappear from upcoming list
        val state2 = viewModel.uiState.first { it.historyFollowUps.isNotEmpty() }
        assertTrue(state2.upcomingFollowUps.isEmpty())
        assertEquals(1, state2.historyFollowUps.size)

        val historyItem = state2.historyFollowUps.first()
        assertEquals("রুটিন ব্লাড সুগার টেস্ট", historyItem.title)
        assertTrue(historyItem.isCompleted)
        org.junit.Assert.assertNotNull(historyItem.completedAt)

        // Verify selectTab switches tabs
        viewModel.selectTab(com.mediciplanner.app.ui.screens.followup.FollowUpTab.HISTORY)
        assertEquals(com.mediciplanner.app.ui.screens.followup.FollowUpTab.HISTORY, viewModel.selectedTab.value)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testMigration4To5ColumnsExistInDatabase() {
        val sqliteDb = database.openHelper.writableDatabase
        val cursor = sqliteDb.query("PRAGMA table_info(follow_ups)")
        val columnNames = mutableListOf<String>()
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                columnNames.add(cursor.getString(nameIndex))
            }
        }
        cursor.close()

        assertTrue("follow_ups table must have isCompleted column", columnNames.contains("isCompleted"))
        assertTrue("follow_ups table must have reminderTime column", columnNames.contains("reminderTime"))
        assertTrue("follow_ups table must have completedAt column", columnNames.contains("completedAt"))
    }

    @Test
    fun testSavedStateHandlePreservesAndRestoresDraftOnInit() = runTest {
        val handle = androidx.lifecycle.SavedStateHandle()
        val firstVm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)

        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TYPE, FollowUpType.TEST)
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TITLE, "Lipid Profile")
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.LOCATION, "Popular Diagnostic")
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.DATE, "2026-12-10")
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TIME, "14:15")
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.NOTE, "Fasting 12 hours")
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.DOCUMENT_PATHS, listOf("doc1.jpg", "doc2.jpg"))
        firstVm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.REMINDER_ENABLED, false)

        // Verify SavedStateHandle keys were updated directly
        assertEquals("Lipid Profile", handle.get<String>(FollowUpViewModel.KEY_DRAFT_TITLE))
        assertEquals(FollowUpType.TEST.name, handle.get<String>(FollowUpViewModel.KEY_DRAFT_TYPE))
        assertEquals("2026-12-10", handle.get<String>(FollowUpViewModel.KEY_DRAFT_DATE))
        assertEquals("14:15", handle.get<String>(FollowUpViewModel.KEY_DRAFT_TIME))
        assertEquals("Fasting 12 hours", handle.get<String>(FollowUpViewModel.KEY_DRAFT_NOTE))
        assertEquals(arrayListOf("doc1.jpg", "doc2.jpg"), handle.get<ArrayList<String>>(FollowUpViewModel.KEY_DRAFT_DOCUMENT_PATHS))

        // Recreate ViewModel with the same SavedStateHandle and verify restoration on init
        val restoredVm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)
        val restoredDraft = restoredVm.draftFollowUp.value
        assertEquals(FollowUpType.TEST, restoredDraft.type)
        assertEquals("Lipid Profile", restoredDraft.title)
        assertEquals("Popular Diagnostic", restoredDraft.location)
        assertEquals("2026-12-10", restoredDraft.date)
        assertEquals("14:15", restoredDraft.time)
        assertEquals("Fasting 12 hours", restoredDraft.note)
        assertEquals(listOf("doc1.jpg", "doc2.jpg"), restoredDraft.documentPaths)
        assertFalse(restoredDraft.reminderEnabled)
        assertEquals("Lipid Profile", restoredVm.uiState.value.draftFollowUp.title)
    }

    @Test
    fun testClearDraftRemovesSavedStateAndResetsStateFlow() = runTest {
        val handle = androidx.lifecycle.SavedStateHandle()
        val vm = FollowUpViewModel(followUpRepository, reminderScheduler, handle)

        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TYPE, FollowUpType.TEST)
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TITLE, "CBC")
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.DATE, "2026-12-15")
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TIME, "11:00")
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.NOTE, "Morning")
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.DOCUMENT_PATHS, listOf("a.jpg"))

        vm.clearDraft()

        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TYPE))
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_DATE))
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TIME))
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_NOTE))
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_DOCUMENT_PATHS))
        assertEquals(com.mediciplanner.app.ui.screens.followup.DraftFollowUp(), vm.draftFollowUp.value)
        assertEquals(com.mediciplanner.app.ui.screens.followup.DraftFollowUp(), vm.uiState.value.draftFollowUp)

        // Also verify saveFollowUp() clears draft after saving
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.TITLE, "Dr. Karim")
        vm.updateDraft(com.mediciplanner.app.ui.screens.followup.FollowUpDraftField.DATE, "2026-12-20")
        var saved = false
        vm.saveFollowUp(onSaved = { saved = true })
        val state = vm.uiState.first {
            it.followUps.any { f -> f.title == "Dr. Karim" } &&
                it.draftFollowUp == com.mediciplanner.app.ui.screens.followup.DraftFollowUp()
        }
        testScheduler.advanceUntilIdle()
        assertTrue(state.followUps.any { it.title == "Dr. Karim" })
        assertTrue(saved)
        assertFalse(handle.contains(FollowUpViewModel.KEY_DRAFT_TITLE))
        assertEquals(com.mediciplanner.app.ui.screens.followup.DraftFollowUp(), vm.draftFollowUp.value)
    }
}
