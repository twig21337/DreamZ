package com.twig.dreamzversion3.data

data class DreamDraft(
    val title: String = "",
    val body: String = "",
    val mood: String = "",
    val lucid: Boolean = false,
    val tags: List<String> = emptyList(),
    val intensityRating: Int = 0,
    val emotionRating: Int = 0,
    val lucidityRating: Int = 0,
    val recurring: Boolean = false
) {
    fun isBlank(): Boolean = title.isBlank() && body.isBlank()

    fun toEntry(): DreamEntry = DreamEntry(
        title = title.trim(),
        body = body.trim(),
        mood = mood.trim().takeIf { it.isNotEmpty() },
        lucid = lucid,
        tags = tags.distinct().map { it.trim() }.filter { it.isNotEmpty() },
        intensityRating = intensityRating,
        emotionRating = emotionRating,
        lucidityRating = lucidityRating
    )
}
