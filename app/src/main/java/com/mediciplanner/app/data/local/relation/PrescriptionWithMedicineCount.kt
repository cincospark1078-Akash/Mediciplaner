package com.mediciplanner.app.data.local.relation

import androidx.room.Embedded
import com.mediciplanner.app.data.local.entity.PrescriptionEntity

/**
 * Lightweight relation projection holding a [PrescriptionEntity] and its total medicine count.
 */
data class PrescriptionWithMedicineCount(
    @Embedded
    val prescription: PrescriptionEntity,
    val medicineCount: Int
)
