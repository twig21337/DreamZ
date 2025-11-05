package com.twig.dreamzversion3.data.dream

import com.twig.dreamzversion3.model.dream.Dream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface DreamRepository {
    val dreams: StateFlow<List<Dream>>
    fun addDream(dream: Dream)
}

class InMemoryDreamRepository : DreamRepository {
    private val _dreams = MutableStateFlow<List<Dream>>(emptyList())
    override val dreams: StateFlow<List<Dream>> = _dreams.asStateFlow()

    override fun addDream(dream: Dream) {
        _dreams.update { current -> current + dream }
    }
}

object DreamRepositories {
    val inMemory: DreamRepository by lazy { InMemoryDreamRepository() }
}
