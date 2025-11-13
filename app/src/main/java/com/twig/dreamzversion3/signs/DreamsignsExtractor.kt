package com.twig.dreamzversion3.signs

data class Dreamsign(val text: String, val count: Int)

private val tokenRegex = Regex("[A-Za-z][A-Za-z']+")

private val baseStopwords = setOf(
    "the", "and", "but", "or", "a", "an", "to", "of", "for", "in", "on", "at", "is", "am", "are",
    "was", "were", "be", "been", "being", "i", "im", "ive", "you", "your", "yours", "me", "my", "we",
    "our", "they", "them", "their", "dream", "dreams", "test", "tests", "testing", "today", "tonight",
    "yesterday", "thing", "things", "someone", "something", "everything"
)

internal fun buildDreamsignStopwords(extra: Set<String> = emptySet()): Set<String> {
    if (extra.isEmpty()) return baseStopwords
    return baseStopwords + extra.map { it.lowercase() }
}

private fun normalizeText(text: String): String = text
    .lowercase()
    .replace('\r', ' ')
    .replace('\n', ' ')
    .replace('\t', ' ')
    .replace("’", "'")
    .replace("‘", "'")
    .replace("“", "\"")
    .replace("”", "\"")

internal fun tokenizeDreamsignWords(
    text: String,
    stopwords: Set<String> = baseStopwords
): List<String> {
    if (text.isBlank()) return emptyList()

    val normalizedText = normalizeText(text)
    val tokens = mutableListOf<String>()

    tokenRegex.findAll(normalizedText).forEach { matchResult ->
        var token = matchResult.value
        if (token.endsWith("'s")) {
            token = token.dropLast(2)
        }
        if (token.length < 3) return@forEach
        if (token in stopwords) return@forEach
        tokens += token
    }

    return tokens
}

/**
 * Returns the top-K recurring dream sign candidates extracted from dream bodies.
 *
 * Tokens are normalized, filtered, and counted according to the dream sign specification.
 */
fun extractDreamsigns(
    allTexts: List<String>,
    topK: Int = 20,
    minGlobalCount: Int = 3,
    minPerDreamCount: Int = 2,
    extraStopwords: Set<String> = emptySet()
): List<Dreamsign> {
    if (allTexts.isEmpty()) return emptyList()

    val stopwords = buildDreamsignStopwords(extraStopwords)

    val globalWordFrequency = mutableMapOf<String, Int>()
    val perDreamWordFrequency = mutableListOf<Map<String, Int>>()
    val bigramFrequency = mutableMapOf<String, Int>()

    for (text in allTexts) {
        val tokens = tokenizeDreamsignWords(text, stopwords)
        if (tokens.isEmpty()) {
            perDreamWordFrequency += emptyMap()
            continue
        }

        val perDreamCounts = mutableMapOf<String, Int>()
        tokens.forEach { token ->
            val updated = (perDreamCounts[token] ?: 0) + 1
            perDreamCounts[token] = updated
            globalWordFrequency[token] = (globalWordFrequency[token] ?: 0) + 1
        }
        perDreamWordFrequency += perDreamCounts

        for (i in 0 until tokens.size - 1) {
            val bigram = tokens[i] + " " + tokens[i + 1]
            bigramFrequency[bigram] = (bigramFrequency[bigram] ?: 0) + 1
        }
    }

    val qualifyingWords = globalWordFrequency.filter { (word, count) ->
        val meetsGlobal = count >= minGlobalCount
        val meetsPerDream = perDreamWordFrequency.any { (it[word] ?: 0) >= minPerDreamCount }
        (meetsGlobal || meetsPerDream) && word !in stopwords
    }

    val qualifyingBigrams = bigramFrequency.filter { (_, count) -> count >= 2 }

    return (qualifyingWords.entries + qualifyingBigrams.entries)
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(topK)
        .map { Dreamsign(it.key, it.value) }
}
