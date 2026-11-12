package io.github.steven_stu77.lazee.core.logic

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Simple interest (spec §5.9): principal × annual rate × days / 365, rounded to the cent. */
object LoanMath {
    fun interest(principal: Long, annualRatePercent: Double?, days: Long): Long {
        if (annualRatePercent == null || annualRatePercent <= 0 || days <= 0) return 0
        return Math.round(principal * annualRatePercent / 100.0 * days / 365.0)
    }

    fun interestUntil(principal: Long, rate: Double?, start: LocalDate, asOf: LocalDate): Long =
        interest(principal, rate, ChronoUnit.DAYS.between(start, asOf))

    /** What is still owed: principal + interest so far − repaid, never below zero. */
    fun outstanding(principal: Long, rate: Double?, start: LocalDate, repaid: Long, asOf: LocalDate): Long =
        (principal + interestUntil(principal, rate, start, asOf) - repaid).coerceAtLeast(0)

    data class Part(val interest: Long, val principal: Long)

    /**
     * Replays repayments in date order. Each one first pays interest accrued (and not yet paid)
     * up to its date; the rest reduces principal.
     */
    fun allocate(principal: Long, rate: Double?, start: LocalDate, repayments: List<Pair<LocalDate, Long>>): List<Part> {
        var interestPaid = 0L
        return repayments.sortedBy { it.first }.map { (date, amount) ->
            val due = (interestUntil(principal, rate, start, date) - interestPaid).coerceAtLeast(0)
            val toInterest = minOf(amount, due)
            interestPaid += toInterest
            Part(toInterest, amount - toInterest)
        }
    }
}
