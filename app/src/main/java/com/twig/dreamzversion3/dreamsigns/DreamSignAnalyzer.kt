package com.twig.dreamzversion3.dreamsigns

import com.twig.dreamzversion3.model.dream.Dream
import com.twig.dreamzversion3.signs.extractDreamsigns

private val dreamSignStopwords = setOf(
    "dream", "dreams", "test", "tests", "testing", "today", "tonight", "yesterday",
    "thing", "things", "someone", "something", "everything"
)

private data class MutableDreamSignCandidate(
    val key: String,
    var displayText: String,
    var count: Int,
    val sources: MutableSet<DreamSignSource>,
    val dreamTitles: LinkedHashSet<String>
)

private data class DreamContext(
    val title: String,
    val features: Set<String>
)

internal fun buildDreamSignCandidates(
    dreams: List<Dream>,
    blacklist: Set<String> = emptySet(),
    maxItems: Int = 40
): List<DreamSignCandidate> {
    if (dreams.isEmpty()) return emptyList()

    val candidateMap = mutableMapOf<String, MutableDreamSignCandidate>()
    val combinedStopwords = dreamSignStopwords + blacklist
    val contexts = dreams.map { it.toContext(combinedStopwords) }

    val descriptionTexts = dreams.mapNotNull { dream ->
        dream.description.takeIf { it.isNotBlank() }
    }
    if (descriptionTexts.isNotEmpty()) {
        val extracted = extractDreamsigns(descriptionTexts, topK = maxItems * 2)
        extracted.forEach { sign ->
            val key = sign.text.lowercase()
            if (!key.isValidCandidate(combinedStopwords, blacklist)) return@forEach
            val display = sign.text
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { token ->
                    token.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                .ifBlank { sign.text }
            val candidate = candidateMap.getOrPut(key) {
                MutableDreamSignCandidate(
                    key = key,
                    displayText = display,
                    count = 0,
                    sources = mutableSetOf(),
                    dreamTitles = linkedSetOf()
                )
            }
            candidate.count += sign.count
            candidate.sources += DreamSignSource.Description
            contexts.forEach { context ->
                if (context.features.contains(key)) {
                    candidate.dreamTitles += context.title
                }
            }
        }
    }

    dreams.forEach { dream ->
        val title = dream.title.ifBlank { "Untitled Dream" }
        dream.tags.forEach { rawTag ->
            val tag = rawTag.trim()
            if (tag.isNotEmpty()) {
                val key = tag.lowercase()
                if (!key.isValidCandidate(combinedStopwords, blacklist)) return@forEach
                val candidate = candidateMap.getOrPut(key) {
                    MutableDreamSignCandidate(
                        key = key,
                        displayText = tag.ifBlank { key },
                        count = 0,
                        sources = mutableSetOf(),
                        dreamTitles = linkedSetOf()
                    )
                }
                candidate.count += 1
                candidate.sources += DreamSignSource.Tag
                candidate.dreamTitles += title
            }
        }
    }

    return candidateMap.values
        .sortedWith(compareByDescending<MutableDreamSignCandidate> { it.count }.thenBy { it.displayText })
        .filterIndexed { index, _ -> index < maxItems }
        .map { mutable ->
            DreamSignCandidate(
                key = mutable.key,
                displayText = mutable.displayText,
                count = mutable.count,
                sources = mutable.sources.toSet(),
                dreamTitles = mutable.dreamTitles.toList()
            )
        }
}

private fun Dream.toContext(stopwords: Set<String>): DreamContext {
    if (description.isBlank()) return DreamContext(title.ifBlank { "Untitled Dream" }, emptySet())
    val tokens = normalize(description)
    val filtered = tokens.filterNot { it in stopwords }
    if (filtered.isEmpty()) return DreamContext(title.ifBlank { "Untitled Dream" }, emptySet())
    val features = mutableSetOf<String>()
    filtered.forEach { features += it }
    filtered.zipWithNext { a, b ->
        if (a !in stopwords && b !in stopwords) {
            features += "$a $b"
        }
    }
    return DreamContext(title.ifBlank { "Untitled Dream" }, features)
}

private fun normalize(text: String): List<String> =
    text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

private fun String.isValidCandidate(stopwords: Set<String>, blacklist: Set<String>): Boolean {
    val tokens = split(" ").filter { it.isNotBlank() }
    if (tokens.isEmpty()) return false
    val normalized = tokens.joinToString(" ") { it.lowercase() }
    if (normalized in blacklist) return false
    val remaining = tokens.filterNot { it.lowercase() in stopwords }
    return remaining.isNotEmpty()
}
