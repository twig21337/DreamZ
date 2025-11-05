package com.twig.dreamzversion3.model.dream

data class Dream(
    val id: String,
    val title: String,
    val description: String,
    val mood: String,
    val lucidity: Float,
    val intensity: Float,
    val emotion: Float,
    val isRecurring: Boolean,
    val tags: List<String>
)
