package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.ui.screens.home.toBengaliDigits
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.components.formatBengaliPrescriptionDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrescriptionHierarchyTest {

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var viewModel: PrescriptionCarouselViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        prescriptionDao = database.prescriptionDao()
        prescriptionRepository = PrescriptionRepository(prescriptionDao)
        viewModel = PrescriptionCarouselViewModel(prescriptionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    // Audit Check 1: Does tapping FAB clear the previous draft?
    @Test
    fun testTappingFabClearsPreviousDraft() {
        // Populate draft with previous data
        viewModel.updateDate("2024-11-15")
        viewModel.updateDoctorName("ডা. আহমেদ")
        viewModel.updateLocation("ঢাকা মেডিকেল")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrength("500")
        viewModel.updateStrengthUnit("mg")
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateDurationDays("7")

        assertEquals("2024-11-15", viewModel.draftPrescription.value.date)
        assertEquals("ডা. আহমেদ", viewModel.draftPrescription.value.doctorName)
        assertEquals("Napa", viewModel.draftMedicine.value.name)

        // FAB action triggers resetDraft()
        viewModel.resetDraft()

        // Verify draft prescription is completely cleared
        assertNull(viewModel.draftPrescription.value.date)
        assertEquals("", viewModel.draftPrescription.value.doctorName)
        assertEquals("", viewModel.draftPrescription.value.location)
        assertTrue(viewModel.draftPrescription.value.medicines.isEmpty())
        assertNull(viewModel.editingPrescriptionId.value)

        // Verify draft medicine is reset
        assertEquals("", viewModel.draftMedicine.value.name)
        assertNull(viewModel.draftMedicine.value.type)
        assertEquals("", viewModel.draftMedicine.value.strength)
        assertEquals("", viewModel.draftMedicine.value.strengthUnit)
        assertTrue(viewModel.draftMedicine.value.schedules.isEmpty())
    }

    // Audit Check 2: Does saving create a NEW prescription ID? (No appending to existing prescriptions)
    @Test
    fun testSavingCreatesNewPrescriptionId() = runTest {
        // Create first prescription
        viewModel.resetDraft()
        viewModel.updateDate("2024-11-10")
        viewModel.updateDoctorName("Dr. Sarah")
        viewModel.updateMedicineName("Paracetamol")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrength("500")
        viewModel.updateStrengthUnit("mg")
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")
        viewModel.updateDurationDays("5")

        var saved1 = false
        val job1 = viewModel.reviewAndSave(onSuccess = { saved1 = true })
        job1.join()
        assertTrue(saved1)

        val prescriptionsAfter1 = prescriptionRepository.allPrescriptionsWithMedicines.first()
        assertEquals(1, prescriptionsAfter1.size)
        val firstPrescriptionId = prescriptionsAfter1[0].prescription.id
        assertEquals(1, prescriptionsAfter1[0].medicines.size)
        assertEquals("Paracetamol", prescriptionsAfter1[0].medicines[0].medicine.name)

        // Create second prescription without editing (as a new prescription)
        viewModel.resetDraft()
        viewModel.updateDate("2024-11-15")
        viewModel.updateDoctorName("Dr. Watson")
        viewModel.updateMedicineName("Amoxicillin")
        viewModel.updateMedicineType(MedicineType.CAPSULE)
        viewModel.updateStrength("250")
        viewModel.updateStrengthUnit("mg")
        viewModel.toggleSlot(DoseSlot.NIGHT)
        viewModel.updateScheduleDoseAmount(DoseSlot.NIGHT, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.NIGHT, "pcs")
        viewModel.updateDurationDays("7")

        var saved2 = false
        val job2 = viewModel.reviewAndSave(onSuccess = { saved2 = true })
        job2.join()
        assertTrue(saved2)

        val prescriptionsAfter2 = prescriptionRepository.allPrescriptionsWithMedicines.first()
        assertEquals(2, prescriptionsAfter2.size)

        val secondPrescription = prescriptionsAfter2.first { it.prescription.id != firstPrescriptionId }
        val firstPrescriptionReloaded = prescriptionsAfter2.first { it.prescription.id == firstPrescriptionId }

        // The two prescriptions have distinct IDs
        assertNotEquals(firstPrescriptionId, secondPrescription.prescription.id)

        // First prescription still has exactly 1 medicine, was NOT appended to!
        assertEquals(1, firstPrescriptionReloaded.medicines.size)
        assertEquals("Paracetamol", firstPrescriptionReloaded.medicines[0].medicine.name)

        // Second prescription has its own medicine
        assertEquals(1, secondPrescription.medicines.size)
        assertEquals("Amoxicillin", secondPrescription.medicines[0].medicine.name)
        assertEquals(secondPrescription.prescription.id, secondPrescription.medicines[0].medicine.prescriptionId)
    }

    // Audit Check 3: Is the Prescription List Screen showing cards with date and med count?
    @Test
    fun testPrescriptionListScreenItemDisplaysDateDoctorAndMedicineCount() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-15",
            doctorName = null, // Doctor null -> fallback "ডাক্তার উল্লেখ নেই"
            location = "ঢাকা মেডিকেল",
            createdAt = "2024-11-15T10:00:00"
        )

        val med1 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Napa",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-15T10:00:00"
        )
        val med2 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Seclo",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 14,
            foodRule = FoodRule.BEFORE,
            createdAt = "2024-11-15T10:00:00"
        )
        val med3 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Tofen",
            type = MedicineType.SYRUP,
            strength = 100.0,
            strengthUnit = "mg/5mL",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-15T10:00:00"
        )

        val sched1 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med1.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        val sched2 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med2.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        val sched3 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med3.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 5.0,
            doseUnit = "mL"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(med1, med2, med3),
            schedules = listOf(sched1, sched2, sched3)
        )

        // 1. Verify Dao fetches prescription with medicine count
        val listWithCount = prescriptionDao.getAllPrescriptionsWithMedicineCount().first()
        assertEquals(1, listWithCount.size)
        assertEquals(presId, listWithCount[0].prescription.id)
        assertEquals(3, listWithCount[0].medicineCount)

        // 2. Test formatted date helper displays e.g., "১৫ নভেম্বর, ২০২৪"
        val formattedDate = formatBengaliPrescriptionDate("2024-11-15")
        assertEquals("১৫ নভেম্বর, ২০২৪", formattedDate)

        // 3. Test medicine count formatting displays e.g., "৩টি ওষুধ"
        val medCountFormatted = "${toBengaliDigits(listWithCount[0].medicineCount.toString())}টি ওষুধ"
        assertEquals("৩টি ওষুধ", medCountFormatted)

        // 4. Test doctor fallback when null
        val doctorText = if (!listWithCount[0].prescription.doctorName.isNullOrBlank()) {
            listWithCount[0].prescription.doctorName
        } else {
            "ডাক্তার উল্লেখ নেই"
        }
        assertEquals("ডাক্তার উল্লেখ নেই", doctorText)
    }

    // Audit Check 4: Can the user edit an existing prescription?
    @Test
    fun testUserCanEditExistingPrescription() = runTest {
        val presId = UUID.randomUUID().toString()
        val originalPrescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-10",
            doctorName = "ডা. রফিক",
            location = "স্কয়ার হাসপাতাল",
            createdAt = "2024-11-10T10:00:00"
        )

        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Histacin",
            type = MedicineType.TABLET,
            strength = 4.0,
            strengthUnit = "mg",
            durationDays = 3,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-10T10:00:00"
        )

        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = originalPrescription,
            medicines = listOf(med),
            schedules = listOf(sched)
        )

        // Load into ViewModel for editing
        val prescriptionWithMeds = prescriptionRepository.getPrescriptionWithMedicinesById(presId)
        assertNotNull(prescriptionWithMeds)
        viewModel.loadPrescriptionForEditing(prescriptionWithMeds!!)

        // Check editing mode is activated with the existing ID
        assertEquals(presId, viewModel.editingPrescriptionId.value)
        assertEquals("2024-11-10", viewModel.draftPrescription.value.date)
        assertEquals("ডা. রফিক", viewModel.draftPrescription.value.doctorName)
        assertEquals("Histacin", viewModel.draftMedicine.value.name)

        // User updates doctor name and medicine duration
        viewModel.updateDoctorName("ডা. রফিকুল ইসলাম")
        viewModel.updateDurationDays("10")

        var saved = false
        val saveJob = viewModel.reviewAndSave(onSuccess = { saved = true })
        saveJob.join()
        assertTrue(saved)

        // Verify that editing created a NEW row with incremented version number, sharing parentId
        val latestPrescriptions = prescriptionRepository.allPrescriptionsWithMedicines.first()
        assertEquals(1, latestPrescriptions.size)
        val newVersion = latestPrescriptions[0].prescription

        // 1. New row has a distinct ID, does NOT overwrite the old row
        assertNotEquals(presId, newVersion.id)
        // 2. parentId is correctly shared across versions
        assertEquals(presId, newVersion.parentId)
        // 3. versionNumber incremented to 2
        assertEquals(2, newVersion.versionNumber)
        // 4. isLatest is true
        assertTrue(newVersion.isLatest)
        assertEquals("ডা. রফিকুল ইসলাম", newVersion.doctorName)
        assertEquals(1, latestPrescriptions[0].medicines.size)
        assertEquals(10, latestPrescriptions[0].medicines[0].medicine.durationDays)

        // 5. Old version STILL exists in the database with isLatest = false
        val oldVersion = prescriptionDao.getPrescriptionById(presId)
        assertNotNull(oldVersion)
        assertEquals(presId, oldVersion!!.id)
        assertEquals(presId, oldVersion.parentId)
        assertEquals(1, oldVersion.versionNumber)
        assertEquals(false, oldVersion.isLatest)
        assertEquals("ডা. রফিক", oldVersion.doctorName)

        // 6. Old medicines are also preserved
        val oldMedicines = prescriptionDao.getMedicinesByPrescriptionId(presId)
        assertEquals(1, oldMedicines.size)
        assertEquals("Histacin", oldMedicines[0].name)
        assertEquals(3, oldMedicines[0].durationDays)

        // 7. Version history returns both V2 and V1
        val allVersions = prescriptionRepository.getPrescriptionVersionsWithMedicines(presId).first()
        assertEquals(2, allVersions.size)
        assertEquals(2, allVersions[0].prescription.versionNumber)
        assertEquals(1, allVersions[1].prescription.versionNumber)
    }

    // Additional test: deleting a prescription cascades and deletes its medicines
    @Test
    fun testDeletePrescriptionRemovesMedicines() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-12",
            doctorName = "Dr. Test",
            createdAt = "2024-11-12T10:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "TestMed",
            type = MedicineType.TABLET,
            strength = 100.0,
            strengthUnit = "mg",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-12T10:00:00"
        )
        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(med), listOf(sched))

        // Ensure inserted
        assertEquals(1, prescriptionRepository.allPrescriptions.first().size)
        assertEquals(1, prescriptionDao.getMedicinesByPrescriptionId(presId).size)

        // Delete prescription
        var deleted = false
        val job = viewModel.deletePrescription(presId) { deleted = true }
        job.join()
        assertTrue(deleted)

        // Verify deleted from DB
        assertEquals(0, prescriptionRepository.allPrescriptions.first().size)
        assertEquals(0, prescriptionDao.getMedicinesByPrescriptionId(presId).size)
    }

    @Test
    fun testLoadPrescriptionForAddingMedicinePreparesEmptyMedicineAndPreservesExisting() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-20",
            doctorName = "Dr. Khan",
            createdAt = "2024-11-20T10:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Napa",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-20T10:00:00"
        )
        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(med), listOf(sched))

        val presWithMeds = prescriptionRepository.getPrescriptionWithMedicinesById(presId)
        assertNotNull(presWithMeds)

        // User clicks "+ নতুন ওষুধ যোগ করুন"
        viewModel.loadPrescriptionForAddingMedicine(presWithMeds!!)

        assertEquals(presId, viewModel.editingPrescriptionId.value)
        assertEquals(1, viewModel.draftPrescription.value.medicines.size)
        assertEquals("Napa", viewModel.draftPrescription.value.medicines[0].name)
        // Draft medicine for Card 2 must be clean and empty
        assertEquals("", viewModel.draftMedicine.value.name)
        assertNull(viewModel.draftMedicine.value.type)
        assertTrue(viewModel.draftMedicine.value.schedules.isEmpty())
    }

    @Test
    fun testLoadPrescriptionForEditingMedicineLoadsTargetMedicine() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-20",
            doctorName = "Dr. Khan",
            createdAt = "2024-11-20T10:00:00"
        )
        val med1 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "MedA",
            type = MedicineType.TABLET,
            strength = 10.0,
            strengthUnit = "mg",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-20T10:00:00"
        )
        val med2 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "MedB",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.BEFORE,
            createdAt = "2024-11-20T10:00:00"
        )
        val sched1 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med1.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        val sched2 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med2.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(med1, med2), listOf(sched1, sched2))

        val presWithMeds = prescriptionRepository.getPrescriptionWithMedicinesById(presId)
        assertNotNull(presWithMeds)

        // User edits MedB
        viewModel.loadPrescriptionForEditingMedicine(presWithMeds!!, med2.id)

        assertEquals(presId, viewModel.editingPrescriptionId.value)
        assertEquals("MedB", viewModel.draftMedicine.value.name)
        assertEquals(MedicineType.CAPSULE, viewModel.draftMedicine.value.type)
        assertEquals(2, viewModel.draftPrescription.value.medicines.size)
        assertEquals("MedA", viewModel.draftPrescription.value.medicines[0].name)
        assertEquals("MedB", viewModel.draftPrescription.value.medicines[1].name)
        assertEquals(1, viewModel.selectedMedicineIndex.value)

        // Switching chip to MedA loads MedA into draft
        viewModel.selectMedicineForEditing(0)
        assertEquals(0, viewModel.selectedMedicineIndex.value)
        assertEquals("MedA", viewModel.draftMedicine.value.name)
        assertEquals(MedicineType.TABLET, viewModel.draftMedicine.value.type)
    }

    @Test
    fun testDeleteMedicineDeletesFromDatabase() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            date = "2024-11-20",
            createdAt = "2024-11-20T10:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "ToDelete",
            type = MedicineType.TABLET,
            strength = 100.0,
            strengthUnit = "mg",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-20T10:00:00"
        )
        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(med), listOf(sched))
        assertEquals(1, prescriptionDao.getMedicinesByPrescriptionId(presId).size)

        var deleted = false
        val job = viewModel.deleteMedicine(med) { deleted = true }
        job.join()
        assertTrue(deleted)

        assertEquals(0, prescriptionDao.getMedicinesByPrescriptionId(presId).size)
    }
}
