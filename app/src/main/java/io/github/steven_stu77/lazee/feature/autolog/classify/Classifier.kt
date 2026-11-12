package io.github.steven_stu77.lazee.feature.autolog.classify

import io.github.steven_stu77.lazee.core.data.Confidence
import kotlinx.serialization.json.Json

/** The user's own memory for a merchant (from MerchantRule). */
data class Memory(val categoryId: Long, val confirmCount: Int, val alwaysAsk: Boolean)

/** Either a category the user chose (id) or a built-in key from the bundled word lists. */
data class Classification(val categoryId: Long?, val builtInKey: String?, val confidence: Confidence)

/**
 * Step 8 of spec §6, first hit wins:
 * merchant memory (HIGH) → merchants.json (MEDIUM) → keywords.json (MEDIUM) → "other" (LOW).
 * Transfers to a person are never guessed.
 */
class Classifier(
    private val merchants: Map<String, List<String>>,
    private val keywords: Map<String, List<String>>,
) {
    // Longest prefix first, so GRABFOOD (food) wins over GRAB (transport).
    private val prefixes: List<Pair<String, String>> =
        merchants.flatMap { (key, list) -> list.map { it.uppercase() to key } }.sortedByDescending { it.first.length }

    fun classify(merchantKey: String, memory: Memory?, isPersonTransfer: Boolean = false): Classification {
        if (memory != null) return Classification(memory.categoryId, null, Confidence.HIGH)
        if (isPersonTransfer) return Classification(null, OTHER, Confidence.LOW)
        val key = merchantKey.uppercase()
        prefixes.firstOrNull { (p, _) -> key == p || key.startsWith(p) }?.let { return Classification(null, it.second, Confidence.MEDIUM) }
        for ((cat, words) in keywords) {
            if (words.any { w -> Regex("\\b${Regex.escape(w.uppercase())}\\b").containsMatchIn(key) }) return Classification(null, cat, Confidence.MEDIUM)
        }
        return Classification(null, OTHER, Confidence.LOW)
    }

    companion object {
        const val OTHER = "other"
        private val json = Json { ignoreUnknownKeys = true }
        fun decodeMap(text: String): Map<String, List<String>> = json.decodeFromString(text)
    }
}
