package com.twig.dreamzversion3.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twig.dreamzversion3.data.DreamEntry
import com.twig.dreamzversion3.data.DreamRepo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DreamViewModel(private val repo: DreamRepo) : ViewModel() {

    val entries = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addDream(title: String, body: String, mood: String?, lucid: Boolean) {
        if (title.isBlank() && body.isBlank()) return
        viewModelScope.launch {
            repo.save(
                DreamEntry(
                    title = title.trim(),
                    body = body.trim(),
                    mood = mood?.takeIf { it.isNotBlank() }?.trim(),
                    lucid = lucid
                )
            )
        }
    }

    fun delete(entry: DreamEntry) {
        viewModelScope.launch { repo.delete(entry) }
    }
}
