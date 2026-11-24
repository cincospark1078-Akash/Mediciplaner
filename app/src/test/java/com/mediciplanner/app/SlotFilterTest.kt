package com.mediciplanner.app

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.relation.DoseScheduleWithReminders
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.ui.components.SlotFilterRow
import com.mediciplanner.app.ui.screens.prescription.components.PrescriptionDetailBottomSheet
import com.mediciplanner.app.ui.screens.track.TrackScreen
import com.mediciplanner.app.ui.screens.track.TrackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class SlotFilterTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        prescriptionRepository = PrescriptionRepository(database.prescriptionDao())
        reminderRepository = ReminderRepository(database.reminderDao())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testSlotFilterRowDefaultAllAndSelectionCallbacks() {
        val selectedHistory = mutableListOf<DoseSlot?>()

        composeTestRule.setContent {
            MediciTheme {
                SlotFilterRow(
                    selectedSlot = null,
                    onSlotSelected = { selectedHistory.add(it) }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Default "সব" chip is selected; others are not selected
        composeTestRule.onNodeWithTag("slot_filter_chip_all").assertIsSelected()
        composeTestRule.onNodeWithTag("slot_filter_chip_morning").assertIsNotSelected()
        composeTestRule.onNodeWithText("সব").assertIsDisplayed()
        composeTestRule.onNodeWithText("সকাল").assertIsDisplayed()
        composeTestRule.onNodeWithText("দুপুর").assertIsDisplayed()

        // Click সকাল -> MORNING
        composeTestRule.onNodeWithTag("slot_filter_chip_morning").performClick()
        assertEquals(DoseSlot.MORNING, selectedHistory.last())

        // Scroll to রাত and click -> NIGHT
        composeTestRule.onNodeWithTag("slot_filter_chip_night").performScrollTo().performClick()
        assertEquals(DoseSlot.NIGHT, selectedHistory.last())

        // Scroll back to সব and click -> null
        composeTestRule.onNodeWithTag("slot_filter_chip_all").performScrollTo().performClick()
        assertNull(selectedHistory.last())
    }

    @Test
    fun testPrescriptionDetailBottomSheetSlotFilterFiltersMedicines() {
        val presId = "pres-filter-1"
        val prescription = PrescriptionEntity(
            id = presId,
            parentId = presId,
            versionNumber = 1,
            isLatest = true,
            date = "2024-11-20",
            doctorName = "ডা. করিম",
            createdAt = "2024-11-20T10:00:00"
        )

        val morningMed = MedicineEntity(
            id = "med-morning-1",
            prescriptionId = presId,
            name = "নাপা এক্সট্রা",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-20T10:00:00"
        )
        val nightMed = MedicineEntity(
            id = "med-night-1",
            prescriptionId = presId,
            name = "সেকলো ২০",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 14,
            foodRule = FoodRule.BEFORE,
            createdAt = "2024-11-20T10:01:00"
        )

        val prescriptionWithMedicines = PrescriptionWithMedicines(
            prescription = prescription,
            medicines = listOf(
                MedicineWithSchedules(
                    medicine = morningMed,
                    doseSchedules = listOf(
                        DoseScheduleWithReminders(
                            doseSchedule = DoseScheduleEntity(
                                id = "sched-m",
                                medicineId = morningMed.id,
                                slot = DoseSlot.MORNING,
                                time = "08:00",
                                doseAmount = 1.0,
                                doseUnit = "টি"
                            )
                        )
                    )
                ),
                MedicineWithSchedules(
                    medicine = nightMed,
                    doseSchedules = listOf(
                        DoseScheduleWithReminders(
                            doseSchedule = DoseScheduleEntity(
                                id = "sched-n",
                                medicineId = nightMed.id,
                                slot = DoseSlot.NIGHT,
                                time = "22:00",
                                doseAmount = 1.0,
                                doseUnit = "টি"
                            )
                        )
                    )
                )
            )
        )

        composeTestRule.setContent {
            MediciTheme {
                PrescriptionDetailBottomSheet(
                    prescriptionWithMedicines = prescriptionWithMedicines,
                    onDismiss = {},
                    onEditClick = {},
                    onDeleteClick = {}
                )
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(300)

        // Default "সব" is selected and both medicines are visible
        composeTestRule.onNodeWithTag("slot_filter_row").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("slot_filter_chip_all").assertIsSelected()
        composeTestRule.onNodeWithTag("medicine_card_med-morning-1").assertExists()
        composeTestRule.onNodeWithTag("medicine_card_med-night-1").assertExists()

        // Filter by সকাল -> only নাপা এক্সট্রা remains
        composeTestRule.onNodeWithTag("slot_filter_chip_morning").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("slot_filter_chip_morning").assertIsSelected()
        composeTestRule.onNodeWithTag("medicine_card_med-morning-1").assertExists()
        composeTestRule.onNodeWithTag("medicine_card_med-night-1").assertDoesNotExist()

        // Filter by রাত -> only সেকলো ২০ remains
        composeTestRule.onNodeWithTag("slot_filter_chip_night").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_card_med-night-1").assertExists()
        composeTestRule.onNodeWithTag("medicine_card_med-morning-1").assertDoesNotExist()

        // Filter by বিকেল -> empty filter state
        composeTestRule.onNodeWithTag("slot_filter_chip_afternoon").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("detail_filtered_empty_text").assertExists()

        // Reset to সব -> both medicines visible again
        composeTestRule.onNodeWithTag("slot_filter_chip_all").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_card_med-morning-1").assertExists()
        composeTestRule.onNodeWithTag("medicine_card_med-night-1").assertExists()
    }

    @Test
    fun testTrackViewModelFilteredGroupedHistory() = runTest {
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val prescId = UUID.randomUUID().toString()

        database.prescriptionDao().insertPrescription(
            PrescriptionEntity(
                id = prescId,
                date = todayIso,
                doctorName = "ডা. রহমান",
                createdAt = nowIso
            )
        )
        val medId = UUID.randomUUID().toString()
        database.prescriptionDao().insertMedicine(
            MedicineEntity(
                id = medId,
                prescriptionId = prescId,
                name = "মোটিগার্ড",
                type = MedicineType.TABLET,
                strength = 10.0,
                strengthUnit = "mg",
                durationDays = 5,
                foodRule = FoodRule.BEFORE,
                createdAt = nowIso
            )
        )
        database.prescriptionDao().insertDoseSchedule(
            DoseScheduleEntity(
                id = UUID.randomUUID().toString(),
                medicineId = medId,
                slot = DoseSlot.MORNING,
                time = "08:00",
                doseAmount = 1.0,
                doseUnit = "টি"
            )
        )
        database.prescriptionDao().insertDoseSchedule(
            DoseScheduleEntity(
                id = UUID.randomUUID().toString(),
                medicineId = medId,
                slot = DoseSlot.EVENING,
                time = "18:30",
                doseAmount = 1.0,
                doseUnit = "টি"
            )
        )

        val viewModel = TrackViewModel(prescriptionRepository, reminderRepository)
        val initialState = viewModel.uiState.first { !it.isLoading && it.trackedDoses.size == 2 }
        assertNull(initialState.selectedSlot)
        assertEquals(setOf(DoseSlot.MORNING, DoseSlot.EVENING), initialState.filteredGroupedHistory.keys)

        viewModel.selectSlot(DoseSlot.EVENING)
        val eveningState = viewModel.uiState.first { it.selectedSlot == DoseSlot.EVENING }
        assertEquals(setOf(DoseSlot.EVENING), eveningState.filteredGroupedHistory.keys)
        assertFalse(eveningState.filteredGroupedHistory.containsKey(DoseSlot.MORNING))

        viewModel.selectSlot(null)
        val allState = viewModel.uiState.first { it.selectedSlot == null }
        assertTrue(allState.filteredGroupedHistory.containsKey(DoseSlot.MORNING))
        assertTrue(allState.filteredGroupedHistory.containsKey(DoseSlot.EVENING))
    }

    @Test
    fun testTrackScreenSlotFilterUiFiltersHistoryGroups() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val app = context.applicationContext as MediciApplication
            val todayIso = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val prescId = "track-ui-pres-${UUID.randomUUID()}"
            val medId = "track-ui-med-${UUID.randomUUID()}"

            val dao = app.database.prescriptionDao()
            dao.insertPrescription(
                PrescriptionEntity(
                    id = prescId,
                    date = todayIso,
                    doctorName = "ডা. তানভীর",
                    createdAt = nowIso
                )
            )
            dao.insertMedicine(
                MedicineEntity(
                    id = medId,
                    prescriptionId = prescId,
                    name = "এসোমেপ্রাজল",
                    type = MedicineType.CAPSULE,
                    strength = 20.0,
                    strengthUnit = "mg",
                    durationDays = 7,
                    foodRule = FoodRule.BEFORE,
                    createdAt = nowIso
                )
            )
            dao.insertDoseSchedule(
                DoseScheduleEntity(
                    id = "track-sched-morning",
                    medicineId = medId,
                    slot = DoseSlot.MORNING,
                    time = "08:00",
                    doseAmount = 1.0,
                    doseUnit = "টি"
                )
            )
            dao.insertDoseSchedule(
                DoseScheduleEntity(
                    id = "track-sched-night",
                    medicineId = medId,
                    slot = DoseSlot.NIGHT,
                    time = "22:00",
                    doseAmount = 1.0,
                    doseUnit = "টি"
                )
            )

            composeTestRule.setContent {
                MediciTheme {
                    TrackScreen()
                }
            }
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule.onAllNodesWithTagCompat("slot_section_morning")
            }

            // Scroll SlotFilterRow into viewport
            composeTestRule.onNodeWithTag("slot_filter_row").performScrollTo()
            composeTestRule.waitForIdle()

            // Default "সব" selected and both MORNING and NIGHT sections exist
            composeTestRule.onNodeWithTag("slot_filter_chip_all").assertIsSelected()
            composeTestRule.onNodeWithTag("slot_section_morning").assertExists()
            composeTestRule.onNodeWithTag("slot_section_night").assertExists()

            // Tap রাত -> only NIGHT section visible
            composeTestRule.onNodeWithTag("slot_filter_chip_night").performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                !composeTestRule.onAllNodesWithTagCompat("slot_section_morning")
            }
            composeTestRule.onNodeWithTag("slot_section_night").assertExists()
            composeTestRule.onNodeWithTag("slot_section_morning").assertDoesNotExist()

            // Tap সব -> both sections visible again
            composeTestRule.onNodeWithTag("slot_filter_chip_all").performScrollTo().performClick()
            composeTestRule.waitUntil(timeoutMillis = 3_000) {
                composeTestRule.onAllNodesWithTagCompat("slot_section_morning")
            }
            composeTestRule.onNodeWithTag("slot_section_morning").assertExists()
            composeTestRule.onNodeWithTag("slot_section_night").assertExists()
        }
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTagCompat(tag: String): Boolean {
        return try {
            onNodeWithTag(tag).assertExists()
            true
        } catch (_: AssertionError) {
            false
        }
    }
}
