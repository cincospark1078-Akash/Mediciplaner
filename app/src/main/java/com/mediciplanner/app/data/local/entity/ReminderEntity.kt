package com.mediciplanner.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mediciplanner.app.data.util.ReminderIdGenerator

enum class ReminderStatus {
    PENDING,
    TAKEN,
    MISSED
}

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = DoseScheduleEntity::class,
            parentColumns = ["id"],
            childColumns = ["doseScheduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["doseScheduleId"]),
        Index(value = ["scheduledAt"])
    ]
)
data class ReminderEntity(
    @PrimaryKey
    val id: String,
    val doseScheduleId: String,
    val scheduledAt: String,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val takenAt: String? = null
) {
    companion object {
        /**
         * Helper to construct a ReminderEntity with a deterministic ID.
         */
        fun createDeterministic(
            doseScheduleId: String,
            scheduledDate: String,
            scheduledAt: String,
            status: ReminderStatus = ReminderStatus.PENDING,
            takenAt: String? = null
        ): ReminderEntity {
            val id = ReminderIdGenerator.generate(doseScheduleId, scheduledDate)
            return ReminderEntity(
                id = id,
                doseScheduleId = doseScheduleId,
                scheduledAt = scheduledAt,
                status = status,
                takenAt = takenAt
            )
        }
    }
}
