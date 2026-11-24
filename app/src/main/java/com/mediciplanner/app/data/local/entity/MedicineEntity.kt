package com.mediciplanner.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

enum class MedicineType {
    TABLET,
    CAPSULE,
    SYRUP,
    DROPS,
    INHALER,
    OINTMENT,
    INJECTION
}

enum class FoodRule {
    BEFORE,
    AFTER,
    WITH,
    EMPTY
}

@Entity(
    tableName = "medicines",
    foreignKeys = [
        ForeignKey(
            entity = PrescriptionEntity::class,
            parentColumns = ["id"],
            childColumns = ["prescriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["prescriptionId"])
    ]
)
data class MedicineEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val prescriptionId: String,
    val name: String,
    val type: MedicineType,
    val strength: Double? = null,
    val strengthUnit: String? = null,
    val durationDays: Int? = null,
    val intervalDays: Int = 1,
    val foodRule: FoodRule,
    val note: String? = null,
    val createdAt: String,
    val imagePath: String? = null,
    val lineageId: String = id,
    val stockCount: Int = 0,
    val initialStock: Int = 0
)
