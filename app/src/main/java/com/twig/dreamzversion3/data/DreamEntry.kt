package com.twig.dreamzversion3.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "dream_entries")
data class DreamEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val mood: String? = null,
    val lucid: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val editedAt: Long = System.currentTimeMillis()
)
