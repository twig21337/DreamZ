package com.twig.dreamzversion3.data

import kotlinx.coroutines.flow.Flow

class DreamRepo(private val dao: DreamDao) {
    fun observeAll(): Flow<List<DreamEntry>> = dao.observeAll()
    suspend fun getAll(): List<DreamEntry> = dao.getAll()
    suspend fun get(id: String) = dao.getById(id)
    suspend fun save(entry: DreamEntry) =
        dao.upsert(entry.copy(editedAt = System.currentTimeMillis()))
    suspend fun delete(entry: DreamEntry) = dao.delete(entry)
}
