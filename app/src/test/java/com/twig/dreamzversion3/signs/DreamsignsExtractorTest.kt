package com.twig.dreamzversion3.signs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamsignsExtractorTest {

    @Test
    fun tokenizeDreamsignWords_appliesNormalizationAndFiltering() {
        val text = "Dragon's lair\n\t\"Bright\" lights and IM running through tunnels."

        val tokens = tokenizeDreamsignWords(text)

        assertEquals(
            listOf("dragon", "lair", "bright", "lights", "running", "through", "tunnels"),
            tokens
        )
    }

    @Test
    fun extractDreamsigns_countsWordsAndBigramsWithThresholds() {
        val texts = listOf(
            "I walked a dark forest path. The dark forest was silent. Portal portal opened.",
            "Another dark forest dream where a portal guardian waited.",
            "A calm river flows elsewhere."
        )

        val result = extractDreamsigns(texts, topK = 10)
        val counts = result.associate { it.text to it.count }

        assertEquals(3, counts["dark"])
        assertEquals(3, counts["forest"])
        assertEquals(3, counts["portal"])
        assertEquals(3, counts["dark forest"])
        assertFalse(counts.containsKey("river"))
        assertTrue(counts.keys.contains("dark forest"))
    }
}
