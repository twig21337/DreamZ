package com.twig.dreamzversion3.signs

data class Dreamsign(val text: String, val count: Int)

private val stop = setOf(
    "the","a","an","and","or","but","if","then","than","to","of","in","on","at","for","from",
    "with","by","as","was","were","is","are","be","been","being","i","me","my","we","our","you",
    "your","he","she","it","they","them","this","that","there","here","up","down","left","right",
    "over","under","into","out","so","just","really","very","like"
)

private fun normalize(text: String): List<String> =
    text.lowercase()
        .replace(Regex("[^a-z0-9\\s]"), " ")
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }

private fun lemma(token: String): String {
    if (token.length > 5 && token.endsWith("ing")) return token.removeSuffix("ing")
    if (token.length > 4 && token.endsWith("ed"))  return token.removeSuffix("ed")
    if (token.length > 3 && token.endsWith("s"))   return token.removeSuffix("s")
    return token
}

/** Returns top-K recurring single words + simple bigrams. Fully offline. */
fun extractDreamsigns(allTexts: List<String>, topK: Int = 20): List<Dreamsign> {
    val counts = mutableMapOf<String, Int>()

    for (t in allTexts) {
        val toks = normalize(t).map(::lemma).filter { it !in stop }
        // unigrams
        for (w in toks) counts[w] = (counts[w] ?: 0) + 1
        // simple bigrams (optional, helps capture “school hallway”, “black cat”)
        for (i in 0 until toks.size - 1) {
            val a = toks[i]; val b = toks[i + 1]
            if (a !in stop && b !in stop) {
                val bigram = "$a $b"
                counts[bigram] = (counts[bigram] ?: 0) + 1
            }
        }
    }

    return counts.entries
        .sortedByDescending { it.value }
        .take(topK)
        .map { Dreamsign(it.key, it.value) }
}
