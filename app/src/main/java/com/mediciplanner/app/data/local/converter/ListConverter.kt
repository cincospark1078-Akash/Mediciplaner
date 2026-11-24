package com.mediciplanner.app.data.local.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ListConverter {
    @TypeConverter
    fun fromList(list: List<String>?): String {
        return Gson().toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun toList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(json, type)
    }
}
