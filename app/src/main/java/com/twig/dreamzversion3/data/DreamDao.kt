package com.twig.dreamzversion3.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {
    @Query("SELECT * FROM dream_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DreamEntry>>

    @Query("SELECT * FROM dream_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<DreamEntry>

    @Query("SELECT * FROM dream_entries WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DreamEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: DreamEntry)

    @Delete
    suspend fun delete(entry: DreamEntry)
}
