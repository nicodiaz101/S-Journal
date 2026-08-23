package com.example.s_journal.data.local.database

import androidx.room.TypeConverter
import com.example.s_journal.data.model.EntryType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.toString()
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let { LocalDate.parse(it) }
    }

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(millis: Long?): Instant? {
        return millis?.let { Instant.ofEpochMilli(it) }
    }

    @TypeConverter
    fun fromEntryType(type: EntryType?): String? {
        return type?.name
    }

    @TypeConverter
    fun toEntryType(typeName: String?): EntryType? {
        return typeName?.let { EntryType.valueOf(it) }
    }

    @TypeConverter
    fun fromStringList(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }

    @TypeConverter
    fun toStringList(json: String?): List<String> {
        if (json.isNullOrEmpty()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
