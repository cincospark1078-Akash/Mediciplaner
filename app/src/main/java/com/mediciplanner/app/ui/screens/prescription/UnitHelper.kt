package com.mediciplanner.app.ui.screens.prescription

import com.mediciplanner.app.core.util.UnitHelper
import com.mediciplanner.app.data.local.entity.MedicineType

/**
 * Delegates to core UnitHelper as single source of truth.
 */
fun getUnitsForType(type: MedicineType?): List<String> = UnitHelper.getStrengthUnitsForType(type)

fun getStrengthUnitsForType(type: MedicineType?): List<String> = UnitHelper.getStrengthUnitsForType(type)

fun getDoseUnitsForType(type: MedicineType?): List<String> = UnitHelper.getDoseUnitsForType(type)
