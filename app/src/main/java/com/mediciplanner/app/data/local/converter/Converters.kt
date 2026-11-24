package com.mediciplanner.app.data.local.converter

import androidx.room.TypeConverter
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.ReminderStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Converters {

    // MedicineType
    @TypeConverter
    fun fromMedicineType(value: MedicineType?): String? = value?.name

    @TypeConverter
    fun toMedicineType(value: String?): MedicineType? {
        return value?.let {
            try {
                MedicineType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // FoodRule
    @TypeConverter
    fun fromFoodRule(value: FoodRule?): String? = value?.name

    @TypeConverter
    fun toFoodRule(value: String?): FoodRule? {
        return value?.let {
            try {
                FoodRule.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // DoseSlot
    @TypeConverter
    fun fromDoseSlot(value: DoseSlot?): String? = value?.name

    @TypeConverter
    fun toDoseSlot(value: String?): DoseSlot? {
        return value?.let {
            try {
                DoseSlot.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // ReminderStatus
    @TypeConverter
    fun fromReminderStatus(value: ReminderStatus?): String? = value?.name

    @TypeConverter
    fun toReminderStatus(value: String?): ReminderStatus? {
        return value?.let {
            try {
                ReminderStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // FollowUpType
    @TypeConverter
    fun fromFollowUpType(value: FollowUpType?): String? = value?.name

    @TypeConverter
    fun toFollowUpType(value: String?): FollowUpType? {
        return value?.let {
            try {
                FollowUpType.valueOf(it)
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }

    // Date <-> Timestamp (Long)
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    // Date <-> ISO-8601 String
    @TypeConverter
    fun fromIsoString(value: String?): Date? {
        return value?.let {
            try {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(it)
            } catch (e: Exception) {
                null
            }
        }
    }

    @TypeConverter
    fun dateToIsoString(date: Date?): String? {
        return date?.let {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(it)
        }
    }
}
