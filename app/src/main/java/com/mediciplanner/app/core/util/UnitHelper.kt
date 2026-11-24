package com.mediciplanner.app.core.util

import com.mediciplanner.app.data.local.entity.MedicineType

object UnitHelper {
    fun getStrengthUnitsForType(type: MedicineType?): List<String> {
        return when (type) {
            MedicineType.TABLET, MedicineType.CAPSULE -> listOf("mg", "mcg")
            MedicineType.SYRUP -> listOf("mg/5mL")
            MedicineType.DROPS -> listOf("mg/mL")
            MedicineType.INHALER -> listOf("mcg/puff")
            MedicineType.INJECTION -> listOf("mg/mL", "IU/mL")
            MedicineType.OINTMENT -> listOf("%", "mg/g")
            null -> emptyList()
        }
    }

    fun getDoseUnitsForType(type: MedicineType?): List<String> {
        return when (type) {
            MedicineType.TABLET, MedicineType.CAPSULE -> listOf("pcs", "টি")
            MedicineType.SYRUP -> listOf("mL", "tsp")
            MedicineType.DROPS -> listOf("mL", "drop")
            MedicineType.INHALER -> listOf("puff")
            MedicineType.INJECTION -> listOf("mL", "unit")
            MedicineType.OINTMENT -> listOf("application")
            null -> emptyList()
        }
    }
}
