package com.twig.dreamzversion3.model.dream

data class Dream(
    val id: String,
    val title: String,
    val description: String,
    val lucidity: Float,
    val isRecurring: Boolean
)
