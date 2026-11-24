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
import com.mediciplanner.app.data.local.relation.DoseScheduleWithReminders
import com.mediciplanner.app.data.local.relation.MedicineWithSchedules
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.hasPrescriptionChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Bug19Part2NoChangeTest {

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

    private fun createSamplePrescriptionWithMedicines(
        presId: String = UUID.randomUUID().toString(),
        parentId: String = presId,
        versionNumber: Int = 1
    ): PrescriptionWithMedicines {
        val prescription = PrescriptionEntity(
            id = presId,
            parentId = parentId,
            versionNumber = versionNumber,
            isLatest = true,
            date = "2024-11-20",
            doctorName = "ডা. আহমেদ",
            location = "ঢাকা মেডিকেল",
            note = null,
            createdAt = "2024-11-20T10:00:00",
            prescriptionImagePaths = listOf("img1.jpg")
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "প্যারাসিটামল",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            intervalDays = 1,
            foodRule = FoodRule.AFTER,
            note = "খাওয়ার পর খাবেন",
            createdAt = "2024-11-20T10:00:00",
            imagePath = null
        )
        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val medWithSched = MedicineWithSchedules(
            medicine = med,
            doseSchedules = listOf(DoseScheduleWithReminders(doseSchedule = sched))
        )
        return PrescriptionWithMedicines(
            prescription = prescription,
            medicines = listOf(medWithSched)
        )
    }

    private fun createMatchingDraft(sample: PrescriptionWithMedicines): DraftPrescription {
        val med = sample.medicines.first().medicine
        val sched = sample.medicines.first().doseSchedules.first().doseSchedule
        val draftSched = DraftSchedule(
            slot = sched.slot,
            time = sched.time,
            doseAmount = sched.doseAmount.toString().removeSuffix(".0"),
            doseUnit = sched.doseUnit
        )
        val draftMed = DraftMedicine(
            name = med.name,
            type = med.type,
            strength = med.strength?.toString()?.removeSuffix(".0") ?: "",
            strengthUnit = med.strengthUnit ?: "",
            schedules = listOf(draftSched),
            foodRule = med.foodRule,
            durationDays = med.durationDays?.toString() ?: "",
            intervalDays = med.intervalDays,
            note = med.note ?: "",
            imagePath = med.imagePath
        )
        return DraftPrescription(
            id = sample.prescription.id,
            date = sample.prescription.date,
            doctorName = sample.prescription.doctorName ?: "",
            location = sample.prescription.location ?: "",
            medicines = listOf(draftMed),
            prescriptionImagePaths = sample.prescription.prescriptionImagePaths,
            note = sample.prescription.note ?: ""
        )
    }

    @Test
    fun testHasPrescriptionChangedReturnsFalseWhenIdentical() {
        val sample = createSamplePrescriptionWithMedicines()
        val draft = createMatchingDraft(sample)

        val changed = hasPrescriptionChanged(sample, draft)
        assertFalse("Expected no change when all fields match exactly", changed)
    }

    @Test
    fun testHasPrescriptionChangedDetectsAllPrescriptionFields() {
        val sample = createSamplePrescriptionWithMedicines()
        val base = createMatchingDraft(sample)

        assertTrue(hasPrescriptionChanged(sample, base.copy(doctorName = "ডা. করিম")))
        assertTrue(hasPrescriptionChanged(sample, base.copy(date = "2024-11-25")))
        assertTrue(hasPrescriptionChanged(sample, base.copy(location = "চট্টগ্রাম মেডিকেল")))
        assertTrue(hasPrescriptionChanged(sample, base.copy(note = "নতুন নোট")))
        assertTrue(hasPrescriptionChanged(sample, base.copy(prescriptionImagePaths = listOf("img1.jpg", "img2.jpg"))))

        // Non-ISO Bengali dates longer than 10 chars differing only at the end
        val bengaliSample = sample.copy(
            prescription = sample.prescription.copy(date = "১২ নভেম্বর, ২০২৬")
        )
        val bengaliBase = createMatchingDraft(bengaliSample)
        assertFalse(hasPrescriptionChanged(bengaliSample, bengaliBase))
        assertTrue(hasPrescriptionChanged(bengaliSample, bengaliBase.copy(date = "১২ নভেম্বর, ২০২৭")))
    }

    @Test
    fun testHasPrescriptionChangedDetectsAllMedicineFields() {
        val sample = createSamplePrescriptionWithMedicines()
        val base = createMatchingDraft(sample)
        val baseMed = base.medicines.first()
        val baseSched = baseMed.schedules.first()

        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(name = "নাপা")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(type = MedicineType.CAPSULE)))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(strength = "250")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(strengthUnit = "mcg")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(foodRule = FoodRule.BEFORE)))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(durationDays = "10")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(intervalDays = 2)))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(note = "ভিন্ন নোট")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(imagePath = "med.jpg")))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(schedules = listOf(baseSched.copy(doseAmount = "2")))))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(schedules = listOf(baseSched.copy(time = "09:00")))))))
        assertTrue(hasPrescriptionChanged(sample, base.copy(medicines = listOf(baseMed.copy(schedules = listOf(baseSched.copy(doseUnit = "pcs")))))))
    }

    @Test
    fun testNoChangesStringResourceMatchesExpectedBengaliToast() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("কোনো পরিবর্তন করা হয়নি", context.getString(R.string.prescription_no_changes))
    }

    @Test
    fun testSavePrescriptionDoesNotCreateNewVersionWhenNoChangesMade() = runTest {
        // 1. Seed initial prescription in DB
        val sample = createSamplePrescriptionWithMedicines()
        prescriptionRepository.saveFullPrescription(
            prescription = sample.prescription,
            medicines = sample.medicines.map { it.medicine },
            schedules = sample.medicines.flatMap { it.doseSchedules.map { s -> s.doseSchedule } }
        )

        val initialVersions = prescriptionRepository.getPrescriptionVersionsWithMedicines(sample.prescription.parentId).first()
        assertEquals(1, initialVersions.size)
        assertEquals(1, initialVersions[0].prescription.versionNumber)
        assertTrue(initialVersions[0].prescription.isLatest)

        // 2. Load into ViewModel for editing
        viewModel.loadPrescriptionForEditing(sample)
        assertEquals(sample.prescription.id, viewModel.editingPrescriptionId.value)

        // 3. User saves WITHOUT making any modifications
        var successCallbackCalled = false
        var noChangesCallbackCalled = false

        val job = viewModel.savePrescription(
            onSuccess = { successCallbackCalled = true },
            onNoChanges = { noChangesCallbackCalled = true }
        )
        job.join()

        // 4. Verify no changes callback or toast message
        assertTrue(noChangesCallbackCalled || successCallbackCalled)
        assertEquals("কোনো পরিবর্তন করা হয়নি", viewModel.toastMessage.value)

        // 5. Verify NO new version was added in database!
        val versionsAfterSave = prescriptionRepository.getPrescriptionVersionsWithMedicines(sample.prescription.parentId).first()
        assertEquals(1, versionsAfterSave.size)
        assertEquals(1, versionsAfterSave[0].prescription.versionNumber)
        assertEquals(sample.prescription.id, versionsAfterSave[0].prescription.id)
        assertTrue(versionsAfterSave[0].prescription.isLatest)
    }

    @Test
    fun testSavePrescriptionCreatesNewVersionWhenChangesAreMade() = runTest {
        // 1. Seed initial prescription in DB
        val sample = createSamplePrescriptionWithMedicines()
        prescriptionRepository.saveFullPrescription(
            prescription = sample.prescription,
            medicines = sample.medicines.map { it.medicine },
            schedules = sample.medicines.flatMap { it.doseSchedules.map { s -> s.doseSchedule } }
        )

        // 2. Load into ViewModel for editing
        viewModel.loadPrescriptionForEditing(sample)

        // 3. Make a change: update Doctor Name
        viewModel.updateDoctorName("ডা. মাহমুদ হাসান")

        var successCallbackCalled = false
        val job = viewModel.savePrescription(
            onSuccess = { successCallbackCalled = true }
        )
        job.join()

        assertTrue(successCallbackCalled)

        // 4. Verify NEW version was created in database
        val versionsAfterSave = prescriptionRepository.getPrescriptionVersionsWithMedicines(sample.prescription.parentId).first()
        assertEquals(2, versionsAfterSave.size)

        // Latest version is version 2 with updated doctor name
        val latest = versionsAfterSave.find { it.prescription.isLatest }
        assertNotNull(latest)
        assertEquals(2, latest!!.prescription.versionNumber)
        assertEquals("ডা. মাহমুদ হাসান", latest.prescription.doctorName)

        // Old version remains version 1 with isLatest = false
        val old = versionsAfterSave.find { !it.prescription.isLatest }
        assertNotNull(old)
        assertEquals(1, old!!.prescription.versionNumber)
        assertEquals("ডা. আহমেদ", old.prescription.doctorName)
    }
}
