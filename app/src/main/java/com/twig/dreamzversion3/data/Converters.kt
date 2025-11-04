package com.twig.dreamzversion3.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromTags(value: List<String>?): String = value?.joinToString(separator = "|") ?: ""

    @TypeConverter
    fun toTags(value: String?): List<String> =
        value.takeUnless { it.isNullOrBlank() }?.split("|")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()
}
