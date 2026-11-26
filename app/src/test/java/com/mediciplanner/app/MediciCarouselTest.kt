package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.DoseAmountHelper
import com.mediciplanner.app.ui.screens.prescription.DraftMedicine
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import com.mediciplanner.app.ui.screens.prescription.DraftSchedule
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.components.Card3ScheduleDose
import com.mediciplanner.app.ui.screens.prescription.components.Card4DurationNotes
import com.mediciplanner.app.ui.screens.prescription.components.getSlotColor
import com.mediciplanner.app.ui.screens.prescription.getDoseUnitsForType
import com.mediciplanner.app.ui.screens.prescription.getUnitsForType
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MediciCarouselTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository
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
        val reminderDao = database.reminderDao()
        reminderRepository = ReminderRepository(reminderDao)
        viewModel = PrescriptionCarouselViewModel(prescriptionRepository, reminderRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    // Check 1: Add More Logic: Does 'Add More' reset Date, Doctor, and Location?
    @Test
    fun testAddMoreDoesNotResetDateDoctorOrLocation() {
        // Set Base Info
        viewModel.updateDate("15 নভে, 2026")
        viewModel.updateDoctorName("Dr. Sarah")
        viewModel.updateLocation("Central Clinic")

        // Set Medicine Details
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrength("500")
        viewModel.updateStrengthUnit("mg")

        // Set Schedule
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")
        viewModel.updateDurationDays("7")

        var navigatedToCard2 = false
        viewModel.onAddMoreClicked(onNavigateToCard2 = { navigatedToCard2 = true })

        // 1. Navigation callback executed
        assertTrue(navigatedToCard2)

        // 2. DraftPrescription base info preserved
        val draftPrescription = viewModel.draftPrescription.value
        assertEquals("15 নভে, 2026", draftPrescription.date)
        assertEquals("Dr. Sarah", draftPrescription.doctorName)
        assertEquals("Central Clinic", draftPrescription.location)

        // 3. One medicine was committed
        assertEquals(1, draftPrescription.medicines.size)
        assertEquals("Napa", draftPrescription.medicines[0].name)

        // 4. DraftMedicine fields cleared for the next entry
        val currentDraftMed = viewModel.draftMedicine.value
        assertEquals("", currentDraftMed.name)
        assertNull(currentDraftMed.type)
        assertEquals("", currentDraftMed.strength)
        assertEquals("", currentDraftMed.strengthUnit)
        assertTrue(currentDraftMed.schedules.isEmpty())
    }

    // Check 2: Type Change Reset: Does changing medicine type clear invalid units?
    @Test
    fun testChangingMedicineTypeResetsInvalidUnits() {
        // Set Tablet with mg (Strength) and pcs (Dose)
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrengthUnit("mg")
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")

        assertEquals("mg", viewModel.draftMedicine.value.strengthUnit)
        assertEquals("pcs", viewModel.draftMedicine.value.schedules[0].doseUnit)

        // Change to Syrup (allowed dose units: "mL", "tsp")
        viewModel.updateMedicineType(MedicineType.SYRUP)

        // "mg" is invalid for SYRUP strength, so strengthUnit must be cleared
        assertEquals("", viewModel.draftMedicine.value.strengthUnit)
        // Dose unit "pcs" was not valid for SYRUP, so it must be cleared to ""
        assertEquals("", viewModel.draftMedicine.value.schedules[0].doseUnit)

        // Test clearStrengthUnit explicitly
        viewModel.updateStrengthUnit("mg/5mL")
        assertEquals("mg/5mL", viewModel.draftMedicine.value.strengthUnit)
        viewModel.clearStrengthUnit()
        assertEquals("", viewModel.draftMedicine.value.strengthUnit)
    }

    @Test
    fun testDoseUnitsSeparationFromStrengthUnits() {
        // TABLET / CAPSULE
        val tabletDoseUnits = getDoseUnitsForType(MedicineType.TABLET)
        assertTrue(tabletDoseUnits.contains("pcs"))
        assertTrue(tabletDoseUnits.contains("টি"))
        assertFalse(tabletDoseUnits.contains("mg"))
        val tabletStrengthUnits = getUnitsForType(MedicineType.TABLET)
        assertTrue(tabletStrengthUnits.contains("mg"))
        assertTrue(tabletStrengthUnits.contains("mcg"))

        // SYRUP
        val syrupDoseUnits = getDoseUnitsForType(MedicineType.SYRUP)
        assertTrue(syrupDoseUnits.contains("mL"))
        assertTrue(syrupDoseUnits.contains("tsp"))

        // DROPS
        val dropDoseUnits = getDoseUnitsForType(MedicineType.DROPS)
        assertTrue(dropDoseUnits.contains("mL"))
        assertTrue(dropDoseUnits.contains("drop"))

        // INHALER
        val inhalerDoseUnits = getDoseUnitsForType(MedicineType.INHALER)
        assertTrue(inhalerDoseUnits.contains("puff"))

        // INJECTION
        val injectionDoseUnits = getDoseUnitsForType(MedicineType.INJECTION)
        assertTrue(injectionDoseUnits.contains("mL"))
        assertTrue(injectionDoseUnits.contains("unit"))

        // OINTMENT
        val ointmentDoseUnits = getDoseUnitsForType(MedicineType.OINTMENT)
        assertTrue(ointmentDoseUnits.contains("application"))

        // NULL
        assertTrue(getDoseUnitsForType(null).isEmpty())
    }

    // Check 3: Validation Gate: Are Next/Save buttons properly disabled based on required fields?
    @Test
    fun testValidationGates() {
        // Card 1 Gate: Date is required
        assertFalse("Card 1 should be invalid when Date is null", viewModel.isCard1Valid())
        viewModel.updateDate("10 ডিসে, 2026")
        assertTrue("Card 1 should be valid after Date is set", viewModel.isCard1Valid())

        // Card 2 Gate: Name and Type are required
        assertFalse("Card 2 should be invalid initially", viewModel.isCard2Valid())
        viewModel.updateMedicineName("Amoxicillin")
        assertFalse("Card 2 should still be invalid without Type", viewModel.isCard2Valid())
        viewModel.updateMedicineType(MedicineType.CAPSULE)
        assertTrue("Card 2 should be valid with Name and Type", viewModel.isCard2Valid())

        // Card 3 Gate: At least 1 slot with non-blank dose amount and unit
        assertFalse("Card 3 should be invalid with 0 slots", viewModel.isCard3Valid())
        viewModel.toggleSlot(DoseSlot.NIGHT)
        viewModel.updateScheduleDoseAmount(DoseSlot.NIGHT, "")
        assertFalse("Card 3 should be invalid if amount is empty", viewModel.isCard3Valid())
        viewModel.updateScheduleDoseAmount(DoseSlot.NIGHT, "2")
        viewModel.updateScheduleDoseUnit(DoseSlot.NIGHT, "pcs")
        assertTrue("Card 3 should be valid when slot has dose and unit", viewModel.isCard3Valid())
    }

    // Test Review & Save persists successfully to Room
    @Test
    fun testReviewAndSavePersistsToDatabase() = runTest {
        viewModel.updateDate("2026-11-04")
        viewModel.updateDoctorName("Dr. Watson")
        viewModel.updateLocation("221B Baker St")

        viewModel.updateMedicineName("Aspirin")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrength("100")
        viewModel.updateStrengthUnit("mg")
        viewModel.updateFoodRule(FoodRule.AFTER)
        viewModel.updateDurationDays("14")
        viewModel.updateMedicineNote("Take with water")

        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")

        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()

        assertTrue("Prescription should be saved successfully. Error: ${viewModel.saveError.value}", saved)
        assertNull(viewModel.saveError.value)
    }

    @Test
    fun testReviewAndSaveWithTiDoseUnitAndEmptyStrengthPersists() = runTest {
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-09")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        // Strength left completely empty (optional)
        viewModel.updateStrength("")
        viewModel.updateStrengthUnit("")
        viewModel.updateDurationDays("7")

        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        // "টি" unit used for Tablet
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "টি")

        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()

        assertTrue("Prescription with 'টি' unit and empty strength must be saved successfully. Error: ${viewModel.saveError.value}", saved)
        assertNull(viewModel.saveError.value)
    }

    @Test
    fun testDateValidationLogicAndImmediateStateUpdate() {
        // Initially null/blank date: Card 1 must be invalid
        assertNull(viewModel.draftPrescription.value.date)
        assertFalse(viewModel.isCard1Valid())

        // Blank date: must remain invalid
        viewModel.updateDate("   ")
        assertFalse(viewModel.isCard1Valid())

        // Valid date from DatePicker: immediately updates draftPrescription.date and isCard1Valid
        val selectedDate = "05 নভে, 2026"
        viewModel.updateDate(selectedDate)
        assertEquals(selectedDate, viewModel.draftPrescription.value.date)
        assertTrue(viewModel.isCard1Valid())
    }

    @Test
    fun testIsNextEnabledCalculatedDynamically() {
        // Initially incomplete
        assertFalse(viewModel.isNextEnabled)

        // Set name only
        viewModel.updateMedicineName("Paracetamol")
        assertFalse(viewModel.isNextEnabled)

        // Set type
        viewModel.updateMedicineType(MedicineType.TABLET)
        assertFalse(viewModel.isNextEnabled)

        // Add slot without dose amount or unit
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "")
        assertFalse(viewModel.isNextEnabled)

        // Fill dose amount and unit
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "500")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")

        // Now all required fields (Name, Type, Dose slot, amount, unit) are filled
        assertTrue(viewModel.isNextEnabled)
    }

    @Test
    fun testPreciseValidationMessagesAndErrorClearingOnInput() {
        // Card 1: Empty date
        val card1Valid = viewModel.validateCard(0)
        assertFalse(card1Valid)
        assertEquals("অনুগ্রহ করে তারিখ সিলেক্ট করুন", viewModel.errorMessage.value)

        // Type date -> error cleared
        viewModel.updateDate("12 নভে, 2026")
        assertNull(viewModel.errorMessage.value)
        assertTrue(viewModel.validateCard(0))

        // Card 2: Empty medicine name
        val card2Valid = viewModel.validateCard(1)
        assertFalse(card2Valid)
        assertEquals("অনুগ্রহ করে ওষুধের নাম লিখুন", viewModel.errorMessage.value)

        // Type medicine name -> error cleared
        viewModel.updateMedicineName("Napa Extra")
        assertNull(viewModel.errorMessage.value)

        // Missing type
        assertFalse(viewModel.validateCard(1))
        assertEquals("অনুগ্রহ করে ওষুধের ধরন সিলেক্ট করুন", viewModel.errorMessage.value)

        // Select type -> error cleared and Card 2 is already valid because Strength is optional
        viewModel.updateMedicineType(MedicineType.TABLET)
        assertNull(viewModel.errorMessage.value)
        assertTrue("Card 2 should be valid without strength because strength is optional", viewModel.validateCard(1))

        // When strength is entered without unit, it requires unit
        viewModel.updateStrength("500")
        assertFalse("When strength is entered without unit, unit is required", viewModel.validateCard(1))
        assertEquals("অনুগ্রহ করে মাত্রার একক সিলেক্ট করুন", viewModel.errorMessage.value)

        // Select strength unit -> error cleared and valid
        viewModel.updateStrengthUnit("mg")
        assertNull(viewModel.errorMessage.value)
        assertTrue(viewModel.validateCard(1))

        // Clear strength -> still valid because strength is optional
        viewModel.updateStrength("")
        viewModel.clearStrengthUnit()
        assertTrue("Card 2 should remain valid when strength is empty", viewModel.validateCard(1))

        // Card 3: Missing slot
        assertFalse(viewModel.validateCard(2))
        assertEquals("অনুগ্রহ করে সেবনের সময় সিলেক্ট করুন", viewModel.errorMessage.value)

        // Select slot -> slot created with default doseAmount "1" and doseUnit "pcs"
        viewModel.toggleSlot(DoseSlot.MORNING)
        assertNull(viewModel.errorMessage.value)

        // Empty dose amount
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "")
        assertFalse(viewModel.validateCard(2))
        assertEquals("অনুগ্রহ করে ডোজের পরিমাণ লিখুন", viewModel.errorMessage.value)

        // Type dose amount -> error cleared
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        assertNull(viewModel.errorMessage.value)

        // Empty dose unit
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "")
        assertFalse(viewModel.validateCard(2))
        assertEquals("অনুগ্রহ করে ডোজের একক সিলেক্ট করুন", viewModel.errorMessage.value)

        // Select dose unit -> error cleared
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")
        assertNull(viewModel.errorMessage.value)
        assertTrue(viewModel.validateCard(2))

        // Card 4: Missing duration
        assertFalse(viewModel.validateCard(3))
        assertEquals("অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন", viewModel.errorMessage.value)
        assertEquals("অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন", viewModel.durationError.value)

        // Invalid "0" duration
        viewModel.updateDurationDays("0")
        assertFalse(viewModel.validateCard(3))
        assertEquals("অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন", viewModel.durationError.value)

        // Type valid duration
        viewModel.updateDurationDays("5")
        assertNull(viewModel.errorMessage.value)
        assertNull(viewModel.durationError.value)
        assertTrue(viewModel.validateCard(3))
    }

    @Test
    fun testCannotSaveWithoutDuration() = runTest {
        viewModel.updateDate("2026-11-06")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.updateStrength("500")
        viewModel.updateStrengthUnit("mg")
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "pcs")

        // Duration is empty
        viewModel.updateDurationDays("")

        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()

        assertFalse("Prescription must NOT be saved when duration is empty", saved)
        assertEquals("অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন", viewModel.errorMessage.value)
        assertEquals("অনুগ্রহ করে ওষুধ সেবনের মেয়াদ (কত দিন) লিখুন", viewModel.durationError.value)
    }

    @Test
    fun testUnitMatrixForAll7MedicineTypes() {
        // TABLET -> "mg", "mcg"
        assertEquals(listOf("mg", "mcg"), getUnitsForType(MedicineType.TABLET))

        // CAPSULE -> "mg", "mcg"
        assertEquals(listOf("mg", "mcg"), getUnitsForType(MedicineType.CAPSULE))

        // SYRUP -> "mg/5mL"
        assertEquals(listOf("mg/5mL"), getUnitsForType(MedicineType.SYRUP))

        // DROPS -> "mg/mL"
        assertEquals(listOf("mg/mL"), getUnitsForType(MedicineType.DROPS))

        // INHALER -> "mcg/puff"
        assertEquals(listOf("mcg/puff"), getUnitsForType(MedicineType.INHALER))

        // INJECTION -> "mg/mL", "IU/mL"
        assertEquals(listOf("mg/mL", "IU/mL"), getUnitsForType(MedicineType.INJECTION))

        // OINTMENT -> "%", "mg/g"
        assertEquals(listOf("%", "mg/g"), getUnitsForType(MedicineType.OINTMENT))

        // null -> emptyList()
        assertEquals(emptyList<String>(), getUnitsForType(null))
    }

    @Test
    fun testTypeChangeResetsStrengthUnitAcrossAllTypes() {
        val allTypes = listOf(
            MedicineType.TABLET to "mg",
            MedicineType.CAPSULE to "mcg",
            MedicineType.SYRUP to "mg/5mL",
            MedicineType.DROPS to "mg/mL",
            MedicineType.INHALER to "mcg/puff",
            MedicineType.INJECTION to "IU/mL",
            MedicineType.OINTMENT to "%"
        )

        for (i in allTypes.indices) {
            val (currentType, unit) = allTypes[i]
            val (nextType, _) = allTypes[(i + 1) % allTypes.size]

            viewModel.updateMedicineType(currentType)
            viewModel.updateStrengthUnit(unit)
            assertEquals(unit, viewModel.draftMedicine.value.strengthUnit)

            // Switching to another type MUST immediately clear strengthUnit
            viewModel.updateMedicineType(nextType)
            assertEquals("", viewModel.draftMedicine.value.strengthUnit)
        }
    }

    @Test
    fun testDoseUnitsAndAmountsRemainIndependentAcrossSlots() {
        viewModel.updateMedicineType(MedicineType.TABLET)

        // Add multiple slots
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.toggleSlot(DoseSlot.NOON)
        viewModel.toggleSlot(DoseSlot.AFTERNOON)
        viewModel.toggleSlot(DoseSlot.NIGHT)

        // Set initial independent values
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "টি")

        viewModel.updateScheduleDoseAmount(DoseSlot.NOON, "2")
        viewModel.updateScheduleDoseUnit(DoseSlot.NOON, "টি")

        viewModel.updateScheduleDoseAmount(DoseSlot.AFTERNOON, "1.5")
        viewModel.updateScheduleDoseUnit(DoseSlot.AFTERNOON, "mL")

        viewModel.updateScheduleDoseAmount(DoseSlot.NIGHT, "3")
        viewModel.updateScheduleDoseUnit(DoseSlot.NIGHT, "pcs")

        val schedules = viewModel.draftMedicine.value.schedules
        assertEquals(4, schedules.size)

        // Change ONLY Morning's dose unit and amount
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "drop")
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "5")

        val updatedSchedules = viewModel.draftMedicine.value.schedules
        val morningSched = updatedSchedules.first { it.slot == DoseSlot.MORNING }
        val noonSched = updatedSchedules.first { it.slot == DoseSlot.NOON }
        val afternoonSched = updatedSchedules.first { it.slot == DoseSlot.AFTERNOON }
        val nightSched = updatedSchedules.first { it.slot == DoseSlot.NIGHT }

        // Morning is updated
        assertEquals("5", morningSched.doseAmount)
        assertEquals("drop", morningSched.doseUnit)

        // Noon is completely unchanged!
        assertEquals("2", noonSched.doseAmount)
        assertEquals("টি", noonSched.doseUnit)

        // Afternoon is completely unchanged!
        assertEquals("1.5", afternoonSched.doseAmount)
        assertEquals("mL", afternoonSched.doseUnit)

        // Night is completely unchanged!
        assertEquals("3", nightSched.doseAmount)
        assertEquals("pcs", nightSched.doseUnit)

        // Changing Noon's unit does NOT affect Morning
        viewModel.updateScheduleDoseUnit(DoseSlot.NOON, "tsp")
        val finalMorningSched = viewModel.draftMedicine.value.schedules.first { it.slot == DoseSlot.MORNING }
        val finalNoonSched = viewModel.draftMedicine.value.schedules.first { it.slot == DoseSlot.NOON }
        assertEquals("drop", finalMorningSched.doseUnit)
        assertEquals("tsp", finalNoonSched.doseUnit)
    }

    // === INTERVAL LOGIC TESTS ===

    // Check 1: Database has intervalDays column
    @Test
    fun testDatabaseHasIntervalDaysColumn() {
        val cursor = database.openHelper.readableDatabase.query("PRAGMA table_info(medicines)")
        val columnNames = mutableListOf<String>()
        val nameColIdx = cursor.getColumnIndex("name")
        while (cursor.moveToNext()) {
            columnNames.add(cursor.getString(nameColIdx))
        }
        cursor.close()
        assertTrue("Medicines table must contain intervalDays column", columnNames.contains("intervalDays"))
    }

    // Check 2: UI Draft state updates intervalDays properly (standard + custom)
    @Test
    fun testIntervalChipsAndCustomIntervalSelection() {
        // Default interval is 1 (প্রতিদিন)
        assertEquals(1, viewModel.draftMedicine.value.intervalDays)

        // Select every 2 days
        viewModel.updateIntervalDays(2)
        assertEquals(2, viewModel.draftMedicine.value.intervalDays)

        // Select every 3 days
        viewModel.updateIntervalDays(3)
        assertEquals(3, viewModel.draftMedicine.value.intervalDays)

        // Select weekly (7)
        viewModel.updateIntervalDays(7)
        assertEquals(7, viewModel.draftMedicine.value.intervalDays)

        // Select custom interval (e.g. 5 days)
        viewModel.updateIntervalDays(5)
        assertEquals(5, viewModel.draftMedicine.value.intervalDays)

        // Negative or zero defaults to minimum 1
        viewModel.updateIntervalDays(0)
        assertEquals(1, viewModel.draftMedicine.value.intervalDays)
    }

    // Check 3: Interval > Duration warning & validation block
    @Test
    fun testIntervalGreaterThanDurationShowsWarningAndBlocksCard4() {
        viewModel.updateDate("2026-11-09")
        viewModel.updateMedicineName("Insulin")
        viewModel.updateMedicineType(MedicineType.INJECTION)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "10")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "unit")

        // Duration is 5 days, but user sets interval to 7 (weekly)
        viewModel.updateDurationDays("5")
        viewModel.updateIntervalDays(7)

        // Validation should fail
        assertFalse(viewModel.validateIntervalAgainstDuration())
        assertEquals("মেয়াদের চেয়ে বেশি বিরতি দেয়া যাবে না", viewModel.intervalError.value)
        assertFalse("Card 4 must be invalid when interval > duration", viewModel.isCard4Valid())
        assertFalse("validateCard(3) must fail when interval > duration", viewModel.validateCard(3))

        // Fix duration to 14 days (duration >= interval)
        viewModel.updateDurationDays("14")
        assertTrue(viewModel.validateIntervalAgainstDuration())
        assertNull(viewModel.intervalError.value)
        assertTrue("Card 4 must become valid when duration >= interval", viewModel.isCard4Valid())
        assertTrue("validateCard(3) must pass when duration >= interval", viewModel.validateCard(3))
    }

    // Check 4: Reminder generation skips days based on Interval (Nov 9, Duration 10, Interval 2 -> 5 reminders)
    @Test
    fun testIntervalReminderGenerationSkipsDays() = runTest {
        viewModel.resetDraft()
        val startDateStr = "2026-11-09"
        viewModel.updateDate(startDateStr)
        viewModel.updateMedicineName("Insulin Glargine")
        viewModel.updateMedicineType(MedicineType.INJECTION)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "12")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "unit")

        // Start Date = Nov 9, Duration = 10 days, Interval = 2 days
        viewModel.updateDurationDays("10")
        viewModel.updateIntervalDays(2)

        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()

        assertTrue("Prescription save must succeed. Error: ${viewModel.saveError.value}", saved)

        // Query generated reminders from database
        val allRemindersList = database.reminderDao().getAllReminders().first()

        // Expected 5 reminders on Nov 9, Nov 11, Nov 13, Nov 15, Nov 17
        assertEquals("Exactly 5 reminders should be generated for 10 days with interval 2", 5, allRemindersList.size)

        val scheduledDates = allRemindersList.map { it.scheduledAt.take(10) }.sorted()
        val expectedDates = listOf(
            "2026-11-09",
            "2026-11-11",
            "2026-11-13",
            "2026-11-15",
            "2026-11-17"
        )
        assertEquals(expectedDates, scheduledDates)
    }

    @Test
    fun testPerSlotFoodRuleIndependenceAndPersistence() = runTest {
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-12")
        viewModel.updateMedicineName("Omeprazole")
        viewModel.updateMedicineType(MedicineType.CAPSULE)
        viewModel.updateDurationDays("7")

        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.toggleSlot(DoseSlot.NOON)
        viewModel.toggleSlot(DoseSlot.NIGHT)

        val schedules = viewModel.draftMedicine.value.schedules
        val morningId = schedules.first { it.slot == DoseSlot.MORNING }.id
        val noonId = schedules.first { it.slot == DoseSlot.NOON }.id
        val nightId = schedules.first { it.slot == DoseSlot.NIGHT }.id

        // Default foodRule on each DraftSchedule is FoodRule.AFTER and IDs are distinct
        assertEquals(3, setOf(morningId, noonId, nightId).size)
        schedules.forEach { sched ->
            assertEquals(FoodRule.AFTER, sched.foodRule)
        }

        // Update Morning ONLY by slotId -> Noon and Night remain AFTER
        viewModel.updateSlotFoodRule(morningId, FoodRule.BEFORE)
        val afterMorningUpdate = viewModel.draftMedicine.value.schedules
        assertEquals(FoodRule.BEFORE, afterMorningUpdate.first { it.id == morningId }.foodRule)
        assertEquals(FoodRule.AFTER, afterMorningUpdate.first { it.id == noonId }.foodRule)
        assertEquals(FoodRule.AFTER, afterMorningUpdate.first { it.id == nightId }.foodRule)

        // Update Noon & Night independently by slotId
        viewModel.updateSlotFoodRule(noonId, FoodRule.WITH)
        viewModel.updateSlotFoodRule(nightId, FoodRule.EMPTY)
        val afterAllUpdates = viewModel.draftMedicine.value.schedules
        assertEquals(FoodRule.BEFORE, afterAllUpdates.first { it.id == morningId }.foodRule)
        assertEquals(FoodRule.WITH, afterAllUpdates.first { it.id == noonId }.foodRule)
        assertEquals(FoodRule.EMPTY, afterAllUpdates.first { it.id == nightId }.foodRule)

        // Persist to database and verify each DoseScheduleEntity keeps its own foodRule
        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()
        assertTrue("Expected save to succeed. Error: ${viewModel.saveError.value}", saved)

        val latest = prescriptionDao.getAllPrescriptionsWithMedicines().first().first()
        val persistedSchedules = latest.medicines.first().doseSchedules.map { it.doseSchedule }
        assertEquals(FoodRule.BEFORE, persistedSchedules.first { it.slot == DoseSlot.MORNING }.foodRule)
        assertEquals(FoodRule.WITH, persistedSchedules.first { it.slot == DoseSlot.NOON }.foodRule)
        assertEquals(FoodRule.EMPTY, persistedSchedules.first { it.slot == DoseSlot.NIGHT }.foodRule)

        // Load for editing and verify per-slot foodRule is restored
        viewModel.loadPrescriptionForEditing(latest)
        val reloadedSchedules = viewModel.draftMedicine.value.schedules
        assertEquals(FoodRule.BEFORE, reloadedSchedules.first { it.slot == DoseSlot.MORNING }.foodRule)
        assertEquals(FoodRule.WITH, reloadedSchedules.first { it.slot == DoseSlot.NOON }.foodRule)
        assertEquals(FoodRule.EMPTY, reloadedSchedules.first { it.slot == DoseSlot.NIGHT }.foodRule)
    }

    @Test
    fun testGetSlotColorReturnsDistinctSoftPastelColors() {
        assertEquals(Color(0xFFFEF3C7).copy(alpha = 0.5f), getSlotColor(DoseSlot.MORNING))
        assertEquals(Color(0xFFE0F2FE).copy(alpha = 0.5f), getSlotColor(DoseSlot.NOON))
        assertEquals(Color(0xFFFFEDD5).copy(alpha = 0.5f), getSlotColor(DoseSlot.AFTERNOON))
        assertEquals(Color(0xFFEDE9FE).copy(alpha = 0.5f), getSlotColor(DoseSlot.EVENING))
        assertEquals(Color(0xFFE2E8F0).copy(alpha = 0.5f), getSlotColor(DoseSlot.NIGHT))
        assertEquals(Color(0xFFD1FAE5).copy(alpha = 0.5f), getSlotColor(DoseSlot.CUSTOM))

        val allSlotColors = DoseSlot.entries.map { getSlotColor(it) }
        assertEquals(DoseSlot.entries.size, allSlotColors.toSet().size)
        allSlotColors.forEach { color ->
            assertEquals(0.5f, color.alpha, 0.01f)
        }
    }

    @Test
    fun testCalculateTotalMedicineAllUnitTypesAndIntervals() {
        // 1. Tablet ("pcs" / "টি"): 1 Morning + 1 Night for 7 days, every 1 day -> 14 টি
        val tabletSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "1", doseUnit = "pcs"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "1", doseUnit = "টি")
        )
        assertEquals(
            "মোট ওষুধ লাগবে: 14 টি",
            viewModel.calculateTotalMedicine(duration = 7, interval = 1, schedules = tabletSchedules)
        )

        // With interval = 2 on 10 days (5 active days), 1+2 per day -> 15 টি
        val intervalSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "1", doseUnit = "টি"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "2", doseUnit = "টি")
        )
        assertEquals(
            "মোট ওষুধ লাগবে: 15 টি",
            viewModel.calculateTotalMedicine(duration = 10, interval = 2, schedules = intervalSchedules)
        )

        // 2. Syrup ("tsp" converts * 5 to mL, "mL" stays 1x): 1 tsp Morning + 2.5 mL Night for 4 days -> 30 mL
        val syrupSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "1", doseUnit = "tsp"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "2.5", doseUnit = "mL")
        )
        assertEquals(
            "আনুমানিক: 30 mL",
            viewModel.calculateTotalMedicine(duration = 4, interval = 1, schedules = syrupSchedules)
        )

        // Pure mL
        val mlSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "5", doseUnit = "mL")
        )
        assertEquals(
            "আনুমানিক: 35 mL",
            viewModel.calculateTotalMedicine(duration = 7, interval = 1, schedules = mlSchedules)
        )

        // 3. Inhaler ("puff")
        val puffSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "2", doseUnit = "puff"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "2", doseUnit = "puff")
        )
        assertEquals(
            "মোট: 60 puff",
            viewModel.calculateTotalMedicine(duration = 15, interval = 1, schedules = puffSchedules)
        )

        // 4. Drops ("drop")
        val dropSchedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "3", doseUnit = "drop"),
            DraftSchedule(slot = DoseSlot.NOON, doseAmount = "3", doseUnit = "drop"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "3", doseUnit = "drop")
        )
        assertEquals(
            "প্রায়: 45 ফোঁটা",
            viewModel.calculateTotalMedicine(duration = 5, interval = 1, schedules = dropSchedules)
        )

        // 5. Duration <= 0 or empty schedules returns blank
        assertEquals("", viewModel.calculateTotalMedicine(duration = 0, interval = 1, schedules = tabletSchedules))
        assertEquals("", viewModel.calculateTotalMedicine(duration = 7, interval = 1, schedules = emptyList()))
    }

    @Test
    fun testCard4DisplaysTotalMedicineSummaryOnlyWhenDurationGreaterThanZero() {
        val schedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "1", doseUnit = "টি"),
            DraftSchedule(slot = DoseSlot.NIGHT, doseAmount = "1", doseUnit = "টি")
        )

        // Case A: Duration = 7 (> 0) -> Summary box & text must be displayed
        val activeMed = DraftMedicine(
            name = "Napa",
            type = MedicineType.TABLET,
            schedules = schedules,
            durationDays = "7",
            intervalDays = 1
        )
        val expectedSummary = viewModel.calculateTotalMedicine(7, 1, schedules)

        composeTestRule.setContent {
            MediciTheme {
                Card4DurationNotes(
                    draftMedicine = activeMed,
                    draftPrescription = DraftPrescription(),
                    onDurationChanged = {},
                    onIncrementDuration = {},
                    onDecrementDuration = {},
                    onIntervalChanged = {},
                    onNoteChanged = {},
                    totalMedicineSummary = expectedSummary
                )
            }
        }

        composeTestRule.onNodeWithTag("total_medicine_summary_box")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("total_medicine_summary_text")
            .assertTextEquals("মোট ওষুধ লাগবে: 14 টি")
    }

    @Test
    fun testCard4HidesTotalMedicineSummaryWhenDurationIsZeroOrEmpty() {
        val schedules = listOf(
            DraftSchedule(slot = DoseSlot.MORNING, doseAmount = "1", doseUnit = "টি")
        )
        val zeroDurationMed = DraftMedicine(
            name = "Napa",
            type = MedicineType.TABLET,
            schedules = schedules,
            durationDays = "0",
            intervalDays = 1
        )

        composeTestRule.setContent {
            MediciTheme {
                Card4DurationNotes(
                    draftMedicine = zeroDurationMed,
                    draftPrescription = DraftPrescription(),
                    onDurationChanged = {},
                    onIncrementDuration = {},
                    onDecrementDuration = {},
                    onIntervalChanged = {},
                    onNoteChanged = {},
                    totalMedicineSummary = viewModel.calculateTotalMedicine(0, 1, schedules)
                )
            }
        }

        composeTestRule.onNodeWithTag("total_medicine_summary_box").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testCard3PerSlotFoodRuleUiIndependence() {
        viewModel.resetDraft()
        viewModel.updateMedicineName("Pantoprazole")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.toggleSlot(DoseSlot.NIGHT)

        composeTestRule.setContent {
            MediciTheme {
                val draftMedicine by viewModel.draftMedicine.collectAsState()
                Card3ScheduleDose(
                    draftMedicine = draftMedicine,
                    onToggleSlot = { viewModel.toggleSlot(it) },
                    onToggleExpanded = { viewModel.toggleScheduleExpanded(it) },
                    onTimeChanged = { slot, time -> viewModel.updateScheduleTime(slot, time) },
                    onAmountChanged = { slot, amt -> viewModel.updateScheduleDoseAmount(slot, amt) },
                    onUnitChanged = { slot, unit -> viewModel.updateScheduleDoseUnit(slot, unit) },
                    onSlotFoodRuleChanged = { slotId, rule -> viewModel.updateSlotFoodRule(slotId, rule) }
                )
            }
        }

        // Initially both Morning and Night default to AFTER
        composeTestRule.onNodeWithTag("food_rule_row_morning").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_morning_after").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("food_rule_row_night").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_night_after").performScrollTo().assertIsSelected()

        // Change Morning to BEFORE -> Night must remain AFTER
        composeTestRule.onNodeWithTag("food_rule_row_morning").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_morning_before").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("food_rule_chip_morning_before").assertIsSelected()
        composeTestRule.onNodeWithTag("food_rule_chip_morning_after").assertIsNotSelected()
        composeTestRule.onNodeWithTag("food_rule_row_night").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_night_after").performScrollTo().assertIsSelected()
        composeTestRule.onNodeWithTag("food_rule_chip_night_before").assertIsNotSelected()

        // Change Night to EMPTY -> Morning must remain BEFORE
        composeTestRule.onNodeWithTag("food_rule_row_night").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_night_empty").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("food_rule_chip_night_empty").assertIsSelected()
        composeTestRule.onNodeWithTag("food_rule_row_morning").performScrollTo()
        composeTestRule.onNodeWithTag("food_rule_chip_morning_before").performScrollTo().assertIsSelected()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testCard4StockQuantityInputAndViewModelUpdate() = runTest {
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-21")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "টি")
        viewModel.updateDurationDays("7")

        composeTestRule.setContent {
            MediciTheme {
                val draftMedicine by viewModel.draftMedicine.collectAsState()
                val draftPrescription by viewModel.draftPrescription.collectAsState()
                Card4DurationNotes(
                    draftMedicine = draftMedicine,
                    draftPrescription = draftPrescription,
                    onDurationChanged = { viewModel.updateDurationDays(it) },
                    onIncrementDuration = { viewModel.incrementDuration() },
                    onDecrementDuration = { viewModel.decrementDuration() },
                    onIntervalChanged = { viewModel.updateIntervalDays(it) },
                    onNoteChanged = { viewModel.updateMedicineNote(it) },
                    onStockCountChanged = { viewModel.updateStockCount(it) },
                    onIncrementStock = { viewModel.incrementStockCount() },
                    onDecrementStock = { viewModel.decrementStockCount() }
                )
            }
        }

        composeTestRule.onNodeWithTag("stock_quantity_section").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("stock_count_input").performScrollTo().performTextInput("30")
        composeTestRule.waitForIdle()
        assertEquals(30, viewModel.draftMedicine.value.stockCount)

        composeTestRule.onNodeWithTag("stock_increment_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(31, viewModel.draftMedicine.value.stockCount)

        composeTestRule.onNodeWithTag("stock_decrement_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(30, viewModel.draftMedicine.value.stockCount)

        var saved = false
        val job = viewModel.reviewAndSave(onSuccess = { saved = true })
        job.join()
        assertTrue(saved)

        val latest = prescriptionDao.getAllPrescriptionsWithMedicines().first().first()
        assertEquals(30, latest.medicines.first().medicine.stockCount)
    }

    @Test
    fun testLoadMedicineForEditLoadsPrescriptionAndPreservesVersionNoDuplicate() = runTest {
        val presId = java.util.UUID.randomUUID().toString()
        val parentId = "parent-$presId"
        val prescription = com.mediciplanner.app.data.local.entity.PrescriptionEntity(
            id = presId,
            parentId = parentId,
            versionNumber = 1,
            isLatest = true,
            date = "2026-11-21",
            doctorName = "ডা. করিম",
            createdAt = "2026-11-21T09:00:00"
        )
        val medA = com.mediciplanner.app.data.local.entity.MedicineEntity(
            id = java.util.UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Napa",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 5,
            foodRule = FoodRule.AFTER,
            createdAt = "2026-11-21T09:00:00",
            stockCount = 10,
            initialStock = 10
        )
        val medB = com.mediciplanner.app.data.local.entity.MedicineEntity(
            id = java.util.UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Seclo",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.BEFORE,
            createdAt = "2026-11-21T09:00:00",
            stockCount = 14,
            initialStock = 14
        )
        val schedA = com.mediciplanner.app.data.local.entity.DoseScheduleEntity(
            id = java.util.UUID.randomUUID().toString(),
            medicineId = medA.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val schedB = com.mediciplanner.app.data.local.entity.DoseScheduleEntity(
            id = java.util.UUID.randomUUID().toString(),
            medicineId = medB.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(medA, medB), listOf(schedA, schedB))

        viewModel.loadMedicineForEdit(medB.id).join()

        assertEquals(com.mediciplanner.app.ui.screens.prescription.PrescriptionSaveType.EDIT_MEDICINE, viewModel.saveMode.value)
        assertEquals(presId, viewModel.editingPrescriptionId.value)
        assertEquals(parentId, viewModel.editingParentId.value)
        assertEquals(medB.id, viewModel.editingMedicineId.value)
        assertEquals(1, viewModel.initialPage.value)
        assertEquals(1, viewModel.selectedMedicineIndex.value)
        assertEquals("Seclo", viewModel.draftMedicine.value.name)
        assertEquals(14, viewModel.draftMedicine.value.stockCount)
        assertEquals(2, viewModel.draftPrescription.value.medicines.size)

        // Update medicine name and save
        viewModel.updateMedicineName("Seclo MUPS")
        var saved = false
        viewModel.reviewAndSave(onSuccess = { saved = true }).join()
        assertTrue(saved)

        // Ensure NO duplicate prescription is created (still only 1 latest prescription with same parentId)
        val latestList = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(1, latestList.size)
        val updatedPres = latestList.first()
        assertEquals(parentId, updatedPres.prescription.parentId)
        assertEquals(2, updatedPres.prescription.versionNumber)
        assertEquals(2, updatedPres.medicines.size)
        assertTrue(updatedPres.medicines.any { it.medicine.name == "Napa" })
        assertTrue(updatedPres.medicines.any { it.medicine.name == "Seclo MUPS" })
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testPrescriptionCarouselScreenWithMedicineIdJumpsDirectlyToCard2() = runTest {
        val presId = java.util.UUID.randomUUID().toString()
        val prescription = com.mediciplanner.app.data.local.entity.PrescriptionEntity(
            id = presId,
            parentId = "parent-$presId",
            versionNumber = 1,
            isLatest = true,
            date = "2026-11-21",
            doctorName = "ডা. হাসান",
            createdAt = "2026-11-21T09:00:00"
        )
        val med = com.mediciplanner.app.data.local.entity.MedicineEntity(
            id = java.util.UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Maxpro",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 14,
            foodRule = FoodRule.BEFORE,
            createdAt = "2026-11-21T09:00:00"
        )
        val sched = com.mediciplanner.app.data.local.entity.DoseScheduleEntity(
            id = java.util.UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        prescriptionRepository.saveFullPrescription(prescription, listOf(med), listOf(sched))

        composeTestRule.setContent {
            MediciTheme {
                com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselScreen(
                    viewModel = viewModel,
                    initialPage = 1,
                    medicineId = med.id,
                    prescriptionId = presId,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Directly on Card 2 (page 2/4), Card 1 skipped, Edit Mode badge shown, medicine name loaded
        composeTestRule.onNodeWithTag("carousel_card_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_card_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("carousel_step_badge").assertTextEquals("ধাপ ২/৪")
        composeTestRule.onNodeWithTag("carousel_edit_mode_badge").assertIsDisplayed()
        assertEquals("Maxpro", viewModel.draftMedicine.value.name)
        assertEquals(presId, viewModel.editingPrescriptionId.value)
        assertEquals("parent-$presId", viewModel.editingParentId.value)
    }

    @Test
    fun testEditModeSavesNewVersionNewModeCreatesNewPrescriptionAndNoChangeSkipsVersion() = runTest {
        // 1. New Mode creates a brand new prescription with versionNumber = 1 and its own UUID parentId
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-21")
        viewModel.updateDoctorName("ডা. নাসির")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseAmount(DoseSlot.MORNING, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "টি")
        viewModel.updateDurationDays("5")

        var newSaved = false
        viewModel.reviewAndSave(onSuccess = { newSaved = true }).join()
        assertTrue(newSaved)

        val afterNew = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(1, afterNew.size)
        val firstPres = afterNew.first()
        val originalPresId = firstPres.prescription.id
        val originalParentId = firstPres.prescription.parentId
        val medId = firstPres.medicines.first().medicine.id
        assertEquals(1, firstPres.prescription.versionNumber)
        assertEquals(originalPresId, originalParentId)
        assertTrue(firstPres.prescription.isLatest)

        // 2. Edit Mode with NO changes -> No-Change diff logic skips creating a new version
        viewModel.loadMedicineForEdit(medId).join()
        var noChangesTriggered = false
        viewModel.reviewAndSave(
            onSuccess = {},
            onNoChanges = { noChangesTriggered = true }
        ).join()
        assertTrue("No-Change diff logic must trigger when nothing changed", noChangesTriggered)
        assertEquals(1, prescriptionDao.getPrescriptionVersions(originalParentId).first().size)
        assertEquals(1, prescriptionDao.getAllPrescriptions().first().size)

        // 3. Edit Mode WITH changes -> creates version 2 with SAME parentId, NO duplicate prescription
        viewModel.loadMedicineForEdit(medId).join()
        viewModel.updateDurationDays("10")
        var editSaved = false
        viewModel.reviewAndSave(onSuccess = { editSaved = true }).join()
        assertTrue(editSaved)

        val latestAfterEdit = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(1, latestAfterEdit.size)
        val versionedPres = latestAfterEdit.first()
        assertEquals(originalParentId, versionedPres.prescription.parentId)
        assertEquals(2, versionedPres.prescription.versionNumber)
        assertTrue(versionedPres.prescription.isLatest)
        assertEquals(10, versionedPres.medicines.first().medicine.durationDays)

        val allVersions = prescriptionDao.getPrescriptionVersions(originalParentId).first()
        assertEquals(2, allVersions.size)
        assertFalse(allVersions.first { it.id == originalPresId }.isLatest)

        // 4. Subsequent New Mode creates a separate brand new prescription with a new UUID parentId
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-22")
        viewModel.updateDoctorName("ডা. করিম")
        viewModel.updateMedicineName("Ace")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.toggleSlot(DoseSlot.NIGHT)
        viewModel.updateScheduleDoseAmount(DoseSlot.NIGHT, "1")
        viewModel.updateScheduleDoseUnit(DoseSlot.NIGHT, "টি")
        viewModel.updateDurationDays("3")

        var secondNewSaved = false
        viewModel.reviewAndSave(onSuccess = { secondNewSaved = true }).join()
        assertTrue(secondNewSaved)

        val allLatest = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(2, allLatest.size)
        val secondPres = allLatest.first { it.medicines.any { m -> m.medicine.name == "Ace" } }
        assertEquals(1, secondPres.prescription.versionNumber)
        assertTrue(secondPres.prescription.parentId != originalParentId)
    }

    @Test
    fun testDoseAmountHelperFractionAndDecimalParsingAndFormatting() {
        assertEquals(0.5, DoseAmountHelper.parseDoseAmount("1/2")!!, 1e-6)
        assertEquals(0.5, DoseAmountHelper.parseDoseAmount("0.5")!!, 1e-6)
        assertEquals(1.5, DoseAmountHelper.parseDoseAmount("1.5")!!, 1e-6)
        assertEquals(1.5, DoseAmountHelper.parseDoseAmount("1 1/2")!!, 1e-6)
        assertEquals(0.5, DoseAmountHelper.parseDoseAmount("১/২")!!, 1e-6)
        assertEquals(0.25, DoseAmountHelper.parseDoseAmount("1/4")!!, 1e-6)
        assertEquals(0.75, DoseAmountHelper.parseDoseAmount("3/4")!!, 1e-6)

        assertNull(DoseAmountHelper.parseDoseAmount(""))
        assertNull(DoseAmountHelper.parseDoseAmount("1/"))
        assertNull(DoseAmountHelper.parseDoseAmount("1/0"))
        assertNull(DoseAmountHelper.parseDoseAmount("0"))

        assertEquals("1/2", DoseAmountHelper.formatDoseAmount(0.5, useBengaliDigits = false))
        assertEquals("১/২", DoseAmountHelper.formatDoseAmount(0.5, useBengaliDigits = true))
        assertEquals("1.5", DoseAmountHelper.formatDoseAmount(1.5, useBengaliDigits = false))
        assertEquals("১.৫", DoseAmountHelper.formatDoseAmount(1.5, useBengaliDigits = true))
        assertEquals("1", DoseAmountHelper.formatDoseAmount(1.0, useBengaliDigits = false))
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testFractionalDoseInputPersistsDoubleAndDisplaysFractionInCard() = runTest {
        viewModel.resetDraft()
        viewModel.updateDate("2026-11-25")
        viewModel.updateMedicineName("Napa")
        viewModel.updateMedicineType(MedicineType.TABLET)
        viewModel.toggleSlot(DoseSlot.MORNING)
        viewModel.updateScheduleDoseUnit(DoseSlot.MORNING, "টি")
        viewModel.updateDurationDays("6")

        composeTestRule.setContent {
            MediciTheme {
                val draftMedicine by viewModel.draftMedicine.collectAsState()
                Card3ScheduleDose(
                    draftMedicine = draftMedicine,
                    onToggleSlot = { viewModel.toggleSlot(it) },
                    onToggleExpanded = { viewModel.toggleScheduleExpanded(it) },
                    onTimeChanged = { slot, time -> viewModel.updateScheduleTime(slot, time) },
                    onAmountChanged = { slot, amt -> viewModel.updateScheduleDoseAmount(slot, amt) },
                    onUnitChanged = { slot, unit -> viewModel.updateScheduleDoseUnit(slot, unit) },
                    onSlotFoodRuleChanged = { slotId, rule -> viewModel.updateSlotFoodRule(slotId, rule) }
                )
            }
        }

        // Replace default "1" with "1/2"
        composeTestRule.onNodeWithTag("dose_amount_input_morning").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("dose_amount_input_morning").performTextInput("1/2")
        composeTestRule.waitForIdle()

        val morningDraft = viewModel.draftMedicine.value.schedules.first { it.slot == DoseSlot.MORNING }
        assertEquals("1/2", morningDraft.doseAmount)
        assertEquals(0.5, morningDraft.parsedDoseAmount!!, 1e-6)
        assertTrue(viewModel.isCard3Valid())
        assertTrue(viewModel.validateCard(2))
        assertEquals(
            "মোট ওষুধ লাগবে: 3 টি",
            viewModel.calculateTotalMedicine(6, 1, listOf(morningDraft))
        )

        var saved = false
        viewModel.reviewAndSave(onSuccess = { saved = true }).join()
        assertTrue("Expected fractional dose prescription to save", saved)

        val persisted = prescriptionDao.getAllPrescriptionsWithMedicines().first().first()
        val persistedSched = persisted.medicines.first().doseSchedules.first().doseSchedule
        assertEquals(0.5, persistedSched.doseAmount, 1e-6)

        // Reloading for edit formats 0.5 back to "1/2"
        viewModel.loadPrescriptionForEditing(persisted)
        assertEquals("1/2", viewModel.draftMedicine.value.schedules.first().doseAmount)
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun testMedicineCardDisplaysFractionForHalfDose() {
        val med = com.mediciplanner.app.data.local.entity.MedicineEntity(
            id = "med-half",
            prescriptionId = "pres-half",
            name = "Napa",
            type = MedicineType.TABLET,
            foodRule = FoodRule.AFTER,
            createdAt = "2026-11-25T08:00:00"
        )
        val sched = com.mediciplanner.app.data.local.entity.DoseScheduleEntity(
            id = "sched-half",
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 0.5,
            doseUnit = "টি"
        )

        composeTestRule.setContent {
            MediciTheme {
                com.mediciplanner.app.ui.screens.home.components.MedicineCard(
                    medicine = med,
                    schedule = sched,
                    isTaken = false,
                    isReadOnly = true
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.mainClock.advanceTimeBy(500)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_card_dose_chip_med-half", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextContains("১/২ টি")
    }
}
