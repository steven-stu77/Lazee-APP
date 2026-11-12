package io.github.steven_stu77.lazee.core.money

import java.math.BigDecimal
import java.math.RoundingMode

/** Money is always Long cents. Formatting never depends on locale: "RM 1,234.50". */
object Money {
    const val HIDDEN = "RM ••••"

    /** "RM 1,234.50"; negative values keep the minus inside: "-RM 5.00". */
    fun format(cents: Long, hidden: Boolean = false): String {
        if (hidden) return HIDDEN
        val sign = if (cents < 0) "-" else ""
        return "${sign}RM ${plain(cents)}"
    }

    /** "1,234.50" without the RM prefix or sign. */
    fun plain(cents: Long): String {
        val abs = kotlin.math.abs(cents)
        val whole = abs / 100
        val frac = abs % 100
        return "${groupThousands(whole)}.${frac.toString().padStart(2, '0')}"
    }

    /** Compact whole-ringgit form for chart labels: "RM 1,235". */
    fun whole(cents: Long): String = "RM " + groupThousands(Math.round(cents / 100.0))

    private fun groupThousands(n: Long): String {
        val s = kotlin.math.abs(n).toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, ch ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return (if (n < 0) "-" else "") + sb
    }

    /** Parses "RM 1,234.50", "MYR 6.80", "6.8", "1234" → cents. Returns null when not a number. */
    fun parse(text: String): Long? {
        val cleaned = text.trim()
            .removePrefix("-")
            .replace(Regex("(?i)^(RM|MYR)\\s*"), "")
            .replace(",", "")
            .trim()
        if (cleaned.isEmpty() || !cleaned.matches(Regex("\\d+(\\.\\d+)?"))) return null
        val cents = BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()
        return if (text.trim().startsWith("-")) -cents else cents
    }

    fun fromDecimal(v: BigDecimal): Long = v.setScale(2, RoundingMode.HALF_UP).movePointRight(2).toLong()

    /** For text fields: 650 → "6.50", 0 → "". */
    fun toInput(cents: Long): String = if (cents == 0L) "" else BigDecimal(cents).movePointLeft(2).toPlainString()
}
