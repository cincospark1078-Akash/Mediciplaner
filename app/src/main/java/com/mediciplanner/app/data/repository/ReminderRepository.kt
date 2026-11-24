package com.mediciplanner.app.data.repository

import com.mediciplanner.app.data.local.dao.ReminderDao
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.util.ReminderIdGenerator
import kotlinx.coroutines.flow.Flow

class ReminderRepository(
    private val reminderDao: ReminderDao
) {

    val allReminders: Flow<List<ReminderEntity>> = reminderDao.getAllReminders()

    suspend fun getReminderById(id: String): ReminderEntity? {
        return reminderDao.getReminderById(id)
    }

    fun getRemindersForDate(datePrefix: String): Flow<List<ReminderEntity>> {
        return reminderDao.getRemindersForDate(datePrefix)
    }

    fun getRemindersByDoseScheduleId(doseScheduleId: String): Flow<List<ReminderEntity>> {
        return reminderDao.getRemindersByDoseScheduleId(doseScheduleId)
    }

    suspend fun insertReminder(reminder: ReminderEntity) {
        reminderDao.insertReminder(reminder)
    }

    suspend fun insertReminders(reminders: List<ReminderEntity>) {
        reminderDao.insertReminders(reminders)
    }

    /**
     * Creates and saves a reminder with a deterministic ID.
     */
    suspend fun createDeterministicReminder(
        doseScheduleId: String,
        scheduledDate: String,
        scheduledAt: String,
        status: ReminderStatus = ReminderStatus.PENDING
    ): ReminderEntity {
        val reminderId = ReminderIdGenerator.generate(doseScheduleId, scheduledDate)
        val reminder = ReminderEntity(
            id = reminderId,
            doseScheduleId = doseScheduleId,
            scheduledAt = scheduledAt,
            status = status,
            takenAt = null
        )
        reminderDao.insertReminder(reminder)
        return reminder
    }

    suspend fun updateReminderStatus(id: String, status: ReminderStatus, takenAt: String? = null) {
        reminderDao.updateReminderStatus(id, status, takenAt)
    }

    suspend fun markAsTaken(id: String, takenAt: String) {
        reminderDao.updateReminderStatus(id, ReminderStatus.TAKEN, takenAt)
    }

    suspend fun markAsMissed(id: String) {
        reminderDao.updateReminderStatus(id, ReminderStatus.MISSED, null)
    }

    suspend fun deleteReminder(reminder: ReminderEntity) {
        reminderDao.deleteReminder(reminder)
    }

    suspend fun deleteReminderById(id: String) {
        reminderDao.deleteReminderById(id)
    }

    suspend fun getAllPendingReminders(): List<ReminderEntity> {
        return reminderDao.getAllPendingReminders()
    }
}
