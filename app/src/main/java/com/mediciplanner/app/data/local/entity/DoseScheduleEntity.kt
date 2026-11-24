package com.mediciplanner.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class DoseSlot {
    MORNING,
    NOON,
    AFTERNOON,
    EVENING,
    NIGHT,
    CUSTOM
}

@Entity(
    tableName = "dose_schedules",
    foreignKeys = [
        ForeignKey(
            entity = MedicineEntity::class,
            parentColumns = ["id"],
            childColumns = ["medicineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["medicineId"])
    ]
)
data class DoseScheduleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val slot: DoseSlot,
    val time: String,
    val doseAmount: Double,
    val doseUnit: String,
    val foodRuleOverride: FoodRule? = null,
    val foodRule: FoodRule = FoodRule.AFTER
)
