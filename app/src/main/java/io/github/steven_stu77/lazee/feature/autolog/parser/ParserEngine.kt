package io.github.steven_stu77.lazee.feature.autolog.parser

import io.github.steven_stu77.lazee.core.money.Money
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

enum class Direction { OUT, IN, OWN_TRANSFER, REFUND, IGNORE, PERSON_TRANSFER }

@Serializable
data class ParserRule(
    val direction: Direction,
    val pattern: String,
    /** For OWN_TRANSFER: true when money arrives in this app (a reload), false when it leaves. */
    val incoming: Boolean? = null,
)

/** One app's rules: assets/parsers/<package>.json. [preview] = rules written from made-up samples. */
@Serializable
data class ParserConfig(
    val `package`: String,
    val displayName: String,
    val preview: Boolean = false,
    val rules: List<ParserRule>,
)

/** What a notification turned into. Holds no raw text. */
data class Parsed(val amount: Long, val merchant: String, val direction: Direction, val last4: String? = null, val incoming: Boolean = direction == Direction.IN)

/**
 * Tries each rule of the app in order; the first match wins (spec §6 step 3).
 * Rules use named groups: amount, merchant, last4 (optional).
 */
class ParserEngine(configs: List<ParserConfig>) {
    private val byPackage: Map<String, Pair<ParserConfig, List<Pair<ParserRule, Regex>>>> =
        configs.associate { c -> c.`package` to (c to c.rules.map { it to Regex(it.pattern) }) }

    val packages: Set<String> get() = byPackage.keys
    fun config(pkg: String): ParserConfig? = byPackage[pkg]?.first

    fun parse(pkg: String, text: String): Parsed? {
        if (OtpFilter.isOtp(text)) return null
        val rules = byPackage[pkg]?.second ?: return null
        val flat = text.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        for ((rule, regex) in rules) {
            val m = regex.find(flat) ?: continue
            if (rule.direction == Direction.IGNORE) return null
            val amount = group(m, "amount")?.let { Money.parse(it) } ?: return null
            if (amount <= 0) return null
            val merchant = group(m, "merchant")?.trim()?.trimEnd('.', ',')?.ifBlank { null }
                ?: config(pkg)?.displayName ?: pkg
            return Parsed(amount, merchant, rule.direction, group(m, "last4")?.takeLast(4), rule.incoming ?: (rule.direction == Direction.IN))
        }
        return null
    }

    private fun group(m: MatchResult, name: String): String? = runCatching { m.groups[name]?.value }.getOrNull()

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun decode(text: String): ParserConfig = json.decodeFromString(ParserConfig.serializer(), text)
    }
}
