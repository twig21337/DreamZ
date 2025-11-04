package com.twig.dreamzversion3.data

data class DreamDraft(
    val title: String = "",
    val body: String = "",
    val mood: String = "",
    val lucid: Boolean = false
) {
    fun isBlank(): Boolean = title.isBlank() && body.isBlank()

    fun toEntry(): DreamEntry = DreamEntry(
        title = title.trim(),
        body = body.trim(),
        mood = mood.trim().takeIf { it.isNotEmpty() },
        lucid = lucid
    )
}
