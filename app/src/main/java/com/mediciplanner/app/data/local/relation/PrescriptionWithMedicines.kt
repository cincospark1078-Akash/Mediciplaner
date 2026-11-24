package com.mediciplanner.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity

/**
 * Composite relationship: A DoseSchedule with all associated Reminders.
 */
data class DoseScheduleWithReminders(
    @Embedded
    val doseSchedule: DoseScheduleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "doseScheduleId"
    )
    val reminders: List<ReminderEntity> = emptyList()
)

/**
 * Composite relationship: A Medicine with all its DoseSchedules (and nested Reminders).
 */
data class MedicineWithSchedules(
    @Embedded
    val medicine: MedicineEntity,
    @Relation(
        entity = DoseScheduleEntity::class,
        parentColumn = "id",
        entityColumn = "medicineId"
    )
    val doseSchedules: List<DoseScheduleWithReminders> = emptyList()
)

/**
 * Full Composite relationship: A Prescription with its Medicines and nested DoseSchedules & Reminders.
 */
data class PrescriptionWithMedicines(
    @Embedded
    val prescription: PrescriptionEntity,
    @Relation(
        entity = MedicineEntity::class,
        parentColumn = "id",
        entityColumn = "prescriptionId"
    )
    val medicines: List<MedicineWithSchedules> = emptyList()
)
