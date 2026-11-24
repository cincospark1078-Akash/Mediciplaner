package com.mediciplanner.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {

    // --- Prescription CRUD ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity)

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Delete
    suspend fun deletePrescription(prescription: PrescriptionEntity)

    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: String)

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: String): PrescriptionEntity?

    @Query("SELECT * FROM prescriptions WHERE isLatest = 1 ORDER BY date DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    // --- Medicines & Schedules Inserts / Queries ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicines(medicines: List<MedicineEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedicine(medicine: MedicineEntity)

    @Update
    suspend fun updateMedicine(medicine: MedicineEntity)

    @Delete
    suspend fun deleteMedicine(medicine: MedicineEntity)

    @Query("SELECT * FROM medicines WHERE id = :id")
    suspend fun getMedicineById(id: String): MedicineEntity?

    @Query("SELECT * FROM medicines WHERE prescriptionId = :prescriptionId")
    suspend fun getMedicinesByPrescriptionId(prescriptionId: String): List<MedicineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseSchedules(schedules: List<DoseScheduleEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoseSchedule(schedule: DoseScheduleEntity)

    @Update
    suspend fun updateDoseSchedule(schedule: DoseScheduleEntity)

    @Delete
    suspend fun deleteDoseSchedule(schedule: DoseScheduleEntity)

    @Query("SELECT * FROM dose_schedules WHERE id = :id")
    suspend fun getDoseScheduleById(id: String): DoseScheduleEntity?

    @Query("SELECT * FROM dose_schedules WHERE medicineId = :medicineId")
    suspend fun getDoseSchedulesByMedicineId(medicineId: String): List<DoseScheduleEntity>

    // --- Full Relations ---

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionWithMedicinesById(id: String): PrescriptionWithMedicines?

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE parentId = :parentId AND isLatest = 1 LIMIT 1")
    suspend fun getLatestPrescriptionWithMedicinesByParentId(parentId: String): PrescriptionWithMedicines?

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE parentId = :parentId AND versionNumber = :versionNumber LIMIT 1")
    suspend fun getPrescriptionWithMedicinesByVersion(parentId: String, versionNumber: Int): PrescriptionWithMedicines?

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE isLatest = 1 ORDER BY date DESC")
    fun getAllPrescriptionsWithMedicines(): Flow<List<PrescriptionWithMedicines>>

    @Transaction
    @Query("SELECT * FROM prescriptions WHERE parentId = :parentId ORDER BY versionNumber DESC")
    fun getPrescriptionVersionsWithMedicines(parentId: String): Flow<List<PrescriptionWithMedicines>>

    @Query("SELECT * FROM prescriptions WHERE parentId = :parentId ORDER BY versionNumber DESC")
    fun getPrescriptionVersions(parentId: String): Flow<List<PrescriptionEntity>>

    @Query("UPDATE prescriptions SET isLatest = 0 WHERE id = :id")
    suspend fun markPrescriptionAsOldVersion(id: String)

    @Query("UPDATE prescriptions SET isLatest = 0 WHERE parentId = :parentId")
    suspend fun markAllVersionsAsOld(parentId: String)

    @Query("DELETE FROM prescriptions WHERE parentId = :parentId")
    suspend fun deletePrescriptionsByParentId(parentId: String)

    @Query("""
        SELECT p.id, p.parentId, p.versionNumber, p.isLatest, p.date, p.doctorName, p.location, p.note, p.createdAt, p.prescriptionImagePaths, COUNT(m.id) AS medicineCount
        FROM prescriptions p
        LEFT JOIN medicines m ON p.id = m.prescriptionId
        WHERE p.isLatest = 1
        GROUP BY p.id
        ORDER BY p.date DESC
    """)
    fun getAllPrescriptionsWithMedicineCount(): Flow<List<com.mediciplanner.app.data.local.relation.PrescriptionWithMedicineCount>>

    @Query("DELETE FROM medicines WHERE prescriptionId = :prescriptionId")
    suspend fun deleteMedicinesByPrescriptionId(prescriptionId: String)

    // --- Transactional Saving ---

    @Transaction
    suspend fun insertFullPrescription(
        prescription: PrescriptionEntity,
        medicines: List<MedicineEntity>,
        schedules: List<DoseScheduleEntity>
    ) {
        insertPrescription(prescription)
        insertMedicines(medicines)
        insertDoseSchedules(schedules)
    }

    @Query("SELECT m.name FROM medicines m INNER JOIN dose_schedules s ON m.id = s.medicineId WHERE s.id = :scheduleId LIMIT 1")
    suspend fun getMedicineNameByScheduleId(scheduleId: String): String?

    @Query("SELECT m.* FROM medicines m INNER JOIN dose_schedules s ON m.id = s.medicineId WHERE s.id = :scheduleId LIMIT 1")
    suspend fun getMedicineByScheduleId(scheduleId: String): MedicineEntity?

    @Transaction
    suspend fun decrementStockIfAvailable(medicineId: String): MedicineEntity? {
        val med = getMedicineById(medicineId) ?: return null
        if (med.stockCount <= 0) return null
        val updated = med.copy(stockCount = med.stockCount - 1)
        updateMedicine(updated)
        return updated
    }
}
