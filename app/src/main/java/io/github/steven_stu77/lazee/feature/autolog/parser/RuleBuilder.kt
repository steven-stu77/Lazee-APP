package io.github.steven_stu77.lazee.feature.autolog.parser

/**
 * Turns one example notification into a rule (spec: config, not code). The user marks which
 * part is the amount and which is the merchant; everything else must match as written, except
 * that other numbers (dates, reference numbers) and spacing may differ.
 */
object RuleBuilder {
    /** "RM4.20", "RM 1,234.50", "MYR 6.80", "RM12" */
    val AMOUNT = Regex("(?i)(?:RM|MYR)\\s?[\\d,]+(?:\\.\\d{1,2})?")
    private const val AMOUNT_GROUP = "(?:RM|MYR)?\\s?(?<amount>[\\d,]+(?:\\.\\d{1,2})?)"
    private const val MERCHANT_GROUP = "(?<merchant>.+?)"

    sealed class Result {
        data class Ok(val pattern: String) : Result()
        data class Error(val reason: Reason) : Result()
    }

    enum class Reason { NO_AMOUNT, AMOUNT_NOT_IN_SAMPLE, MERCHANT_NOT_IN_SAMPLE, OVERLAP }

    /** First amount in the text, for pre-filling the editor. */
    fun detectAmount(sample: String): String? = AMOUNT.find(sample)?.value

    /**
     * Guess the merchant: the text after the amount and "to"/"at"/"from" up to the end of the sentence.
     * Only a suggestion; the user can change it.
     */
    fun detectMerchant(sample: String): String? {
        val m = Regex("(?i)(?:RM|MYR)\\s?[\\d,]+(?:\\.\\d{1,2})?\\s+(?:to|at|from|kepada|di)\\s+(.+?)(?:\\.|\\s+(?:is|was|has)\\b|$)").find(sample.trim())
        return m?.groupValues?.get(1)?.trim()?.ifEmpty { null }
    }

    fun build(sample: String, amountText: String, merchantText: String?, ignoreOtherNumbers: Boolean = true): Result {
        val text = sample.replace('\n', ' ').replace(Regex("\\s+"), " ").trim()
        val amount = amountText.trim()
        if (amount.isEmpty()) return Result.Error(Reason.NO_AMOUNT)
        val a = text.indexOf(amount)
        if (a < 0) return Result.Error(Reason.AMOUNT_NOT_IN_SAMPLE)
        val merchant = merchantText?.trim()?.ifEmpty { null }
        val m = merchant?.let { text.indexOf(it) } ?: -1
        if (merchant != null && m < 0) return Result.Error(Reason.MERCHANT_NOT_IN_SAMPLE)
        if (merchant != null && m < a + amount.length && a < m + merchant.length) return Result.Error(Reason.OVERLAP)

        // Cut the sample into literal pieces and the two placeholders, in order.
        val marks = listOfNotNull(a to (a + amount.length) to AMOUNT_GROUP, if (merchant != null) (m to (m + merchant.length)) to MERCHANT_GROUP else null)
            .sortedBy { it.first.first }
        val sb = StringBuilder("(?i)")
        var pos = 0
        for ((range, group) in marks) {
            sb.append(literal(text.substring(pos, range.first), ignoreOtherNumbers)).append(group)
            pos = range.second
        }
        val tail = text.substring(pos)
        val merchantLast = marks.last().second == MERCHANT_GROUP && tail.none { it.isLetterOrDigit() }
        if (merchantLast) {
            // Lazy merchant needs an anchor, otherwise it would stop after one letter.
            val punct = tail.trim().takeWhile { !it.isLetterOrDigit() }
            sb.append(if (punct.isEmpty()) "\\s*$" else "(?:" + literal(punct, false) + ")?\\s*$")
        } else {
            sb.append(literal(tail.trimEnd('.', ' '), ignoreOtherNumbers))
        }
        return Result.Ok(sb.toString())
    }

    /** Escapes a literal piece; spaces match any spacing, other digit runs match any number. */
    private fun literal(s: String, ignoreNumbers: Boolean): String {
        if (s.isEmpty()) return ""
        val out = StringBuilder()
        Regex("\\d+|\\s+|[^\\d\\s]+").findAll(s).forEach { t ->
            val v = t.value
            out.append(
                when {
                    v.isBlank() -> "\\s*"
                    ignoreNumbers && v[0].isDigit() -> "\\d+"
                    else -> quote(v)
                },
            )
        }
        return out.toString()
    }

    /** Backslash-escape regex metacharacters (readable, unlike \Q…\E). */
    private fun quote(v: String) = buildString { v.forEach { c -> if (c in "\\.^$|?*+()[]{}/-") append('\\'); append(c) } }
}
