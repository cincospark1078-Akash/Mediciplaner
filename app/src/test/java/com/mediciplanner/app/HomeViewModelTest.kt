package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.dao.ReminderDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.ui.screens.home.HomeViewModel
import com.mediciplanner.app.ui.screens.home.formatBengaliDate
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
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
@Config(sdk = [36])
class HomeViewModelTest {

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var themePreferences: ThemePreferences
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        prescriptionDao = database.prescriptionDao()
        reminderDao = database.reminderDao()
        prescriptionRepository = PrescriptionRepository(prescriptionDao)
        reminderRepository = ReminderRepository(reminderDao)
        themePreferences = ThemePreferences(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    // 1. Test Bengali Date Formatter
    @Test
    fun testBengaliDateFormatting() {
        val testDate = LocalDate.of(2026, 11, 4)
        val formatted = formatBengaliDate(testDate)
        assertEquals("০৪ নভেম্বর, ২০২৬", formatted)

        val sampleNumber = toBengaliDigits("60%")
        assertEquals("৬০%", sampleNumber)
    }

    // 2. Test 0/0 edge case: 0 taken and 0 missed -> progressTotal is 0
    @Test
    fun testProgressZeroZeroEdgeCase() = runTest {
        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val state = viewModel.uiState.value

        assertEquals(0, state.progressTaken)
        assertEquals(0, state.progressTotal)
        assertTrue(state.todayMedicinesGrouped.isEmpty())
        assertNull(state.nextReminder)
    }

    // 3. Test Date Filtering: Medicines for TODAY strictly filtered via LocalDate
    @Test
    fun testDateFilteringTodayMedicinesAndNextReminder() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            doctorName = "Dr. Farhan",
            createdAt = nowIso
        )

        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Paracetamol",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = nowIso
        )

        val scheduleMorning = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "mg"
        )

        val scheduleNight = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "mg"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(medicine),
            schedules = listOf(scheduleMorning, scheduleNight)
        )

        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val state = viewModel.uiState.first { it.todayMedicinesGrouped.isNotEmpty() }

        assertFalse(state.isLoading)
        // Grouped by slot: Morning and Night
        assertTrue("Morning slot should be present", state.todayMedicinesGrouped.containsKey(DoseSlot.MORNING.name))
        assertTrue("Night slot should be present", state.todayMedicinesGrouped.containsKey(DoseSlot.NIGHT.name))
        assertEquals("Paracetamol", state.todayMedicinesGrouped[DoseSlot.MORNING.name]?.first()?.name)

        // Next reminder should be Morning (08:00 is earlier than 22:00)
        assertNotNull(state.nextReminder)
        assertEquals(scheduleMorning.id, state.nextReminder?.doseScheduleId)
        assertEquals("Paracetamol", state.nextReminderMedicine?.name)

        // Progress initially 0 taken out of 2 scheduled
        assertEquals(0, state.progressTaken)
        assertEquals(0, state.progressTotal) // evaluated (taken + missed) is 0
        assertEquals(2, state.progressScheduled)

        // Mark Morning reminder as taken
        viewModel.markAsTaken(state.nextReminder!!.id)

        val updatedState = viewModel.uiState.first { it.progressTaken == 1 }
        assertEquals(1, updatedState.progressTaken)
        assertEquals(1, updatedState.progressTotal) // 1 taken, 0 missed = 1
        // Next reminder should advance to Night schedule
        assertEquals(scheduleNight.id, updatedState.nextReminder?.doseScheduleId)
        testScheduler.advanceUntilIdle()
    }

    // 4. Test Expired Medicine (created in the past with durationDays < past days) is NOT shown today
    @Test
    fun testPastExpiredMedicineNotScheduledForToday() = runTest {
        val today = LocalDate.now()
        val pastDate = today.minusDays(30)
        val pastIso = pastDate.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = pastDate.toString(),
            createdAt = pastIso
        )

        val expiredMedicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Old Antibiotic",
            type = MedicineType.CAPSULE,
            strength = 250.0,
            strengthUnit = "mg",
            durationDays = 5, // expired 25 days ago!
            foodRule = FoodRule.BEFORE,
            createdAt = pastIso
        )

        val schedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = expiredMedicine.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "mg"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(expiredMedicine),
            schedules = listOf(schedule)
        )

        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val state = viewModel.uiState.first { !it.isLoading }

        // Expired medicine must NOT be included today
        assertTrue("Expired medicine should not be scheduled for today", state.todayMedicinesGrouped.isEmpty())
        assertNull(state.nextReminder)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testAllRemindersTakenClearsNextReminder() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            createdAt = nowIso
        )
        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Vitamin C",
            type = MedicineType.TABLET,
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = nowIso
        )
        val schedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.MORNING,
            time = "09:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(medicine),
            schedules = listOf(schedule)
        )

        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val stateWithReminder = viewModel.uiState.first { it.nextReminder != null }
        assertNotNull(stateWithReminder.nextReminder)

        // Mark the only reminder as taken
        viewModel.markAsTaken(stateWithReminder.nextReminder!!.id)

        val stateCompleted = viewModel.uiState.first { it.nextReminder == null && it.progressTaken == 1 }
        assertNull(stateCompleted.nextReminder)
        assertEquals(1, stateCompleted.progressTaken)
        assertEquals(1, stateCompleted.progressScheduled)
        testScheduler.advanceUntilIdle()
    }

    // 5. Test State A: No Prescriptions at all
    @Test
    fun testEmptyStateA_NoPrescriptions() = runTest {
        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val state = viewModel.uiState.first { it.emptyState != com.mediciplanner.app.ui.screens.home.HomeEmptyState.NONE }

        assertEquals(com.mediciplanner.app.ui.screens.home.HomeEmptyState.NO_PRESCRIPTIONS, state.emptyState)
        assertFalse(state.hasPrescriptions)
        assertTrue(state.todayMedicinesGrouped.isEmpty())
        assertNull(state.nextReminder)
    }

    // 6. Test State B: Prescriptions exist, but no doses TODAY (e.g. interval skips today)
    @Test
    fun testEmptyStateB_PrescriptionsExistButNoDosesToday() = runTest {
        val today = LocalDate.now()
        // Prescription created yesterday with interval 2 days (so doses are on day 0, day 2, etc. - today is day 1 so no doses today)
        val yesterday = today.minusDays(1)
        val yesterdayIso = yesterday.atStartOfDay().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = yesterday.toString(),
            createdAt = yesterdayIso
        )
        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Insulin",
            type = MedicineType.INJECTION,
            intervalDays = 2, // every 2 days
            durationDays = 10,
            foodRule = FoodRule.BEFORE,
            createdAt = yesterdayIso
        )
        val schedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 10.0,
            doseUnit = "unit"
        )
        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(medicine),
            schedules = listOf(schedule)
        )

        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val state = viewModel.uiState.first { it.hasPrescriptions }

        assertEquals(com.mediciplanner.app.ui.screens.home.HomeEmptyState.NO_DOSES_TODAY, state.emptyState)
        assertTrue(state.hasPrescriptions)
        assertTrue(state.todayMedicinesGrouped.isEmpty())
        assertNotNull(state.nextDoseDateFormatted)
    }

    // 7. Test State C: All doses for today completed
    @Test
    fun testEmptyStateC_AllDosesCompleted() = runTest {
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            createdAt = nowIso
        )
        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Napa",
            type = MedicineType.TABLET,
            durationDays = 3,
            foodRule = FoodRule.AFTER,
            createdAt = nowIso
        )
        val schedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(medicine),
            schedules = listOf(schedule)
        )

        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences)
        val stateWithReminder = viewModel.uiState.first { it.nextReminder != null }
        assertNotNull(stateWithReminder.nextReminder)

        // Mark dose as taken
        viewModel.markAsTaken(stateWithReminder.nextReminder!!.id)

        val stateCompleted = viewModel.uiState.first { it.emptyState == com.mediciplanner.app.ui.screens.home.HomeEmptyState.ALL_DOSES_COMPLETED }
        assertEquals(com.mediciplanner.app.ui.screens.home.HomeEmptyState.ALL_DOSES_COMPLETED, stateCompleted.emptyState)
        assertEquals(1, stateCompleted.progressTaken)
        assertEquals(1, stateCompleted.progressScheduled)
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun testStockDecrementsOnMarkAsTakenAndNotifiesOnceAtFive() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val today = LocalDate.now()
        val todayStr = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = todayStr,
            createdAt = nowIso
        )
        val medicine = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Napa Extra",
            type = MedicineType.TABLET,
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = nowIso,
            stockCount = 6,
            initialStock = 6
        )
        val morningSched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val nightSched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = medicine.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(medicine),
            schedules = listOf(morningSched, nightSched)
        )

        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val scheduler = com.mediciplanner.app.notification.ReminderScheduler(context)
        viewModel = HomeViewModel(prescriptionRepository, reminderRepository, themePreferences, scheduler)

        val initialState = viewModel.uiState.first { it.todayScheduleInfoList.size == 2 }
        val morningReminderId = initialState.todayScheduleInfoList
            .first { it.schedule.id == morningSched.id }
            .reminder!!.id
        val nightReminderId = initialState.todayScheduleInfoList
            .first { it.schedule.id == nightSched.id }
            .reminder!!.id

        // First dose: 6 -> 5 (triggers low stock notification)
        viewModel.clearToastMessage()
        viewModel.markAsTaken(morningReminderId)
        viewModel.toastMessage.first { it != null }
        val afterFirst = viewModel.uiState.first { state ->
            state.todayScheduleInfoList.any { it.reminder?.id == morningReminderId && it.reminder.status == ReminderStatus.TAKEN } &&
                state.todayScheduleInfoList.first().medicine.stockCount == 5
        }
        assertEquals(5, afterFirst.todayScheduleInfoList.first().medicine.stockCount)
        assertEquals(5, prescriptionDao.getMedicineById(medicine.id)?.stockCount)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val notifId = "low_stock_${medicine.id}".hashCode()
        val postedNotification = shadowNm.getNotification(notifId)
        assertNotNull("Expected low stock notification when stock hits 5", postedNotification)

        // Calling markAsTaken again on already-TAKEN morning reminder must NOT double-decrement
        viewModel.clearToastMessage()
        viewModel.markAsTaken(morningReminderId)
        viewModel.toastMessage.first { it != null }
        assertEquals(5, prescriptionDao.getMedicineById(medicine.id)?.stockCount)

        // Second dose: 5 -> 4 (no duplicate low-stock notification)
        nm.cancelAll()
        viewModel.clearToastMessage()
        viewModel.markAsTaken(nightReminderId)
        viewModel.toastMessage.first { it != null }
        val afterSecond = viewModel.uiState.first { state ->
            state.todayScheduleInfoList.any { it.reminder?.id == nightReminderId && it.reminder.status == ReminderStatus.TAKEN } &&
                state.todayScheduleInfoList.first().medicine.stockCount == 4
        }
        assertEquals(4, afterSecond.todayScheduleInfoList.first().medicine.stockCount)
        assertNull("Low stock notification must only fire once at 5, not at 4", shadowNm.getNotification(notifId))
        testScheduler.advanceUntilIdle()
    }
}

