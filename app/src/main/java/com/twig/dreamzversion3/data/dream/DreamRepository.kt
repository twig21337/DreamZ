package com.twig.dreamzversion3.data.dream

import android.content.Context
import com.twig.dreamzversion3.model.dream.Dream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface DreamRepository {
    val dreams: StateFlow<List<Dream>>
    fun addDream(dream: Dream)
    fun updateDream(dream: Dream)
    fun deleteDream(id: String)
    fun getDream(id: String): Dream?
    fun getDreams(): List<Dream>
}

class InMemoryDreamRepository : DreamRepository {
    private val _dreams = MutableStateFlow<List<Dream>>(emptyList())
    override val dreams: StateFlow<List<Dream>> = _dreams.asStateFlow()

    override fun addDream(dream: Dream) {
        _dreams.update { current -> current + dream }
    }

    override fun updateDream(dream: Dream) {
        _dreams.update { current ->
            current.map { existing -> if (existing.id == dream.id) dream else existing }
        }
    }

    override fun deleteDream(id: String) {
        _dreams.update { current -> current.filterNot { it.id == id } }
    }

    override fun getDream(id: String): Dream? {
        return _dreams.value.firstOrNull { it.id == id }
    }

    override fun getDreams(): List<Dream> {
        return _dreams.value
    }
}

object DreamRepositories {
    val inMemory: DreamRepository by lazy { InMemoryDreamRepository() }

    @Volatile
    private var persistentRepository: DreamRepository? = null

    fun persistent(context: Context): DreamRepository {
        val existing = persistentRepository
        if (existing != null) return existing
        return synchronized(this) {
            persistentRepository ?: DataStoreDreamRepository(context.applicationContext).also {
                persistentRepository = it
            }
        }
    }
}
