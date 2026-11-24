package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.ui.screens.track.TrackViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
class TrackViewModelTest {

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var viewModel: TrackViewModel

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
        viewModel = TrackViewModel(prescriptionRepository, reminderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testSelectDateUpdatesState() = runTest {
        val targetDate = LocalDate.now().minusDays(2)
        viewModel.selectDate(targetDate)

        assertEquals(targetDate, viewModel.selectedDate.value)
    }

    @Test
    fun testTrackScreenMissedAndTakenFlow() = runTest {
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val prescId = UUID.randomUUID().toString()

        val prescription = PrescriptionEntity(
            id = prescId,
            date = todayIso,
            doctorName = "ডা. জামান",
            location = "ঢাকা",
            createdAt = nowIso
        )
        prescriptionDao.insertPrescription(prescription)

        val medId = UUID.randomUUID().toString()
        val medicine = MedicineEntity(
            id = medId,
            prescriptionId = prescId,
            name = "প্যারাসিটামল",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = nowIso
        )
        prescriptionDao.insertMedicine(medicine)

        val schedId1 = UUID.randomUUID().toString()
        val sched1 = DoseScheduleEntity(
            id = schedId1,
            medicineId = medId,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val schedId2 = UUID.randomUUID().toString()
        val sched2 = DoseScheduleEntity(
            id = schedId2,
            medicineId = medId,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        prescriptionDao.insertDoseSchedule(sched1)
        prescriptionDao.insertDoseSchedule(sched2)

        // Add a reminder with MISSED status for morning
        val remId1 = UUID.randomUUID().toString()
        reminderDao.insertReminder(
            ReminderEntity(
                id = remId1,
                doseScheduleId = schedId1,
                scheduledAt = "${todayIso}T08:00:00",
                status = ReminderStatus.MISSED
            )
        )

        // Add a reminder with TAKEN status for night
        val remId2 = UUID.randomUUID().toString()
        reminderDao.insertReminder(
            ReminderEntity(
                id = remId2,
                doseScheduleId = schedId2,
                scheduledAt = "${todayIso}T22:00:00",
                status = ReminderStatus.TAKEN,
                takenAt = "${todayIso}T22:05:00"
            )
        )

        val state = viewModel.uiState.first { !it.isLoading && it.trackedDoses.isNotEmpty() }

        assertEquals(2, state.totalDosesForDate)
        assertEquals(1, state.completedDosesForDate)
        assertEquals(1, state.missedCount)
        assertTrue(state.missedMedicinesGroupedBySlot.containsKey(DoseSlot.MORNING.name))
        assertEquals("প্যারাসিটামল", state.missedMedicinesGroupedBySlot[DoseSlot.MORNING.name]?.first()?.name)

        // Verify groupedHistory contains MORNING and NIGHT in proper order
        assertTrue(state.groupedHistory.containsKey(DoseSlot.MORNING))
        assertTrue(state.groupedHistory.containsKey(DoseSlot.NIGHT))
        val keysList = state.groupedHistory.keys.toList()
        assertEquals(listOf(DoseSlot.MORNING, DoseSlot.NIGHT), keysList)
        assertEquals(1, state.groupedHistory[DoseSlot.MORNING]?.size)
        assertEquals(1, state.groupedHistory[DoseSlot.NIGHT]?.size)

        // Test markAllMissedAsTaken()
        viewModel.markAllMissedAsTaken()

        val updatedState = viewModel.uiState.first { it.missedCount == 0 }
        assertEquals(0, updatedState.missedCount)
        assertEquals(2, updatedState.completedDosesForDate)
    }

    @Test
    fun testDateStripIncludesPastAndFutureDates() = runTest {
        val today = LocalDate.now()
        val state = viewModel.uiState.first { !it.isLoading }

        assertEquals(14, state.dateStripDates.size)
        assertEquals(today.minusDays(7), state.dateStripDates.first())
        assertEquals(today.plusDays(6), state.dateStripDates.last())
        assertTrue(state.dateStripDates.contains(today))
        assertTrue(state.dateStripDates.contains(today.plusDays(1)))
        assertTrue(state.dateStripDates.contains(today.plusDays(6)))
    }

    @Test
    fun testSelectFutureDateLoadsScheduledMedicines() = runTest {
        val today = LocalDate.now()
        val todayIso = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val prescId = UUID.randomUUID().toString()

        prescriptionDao.insertPrescription(
            PrescriptionEntity(
                id = prescId,
                date = todayIso,
                doctorName = "ডা. করিম",
                location = "ঢাকা",
                createdAt = nowIso
            )
        )

        val medId = UUID.randomUUID().toString()
        prescriptionDao.insertMedicine(
            MedicineEntity(
                id = medId,
                prescriptionId = prescId,
                name = "নাপা এক্সট্রা",
                type = MedicineType.TABLET,
                strength = 500.0,
                strengthUnit = "mg",
                durationDays = 5,
                foodRule = FoodRule.AFTER,
                createdAt = nowIso
            )
        )

        prescriptionDao.insertDoseSchedule(
            DoseScheduleEntity(
                id = UUID.randomUUID().toString(),
                medicineId = medId,
                slot = DoseSlot.MORNING,
                time = "08:00",
                doseAmount = 1.0,
                doseUnit = "টি"
            )
        )

        val futureDate = today.plusDays(3)
        viewModel.selectDate(futureDate)

        val futureState = viewModel.uiState.first {
            !it.isLoading && it.selectedDate == futureDate && it.trackedDoses.isNotEmpty()
        }
        assertEquals(1, futureState.totalDosesForDate)
        assertEquals(0, futureState.missedCount)
        assertEquals(ReminderStatus.PENDING, futureState.trackedDoses.first().status)
        assertEquals("নাপা এক্সট্রা", futureState.trackedDoses.first().medicine.name)
    }
}
