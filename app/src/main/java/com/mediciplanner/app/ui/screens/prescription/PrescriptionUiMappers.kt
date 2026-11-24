package com.mediciplanner.app.ui.screens.prescription

import androidx.annotation.StringRes
import com.mediciplanner.app.R
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType

@StringRes
fun getMedicineTypeResId(type: MedicineType): Int {
    return when (type) {
        MedicineType.TABLET -> R.string.type_tablet
        MedicineType.CAPSULE -> R.string.type_capsule
        MedicineType.SYRUP -> R.string.type_syrup
        MedicineType.DROPS -> R.string.type_drops
        MedicineType.INHALER -> R.string.type_inhaler
        MedicineType.OINTMENT -> R.string.type_ointment
        MedicineType.INJECTION -> R.string.type_injection
    }
}

@StringRes
fun getDoseSlotResId(slot: DoseSlot): Int {
    return when (slot) {
        DoseSlot.MORNING -> R.string.slot_morning
        DoseSlot.NOON -> R.string.slot_noon
        DoseSlot.AFTERNOON -> R.string.slot_afternoon
        DoseSlot.EVENING -> R.string.slot_evening
        DoseSlot.NIGHT -> R.string.slot_night
        DoseSlot.CUSTOM -> R.string.slot_custom
    }
}

@StringRes
fun getDoseSlotEmojiResId(slot: DoseSlot): Int {
    return when (slot) {
        DoseSlot.MORNING -> R.string.slot_emoji_morning
        DoseSlot.NOON -> R.string.slot_emoji_noon
        DoseSlot.AFTERNOON -> R.string.slot_emoji_afternoon
        DoseSlot.EVENING -> R.string.slot_emoji_evening
        DoseSlot.NIGHT -> R.string.slot_emoji_night
        DoseSlot.CUSTOM -> R.string.slot_emoji_custom
    }
}

@StringRes
fun getSlotMedicinesHeaderResId(slot: DoseSlot): Int {
    return when (slot) {
        DoseSlot.MORNING -> R.string.slot_medicines_morning
        DoseSlot.NOON -> R.string.slot_medicines_noon
        DoseSlot.AFTERNOON -> R.string.slot_medicines_afternoon
        DoseSlot.EVENING -> R.string.slot_medicines_evening
        DoseSlot.NIGHT -> R.string.slot_medicines_night
        DoseSlot.CUSTOM -> R.string.slot_medicines_custom
    }
}

@StringRes
fun getSlotSwitchedMessageResId(slot: DoseSlot): Int {
    return when (slot) {
        DoseSlot.MORNING -> R.string.msg_slot_switched_morning
        DoseSlot.NOON -> R.string.msg_slot_switched_noon
        DoseSlot.AFTERNOON -> R.string.msg_slot_switched_afternoon
        DoseSlot.EVENING -> R.string.msg_slot_switched_evening
        DoseSlot.NIGHT -> R.string.msg_slot_switched_night
        DoseSlot.CUSTOM -> R.string.msg_slot_switched_custom
    }
}

@StringRes
fun getFoodRuleResId(rule: FoodRule): Int {
    return when (rule) {
        FoodRule.BEFORE -> R.string.food_before
        FoodRule.AFTER -> R.string.food_after
        FoodRule.WITH -> R.string.food_with
        FoodRule.EMPTY -> R.string.food_empty
    }
}

@StringRes
fun getFoodRuleLabelResId(type: MedicineType?): Int {
    return when (type) {
        MedicineType.TABLET,
        MedicineType.CAPSULE,
        MedicineType.SYRUP -> R.string.rule_consumption
        MedicineType.INHALER,
        MedicineType.INJECTION,
        MedicineType.OINTMENT,
        MedicineType.DROPS,
        null -> R.string.rule_administration
    }
}

@StringRes
fun getUnitResId(unit: String): Int {
    return when (unit) {
        "mg" -> R.string.unit_mg
        "mcg" -> R.string.unit_mcg
        "mg/5mL" -> R.string.unit_mg_5ml
        "mg/mL" -> R.string.unit_mg_ml
        "mcg/puff" -> R.string.unit_mcg_puff
        "IU/mL" -> R.string.unit_iu_ml
        "%" -> R.string.unit_percent
        "mg/g" -> R.string.unit_mg_g
        "pcs" -> R.string.unit_pcs
        "টি" -> R.string.unit_ti
        "mL" -> R.string.unit_ml
        "tsp" -> R.string.unit_tsp
        "drop" -> R.string.unit_drop
        "puff" -> R.string.unit_puff
        "unit" -> R.string.unit_unit
        "application" -> R.string.unit_application
        else -> R.string.unit_mg
    }
}
