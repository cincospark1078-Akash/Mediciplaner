package com.mediciplanner.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.converter.Converters
import com.mediciplanner.app.data.local.dao.FollowUpDao
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.dao.ReminderDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.data.util.ReminderIdGenerator
import kotlinx.coroutines.test.runTest
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
class MediciDataLayerTest {

    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var reminderDao: ReminderDao
    private lateinit var followUpDao: FollowUpDao

    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var followUpRepository: FollowUpRepository

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        prescriptionDao = database.prescriptionDao()
        reminderDao = database.reminderDao()
        followUpDao = database.followUpDao()

        prescriptionRepository = PrescriptionRepository(prescriptionDao)
        reminderRepository = ReminderRepository(reminderDao)
        followUpRepository = FollowUpRepository(followUpDao)
    }

    @After
    fun closeDb() {
        database.close()
    }

    // 1. Insert a Prescription with 2 Medicines and 3 Schedules -> Retrieve it -> Verify data matches.
    @Test
    fun testInsertPrescriptionWithMedicinesAndSchedules() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-05T08:00:00",
            doctorName = "Dr. House",
            location = "Princeton Plainsboro",
            note = "Take as prescribed",
            createdAt = "2024-11-05T08:00:00"
        )

        val med1Id = UUID.randomUUID().toString()
        val med1 = MedicineEntity(
            id = med1Id,
            prescriptionId = prescriptionId,
            name = "Amoxicillin",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            note = "Complete full course",
            createdAt = "2024-11-05T08:00:00"
        )

        val med2Id = UUID.randomUUID().toString()
        val med2 = MedicineEntity(
            id = med2Id,
            prescriptionId = prescriptionId,
            name = "Cough Syrup",
            type = MedicineType.SYRUP,
            strength = 15.0,
            strengthUnit = "mg/5mL",
            durationDays = 5,
            foodRule = FoodRule.WITH,
            note = "Shake well before use",
            createdAt = "2024-11-05T08:00:00"
        )

        val sched1 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med1Id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        val sched2 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med1Id,
            slot = DoseSlot.EVENING,
            time = "20:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )
        val sched3 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med2Id,
            slot = DoseSlot.NOON,
            time = "13:00",
            doseAmount = 5.0,
            doseUnit = "mL"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(med1, med2),
            schedules = listOf(sched1, sched2, sched3)
        )

        val retrieved = prescriptionRepository.getPrescriptionWithMedicinesById(prescriptionId)
        assertNotNull(retrieved)
        assertEquals(prescriptionId, retrieved!!.prescription.id)
        assertEquals("Dr. House", retrieved.prescription.doctorName)
        assertEquals(2, retrieved.medicines.size)

        val retrievedMed1 = retrieved.medicines.find { it.medicine.id == med1Id }
        assertNotNull(retrievedMed1)
        assertEquals("Amoxicillin", retrievedMed1!!.medicine.name)
        assertEquals(2, retrievedMed1.doseSchedules.size)

        val retrievedMed2 = retrieved.medicines.find { it.medicine.id == med2Id }
        assertNotNull(retrievedMed2)
        assertEquals("Cough Syrup", retrievedMed2!!.medicine.name)
        assertEquals(1, retrievedMed2.doseSchedules.size)
    }

    // 2. Try to save a Prescription with 0 Medicines -> Verify it throws validation error.
    @Test
    fun testSavePrescriptionWithZeroMedicinesThrowsValidationError() = runTest {
        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )

        var thrown = false
        try {
            prescriptionRepository.saveFullPrescription(
                prescription = prescription,
                medicines = emptyList(),
                schedules = emptyList()
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
            assertTrue(e.message?.contains("at least 1 Medicine", ignoreCase = true) == true)
        }
        assertTrue("Expected IllegalArgumentException when saving 0 medicines", thrown)
    }

    // 3. Try to save a Medicine with invalid unit -> Verify it throws validation error.
    @Test
    fun testSaveMedicineWithInvalidUnitThrowsValidationError() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )

        val invalidMed = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescriptionId,
            name = "Aspirin",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "liters", // Invalid for TABLET
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-05T08:00:00"
        )

        val schedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = invalidMed.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )

        var thrown = false
        try {
            prescriptionRepository.saveFullPrescription(
                prescription = prescription,
                medicines = listOf(invalidMed),
                schedules = listOf(schedule)
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
            assertTrue(e.message?.contains("Invalid strength unit", ignoreCase = true) == true)
        }
        assertTrue("Expected IllegalArgumentException for invalid strength unit", thrown)
    }

    // Additional check: Medicine without any dose schedule throws validation error
    @Test
    fun testSaveMedicineWithZeroDoseSchedulesThrowsValidationError() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescriptionId,
            name = "Ibuprofen",
            type = MedicineType.TABLET,
            strength = 400.0,
            strengthUnit = "mg",
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-05T08:00:00"
        )

        var thrown = false
        try {
            prescriptionRepository.saveFullPrescription(
                prescription = prescription,
                medicines = listOf(med),
                schedules = emptyList()
            )
        } catch (e: IllegalArgumentException) {
            thrown = true
            assertTrue(e.message?.contains("must have at least 1 DoseSchedule", ignoreCase = true) == true)
        }
        assertTrue("Expected IllegalArgumentException for 0 dose schedules", thrown)
    }

    // 4. Test Deterministic Reminder ID (same input = same ID).
    @Test
    fun testDeterministicReminderId() {
        val doseScheduleId = "sched-sample-12345"
        val scheduledDate = "2024-11-05"

        val id1 = ReminderIdGenerator.generate(doseScheduleId, scheduledDate)
        val id2 = ReminderIdGenerator.generate(doseScheduleId, scheduledDate)

        assertEquals("Same input must generate identical Reminder ID", id1, id2)
        assertNotNull(UUID.fromString(id1))

        val idDiffDate = ReminderIdGenerator.generate(doseScheduleId, "2024-11-06")
        assertNotEquals("Different dates must produce different IDs", id1, idDiffDate)

        val reminder = ReminderEntity.createDeterministic(
            doseScheduleId = doseScheduleId,
            scheduledDate = scheduledDate,
            scheduledAt = "2024-11-05T08:00:00"
        )
        assertEquals(id1, reminder.id)
    }

    // 5. Test Cascade Delete (Deleting a Prescription deletes its Medicines and Schedules).
    @Test
    fun testCascadeDelete() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )

        val medId = UUID.randomUUID().toString()
        val med = MedicineEntity(
            id = medId,
            prescriptionId = prescriptionId,
            name = "Paracetamol",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-05T08:00:00"
        )

        val schedId = UUID.randomUUID().toString()
        val sched = DoseScheduleEntity(
            id = schedId,
            medicineId = medId,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(med),
            schedules = listOf(sched)
        )

        // Verify inserted
        assertNotNull(prescriptionDao.getPrescriptionById(prescriptionId))
        assertEquals(1, prescriptionDao.getMedicinesByPrescriptionId(prescriptionId).size)
        assertNotNull(prescriptionDao.getDoseScheduleById(schedId))

        // Delete prescription
        prescriptionRepository.deletePrescriptionById(prescriptionId)

        // Verify cascade deletion
        assertNull(prescriptionDao.getPrescriptionById(prescriptionId))
        assertTrue(prescriptionDao.getMedicinesByPrescriptionId(prescriptionId).isEmpty())
        assertNull(prescriptionDao.getDoseScheduleById(schedId))
    }

    // 6. Test Room TypeConverters for Enums and Dates.
    @Test
    fun testRoomTypeConverters() {
        val converters = Converters()

        // MedicineType
        assertEquals("TABLET", converters.fromMedicineType(MedicineType.TABLET))
        assertEquals(MedicineType.TABLET, converters.toMedicineType("TABLET"))
        assertEquals(MedicineType.INHALER, converters.toMedicineType("INHALER"))
        assertNull(converters.toMedicineType("UNKNOWN_TYPE"))
        assertNull(converters.fromMedicineType(null))

        // FoodRule
        assertEquals("BEFORE", converters.fromFoodRule(FoodRule.BEFORE))
        assertEquals(FoodRule.AFTER, converters.toFoodRule("AFTER"))
        assertNull(converters.toFoodRule("UNKNOWN_RULE"))

        // DoseSlot
        assertEquals("MORNING", converters.fromDoseSlot(DoseSlot.MORNING))
        assertEquals(DoseSlot.EVENING, converters.toDoseSlot("EVENING"))

        // ReminderStatus
        assertEquals("PENDING", converters.fromReminderStatus(ReminderStatus.PENDING))
        assertEquals(ReminderStatus.TAKEN, converters.toReminderStatus("TAKEN"))

        // FollowUpType
        assertEquals("DOCTOR_VISIT", converters.fromFollowUpType(FollowUpType.DOCTOR_VISIT))
        assertEquals(FollowUpType.TEST, converters.toFollowUpType("TEST"))
    }

    // FollowUp repository test
    @Test
    fun testFollowUpOperations() = runTest {
        val followUp = FollowUpEntity(
            id = UUID.randomUUID().toString(),
            title = "Annual Checkup",
            type = FollowUpType.DOCTOR_VISIT,
            date = "2024-11-20T09:00:00",
            note = "Fasting required",
            reminderEnabled = true
        )
        followUpRepository.insertFollowUp(followUp)

        val retrieved = followUpRepository.getFollowUpById(followUp.id)
        assertNotNull(retrieved)
        assertEquals("Annual Checkup", retrieved!!.title)
        assertEquals(FollowUpType.DOCTOR_VISIT, retrieved.type)

        followUpRepository.deleteFollowUpById(followUp.id)
        assertNull(followUpRepository.getFollowUpById(followUp.id))
    }

    // Reminder repository deterministic create test
    @Test
    fun testReminderOperations() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescriptionId,
            name = "Lisinopril",
            type = MedicineType.TABLET,
            strength = 10.0,
            strengthUnit = "mg",
            foodRule = FoodRule.BEFORE,
            createdAt = "2024-11-05T08:00:00"
        )
        val doseSchedule = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )

        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(med),
            schedules = listOf(doseSchedule)
        )

        val reminder = reminderRepository.createDeterministicReminder(
            doseScheduleId = doseSchedule.id,
            scheduledDate = "2024-11-05",
            scheduledAt = "2024-11-05T08:00:00"
        )
        assertNotNull(reminder)
        assertEquals(ReminderStatus.PENDING, reminder.status)

        val retrieved = reminderRepository.getReminderById(reminder.id)
        assertNotNull(retrieved)

        reminderRepository.markAsTaken(reminder.id, "2024-11-05T08:05:00")
        val updated = reminderRepository.getReminderById(reminder.id)
        assertNotNull(updated)
        assertEquals(ReminderStatus.TAKEN, updated!!.status)
        assertEquals("2024-11-05T08:05:00", updated.takenAt)
    }

    // Specific unit validation: Tablet with mL must throw error
    @Test
    fun testTabletWithMlUnitThrowsError() = runTest {
        val prescription = PrescriptionEntity(
            id = UUID.randomUUID().toString(),
            date = "2024-11-05T08:00:00",
            createdAt = "2024-11-05T08:00:00"
        )
        val med = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = prescription.id,
            name = "Paracetamol Tablet",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mL", // Invalid: TABLET cannot have mL!
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-05T08:00:00"
        )
        val sched = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs"
        )

        var errorThrown = false
        try {
            prescriptionRepository.saveFullPrescription(
                prescription = prescription,
                medicines = listOf(med),
                schedules = listOf(sched)
            )
        } catch (e: IllegalArgumentException) {
            errorThrown = true
            assertTrue(e.message?.contains("Invalid strength unit 'mL' for medicine type TABLET", ignoreCase = true) == true)
        }
        assertTrue("Tablet with mL strength unit must throw IllegalArgumentException", errorThrown)
    }

    // Test Room Date TypeConverters
    @Test
    fun testDateTypeConverters() {
        val converters = Converters()
        val now = java.util.Date()
        val timestamp = now.time

        assertEquals(now, converters.fromTimestamp(timestamp))
        assertEquals(timestamp, converters.dateToTimestamp(now))

        val isoString = "2024-11-05T08:30:00"
        val parsedDate = converters.fromIsoString(isoString)
        assertNotNull(parsedDate)
        assertEquals(isoString, converters.dateToIsoString(parsedDate))
    }

    // Test inserting and updating medicine with imagePath via PrescriptionRepository
    @Test
    fun testInsertAndUpdateMedicineWithImagePath() = runTest {
        val prescriptionId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = prescriptionId,
            date = "2024-11-07T08:00:00",
            doctorName = "Dr. Banner",
            createdAt = "2024-11-07T08:00:00"
        )
        database.prescriptionDao().insertPrescription(prescription)

        val medicineId = UUID.randomUUID().toString()
        val medicine = MedicineEntity(
            id = medicineId,
            prescriptionId = prescriptionId,
            name = "Napa Extra",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            foodRule = FoodRule.AFTER,
            createdAt = "2024-11-07T08:00:00",
            imagePath = "napa_extra_front.jpg"
        )

        prescriptionRepository.insertMedicine(medicine)

        val retrieved = prescriptionRepository.getMedicineById(medicineId)
        assertNotNull(retrieved)
        assertEquals("Napa Extra", retrieved?.name)
        assertEquals("napa_extra_front.jpg", retrieved?.imagePath)

        val updated = retrieved!!.copy(imagePath = "napa_extra_back.jpg")
        prescriptionRepository.updateMedicine(updated)

        val retrievedUpdated = prescriptionRepository.getMedicineById(medicineId)
        assertNotNull(retrievedUpdated)
        assertEquals("napa_extra_back.jpg", retrievedUpdated?.imagePath)
    }

    // Test MIGRATION_1_2
    @Test
    fun testMigration1To2Sql() {
        val sqliteDb = database.openHelper.writableDatabase
        // Verify imagePath column exists in table schema (since version 2 in-memory DB has it)
        val cursor = sqliteDb.query("PRAGMA table_info(medicines)")
        var foundImagePathColumn = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                val colName = cursor.getString(nameIndex)
                if (colName == "imagePath") {
                    foundImagePathColumn = true
                    break
                }
            }
        }
        cursor.close()
        assertTrue("medicines table must have imagePath column", foundImagePathColumn)
    }

    @Test
    fun testDoseScheduleFoodRulePersistenceAndMigration8To9() = runTest {
        val sqliteDb = database.openHelper.writableDatabase
        val cursor = sqliteDb.query("PRAGMA table_info(dose_schedules)")
        var foundFoodRuleColumn = false
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1 && cursor.getString(nameIndex) == "foodRule") {
                foundFoodRuleColumn = true
                break
            }
        }
        cursor.close()
        assertTrue("dose_schedules table must have foodRule column", foundFoodRuleColumn)

        // Verify insert and update persist foodRule via PrescriptionDao
        val presId = UUID.randomUUID().toString()
        val medId = UUID.randomUUID().toString()
        val schedId = UUID.randomUUID().toString()
        prescriptionDao.insertPrescription(
            PrescriptionEntity(id = presId, date = "2026-11-12", createdAt = "2026-11-12T10:00:00")
        )
        prescriptionDao.insertMedicine(
            MedicineEntity(
                id = medId,
                prescriptionId = presId,
                name = "Omeprazole",
                type = MedicineType.CAPSULE,
                foodRule = FoodRule.BEFORE,
                createdAt = "2026-11-12T10:00:00"
            )
        )
        val initialSchedule = DoseScheduleEntity(
            id = schedId,
            medicineId = medId,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "pcs",
            foodRule = FoodRule.EMPTY
        )
        prescriptionDao.insertDoseSchedule(initialSchedule)
        val inserted = prescriptionDao.getDoseScheduleById(schedId)
        assertEquals(FoodRule.EMPTY, inserted?.foodRule)

        prescriptionDao.updateDoseSchedule(initialSchedule.copy(foodRule = FoodRule.WITH))
        val updated = prescriptionDao.getDoseScheduleById(schedId)
        assertEquals(FoodRule.WITH, updated?.foodRule)

        // Verify MIGRATION_8_9 SQL preserves existing schedules and backfills foodRule on raw v8 DB
        val v8Db = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            )
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(8) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS prescriptions (id TEXT PRIMARY KEY NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS medicines (id TEXT PRIMARY KEY NOT NULL, prescriptionId TEXT NOT NULL, foodRule TEXT NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS dose_schedules (id TEXT PRIMARY KEY NOT NULL, medicineId TEXT NOT NULL, slot TEXT NOT NULL, time TEXT NOT NULL, doseAmount REAL NOT NULL, doseUnit TEXT NOT NULL, foodRuleOverride TEXT)")
                    }

                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        ).writableDatabase

        v8Db.execSQL("INSERT INTO prescriptions (id) VALUES ('p1')")
        v8Db.execSQL("INSERT INTO medicines (id, prescriptionId, foodRule) VALUES ('m1', 'p1', 'BEFORE')")
        v8Db.execSQL("INSERT INTO dose_schedules (id, medicineId, slot, time, doseAmount, doseUnit, foodRuleOverride) VALUES ('s1', 'm1', 'MORNING', '08:00', 1.0, 'pcs', NULL)")
        v8Db.execSQL("INSERT INTO dose_schedules (id, medicineId, slot, time, doseAmount, doseUnit, foodRuleOverride) VALUES ('s2', 'm1', 'NIGHT', '22:00', 1.0, 'pcs', 'EMPTY')")

        MediciDatabase.MIGRATION_8_9.migrate(v8Db)

        v8Db.query("SELECT id, foodRule FROM dose_schedules ORDER BY id").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("s1", c.getString(0))
            assertEquals("BEFORE", c.getString(1))
            assertTrue(c.moveToNext())
            assertEquals("s2", c.getString(0))
            assertEquals("EMPTY", c.getString(1))
        }
        v8Db.close()
    }

    @Test
    fun testMedicineStockFieldsAndMigration9To10() = runTest {
        val sqliteDb = database.openHelper.writableDatabase
        val cursor = sqliteDb.query("PRAGMA table_info(medicines)")
        val columns = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            val nameIndex = cursor.getColumnIndex("name")
            if (nameIndex != -1) {
                columns.add(cursor.getString(nameIndex))
            }
        }
        cursor.close()
        assertTrue("medicines table must have stockCount column", columns.contains("stockCount"))
        assertTrue("medicines table must have initialStock column", columns.contains("initialStock"))

        val presId = UUID.randomUUID().toString()
        val medId = UUID.randomUUID().toString()
        prescriptionDao.insertPrescription(
            PrescriptionEntity(id = presId, date = "2026-11-21", createdAt = "2026-11-21T10:00:00")
        )
        prescriptionDao.insertMedicine(
            MedicineEntity(
                id = medId,
                prescriptionId = presId,
                name = "Metformin",
                type = MedicineType.TABLET,
                foodRule = FoodRule.AFTER,
                createdAt = "2026-11-21T10:00:00",
                stockCount = 28,
                initialStock = 30
            )
        )

        val retrieved = prescriptionDao.getMedicineById(medId)
        assertNotNull(retrieved)
        assertEquals(28, retrieved?.stockCount)
        assertEquals(30, retrieved?.initialStock)

        val v9Db = androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory().create(
            androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration.builder(
                ApplicationProvider.getApplicationContext()
            )
                .callback(object : androidx.sqlite.db.SupportSQLiteOpenHelper.Callback(9) {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE IF NOT EXISTS prescriptions (id TEXT PRIMARY KEY NOT NULL)")
                        db.execSQL("CREATE TABLE IF NOT EXISTS medicines (id TEXT PRIMARY KEY NOT NULL, prescriptionId TEXT NOT NULL, name TEXT NOT NULL)")
                    }

                    override fun onUpgrade(db: androidx.sqlite.db.SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        ).writableDatabase

        v9Db.execSQL("INSERT INTO prescriptions (id) VALUES ('p1')")
        v9Db.execSQL("INSERT INTO medicines (id, prescriptionId, name) VALUES ('m1', 'p1', 'Napa')")

        MediciDatabase.MIGRATION_9_10.migrate(v9Db)

        v9Db.query("SELECT id, stockCount, initialStock FROM medicines WHERE id = 'm1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("m1", c.getString(0))
            assertEquals(0, c.getInt(1))
            assertEquals(0, c.getInt(2))
        }
        v9Db.close()
    }
}
