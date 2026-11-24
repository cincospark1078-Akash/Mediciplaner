package com.mediciplanner.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Update
    suspend fun updateReminder(reminder: ReminderEntity)

    @Delete
    suspend fun deleteReminder(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getReminderById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders ORDER BY scheduledAt ASC")
    fun getAllReminders(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE doseScheduleId = :doseScheduleId ORDER BY scheduledAt ASC")
    fun getRemindersByDoseScheduleId(doseScheduleId: String): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE scheduledAt LIKE :datePrefix || '%' ORDER BY scheduledAt ASC")
    fun getRemindersForDate(datePrefix: String): Flow<List<ReminderEntity>>

    @Query("UPDATE reminders SET status = :status, takenAt = :takenAt WHERE id = :id")
    suspend fun updateReminderStatus(id: String, status: ReminderStatus, takenAt: String?)

    @Query("SELECT * FROM reminders WHERE status = 'PENDING'")
    suspend fun getAllPendingReminders(): List<ReminderEntity>

    @Query("""
        SELECT r.id
        FROM reminders r
        INNER JOIN dose_schedules ds ON r.doseScheduleId = ds.id
        INNER JOIN medicines m ON ds.medicineId = m.id
        INNER JOIN prescriptions p ON m.prescriptionId = p.id
        WHERE p.parentId = :parentId
          AND r.status = 'PENDING'
          AND r.scheduledAt > :now
    """)
    suspend fun getFuturePendingReminderIdsByParentId(parentId: String, now: String): List<String>

    @Query("""
        SELECT r.id
        FROM reminders r
        INNER JOIN dose_schedules ds ON r.doseScheduleId = ds.id
        INNER JOIN medicines m ON ds.medicineId = m.id
        INNER JOIN prescriptions p ON m.prescriptionId = p.id
        WHERE p.id = :prescriptionId
          AND r.status = 'PENDING'
    """)
    suspend fun getPendingReminderIdsByPrescriptionId(prescriptionId: String): List<String>

    @Query("""
        SELECT r.id
        FROM reminders r
        INNER JOIN dose_schedules ds ON r.doseScheduleId = ds.id
        INNER JOIN medicines m ON ds.medicineId = m.id
        INNER JOIN prescriptions p ON m.prescriptionId = p.id
        WHERE m.id = :medicineId
          AND r.status = 'PENDING'
    """)
    suspend fun getPendingReminderIdsByMedicineId(medicineId: String): List<String>

    @Query("DELETE FROM reminders WHERE id IN (:ids)")
    suspend fun deleteRemindersByIds(ids: List<String>)
}
