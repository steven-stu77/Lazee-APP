package io.github.steven_stu77.lazee.core.logic

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** A transaction reduced to what the maths needs. Kept Android-free for unit tests. */
data class Entry(
    val id: Long,
    val type: Kind,
    val amount: Long,
    val date: LocalDate,
    val accountId: Long,
    val toAccountId: Long? = null,
    val categoryId: Long? = null,
) {
    enum class Kind { EXPENSE, INCOME, TRANSFER }
}

object LedgerMath {
    /** Balance = opening + income − expense − transfers out + transfers in. Refunds are negative expenses. */
    fun balance(accountId: Long, opening: Long, entries: List<Entry>): Long =
        entries.fold(opening) { acc, e ->
            when {
                e.type == Entry.Kind.INCOME && e.accountId == accountId -> acc + e.amount
                e.type == Entry.Kind.EXPENSE && e.accountId == accountId -> acc - e.amount
                e.type == Entry.Kind.TRANSFER && e.accountId == accountId -> acc - e.amount
                e.type == Entry.Kind.TRANSFER && e.toAccountId == accountId -> acc + e.amount
                else -> acc
            }
        }

    fun totalExpense(entries: List<Entry>) = entries.filter { it.type == Entry.Kind.EXPENSE }.sumOf { it.amount }
    fun totalIncome(entries: List<Entry>) = entries.filter { it.type == Entry.Kind.INCOME }.sumOf { it.amount }

    /** Monday–Sunday of the week containing [today]; true where something was logged. */
    fun weekDots(today: LocalDate, loggedDays: Set<LocalDate>): List<Pair<LocalDate, Boolean>> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 7L).map { monday.plusDays(it) }.map { it to (it in loggedDays) }
    }

    /** Groups newest-first by day; the total is that day's spending. */
    fun <T> groupByDay(items: List<T>, date: (T) -> LocalDate, spend: (T) -> Long): List<DayGroup<T>> =
        items.groupBy(date).toSortedMap(compareByDescending { it })
            .map { (d, list) -> DayGroup(d, list, list.sumOf(spend)) }

    data class DayGroup<T>(val date: LocalDate, val items: List<T>, val spent: Long)
}
