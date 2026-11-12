package io.github.steven_stu77.lazee.feature.autolog.classify

/** Step 4 of spec §6: a stable upper-case key for a merchant name. Display keeps the original casing. */
object MerchantKey {
    private val suffixes = Regex(
        "\\b(SDN\\.?\\s*BHD\\.?|BHD\\.?|ENTERPRISE|ENTERPRISES|TRADING|RESOURCES|PLT|S/B)\\b.*$",
        RegexOption.IGNORE_CASE,
    )
    private val branch = Regex("\\s+[-@]\\s+[A-Z][A-Z .]{1,30}$|\\s*\\(.*\\)$|\\s*#?\\d{2,}$|\\s+(KL|PJ|JB|KK|KCH)$", RegexOption.IGNORE_CASE)

    fun of(raw: String): String {
        var s = raw.trim().uppercase()
        s = s.replace(suffixes, "").trim()
        // Strip trailing branch info a few times: "7-ELEVEN #1234 - KL" → "7-ELEVEN"
        repeat(3) { s = s.replace(branch, "").trim() }
        return s.replace(Regex("\\s+"), " ").trim().ifEmpty { raw.trim().uppercase() }
    }
}
