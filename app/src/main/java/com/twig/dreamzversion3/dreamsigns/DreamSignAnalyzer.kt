package com.twig.dreamzversion3.dreamsigns

import com.twig.dreamzversion3.model.dream.Dream
import com.twig.dreamzversion3.signs.extractDreamsigns

private data class MutableDreamSignCandidate(
    val key: String,
    var displayText: String,
    var count: Int,
    val sources: MutableSet<DreamSignSource>
)

internal fun buildDreamSignCandidates(
    dreams: List<Dream>,
    maxItems: Int = 40
): List<DreamSignCandidate> {
    if (dreams.isEmpty()) return emptyList()

    val candidateMap = mutableMapOf<String, MutableDreamSignCandidate>()

    val descriptionTexts = dreams.mapNotNull { dream ->
        dream.description.takeIf { it.isNotBlank() }
    }
    if (descriptionTexts.isNotEmpty()) {
        val extracted = extractDreamsigns(descriptionTexts, topK = maxItems)
        extracted.forEach { sign ->
            val key = sign.text.lowercase()
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
                    sources = mutableSetOf()
                )
            }
            candidate.count += sign.count
            candidate.sources += DreamSignSource.Description
        }
    }

    val tagOccurrences = mutableMapOf<String, Int>()
    val tagDisplay = mutableMapOf<String, String>()
    dreams.forEach { dream ->
        dream.tags.forEach { rawTag ->
            val tag = rawTag.trim()
            if (tag.isNotEmpty()) {
                val key = tag.lowercase()
                tagOccurrences[key] = (tagOccurrences[key] ?: 0) + 1
                tagDisplay.putIfAbsent(key, tag)
            }
        }
    }

    tagOccurrences.forEach { (key, count) ->
        val display = tagDisplay[key] ?: key
        val candidate = candidateMap.getOrPut(key) {
            MutableDreamSignCandidate(
                key = key,
                displayText = display,
                count = 0,
                sources = mutableSetOf()
            )
        }
        candidate.count += count
        candidate.sources += DreamSignSource.Tag
    }

    return candidateMap.values
        .sortedWith(compareByDescending<MutableDreamSignCandidate> { it.count }.thenBy { it.displayText })
        .take(maxItems)
        .map { mutable ->
            DreamSignCandidate(
                key = mutable.key,
                displayText = mutable.displayText,
                count = mutable.count,
                sources = mutable.sources.toSet()
            )
        }
}
