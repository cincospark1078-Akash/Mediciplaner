package com.mediciplanner.app.data.util

import com.mediciplanner.app.data.local.entity.MedicineType

object UnitValidator {

    /**
     * Map of MedicineType to valid Pair(Allowed Strength Units, Allowed Dose Units).
     * Units are matched case-insensitively (or exact common medical casing).
     */
    private val VALID_UNITS_CONFIG = mapOf(
        MedicineType.TABLET to Pair(
            setOf("mg", "mcg"),
            setOf("pcs", "টি", "tablet", "tablets", "mg", "mcg")
        ),
        MedicineType.CAPSULE to Pair(
            setOf("mg", "mcg"),
            setOf("pcs", "টি", "capsule", "capsules", "mg", "mcg")
        ),
        MedicineType.SYRUP to Pair(
            setOf("mg/5ml", "mg/5mL"),
            setOf("ml", "mL", "tsp", "mg/5ml", "mg/5mL")
        ),
        MedicineType.DROPS to Pair(
            setOf("mg/ml", "mg/mL"),
            setOf("ml", "mL", "drop", "drops", "mg/ml", "mg/mL")
        ),
        MedicineType.INHALER to Pair(
            setOf("mcg/puff"),
            setOf("puff", "puffs", "mcg/puff")
        ),
        MedicineType.INJECTION to Pair(
            setOf("mg/ml", "mg/mL", "iu/ml", "IU/mL"),
            setOf("ml", "mL", "unit", "units", "mg/ml", "mg/mL", "iu/ml", "IU/mL")
        ),
        MedicineType.OINTMENT to Pair(
            setOf("%", "mg/g"),
            setOf("application", "applications", "g", "%", "mg/g")
        )
    )

    fun isStrengthUnitValid(type: MedicineType, strengthUnit: String?): Boolean {
        if (strengthUnit.isNullOrBlank()) return true // strengthUnit is nullable
        val config = VALID_UNITS_CONFIG[type] ?: return false
        return config.first.any { it.equals(strengthUnit.trim(), ignoreCase = true) }
    }

    fun isDoseUnitValid(type: MedicineType, doseUnit: String): Boolean {
        val config = VALID_UNITS_CONFIG[type] ?: return false
        return config.second.any { it.equals(doseUnit.trim(), ignoreCase = true) }
    }

    /**
     * Validates both strength unit and optional dose unit.
     * Throws IllegalArgumentException if invalid.
     */
    @Throws(IllegalArgumentException::class)
    fun validateUnits(type: MedicineType, strengthUnit: String?, doseUnit: String? = null) {
        if (!strengthUnit.isNullOrBlank() && !isStrengthUnitValid(type, strengthUnit)) {
            val allowed = VALID_UNITS_CONFIG[type]?.first?.joinToString(", ") ?: "none"
            throw IllegalArgumentException(
                "Invalid strength unit '$strengthUnit' for medicine type ${type.name}. ওষুধের ধরন ${type.name}-এর জন্য মাত্রার একক '$strengthUnit' সঠিক নয়। অনুমোদিত একক: $allowed"
            )
        }

        if (doseUnit != null && !isDoseUnitValid(type, doseUnit)) {
            val allowed = VALID_UNITS_CONFIG[type]?.second?.joinToString(", ") ?: "none"
            throw IllegalArgumentException(
                "Invalid dose unit '$doseUnit' for medicine type ${type.name}. ওষুধের ধরন ${type.name}-এর জন্য ডোজের একক '$doseUnit' সঠিক নয়। অনুমোদিত একক: $allowed"
            )
        }
    }
}
