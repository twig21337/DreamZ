package com.twig.dreamzversion3.data

import kotlinx.coroutines.flow.Flow

class DreamRepo(private val dao: DreamDao) {
    fun observeFiltered(filter: DreamFilter): Flow<List<DreamEntry>> =
        dao.observeFiltered(
            filter.query.trim(),
            filter.tag,
            filter.startMillis,
            filter.endMillis
        )
    suspend fun getAll(): List<DreamEntry> = dao.getAll()
    suspend fun get(id: String) = dao.getById(id)
    suspend fun save(entry: DreamEntry) =
        dao.upsert(entry.copy(editedAt = System.currentTimeMillis()))
    suspend fun delete(entry: DreamEntry) = dao.delete(entry)
}

data class DreamFilter(
    val query: String = "",
    val tag: String? = null,
    val startMillis: Long? = null,
    val endMillis: Long? = null
)
