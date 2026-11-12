package io.github.steven_stu77.lazee.core.logic

import java.time.LocalDate
import java.time.YearMonth

/** Credit-card statement maths. */
object CardMath {
    /** The most recent statement date on or before [today]. */
    fun lastStatementDate(statementDay: Int, today: LocalDate): LocalDate {
        val thisMonth = BillMath.dueDate(statementDay, YearMonth.from(today))
        return if (!today.isBefore(thisMonth)) thisMonth else BillMath.dueDate(statementDay, YearMonth.from(today).minusMonths(1))
    }

    /** Amount due this period: what was owed at the last statement date, minus payments made since. */
    fun amountDue(accountId: Long, opening: Long, entries: List<Entry>, statementDay: Int, today: LocalDate): Long {
        val cutoff = lastStatementDate(statementDay, today)
        val owedAtStatement = -LedgerMath.balance(accountId, opening, entries.filter { !it.date.isAfter(cutoff) })
        val paidSince = entries.filter { it.date.isAfter(cutoff) && it.type == Entry.Kind.TRANSFER && it.toAccountId == accountId }.sumOf { it.amount }
        return (owedAtStatement - paidSince).coerceAtLeast(0)
    }
}
