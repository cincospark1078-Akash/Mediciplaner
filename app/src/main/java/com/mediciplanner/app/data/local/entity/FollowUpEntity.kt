package com.mediciplanner.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class FollowUpType {
    DOCTOR_VISIT,
    TEST
}

@Entity(tableName = "follow_ups")
data class FollowUpEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: FollowUpType,
    val date: String,
    val note: String? = null,
    val reminderEnabled: Boolean = true,
    val location: String? = null,
    val isCompleted: Boolean = false,
    val reminderTime: String = "09:00",
    val completedAt: String? = null,
    val documentPaths: List<String> = emptyList()
)
